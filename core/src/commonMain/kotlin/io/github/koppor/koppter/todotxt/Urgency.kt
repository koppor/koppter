package io.github.koppor.koppter.todotxt

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus

/**
 * How far an open task is on its way to its due date.
 *
 * @param daysLeft the days until the due date; negative if it has passed
 * @param elapsed the part of the time from the start of the task to its due date that has passed, from 0 to 1
 */
data class Urgency(val daysLeft: Int, val elapsed: Float) {
    val state: UrgencyState
        get() = when {
            daysLeft < 0 -> UrgencyState.OVERDUE
            daysLeft == 0 -> UrgencyState.DUE
            else -> UrgencyState.FINE
        }

    val text: String
        get() = when {
            daysLeft < -1 -> "${-daysLeft} days overdue"
            daysLeft == -1 -> "1 day overdue"
            daysLeft == 0 -> "Due today"
            daysLeft == 1 -> "Due tomorrow"
            else -> "Due in $daysLeft days"
        }
}

/** Ordered from the best to the worst state. */
enum class UrgencyState { FINE, DUE, OVERDUE }

/**
 * The urgency of the task; `null` if it is completed or has no due date.
 * The task starts on its threshold date, else on its creation date.
 */
// ponytail: a task without either starts two weeks before its due date; the date from the git history if the bar should be exact.
// [impl->dsn~overview~1]
fun Task.urgency(today: LocalDate): Urgency? {
    val due = due?.takeIf { !completed } ?: return null
    val start = (threshold ?: creationDate)?.takeIf { it < due } ?: due.minus(DatePeriod(days = 14))
    return Urgency(today.daysUntil(due), (start.daysUntil(today).toFloat() / start.daysUntil(due)).coerceIn(0f, 1f))
}

/**
 * The state of a context of the outermost level, with the contexts below it.
 *
 * @param context `null` for the tasks without context
 * @param entries the open tasks, the most urgent first and those without due date last
 */
data class ContextState(val context: String?, val entries: List<Entry>, val dueToday: Int, val overdue: Int) {
    val state: UrgencyState
        get() = when {
            overdue > 0 -> UrgencyState.OVERDUE
            dueToday > 0 -> UrgencyState.DUE
            else -> UrgencyState.FINE
        }
}

/** The contexts of the outermost level that have open tasks, sorted by name; the tasks without context last. */
// [impl->dsn~overview~1]
fun contextStates(entries: List<Entry>, today: LocalDate): List<ContextState> {
    val open = entries.filterNot { it.task.completed }
    val contexts = open.flatMap { it.task.contexts }.map { it.substringBefore(LEVEL_SEPARATOR) }.distinct().sorted()
    return (contexts + null).mapNotNull { context ->
        val own = open.filter { entry ->
            if (context == null) entry.task.contexts.isEmpty() else entry.task.contexts.any { it.isWithin(context) }
        }.sortedWith(compareBy<Entry> { it.task.urgency(today)?.daysLeft ?: Int.MAX_VALUE }.then(listOrder))
        if (own.isEmpty()) return@mapNotNull null
        val states = own.mapNotNull { it.task.urgency(today)?.state }
        ContextState(context, own, states.count { it == UrgencyState.DUE }, states.count { it == UrgencyState.OVERDUE })
    }
}
