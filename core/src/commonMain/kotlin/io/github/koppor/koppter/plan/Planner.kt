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
 * What the user set for the time that is planned for the tasks of a project or a context, or for all tasks with the key `default`.
 * The times are minutes.
 *
 * @param block a task is not cut into parts shorter than this
 * @param maxDay not more than this is planned on a day
 * @param maxWeek not more than this is planned in a week, which starts on Monday
 * @param minWeek this much is planned in a week before the tasks get their times in the order of their priority
 */
class Limit(val key: String, val block: Int? = null, val maxDay: Int? = null, val maxWeek: Int? = null, val minWeek: Int? = null) {
    fun names(task: Task): Boolean = key == PlanConfig.DEFAULT || key.names(task)
}

/** Whether the key, `+project` or `@context`, names a project of the task or a context that a context of the task is within. */
private fun String.names(task: Task): Boolean = when {
    startsWith("+") -> substring(1) in task.projects
    startsWith("@") -> task.contexts.any { it.isWithin(substring(1)) }
    else -> false
}

/**
 * What the user configured for planning.
 *
 * @param contextMaps the time map of a context, which holds for the contexts below it unless they name their own
 * @param barriers the date behind which no task falls: `default`, `+project` or `@context` to `week`, `month`, `quarter`, `year` or a date
 * @param limits `default`, `+project` or `@context` to rules separated by commas: `block 1h`, `max 2h/day`, `max 5h/week`, `min 1h/week`
 */
