package io.github.koppor.koppter.todotxt

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

/**
 * One entry of an `ice_recur.txt` file: a schedule and the task it adds on the days of the schedule.
 *
 * The format is that of the `ice_recur` add-on: `[@start] [@@end] timing - task`, where the timing is
 * `daily`, `weekly`, `monthly` or `yearly` with an optional interval, followed by specifiers separated by ` ; `:
 * `day`, `day_of_month`, `day_of_week`, `day_of_year` and `month_of_year`.
 */
// [impl->dsn~ice-recur-schedule~1]
data class IceRecurEntry(
    val task: String,
    val start: LocalDate,
    val end: LocalDate?,
    val frequency: Frequency,
    val interval: Int,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val daysOfMonth: Set<Int> = emptySet(),
    val weekdayOrdinals: Map<DayOfWeek, Set<Int>> = emptyMap(),
    val daysOfYear: Set<Int> = emptySet(),
    val months: Set<Int> = emptySet(),
) {
    enum class Frequency { DAILY, WEEKLY, MONTHLY, YEARLY }

    fun occursOn(date: LocalDate): Boolean {
        if (date < start || (end != null && date > end)) return false
        return when (frequency) {
            Frequency.DAILY -> start.daysUntil(date) % interval == 0
            Frequency.WEEKLY ->
                (weekStart(start).daysUntil(weekStart(date)) / 7) % interval == 0 &&
                    (if (weekdays.isEmpty()) date.dayOfWeek == start.dayOfWeek else date.dayOfWeek in weekdays)
            Frequency.MONTHLY ->
                ((date.year - start.year) * 12 + (date.month.number - start.month.number)) % interval == 0 && when {
                    daysOfMonth.isNotEmpty() -> daysOfMonth.any { dayMatches(it, date.day, daysIn(date)) }
                    weekdayOrdinals.isNotEmpty() -> weekdayOrdinals[date.dayOfWeek].orEmpty().any {
                        dayMatches(it, (date.day - 1) / 7 + 1, (date.day - 1) / 7 + 1 + (daysIn(date) - date.day) / 7)
                    }
                    else -> date.day == start.day
                }
            Frequency.YEARLY ->
                (date.year - start.year) % interval == 0 && when {
                    daysOfYear.isNotEmpty() -> daysOfYear.any { dayMatches(it, date.dayOfYear, if (isLeap(date.year)) 366 else 365) }
                    months.isNotEmpty() -> date.month.number in months && date.day == start.day
                    else -> date.month == start.month && date.day == start.day
                }
        }
    }

    companion object {
        /** The start the add-on assumes for an entry without one. */
        private val DEFAULT_START = LocalDate(2018, 1, 1)
        private val DATES = Regex("^(?:@(\\d{4}-\\d{2}-\\d{2})\\s+)?(?:@@(\\d{4}-\\d{2}-\\d{2})\\s+)?(.*)$")

        /** Parses one line; `null` for a blank line, a comment or a line that is not understood. */
        fun parse(line: String): IceRecurEntry? {
            if (line.isBlank() || line.trimStart().startsWith("#")) return null
            val separator = line.indexOf(" - ")
            if (separator < 0) return null
            val task = line.substring(separator + 3).trim()
            val match = DATES.matchEntire(line.substring(0, separator).trim()) ?: return null
            val start = match.groupValues[1].takeIf { it.isNotEmpty() }?.let { Task.parseDate(it) ?: return null } ?: DEFAULT_START
            val end = match.groupValues[2].takeIf { it.isNotEmpty() }?.let { Task.parseDate(it) ?: return null }
            val specifiers = match.groupValues[3].split(';').map { it.trim() }.filter { it.isNotEmpty() }
            val head = specifiers.firstOrNull()?.split(Regex("\\s+")) ?: return null
            val frequency = Frequency.entries.firstOrNull { it.name.equals(head[0], ignoreCase = true) } ?: return null
            val interval = if (head.size > 1) head[1].toIntOrNull()?.takeIf { it > 0 } ?: return null else 1
            if (task.isEmpty() || head.size > 2) return null

            var entry = IceRecurEntry(task, start, end, frequency, interval)
            for (specifier in specifiers.drop(1)) {
                val name = specifier.substringBefore(' ')
                val argument = specifier.substringAfter(' ', "").trim()
                entry = when (name) {
                    "day" -> entry.copy(weekdays = entry.weekdays + (list(argument, ::weekday) ?: return null))
                    "day_of_month" -> entry.copy(daysOfMonth = entry.daysOfMonth + (list(argument, String::toIntOrNull) ?: return null))
                    "day_of_year" -> entry.copy(daysOfYear = entry.daysOfYear + (list(argument, String::toIntOrNull) ?: return null))
                    "month_of_year" -> entry.copy(months = entry.months + (list(argument, ::month) ?: return null))
                    "day_of_week" -> {
                        val day = weekday(argument.substringBefore("=>").trim()) ?: return null
                        val ordinals = list(argument.substringAfter("=>", ""), String::toIntOrNull) ?: return null
                        entry.copy(weekdayOrdinals = entry.weekdayOrdinals + (day to ordinals))
                    }
                    else -> return null
                }
            }
            return entry
        }

        private fun <T : Any> list(text: String, parse: (String) -> T?): Set<T>? {
            val items = text.split(',').map { it.trim() }
            return items.map { parse(it) ?: return null }.toSet().takeIf { it.isNotEmpty() }
        }

        // The add-on counts weekdays from Sunday = 0.
        private fun weekday(text: String): DayOfWeek? {
            val name = text.trim().removePrefix(":")
            name.toIntOrNull()?.let { return if (it in 0..6) DayOfWeek.entries[(it + 6) % 7] else null }
            return DayOfWeek.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }

        private fun month(text: String): Int? {
            val name = text.trim().removePrefix(":")
            name.toIntOrNull()?.let { return it.takeIf { it in 1..12 } }
            return Month.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }?.number
        }

        /** [wanted] counts from the start, or from the end if negative: -1 is the last. */
        private fun dayMatches(wanted: Int, fromStart: Int, total: Int) = if (wanted > 0) wanted == fromStart else wanted == fromStart - total - 1

        private fun weekStart(date: LocalDate) = date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber % 7))

        private fun daysIn(date: LocalDate): Int {
            val first = LocalDate(date.year, date.month, 1)
            return first.daysUntil(first.plus(DatePeriod(months = 1)))
        }

        private fun isLeap(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }
}

