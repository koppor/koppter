package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate

/**
 * One task line of a todo.txt file.
 *
 * [body] is everything after the completion marker, priority and dates, kept verbatim.
 * Description, projects, contexts and tags are views on it, so that a change of the
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

    /** The `key:value` tags in order of appearance; a key may occur more than once. */
    val tags: List<Pair<String, String>>
        get() = tokens.filter(::isTag).map { it.substringBefore(':') to it.substringAfter(':') }

    /** The body without projects, contexts and tags. */
    val description: String get() = tokens.filterNot(::isMetadata).joinToString(" ")

    /** The value of the first tag with the given key. */
    fun tag(key: String): String? = tags.firstOrNull { it.first == key }?.second

    val due: LocalDate? get() = tag(DUE)?.let(::parseDate)

    /** The threshold date: the task is not relevant before it. */
    val threshold: LocalDate? get() = tag(THRESHOLD)?.let(::parseDate)

    val recurrence: Recurrence? get() = tag(RECURRENCE)?.let(Recurrence::parse)

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
     * The priority moves to a `pri:` tag, as other todo.txt tools do, so that [uncomplete] restores it.
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

    fun uncomplete(): Task {
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
     * The body is rebuilt in a fixed order: description, projects, contexts, tags.
     */
    // [impl->dsn~canonical-order-on-edit~1]
    fun edit(
        description: String = this.description,
        priority: Char? = this.priority,
        projects: List<String> = this.projects,
        contexts: List<String> = this.contexts,
        tags: List<Pair<String, String>> = this.tags,
    ): Task {
        if (description == this.description && priority == this.priority && projects == this.projects &&
            contexts == this.contexts && tags == this.tags
        ) {
            return this
        }
        val parts = buildList {
            add(description.trim().replace(WHITESPACE, " "))
            projects.distinct().forEach { add("+$it") }
            contexts.distinct().forEach { add("@$it") }
            tags.forEach { (key, value) -> add("$key:$value") }
        }
        return copy(priority = priority, body = parts.filter { it.isNotEmpty() }.joinToString(" "))
    }

    /** Sets the tag, replacing all tags with the same key; `null` or blank removes it. */
    fun withTag(key: String, value: String?): Task {
        val others = tags.filterNot { it.first == key }
        return edit(tags = if (value.isNullOrBlank()) others else others + (key to value))
    }

    companion object {
        const val DUE = "due"
        const val THRESHOLD = "t"
        const val RECURRENCE = "rec"
        const val PRIORITY = "pri"
        const val NOTE = "note"

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

        // "http://example.org" and "12:30" are words, not tags.
        private fun isTag(token: String) = TAG.matches(token) && !token.first().isDigit()
        private fun isMetadata(token: String) = isProject(token) || isContext(token) || isTag(token)
    }
}