// [impl->dsn~plan-configuration~1]
class PlanConfig(
    val maps: TimeMaps = TimeMaps(emptyList()),
    private val contextMaps: Map<String, String> = emptyMap(),
    private val barriers: List<Pair<String, String>> = emptyList(),
    limits: List<Pair<String, String>> = emptyList(),
) {
    private val limitProblems = mutableListOf<String>()

    // [impl->dsn~plan-limits~1]
    private val limits: List<Limit> = limits.map { (key, text) ->
        var block: Int? = null
        var maxDay: Int? = null
        var maxWeek: Int? = null
        var minWeek: Int? = null
        for (rule in text.split(',').map(String::trim).filter(String::isNotEmpty)) {
            val (kind, time, period) = RULE.matchEntire(rule.lowercase())?.groupValues?.drop(1) ?: listOf("", "", "")
            val minutes = Task.parseDuration(time)?.inWholeMinutes?.toInt()
            when {
                minutes == null -> limitProblems += "$key: \"$rule\" is no limit"
                kind == "block" && period.isEmpty() -> block = minutes
                kind == "max" && period == "day" -> maxDay = minutes
                kind == "max" && period == "week" -> maxWeek = minutes
                kind == "min" && period == "week" -> minWeek = minutes
                else -> limitProblems += "$key: \"$rule\" is no limit"
            }
        }
        Limit(key, block, maxDay, maxWeek, minWeek)
    }

    /** What was not understood, as texts for the user. */
    val problems: List<String>
        get() = maps.problems +
            contextMaps.filterValues { it !in maps }.map { (context, map) -> "@${context.removePrefix("@")}: there is no time map \"$map\"" } +
            barriers.filter { (_, spec) -> barrierDate(spec, LocalDate(2000, 1, 1)) == null }.map { (key, spec) -> "$key: \"$spec\" is no barrier" } +
            limitProblems

    /** The time map of the first context of the task that has one, the nearest context above it included. */
    fun mapOf(task: Task): String? = task.contexts.firstNotNullOfOrNull { context ->
        context.withAncestors().reversed().firstNotNullOfOrNull { contextMaps[it] ?: contextMaps["@$it"] }
    }

    /** The last day on which the task may be planned: the earliest barrier of its projects and contexts, else the default one. */
    fun barrierOf(task: Task, today: LocalDate): LocalDate {
        val own = barriers.filter { (key, _) -> key.names(task) }.mapNotNull { (_, spec) -> barrierDate(spec, today) }
        return own.minOrNull() ?: barrierDate(barriers.firstOrNull { it.first == DEFAULT }?.second ?: "quarter", today) ?: barrierDate("quarter", today)!!
    }

    /** The shortest part the task is cut into: the longest that its projects and contexts state, else the default one, else an hour. */
    // [impl->dsn~plan-limits~1]
    fun blockOf(task: Task): Int = limits.filter { it.key != DEFAULT && it.names(task) }.mapNotNull { it.block }.maxOrNull()
        ?: limits.firstOrNull { it.key == DEFAULT }?.block ?: DEFAULT_BLOCK

    /** The limits that count the time of the task. */
    fun limitsOf(task: Task): List<Limit> = limits.filter { it.names(task) }

    /** The limits that state a time to plan in each week. */
    val minimums: List<Limit> get() = limits.filter { it.minWeek != null }

    companion object {
        const val DEFAULT = "default"

        /** A task is not cut into parts shorter than this many minutes; a task that is shorter is not cut. */
        const val DEFAULT_BLOCK = 60

        private val RULE = Regex("^(block|max|min)\\s+([^/\\s]+)(?:\\s*/\\s*(day|week))?$")

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

/**
 * A time in which a task is to be worked on.
 *
 * @param travel whether the time is the way to the task or back from it
 */
data class Block(val date: LocalDate, val span: Span, val entry: Entry, val travel: Boolean = false) {
    /** What the plan shows for the block. */
    val text: String get() = (if (travel) "Travel: " else "") + CalendarTxt.text(entry.task)
}

/**
 * @param unplaced the tasks that do not fit before their barrier
 * @param problems what was not understood of the tasks, as texts for the user
 */
data class Plan(val blocks: List<Block>, val unplaced: List<Entry>, val problems: List<String> = emptyList())

/** The duration of a task that states none. */
// ponytail: a constant; a setting when half an hour does not fit the tasks without duration.
val DEFAULT_DURATION = 30.minutes

/** A block does not start between these minutes after a time that is taken by something else than a task. */
private const val GRID = 15

private fun List<Span>.aligned(): List<Span> = map { Span((it.start + GRID - 1) / GRID * GRID, it.end) }.filter { it.length > 0 }

/** The days for which the tasks of schedules are shown in the plan when no task is planned behind them. */
private const val UPCOMING_DAYS = 27

/**
 * The blocks of the tasks that state a time: on the due date, else on the threshold date, else today.
 * A block whose time is over is left out.
 */
// [impl->dsn~fixed-time~1]
private fun fixedBlocks(open: List<Entry>, upcoming: List<Pair<LocalDate, Task>>, today: LocalDate, now: Int): List<Block> =
    // What a schedule adds gets an index that no line of the file has.
    (open.map { (it.task.due ?: it.task.threshold ?: today) to it } + upcoming.mapIndexed { index, (date, task) -> date to Entry(-1 - index, task) })
        .flatMap { (date, entry) ->
            val start = entry.task.at ?: return@flatMap emptyList()
            val end = minOf(start + (entry.task.duration ?: DEFAULT_DURATION).inWholeMinutes.toInt(), 24 * 60)
            if (date > today || (date == today && end > now)) withTravel(Block(date, Span(start, end), entry)) else emptyList()
        }
        // A task that is in the list already for a later day is not planned a second time for its schedule.
        .distinctBy { Triple(it.date, it.span, it.text) }

/** The block of a task with the times before and after it that the way there and the way back take. */
// [impl->dsn~travel-time~1]
private fun withTravel(block: Block): List<Block> {
    val travel = block.entry.task.travel?.inWholeMinutes?.toInt() ?: return listOf(block)
    return listOf(
        block.copy(span = Span(maxOf(block.span.start - travel, 0), block.span.start), travel = true),
        block,
        block.copy(span = Span(block.span.end, minOf(block.span.end + travel, 24 * 60)), travel = true),
    ).filter { it.span.length > 0 }
}

private fun monday(date: LocalDate) = date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1))

private val MOMENTS = compareBy<Pair<LocalDate, Int>>({ it.first }, { it.second })

/**
 * The order in which the tasks get their times: that of the task list, but a task after the tasks it waits for.
 * As topydo writes it, a task with the tag `id:` waits for the tasks whose tag `p:` has the same value.
 */
// [impl->dsn~dependencies~1]
private class Dependencies(open: List<Entry>) {
    val problems = mutableListOf<String>()

    /** The open tasks a task waits for. */
    val of = mutableMapOf<Entry, List<Entry>>()
    val order: List<Entry>

    init {
        val sorted = open.sortedWith(listOrder)
        val parts = sorted.flatMap { entry -> entry.task.tags.filter { it.first == Task.PARENT }.map { it.second to entry } }.groupBy({ it.first }, { it.second })
        val result = LinkedHashSet<Entry>()
        val visiting = mutableSetOf<Entry>()

        fun visit(entry: Entry) {
            if (entry in result) return
            visiting += entry
            val waitsFor = entry.task.tags.filter { it.first == Task.ID }.flatMap { parts[it.second].orEmpty() }.distinct().filter { it != entry }.filter { other ->
                // Tasks that wait for each other are planned as if the one that closes the circle did not wait.
                (other !in visiting).also { fine ->
                    if (!fine) problems += "\"${entry.task.description}\" and \"${other.task.description}\" wait for each other"
                }
            }
            of[entry] = waitsFor
            waitsFor.forEach(::visit)
            visiting -= entry
            result += entry
        }
        sorted.forEach(::visit)
        order = result.toList()
    }
}

