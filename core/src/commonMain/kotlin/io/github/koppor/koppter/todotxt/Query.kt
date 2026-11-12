package io.github.koppor.koppter.todotxt

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * A filter expression in the syntax of topydo: terms separated by blanks, all of which a task has to match.
 *
 * * `+project`, `@context`, `#hashtag`: the task has this project, context or hashtag.
 * * `(A)`, `(>B)`, `(<=C)`: the task has this priority, a higher one, or this or a lower one.
 *   A task without priority matches none of them.
 * * `due:today`, `due:<1w`, `t:>=2026-12-01`: the tag has this value, or compares so to it.
 *   Dates are ISO dates, `today`, `tomorrow`, `yesterday`, or an interval from today such as `3d` or `1w`.
 *   A task without the tag does not match.
 * * `-term`: the task does not match the term.
 * * Any other word: the line contains it, ignoring case.
 */
// [impl->dsn~query~1]
class Query private constructor(private val terms: List<Term>) {
    fun matches(task: Task, today: LocalDate): Boolean = terms.all { it.matches(task, today) }

    /**
     * The task with the smallest change that makes it match: each term that it does not match adds or removes
     * a project, a context, a hashtag, the priority or a tag. Nothing else of the task changes.
     *
     * @return `null` if a term cannot be established that way: a word, a negated comparison,
     *   or a comparison of a tag with a value that is not a date
     */
    // [impl->dsn~board-move-to-filter~1]
    fun applyTo(task: Task, today: LocalDate): Task? {
        var result = task
        for (term in terms) {
            if (!term.matches(result, today)) result = term.establish(result, today) ?: return null
        }
        // Terms that contradict each other undo each other.
        return result.takeIf { matches(it, today) }
    }

    private sealed interface Term {
        fun matches(task: Task, today: LocalDate): Boolean

        /** The task changed so that it matches the term; `null` if no change achieves that. */
        fun establish(task: Task, today: LocalDate): Task?

        /** The task changed so that it does not match the term; `null` if no change achieves that. */
        fun revoke(task: Task): Task?
    }

    private class Not(val term: Term) : Term {
        override fun matches(task: Task, today: LocalDate) = !term.matches(task, today)
        override fun establish(task: Task, today: LocalDate) = term.revoke(task)
        override fun revoke(task: Task): Task? = null
    }

    private class Project(val name: String) : Term {
        override fun matches(task: Task, today: LocalDate) = name in task.projects
        override fun establish(task: Task, today: LocalDate) = task.edit(projects = task.projects + name)
        override fun revoke(task: Task) = task.edit(projects = task.projects - name)
    }

    private class Context(val name: String) : Term {
        // [impl->dsn~context-hierarchy~1]
        override fun matches(task: Task, today: LocalDate) = task.contexts.any { it.isWithin(name) }
        override fun establish(task: Task, today: LocalDate) = task.edit(contexts = task.contexts + name)
        override fun revoke(task: Task) = task.edit(contexts = task.contexts.filterNot { it.isWithin(name) })
    }

    private class Hashtag(val name: String) : Term {
        override fun matches(task: Task, today: LocalDate) = name in task.hashtags
        override fun establish(task: Task, today: LocalDate) = task.edit(hashtags = task.hashtags + name)
        override fun revoke(task: Task) = task.edit(hashtags = task.hashtags - name)
    }

    private class Priority(val operator: String, val letter: Char) : Term {
        // A is the highest priority, so the order of the letters is reversed.
        override fun matches(task: Task, today: LocalDate) = task.priority?.let { compare(letter.compareTo(it), operator) } ?: false

        // The priority nearest to the one of the term.
        override fun establish(task: Task, today: LocalDate): Task? = when (operator) {
            ">" -> (letter - 1).takeIf { it >= 'A' }
            "<" -> (letter + 1).takeIf { it <= 'Z' }
            else -> letter
        }?.let { task.edit(priority = it) }

        override fun revoke(task: Task) = task.edit(priority = null)
    }

    private class Tag(val key: String, val operator: String, val value: String) : Term {
        override fun matches(task: Task, today: LocalDate): Boolean {
            val actual = task.tag(key) ?: return false
            val date = date(value, today)
            val actualDate = Task.parseDate(actual)
            return if (date != null && actualDate != null) compare(actualDate.compareTo(date), operator) else compare(actual.compareTo(value), operator)
        }

        // The date nearest to the one of the term; text has no nearest value before or after it.
        override fun establish(task: Task, today: LocalDate): Task? {
            val date = date(value, today)
            return when (operator) {
                "<" -> date?.minus(DatePeriod(days = 1))?.toString()
                ">" -> date?.plus(DatePeriod(days = 1))?.toString()
                else -> date?.toString() ?: value
            }?.let { task.withTag(key, it) }
        }

        // Only for equality: removing the due date to leave "due before today" would lose more than the move says.
        override fun revoke(task: Task) = if (operator.isEmpty() || operator == "=") task.withTag(key, null) else null
    }

    private class Word(val word: String) : Term {
        override fun matches(task: Task, today: LocalDate) = task.toLine().contains(word, ignoreCase = true)
        override fun establish(task: Task, today: LocalDate): Task? = null
        override fun revoke(task: Task): Task? = null
    }

    companion object {
        private val PRIORITY = Regex("^\\((<=|>=|<|>|=)?([A-Z])\\)$")
        private val TAG = Regex("^([^\\s:]+):(<=|>=|<|>|=)?(\\S+)$")

        fun parse(expression: String): Query = Query(expression.split(Regex("\\s+")).filter(String::isNotEmpty).map(::term))

        private fun term(word: String): Term {
            if (word.length > 1) {
                when (word[0]) {
                    '-' -> return Not(term(word.substring(1)))
                    '+' -> return Project(word.substring(1))
                    '@' -> return Context(word.substring(1))
                    // As in a task, "#123" is a word.
                    '#' -> if (word[1].isLetter()) return Hashtag(word.substring(1))
                }
            }
            PRIORITY.matchEntire(word)?.let { return Priority(it.groupValues[1], it.groupValues[2].single()) }
            TAG.matchEntire(word)?.let { return Tag(it.groupValues[1], it.groupValues[2], it.groupValues[3]) }
            return Word(word)
        }

        private fun compare(order: Int, operator: String): Boolean = when (operator) {
            "<" -> order < 0
            "<=" -> order <= 0
            ">" -> order > 0
            ">=" -> order >= 0
            else -> order == 0
        }

        private fun date(value: String, today: LocalDate): LocalDate? = when (value) {
            "today" -> today
            "tomorrow" -> today.plus(DatePeriod(days = 1))
            "yesterday" -> today.minus(DatePeriod(days = 1))
            // An interval has an amount here, so that a value such as the "m" of `size:m` stays text.
            else -> Task.parseDate(value) ?: value.takeIf { it.first().isDigit() }?.let(Recurrence::parse)?.next(today)
        }
    }
}
