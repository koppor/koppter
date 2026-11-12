package io.github.koppor.koppter.plan

import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.isWithin
import io.github.koppor.koppter.todotxt.listOrder
import io.github.koppor.koppter.todotxt.withAncestors
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.minutes

/**
 * What the user configured for planning.
 *
 * @param contextMaps the time map of a context, which holds for the contexts below it unless they name their own
 * @param barriers the date behind which no task falls: `default`, `+project` or `@context` to `week`, `month`, `quarter`, `year` or a date
 */
// [impl->dsn~plan-configuration~1]
class PlanConfig(
    val maps: TimeMaps = TimeMaps(emptyList()),
    private val contextMaps: Map<String, String> = emptyMap(),
    private val barriers: List<Pair<String, String>> = emptyList(),
) {
    /** What was not understood, as texts for the user. */
    val problems: List<String>
        get() = maps.problems +
            contextMaps.filterValues { it !in maps }.map { (context, map) -> "@${context.removePrefix("@")}: there is no time map \"$map\"" } +
            barriers.filter { (_, spec) -> barrierDate(spec, LocalDate(2000, 1, 1)) == null }.map { (key, spec) -> "$key: \"$spec\" is no barrier" }

    /** The time map of the first context of the task that has one, the nearest context above it included. */
    fun mapOf(task: Task): String? = task.contexts.firstNotNullOfOrNull { context ->
        context.withAncestors().reversed().firstNotNullOfOrNull { contextMaps[it] ?: contextMaps["@$it"] }
    }

    /** The last day on which the task may be planned: the earliest barrier of its projects and contexts, else the default one. */
    fun barrierOf(task: Task, today: LocalDate): LocalDate {
        val own = barriers.filter { (key, _) ->
            when {
                key.startsWith("+") -> key.substring(1) in task.projects
                key.startsWith("@") -> task.contexts.any { it.isWithin(key.substring(1)) }
                else -> false
            }
        }.mapNotNull { (_, spec) -> barrierDate(spec, today) }
        return own.minOrNull() ?: barrierDate(barriers.firstOrNull { it.first == DEFAULT }?.second ?: "quarter", today) ?: barrierDate("quarter", today)!!
    }

    companion object {
        const val DEFAULT = "default"

        /** The date a barrier names on the given day; `null` if the text names none. */
        fun barrierDate(spec: String, today: LocalDate): LocalDate? = when (spec.trim().lowercase()) {
            "week" -> today.plus(DatePeriod(days = 7 - today.dayOfWeek.isoDayNumber))
            "month" -> endOfMonth(today.year, today.month.ordinal + 1)
            "quarter" -> endOfMonth(today.year, (today.month.ordinal / 3) * 3 + 3)
            "year" -> LocalDate(today.year, 12, 31)
            else -> Task.parseDate(spec.trim())
        }

        private fun endOfMonth(year: Int, month: Int) = LocalDate(year, month, 1).plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
    }
}

/** A time in which a task is to be worked on. */
data class Block(val date: LocalDate, val span: Span, val entry: Entry)

/** @param unplaced the tasks that do not fit before their barrier */
data class Plan(val blocks: List<Block>, val unplaced: List<Entry>)

/** The duration of a task that states none. */
// ponytail: a constant; a setting when half an hour does not fit the tasks without duration.
val DEFAULT_DURATION = 30.minutes

/** A task is not cut into parts shorter than this, unless what is left of it is shorter. */
private const val SHORTEST_PART = 30

/**
 * Plans the open tasks: each gets the earliest free times of its time map, in the order of the task list,
 * so that a task with a higher priority is planned before the others.
 *
 * A task is not planned before its threshold date, and not behind its barrier: if its duration does not fit before, it is not planned at all.
 * A task is cut into parts if a free time is too short for it.
 *
 * @param busy the times that are taken, by day
 * @param now the minutes of today that have passed
 */
// [impl->dsn~planner~1]
fun plan(entries: List<Entry>, config: PlanConfig, busy: Map<LocalDate, List<Span>>, today: LocalDate, now: Int): Plan {
    val taken = busy.mapValuesTo(mutableMapOf()) { it.value.toMutableList() }
    taken.getOrPut(today) { mutableListOf() } += Span(0, now)
    val blocks = mutableListOf<Block>()
    val unplaced = mutableListOf<Entry>()
    for (entry in entries.filterNot { it.task.completed }.sortedWith(listOrder)) {
        val task = entry.task
        var left = (task.duration ?: DEFAULT_DURATION).inWholeMinutes.toInt()
        val parts = mutableListOf<Block>()
        var date = maxOf(today, task.threshold ?: today)
        val barrier = config.barrierOf(task, today)
        val map = config.mapOf(task)
        while (left > 0 && date <= barrier) {
            for (free in config.maps.spans(map, date).without(taken[date].orEmpty())) {
                if (left == 0) break
                if (free.length < minOf(SHORTEST_PART, left)) continue
                val part = Span(free.start, free.start + minOf(free.length, left))
                parts += Block(date, part, entry)
                left -= part.length
            }
            date = date.plus(DatePeriod(days = 1))
        }
        if (left > 0) {
            unplaced += entry
        } else {
            blocks += parts
            parts.forEach { taken.getOrPut(it.date) { mutableListOf() } += it.span }
        }
    }
    return Plan(blocks.sortedWith(compareBy<Block> { it.date }.thenBy { it.span.start }), unplaced)
}
