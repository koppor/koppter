package io.github.koppor.koppter.plan

import io.github.koppor.koppter.todotxt.Task
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/** When a task is planned: the start of its first block and the day of its last one. */
data class Planned(val date: LocalDate, val start: Int, val lastDate: LocalDate)

/** A block as the file has it: a time of a day and the text of the task. */
data class TextBlock(val date: LocalDate, val span: Span, val text: String)

/** How the plan of a task relates to its due date. */
enum class PlanState { FITS, TIGHT, LATE }

/**
 * The plan as a file in the format of calendar.txt: one line per day with the date, the week, the weekday and the blocks of the day,
 * such as `2026-11-16 w47 Mon  09:00-10:00 Read the survey @research>reading. 10:00-10:30 Call the landlord @phone`.
 */
// [impl->dsn~calendar-txt~1]
object CalendarTxt {
    const val NAME = "calendar.txt"

    private val DAY_LINE = Regex("^(\\d{4}-\\d{2}-\\d{2}) w\\d{2} [A-Z][a-z]{2}(?:  (.*))?$")
    private val BLOCK = Regex("^(\\d{2}):(\\d{2})-(\\d{2}):(\\d{2}) (.*)$")
    private val NEXT_BLOCK = Regex("\\. (?=\\d{2}:\\d{2}-\\d{2}:\\d{2} )")
    private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    /** What a block shows of its task: the body without tags, which change in the life of a task. */
    fun text(task: Task): String = (listOf(task.description) + task.projects.map { "+$it" } + task.contexts.map { "@$it" }).joinToString(" ")

    /** The week of the year according to ISO 8601: the week of a day is that of its Thursday. */
    fun week(date: LocalDate): Int = (date.plus(DatePeriod(days = 4 - date.dayOfWeek.isoDayNumber)).dayOfYear - 1) / 7 + 1

    private fun time(minutes: Int) = "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"

    private fun dateOf(line: String): LocalDate? = DAY_LINE.matchEntire(line)?.groupValues?.get(1)?.let(Task::parseDate)

    /**
     * The file with the plan from today on.
     * The lines of the days before today stay, as do lines that are no day; the lines from today on are replaced.
     */
    fun write(existing: String, blocks: List<Block>, today: LocalDate): String {
        val kept = existing.lines().filter { line -> line.isNotBlank() && dateOf(line).let { it == null || it < today } }
        val byDay = blocks.groupBy { it.date }
        val days = generateSequence(today) { it.plus(DatePeriod(days = 1)) }.takeWhile { day -> blocks.any { it.date >= day } }
        val planned = days.map { day ->
            val events = byDay[day].orEmpty().sortedBy { it.span.start }.joinToString(". ") { "${timesText(it.span)} ${text(it.entry.task)}" }
            "$day w${week(day).toString().padStart(2, '0')} ${WEEKDAYS[day.dayOfWeek.isoDayNumber - 1]}  $events".trimEnd()
        }
        return (kept + planned).joinToString("\n", postfix = "\n")
    }

    /** The blocks of the file in the order of time, those of past days included. */
    fun blocks(calendar: String): List<TextBlock> = calendar.lines().flatMap { line ->
        val date = dateOf(line) ?: return@flatMap emptyList()
        DAY_LINE.matchEntire(line)!!.groupValues[2].split(NEXT_BLOCK).mapNotNull { event ->
            val (h1, m1, h2, m2, text) = BLOCK.matchEntire(event.trim())?.destructured ?: return@mapNotNull null
            TextBlock(date, Span(h1.toInt() * 60 + m1.toInt(), h2.toInt() * 60 + m2.toInt()), text)
        }
    }.sortedWith(compareBy<TextBlock> { it.date }.thenBy { it.span.start })

    /** When the tasks are planned from today on, by the text of their blocks. */
    fun planned(blocks: List<TextBlock>, today: LocalDate): Map<String, Planned> {
        val result = mutableMapOf<String, Planned>()
        for (block in blocks.filter { it.date >= today }) {
            result[block.text] = result[block.text]?.copy(lastDate = block.date) ?: Planned(block.date, block.span.start, block.date)
        }
        return result
    }

    fun planned(calendar: String, today: LocalDate): Map<String, Planned> = planned(blocks(calendar), today)

    /** The times of a block as a text, such as `09:00-10:30`. */
    fun timesText(span: Span): String = "${time(span.start)}-${time(span.end)}"

    /** A day as a text, such as `Mon 2026-11-16`. */
    fun dayText(date: LocalDate): String = "${WEEKDAYS[date.dayOfWeek.isoDayNumber - 1]} $date"

    /** The state of an open task in a plan: late if it is not planned at all or its last block lies behind its due date. */
    fun state(task: Task, planned: Planned?): PlanState {
        val due = task.due
        return when {
            planned == null -> PlanState.LATE
            due == null || planned.lastDate < due -> PlanState.FITS
            planned.lastDate == due -> PlanState.TIGHT
            else -> PlanState.LATE
        }
    }

    /** The start of the plan of a task as a text, such as `Mon 2026-11-16 09:00`. */
    fun startText(planned: Planned): String = "${dayText(planned.date)} ${time(planned.start)}"
}