/** One run of the planner: the times that are taken, and what is left of the tasks. */
private class Planning(
    private val config: PlanConfig,
    busy: Map<LocalDate, List<Span>>,
    private val today: LocalDate,
    now: Int,
    private val dependencies: Dependencies,
    fixed: List<Block>,
) {
    private val busy = busy + (today to busy[today].orEmpty() + Span(0, now))
    private val taken = mutableMapOf<LocalDate, MutableList<Span>>()
    private val blocks = mutableMapOf<Entry, MutableList<Block>>()
    private val left = dependencies.order.associateWithTo(mutableMapOf()) { (it.task.duration ?: DEFAULT_DURATION).inWholeMinutes.toInt() }
    private val unplaced = mutableListOf<Entry>()

    /** The tasks that are not to be planned yet: their threshold date lies behind their barrier, or they wait for such a task. */
    private val later = mutableSetOf<Entry>()

    /** The minutes planned for the tasks of a limit on a day, and in the week that starts on the day. */
    private val usedDay = mutableMapOf<Pair<String, LocalDate>, Int>()
    private val usedWeek = mutableMapOf<Pair<String, LocalDate>, Int>()

    /** The blocks of what a schedule adds on a later day: no task of the list stands behind them yet. */
    private val upcoming = fixed.filter { it.entry !in left }

    // [impl->dsn~fixed-time~1]
    init {
        for (block in fixed) {
            taken.getOrPut(block.date) { mutableListOf() } += block.span
            // The way to a task is no time of its project or context.
            if (!block.travel) use(block, 1)
            if (block.entry in left) blocks.getOrPut(block.entry) { mutableListOf() } += block
        }
        // A task with a time is not moved: nothing of it is left for a free time, also when its time is over.
        dependencies.order.filter { it.task.at != null }.forEach { left[it] = 0 }
    }

    private fun use(block: Block, sign: Int) {
        for (limit in config.limitsOf(block.entry.task)) {
            usedDay[limit.key to block.date] = (usedDay[limit.key to block.date] ?: 0) + sign * block.span.length
            usedWeek[limit.key to monday(block.date)] = (usedWeek[limit.key to monday(block.date)] ?: 0) + sign * block.span.length
        }
    }

    private fun count(block: Block, sign: Int) {
        use(block, sign)
        left[block.entry] = left.getValue(block.entry) - sign * block.span.length
    }

    /** The first moment at which the task may be worked on; `null` while a task it waits for is not planned completely. */
    private fun start(entry: Entry): Pair<LocalDate, Int>? {
        var start = maxOf(today, entry.task.threshold ?: today) to 0
        for (other in dependencies.of[entry].orEmpty()) {
            if (left.getValue(other) > 0 || other in unplaced) return null
            val end = blocks[other].orEmpty().map { it.date to it.span.end }.maxWithOrNull(MOMENTS) ?: continue
            if (MOMENTS.compare(end, start) > 0) start = end
        }
        return start
    }

    /** Gives the task the earliest free times from the moment on up to the day, up to the wanted minutes. */
    private fun put(entry: Entry, wanted: Int, from: Pair<LocalDate, Int>, until: LocalDate) {
        val task = entry.task
        val limits = config.limitsOf(task)
        val block = config.blockOf(task)
        val map = config.mapOf(task)
        val travel = task.travel?.inWholeMinutes?.toInt() ?: 0
        var missing = wanted
        var date = from.first
        while (missing > 0 && left.getValue(entry) > 0 && date <= until) {
            // After a meeting, a block starts on the grid; after another block, it starts at once.
            for (free in config.maps.spans(map, date).without(busy[date].orEmpty()).aligned().without(taken[date].orEmpty())) {
                val rest = left.getValue(entry)
                if (missing <= 0 || rest == 0) break
                val start = if (date == from.first) maxOf(free.start, from.second) else free.start
                val allowed = limits.minOfOrNull { limit ->
                    minOf(
                        (limit.maxDay ?: Int.MAX_VALUE) - (usedDay[limit.key to date] ?: 0),
                        (limit.maxWeek ?: Int.MAX_VALUE) - (usedWeek[limit.key to monday(date)] ?: 0),
                    )
                } ?: Int.MAX_VALUE
                // A task with a way to it is not cut: each part would need the way.
                val shortest = if (travel > 0) rest else minOf(block, rest)
                var length = minOf(free.end - start - 2 * travel, maxOf(missing, shortest), rest, allowed)
                // What stays of the task for later is a whole block as well.
                if (rest - length in 1 until block) length = rest - block
                if (length < shortest) continue
                val part = Block(date, Span(start + travel, start + travel + length), entry)
                for (placed in withTravel(part)) {
                    blocks.getOrPut(entry) { mutableListOf() } += placed
                    taken.getOrPut(date) { mutableListOf() } += placed.span
                }
                count(part, 1)
                missing -= length
            }
            date = date.plus(DatePeriod(days = 1))
        }
    }

    /**
     * @param reserveUntil for the key of a limit with a time to plan in each week, the last day up to which that time is planned first
     */
    fun run(reserveUntil: Map<String, LocalDate>): Plan {
        // [impl->dsn~plan-limits~1]
        var week = monday(today)
        while (reserveUntil.values.any { week <= it }) {
            for (limit in config.minimums) {
                val until = reserveUntil[limit.key] ?: continue
                for (entry in dependencies.order) {
                    val missing = limit.minWeek!! - (usedWeek[limit.key to week] ?: 0)
                    if (missing <= 0) break
                    if (!limit.names(entry.task) || left.getValue(entry) == 0) continue
                    val start = start(entry) ?: continue
                    put(entry, missing, if (start.first < week) week to 0 else start, minOf(week.plus(DatePeriod(days = 6)), until, config.barrierOf(entry.task, today)))
                }
            }
            week = week.plus(DatePeriod(days = 7))
        }
        for (entry in dependencies.order) {
            val barrier = config.barrierOf(entry.task, today)
            if (entry.task.threshold.let { it != null && it > barrier } || dependencies.of[entry].orEmpty().any { it in later }) {
                later += entry
                continue
            }
            start(entry)?.let { put(entry, left.getValue(entry), it, barrier) }
            if (left.getValue(entry) > 0) {
                // ponytail: the times that a task got for the minimum of a week stay free if the task does not fit as a whole;
                // plan again without the task if that leaves too much free.
                blocks.remove(entry).orEmpty().forEach {
                    taken.getValue(it.date) -= it.span
                    if (!it.travel) count(it, -1)
                }
                unplaced += entry
            }
        }
        val planned = blocks.values.flatten()
        // What a schedule adds is shown as far as the tasks are planned, and for four weeks at least.
        val end = maxOf(planned.maxOfOrNull { it.date } ?: today, today.plus(DatePeriod(days = UPCOMING_DAYS)))
        return Plan((planned + upcoming.filter { it.date <= end }).sortedWith(compareBy<Block> { it.date }.thenBy { it.span.start }), unplaced, dependencies.problems)
    }
}