// What changes in the life of a task: the add-on leaves it out when it looks for a task that exists already.
private val CHANGING_TAG = Regex("^(?:(?:t|due):\\d{4}-\\d{2}-\\d{2}|note:\\S+)$")

private fun comparableText(task: Task) = task.body.split(' ').filter { it.isNotEmpty() && !CHANGING_TAG.matches(it) }.joinToString(" ")

/**
 * The tasks that the schedules add on the given days, each with the day it is added for:
 * those of the entries that occur on a day, unless an open task with the same text exists already.
 *
 * As in the add-on, two tasks have the same text if they differ at most in priority, dates,
 * and the tags `t:`, `due:` and `note:`. Several days are given after days on which the schedules were not run.
 */
// [impl->dsn~ice-recur-run~1]
fun iceRecurTasks(schedule: String, days: ClosedRange<LocalDate>, existing: List<Task>): List<Pair<LocalDate, Task>> {
    val entries = schedule.lines().mapNotNull(IceRecurEntry::parse)
    val present = existing.filterNot { it.completed }.map(::comparableText).toMutableSet()
    val added = mutableListOf<Pair<LocalDate, Task>>()
    var day = days.start
    while (day <= days.endInclusive) {
        for (entry in entries) {
            val task = Task.parse(entry.task)
            if (entry.occursOn(day) && present.add(comparableText(task))) added += day to task
        }
        day = day.plus(DatePeriod(days = 1))
    }
    return added
}
