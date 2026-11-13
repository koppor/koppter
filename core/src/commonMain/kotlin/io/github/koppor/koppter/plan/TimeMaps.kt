package io.github.koppor.koppter.plan

import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

/** A part of a day in minutes from midnight; [end] is exclusive. */
data class Span(val start: Int, val end: Int) {
    val length: Int get() = end - start
}

internal const val MINUTES_PER_DAY = 24 * 60

/** The spans sorted, with overlapping and adjoining ones joined. */
internal fun List<Span>.joined(): List<Span> = filter { it.length > 0 }.sortedBy { it.start }.fold(mutableListOf()) { result, span ->
    val last = result.lastOrNull()
    if (last != null && span.start <= last.end) result[result.lastIndex] = Span(last.start, maxOf(last.end, span.end)) else result += span
    result
}

/** The parts of these spans that lie in none of the [taken] ones. */
internal fun List<Span>.without(taken: List<Span>): List<Span> = taken.joined().fold(joined()) { free, busy ->
    free.flatMap { listOf(Span(it.start, minOf(it.end, busy.start)), Span(maxOf(it.start, busy.end), it.end)) }.filter { it.length > 0 }
}

internal fun List<Span>.within(other: List<Span>): List<Span> = without(listOf(Span(0, MINUTES_PER_DAY)).without(other))

/**
 * The time maps: each names the times of the week in which the tasks that refer to it are planned.
 *
 * A definition is a list of rules separated by commas; a rule is days followed by times, such as `Mon-Fri 08:00-12:00 13:00-17:00`.
 * A rule without days holds on every day, and a rule without times for the whole day.
 * A definition that starts with the name of another map is restricted to the times of that map: `General 08:00-11:00`.
 */
// [impl->dsn~time-maps~1]
class TimeMaps(definitions: List<Pair<String, String>>) {
    private class Rule(val days: Set<Int>, val spans: List<Span>)

    private class Definition(val parent: String?, val rules: List<Rule>)

    /** What was not understood, as texts for the user. */
    val problems = mutableListOf<String>()

    private val maps: Map<String, Definition> = definitions.associate { (name, text) ->
        val names = definitions.map { it.first }.toSet() - name
        val first = text.trim().substringBefore(' ')
        val parent = first.takeIf { it in names }
        val rules = (if (parent == null) text else text.trim().removePrefix(first)).split(',').map(String::trim).filter(String::isNotEmpty).map { rule ->
            val days = mutableSetOf<Int>()
            val spans = mutableListOf<Span>()
            for (token in rule.split(WHITESPACE)) {
                val range = TIMES.matchEntire(token)?.destructured?.let { (h1, m1, h2, m2) -> Span(h1.toInt() * 60 + m1.toInt(), h2.toInt() * 60 + m2.toInt()) }
                val from = DAYS.indexOf(token.take(3).lowercase()) + 1
                val to = DAYS.indexOf(token.substringAfter('-', token).lowercase()) + 1
                when {
                    range != null && range.length > 0 && range.end <= MINUTES_PER_DAY -> spans += range
                    DAY_RANGE.matches(token) && from > 0 && to > 0 -> days += if (from <= to) from..to else (from..7) + (1..to)
                    else -> problems += "$name: \"$token\" is neither days nor times"
                }
            }
            Rule(days.ifEmpty { (1..7).toSet() }, spans.ifEmpty { listOf(Span(0, MINUTES_PER_DAY)) })
        }
        name to Definition(parent, rules.ifEmpty { listOf(Rule((1..7).toSet(), listOf(Span(0, MINUTES_PER_DAY)))) })
    }

    /** The map for tasks whose contexts name none: the first one. */
    val default: String? = definitions.firstOrNull()?.first

    operator fun contains(name: String): Boolean = name in maps

    /** The times of the map on the day; those of the [default] map for `null` or a name that no map has. */
    fun spans(name: String?, date: LocalDate): List<Span> = spans(name?.takeIf { it in maps } ?: default, date.dayOfWeek.isoDayNumber, emptySet())

    private fun spans(name: String?, day: Int, seen: Set<String>): List<Span> {
        // Without any map, the working hours of an office.
        val map = maps[name] ?: return if (day <= 5) listOf(Span(9 * 60, 17 * 60)) else emptyList()
        val own = map.rules.filter { day in it.days }.flatMap { it.spans }.joined()
        // A map that is restricted to itself over other maps is not restricted.
        return if (map.parent == null || map.parent in seen) own else own.within(spans(map.parent, day, seen + name!!))
    }

    private companion object {
        val DAYS = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
        val DAY_RANGE = Regex("^[A-Za-z]{3}(-[A-Za-z]{3})?$")
        val TIMES = Regex("^(\\d{1,2}):(\\d{2})-(\\d{1,2}):(\\d{2})$")
        val WHITESPACE = Regex("\\s+")
    }
}