/**
 * Plans the open tasks: each gets the earliest free times of its time map, in the order of the task list,
 * so that a task with a higher priority is planned before the others. A task is planned after the tasks it waits for.
 *
 * A task is not planned before its threshold date, and not behind its barrier: if its duration does not fit before, it is not planned at all.
 * A task whose threshold date lies behind its barrier is not due for planning: it is left out without being returned as not planned.
 * A task is cut into parts if a free time is too short for it or a limit of its project or context allows no more on a day or in a week.
 * A limit with a time for each week gets that time first, in the weeks up to the day on which its tasks would be done without it.
 *
 * A task with a travel time gets that time before and after its block, and is not cut into parts.
 * A task with a time of the day is not moved: it takes that time, whatever the time maps and the calendars say, and the other tasks are planned around it.
 *
 * @param busy the times that are taken, by day
 * @param now the minutes of today that have passed
 * @param upcoming the tasks that schedules add on later days; those with a time of the day take that time
 */
// [impl->dsn~planner~1]
fun plan(
    entries: List<Entry>,
    config: PlanConfig,
    busy: Map<LocalDate, List<Span>>,
    today: LocalDate,
    now: Int,
    upcoming: List<Pair<LocalDate, Task>> = emptyList(),
): Plan {
    val dependencies = Dependencies(entries.filterNot { it.task.completed })
    val fixed = fixedBlocks(dependencies.order, upcoming, today, now)
    val plain = Planning(config, busy, today, now, dependencies, fixed).run(emptyMap())
    val reserveUntil = config.minimums.mapNotNull { limit ->
        val days = plain.blocks.filter { limit.names(it.entry.task) }.map { it.date } +
            plain.unplaced.filter { limit.names(it.task) }.map { config.barrierOf(it.task, today) }
        days.maxOrNull()?.let { limit.key to it }
    }.toMap()
    return if (reserveUntil.isEmpty()) plain else Planning(config, busy, today, now, dependencies, fixed).run(reserveUntil)
}
