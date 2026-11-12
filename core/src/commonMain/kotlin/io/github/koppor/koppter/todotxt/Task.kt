package io.github.koppor.koppter.todotxt

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlin.time.Duration

/**
 * One task line of a todo.txt file.
 *
 * [body] is everything after the completion marker, priority and dates, kept verbatim.
 * Description, projects, contexts, hashtags and tags are views on it, so that a change of the
 * header (completing, setting the priority) leaves the user's wording untouched.
 */
// [impl->dsn~task-model~1]
data class Task(
    val completed: Boolean = false,
    val priority: Char? = null,
    val completionDate: LocalDate? = null,
    val creationDate: LocalDate? = null,
    val body: String,
) {
    private val tokens: List<String> get() = body.split(' ').filter { it.isNotEmpty() }

    /** Project names without the leading `+`, in order of appearance. */
    val projects: List<String> get() = tokens.filter(::isProject).map { it.substring(1) }.distinct()

    /** Context names without the leading `@`, in order of appearance. */
    val contexts: List<String> get() = tokens.filter(::isContext).map { it.substring(1) }.distinct()

    /**
     * Hashtag names without the leading `#`, in order of appearance.
     * A hashtag starts with a letter, so that a number such as `#123` stays a word of the description.
     */
    // [impl->dsn~hashtags~1]
    val hashtags: List<String> get() = tokens.filter(::isHashtag).map { it.substring(1) }.distinct()

    /** The `key:value` tags in order of appearance; a key may occur more than once. */
    val tags: List<Pair<String, String>>
        get() = tokens.filter(::isTag).map { it.substringBefore(':') to it.substringAfter(':') }

    /** The body without projects, contexts, hashtags and tags. */
    val description: String get() = tokens.filterNot(::isMetadata).joinToString(" ")

    /** The value of the first tag with the given key. */
    fun tag(key: String): String? = tags.firstOrNull { it.first == key }?.second

    /** Replaces links that were copied from ContextSwitcher by `cs:` tags behind the other metadata. */
    // [impl->dsn~context-switcher-tag~1]
    fun withContextSwitcherTags(): Task {
        val ids = tokens.mapNotNull { CONTEXT_SWITCHER_LINK.matchEntire(it)?.groupValues?.get(1) }
        if (ids.isEmpty()) return this
        return copy(body = (tokens.filterNot(CONTEXT_SWITCHER_LINK::matches) + ids.map { "$CONTEXT_SWITCHER:$it" }).joinToString(" "))
    }

    val due: LocalDate? get() = tag(DUE)?.let(::parseDate)

    /** The threshold date: the task is not relevant before it. */
    val threshold: LocalDate? get() = tag(THRESHOLD)?.let(::parseDate)

    val recurrence: Recurrence? get() = tag(RECURRENCE)?.let(Recurrence::parse)

    /** The time the task is expected to take. */
    val duration: Duration? get() = tag(DURATION)?.let(::parseDuration)

    /** The minutes of the day at which the task takes place, for a task with a fixed time. */
    // [impl->dsn~fixed-time~1]
    val at: Int? get() = tag(AT)?.let(::parseTime)

    /** The minutes of the due date by which the task is due, for a task that is due at a time of that day. */
    // [impl->dsn~due-time~1]
    val by: Int? get() = tag(BY)?.let(::parseTime)

    // [impl->dsn~task-serializer~1]
    fun toLine(): String = buildString {
        if (completed) append("x ")
        if (priority != null) append('(').append(priority).append(") ")
        if (completionDate != null) append(completionDate).append(' ')
        if (creationDate != null) append(creationDate).append(' ')
        append(body)
    }

    /**
     * Marks the task as completed.
     * The priority moves to a `pri:` tag, as other todo.txt tools do, so that [reopen] restores it.
     */
    // [impl->dsn~completion-dates~1]
    fun complete(today: LocalDate, writeDate: Boolean): Task {
        if (completed) return this
        return copy(
            completed = true,
            priority = null,
            // A creation date without a completion date in front of it would be read as the completion date.
            completionDate = if (writeDate || creationDate != null) today else null,
            body = if (priority == null) body else "$body $PRIORITY:$priority",
        )
    }

    fun reopen(): Task {
        if (!completed) return this
        val kept = tag(PRIORITY)?.singleOrNull()?.takeIf { it in 'A'..'Z' }
        return copy(
            completed = false,
            priority = kept,
            completionDate = null,
            body = if (kept == null) body else tokens.filterNot { it == "$PRIORITY:$kept" }.joinToString(" "),
        )
    }

    /**
     * Replaces description and metadata.
     * The body is rebuilt in a fixed order: description, projects, contexts, hashtags, tags.
     */
    // [impl->dsn~canonical-order-on-edit~1]
    fun edit(
        description: String = this.description,
        priority: Char? = this.priority,
        projects: List<String> = this.projects,
        contexts: List<String> = this.contexts,
        tags: List<Pair<String, String>> = this.tags,
        hashtags: List<String> = this.hashtags,
    ): Task {
        if (description == this.description && priority == this.priority && projects == this.projects &&
            contexts == this.contexts && tags == this.tags && hashtags == this.hashtags
        ) {
            return this
        }
        val parts = buildList {
            add(description.trim().replace(WHITESPACE, " "))
            projects.distinct().forEach { add("+$it") }
            contexts.distinct().forEach { add("@$it") }
            hashtags.distinct().forEach { add("#$it") }
            tags.forEach { (key, value) -> add("$key:$value") }
        }
        return copy(priority = priority, body = parts.filter { it.isNotEmpty() }.joinToString(" "))
    }

    /**
     * Sets the tag; `null` or blank removes it.
     * An existing tag keeps its position, so that setting a tag to the value it has changes nothing.
     */
    fun withTag(key: String, value: String?): Task {
        val index = tags.indexOfFirst { it.first == key }
        val others = tags.filterIndexed { i, tag -> tag.first != key || i == index }
        return edit(
            tags = when {
                value.isNullOrBlank() -> others.filterNot { it.first == key }
                index < 0 -> others + (key to value)
                else -> others.map { if (it.first == key) key to value else it }
            },
        )
    }

    /**
     * The next occurrence of a recurring task, to be added when this one is completed; `null` without a `rec:` tag.
     *
     * A strict recurrence counts from the due date (or, without one, the threshold date) of this task,
     * a plain one from [today]. The distance between threshold and due date is kept.
     * A task without either date gets a due date, as the recurrence would otherwise be invisible.
     */
    // [impl->dsn~recurrence-on-completion~1]
    fun nextOccurrence(today: LocalDate): Task? {
        val recurrence = recurrence ?: return null
        val due = due
        val threshold = threshold
        val from = if (recurrence.strict) due ?: threshold ?: today else today
        val next = recurrence.next(from)
        val open = if (completed) reopen() else this
        return when {
            due != null && threshold != null ->
                open.withTag(DUE, next.toString()).withTag(THRESHOLD, next.minus(threshold.daysUntil(due), DateTimeUnit.DAY).toString())
            due == null && threshold != null -> open.withTag(THRESHOLD, next.toString())
            else -> open.withTag(DUE, next.toString())
        }.copy(creationDate = null, completionDate = null)
    }

    /** The task as it is added to the file: with today as creation date if dates are written. */
    // [impl->dsn~creation-date~1]
    fun created(today: LocalDate, writeDate: Boolean): Task =
        if (writeDate && creationDate == null && !completed) copy(creationDate = today) else this

    companion object {
        const val DUE = "due"
        const val THRESHOLD = "t"
        const val RECURRENCE = "rec"
        const val PRIORITY = "pri"
        const val NOTE = "note"
        const val CONTEXT_SWITCHER = "cs"
        const val DURATION = "dur"
        const val AT = "at"
        const val BY = "by"

        /** As topydo writes dependencies: a task with [ID] waits for the tasks whose [PARENT] has the same value. */
        const val ID = "id"
        const val PARENT = "p"

        private val TIME = Regex("^([01]?\\d|2[0-3]):([0-5]\\d)$")

        /** Reads a time of the day such as `9:30` or `12:00` as the minutes since midnight; `null` if the text is none. */
        fun parseTime(text: String): Int? = TIME.matchEntire(text)?.destructured?.let { (hours, minutes) -> hours.toInt() * 60 + minutes.toInt() }

        /** Reads a duration such as `45m`, `2h` or `1h30m`; `null` if the text is none or not positive. */
        // [impl->dsn~duration~1]
        fun parseDuration(text: String): Duration? = Duration.parseOrNull(text)?.takeIf { it.isPositive() && it.isFinite() }

        /**
         * The link that makes ContextSwitcher switch to the task that a tag `cs:` names.
         * The tag holds the id as a link of ContextSwitcher has it, so the link needs no encoding.
         */
        // [impl->dsn~context-switcher-tag~1]
        fun contextSwitcherLink(id: String) = "contextswitcher://switch/$id"

        /** The value of the tag `cs:` for what the user entered: the id itself or a link of ContextSwitcher. */
        // [impl->dsn~context-switcher-tag~1]
        fun contextSwitcherId(input: String): String =
            input.trim().let { CONTEXT_SWITCHER_LINK.matchEntire(it)?.groupValues?.get(1) ?: it }.replace(WHITESPACE, "%20")

        private val CONTEXT_SWITCHER_LINK = Regex("^contextswitcher://(?:task|switch)/(.+)$")

        private val WHITESPACE = Regex("\\s+")
        private val PRIORITY_PREFIX = Regex("^\\(([A-Z])\\) ")
        private val DATE_PREFIX = Regex("^(\\d{4}-\\d{2}-\\d{2}) ")
        private val TAG = Regex("^[^:\\s]+:[^:\\s/][^\\s]*$")

        // [impl->dsn~task-parser~1]
        fun parse(line: String): Task {
            var rest = line.trim()
            val completed = rest.startsWith("x ")
            if (completed) rest = rest.substring(2).trimStart()

            var priority: Char? = null
            PRIORITY_PREFIX.find(rest)?.let {
                priority = it.groupValues[1].single()
                rest = rest.substring(it.value.length).trimStart()
            }

            val dates = mutableListOf<LocalDate>()
            while (dates.size < (if (completed) 2 else 1)) {
                val match = DATE_PREFIX.find(rest) ?: break
                val date = parseDate(match.groupValues[1]) ?: break
                dates += date
                rest = rest.substring(match.value.length).trimStart()
            }

            return Task(
                completed = completed,
                priority = priority,
                completionDate = if (completed) dates.getOrNull(0) else null,
                creationDate = if (completed) dates.getOrNull(1) else dates.getOrNull(0),
                body = rest,
            )
        }

        fun parseDate(text: String): LocalDate? = try {
            LocalDate.parse(text)
        } catch (_: IllegalArgumentException) {
            null
        }

        private fun isProject(token: String) = token.length > 1 && token.startsWith('+')
        private fun isContext(token: String) = token.length > 1 && token.startsWith('@')
        private fun isHashtag(token: String) = token.length > 1 && token.startsWith('#') && token[1].isLetter()

        // "http://example.org" and "12:30" are words, not tags.
        private fun isTag(token: String) = TAG.matches(token) && !token.first().isDigit()
        private fun isMetadata(token: String) = isProject(token) || isContext(token) || isHashtag(token) || isTag(token)
    }
}
