package io.github.koppor.koppter.todotxt

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * A filter expression in the syntax of topydo: terms separated by blanks, all of which a task has to match.
 *
 * * `+project`, `@context`: the task has this project or context.
 * * `(A)`, `(>B)`, `(<=C)`: the task has this priority, a higher one, or this or a lower one.
 *   A task without priority matches none of them.
 * * `due:today`, `due:<1w`, `t:>=2026-12-01`: the tag has this value, or compares so to it.
 *   Dates are ISO dates, `today`, `tomorrow`, `yesterday`, or an interval from today such as `3d` or `1w`.
 *   A task without the tag does not match.
 * * `-term`: the task does not match the term.
 * * Any other word: the line contains it, ignoring case.
 */
// [impl->dsn~query~1]
class Query private constructor(private val terms: List<(Task, LocalDate) -> Boolean>) {
    fun matches(task: Task, today: LocalDate): Boolean = terms.all { it(task, today) }

    companion object {
        private val PRIORITY = Regex("^\\((<=|>=|<|>|=)?([A-Z])\\)$")
        private val TAG = Regex("^([^\\s:]+):(<=|>=|<|>|=)?(\\S+)$")

        fun parse(expression: String): Query = Query(expression.split(Regex("\\s+")).filter(String::isNotEmpty).map(::term))

        private fun term(word: String): (Task, LocalDate) -> Boolean {
            if (word.length > 1 && word.startsWith("-")) {
                val negated = term(word.substring(1))
                return { task, today -> !negated(task, today) }
            }
            if (word.length > 1 && word.startsWith("+")) return { task, _ -> word.substring(1) in task.projects }
            if (word.length > 1 && word.startsWith("@")) return { task, _ -> word.substring(1) in task.contexts }
            PRIORITY.matchEntire(word)?.let { match ->
                val (operator, letter) = match.destructured
                // A is the highest priority, so the order of the letters is reversed.
                return { task, _ -> task.priority?.let { compare(letter.single().compareTo(it), operator) } ?: false }
            }
            TAG.matchEntire(word)?.let { match ->
                val (key, operator, value) = match.destructured
                return { task, today ->
                    val actual = task.tag(key)
                    val date = date(value, today)
                    val actualDate = actual?.let(Task::parseDate)
                    when {
                        actual == null -> false
                        date != null && actualDate != null -> compare(actualDate.compareTo(date), operator)
                        else -> compare(actual.compareTo(value), operator)
                    }
                }
            }
            return { task, _ -> task.toLine().contains(word, ignoreCase = true) }
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
            else -> Task.parseDate(value) ?: Recurrence.parse(value)?.next(today)
        }
    }
}
