package io.github.koppor.koppter.todotxt

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * The value of a `rec:` tag: an interval such as `1m` or `+2w`.
 *
 * A [strict] recurrence (leading `+`) counts from the previous due date, a plain one from the day of completion.
 */
// [impl->dsn~recurrence-syntax~1]
data class Recurrence(val amount: Int, val unit: Unit, val strict: Boolean = false) {
    enum class Unit(val letter: Char) { DAY('d'), BUSINESS_DAY('b'), WEEK('w'), MONTH('m'), YEAR('y') }

    fun next(from: LocalDate): LocalDate = when (unit) {
        Unit.DAY -> from.plus(DatePeriod(days = amount))
        Unit.WEEK -> from.plus(DatePeriod(days = 7 * amount))
        Unit.MONTH -> from.plus(DatePeriod(months = amount))
        Unit.YEAR -> from.plus(DatePeriod(years = amount))
        Unit.BUSINESS_DAY -> {
            var date = from
            repeat(amount) {
                do date = date.plus(DatePeriod(days = 1)) while (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY)
            }
            date
        }
    }

    override fun toString(): String = (if (strict) "+" else "") + amount + unit.letter

    companion object {
        private val PATTERN = Regex("^(\\+?)(\\d+)([dbwmy])$")

        fun parse(text: String): Recurrence? {
            val match = PATTERN.matchEntire(text) ?: return null
            val amount = match.groupValues[2].toIntOrNull()?.takeIf { it > 0 } ?: return null
            val unit = Unit.entries.first { it.letter == match.groupValues[3].single() }
            return Recurrence(amount, unit, strict = match.groupValues[1] == "+")
        }
    }
}
