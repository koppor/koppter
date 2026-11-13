package io.github.koppor.koppter.todotxt

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Dates typed as words at the end of the text of a task, such as `next week` or `by Wed 14:00`.
 * The phrases are those that SkedPal reads in the title of a task.
 */
// [impl->dsn~quick-add-dates~1]
internal object DatePhrase {
    private class Plan(val start: LocalDate? = null, val due: LocalDate? = null, val at: String? = null, val by: String? = null)

    private val WEEKDAYS = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
    private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
    private val NUMBERS = listOf("a", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")

    private const val WEEKDAY = "mon(?:day)?|tue(?:s|sday)?|wed(?:nesday)?|thu(?:r|rs|rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?"
    private const val MONTH = "jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?|aug(?:ust)?|sep(?:t|tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?"
    private const val NUMBER = "\\d{1,2}(?:st|nd|rd|th)?"
    private const val DAY = "(today|tomorrow|$WEEKDAY|(?:$MONTH) $NUMBER|$NUMBER (?:$MONTH)|\\d{4}-\\d{2}-\\d{2})"
    private const val TIME = "(\\d{1,2}(?::\\d{2})? ?[ap]m|\\d{1,2}:\\d{2})"
    private val TIME_PARTS = Regex("(\\d{1,2})(?::(\\d{2}))? ?([ap]m)?")

    /** A project, context, hashtag or tag: such words may follow the phrase. */
    private val META = Regex("^(?:[@+#]\\S+|[A-Za-z][^:\\s]*:\\S+)$")

    // The text before the phrase is as short as possible, so that the phrase is as long as possible.
    private fun rule(phrase: String, plan: (List<String>, LocalDate) -> Plan?) = Regex("^(.+?) (?:$phrase)$", RegexOption.IGNORE_CASE) to plan

    /** The first rule whose phrase ends the text decides. */
    private val RULES = listOf(
        rule("from $DAY to $DAY") { (from, to), today -> day(from, today)?.let { Plan(it, day(to, it)) } },
        rule("the week of $DAY") { (of), today -> day(of, today)?.let(::week) },
        rule("(?:delay until|no earlier than|start(?:ing)? on|starting|from) $DAY") { (start), today -> Plan(start = day(start, today)) },
        rule("(?:(?:start|begin) )?in (\\d+|an?|${NUMBERS.drop(1).joinToString("|")}) (day|week|month)s?") { (amount, unit), today ->
            val count = amount.toIntOrNull() ?: maxOf(1, NUMBERS.indexOf(amount.lowercase()))
            Plan(start = today.plus(count, if (unit.lowercase() == "month") DateTimeUnit.MONTH else if (unit.lowercase() == "week") DateTimeUnit.WEEK else DateTimeUnit.DAY))
        },
        rule("(?:due by|due|by) $DAY(?: at)? $TIME") { (due, by), today -> Plan(due = day(due, today), by = time(by)) },
        rule("(?:due by|due|by) $DAY") { (due), today -> Plan(due = day(due, today)) },
        rule("(?:on )?$DAY(?: at)? $TIME") { (on, at), today -> Plan(due = day(on, today), at = time(at)) },
        rule("at $TIME") { (at), _ -> Plan(at = time(at)) },
        rule("on $DAY") { (on), today -> day(on, today).let { Plan(it, it) } },
        rule("(this|next) week") { (which), today -> if (which.lowercase() == "next") week(today.plus(DatePeriod(days = 7))) else Plan(due = week(today).due) },
        rule("(this|next) month") { (which), today ->
            val first = LocalDate(today.year, today.month, 1).plus(DatePeriod(months = if (which.lowercase() == "next") 1 else 0))
            Plan(first.takeIf { it > today }, first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)))
        },
        rule("(?:this )?weekend") { _, today -> week(today).let { Plan(it.due?.minus(DatePeriod(days = 1)), it.due) } },
        // As in SkedPal, a weekday alone names the day of the task, and another day alone the day it is due.
        rule(DAY) { (on), today -> day(on, today).let { if (WEEKDAYS.any(on.lowercase()::startsWith)) Plan(it, it) else Plan(due = it) } },
    )

    /** The task with a phrase at the end of its text replaced by tags; a tag that the task has stays. */
    fun apply(task: Task, today: LocalDate): Task {
        val words = task.body.split(' ')
        val trailing = words.takeLastWhile(META::matches)
        val text = words.dropLast(trailing.size).joinToString(" ")
        val (match, rule) = RULES.firstNotNullOfOrNull { (regex, plan) -> regex.matchEntire(text)?.let { it to plan } } ?: return task
        val plan = rule(match.groupValues.drop(2), today)?.takeIf { it.start != null || it.due != null || it.at != null } ?: return task
        val tags = listOf(Task.THRESHOLD to plan.start?.toString(), Task.DUE to plan.due?.toString(), Task.AT to plan.at, Task.BY to plan.by)
        return tags.fold(task.copy(body = (listOf(match.groupValues[1]) + trailing).joinToString(" "))) { result, (key, value) ->
            if (value == null || result.tag(key) != null) result else result.withTag(key, value)
        }
    }

    /** The day that the words name, which is [from] or the first such day after it; `null` if there is no such day, as for 30 February. */
    private fun day(words: String, from: LocalDate): LocalDate? {
        val text = words.lowercase()
        Task.parseDate(text)?.let { return it }
        if (text == "today") return from
        if (text == "tomorrow") return from.plus(DatePeriod(days = 1))
        WEEKDAYS.indexOfFirst(text::startsWith).takeIf { it >= 0 }?.let { return from.plus(DatePeriod(days = (it + 8 - from.dayOfWeek.isoDayNumber) % 7)) }
        val month = (MONTHS.indexOfFirst { it in text } + 1).toString().padStart(2, '0')
        val number = text.filter(Char::isDigit).padStart(2, '0')
        return listOf(from.year, from.year + 1).firstNotNullOfOrNull { year -> Task.parseDate("$year-$month-$number")?.takeIf { it >= from } }
    }

    private fun week(date: LocalDate): Plan = date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1)).let { Plan(it, it.plus(DatePeriod(days = 6))) }

    /** The time as the tags hold it: `3pm` is `15:00`. */
    private fun time(words: String): String? {
        val (hours, minutes, half) = TIME_PARTS.matchEntire(words.lowercase())?.destructured ?: return null
        val hour = hours.toInt().let { if (half.isEmpty()) it else it % 12 + if (half == "pm") 12 else 0 }
        return "$hour:${minutes.ifEmpty { "00" }}".takeIf { Task.parseTime(it) != null }
    }
}
