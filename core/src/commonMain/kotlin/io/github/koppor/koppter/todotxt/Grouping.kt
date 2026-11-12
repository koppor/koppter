package io.github.koppor.koppter.todotxt

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** The attribute whose values form the columns of the board. */
enum class GroupKey(val label: String) {
    PRIORITY("Priority"),
    PROJECT("Project"),
    CONTEXT("Context"),
    DUE("Due date"),
}

/**
 * One column of the board.
 *
 * @param value the value of the attribute that the tasks of the column share: a priority, a project, a context
 *   or a due date range; `null` for the column of the tasks without a value
 */
data class Group(val title: String, val entries: List<Entry>, val value: String? = null)

/**
 * Distributes the entries over groups by the given attribute, keeping their order within each group.
 *
 * A task with several projects or contexts appears in each of their groups.
 * Tasks without a value form the last group; empty groups are left out.
 */
// [impl->dsn~board-groups~1]
fun groupEntries(entries: List<Entry>, key: GroupKey, today: LocalDate): List<Group> {
    fun byValues(none: String, prefix: String, values: (Task) -> List<String>): List<Group> {
        val named = entries.flatMap { values(it.task) }.distinct().sorted()
            .map { value -> Group(prefix + value, entries.filter { value in values(it.task) }, value) }
        return named + Group(none, entries.filter { values(it.task).isEmpty() })
    }
    return when (key) {
        GroupKey.PRIORITY -> byValues("No priority", "") { listOfNotNull(it.priority?.toString()) }
        GroupKey.PROJECT -> byValues("No project", "+") { it.projects }
        GroupKey.CONTEXT -> byValues("No context", "@") { it.contexts }
        GroupKey.DUE -> listOf(
            Group("Overdue", entries.filter { it.task.due.let { due -> due != null && due < today } }, OVERDUE),
            Group("Today", entries.filter { it.task.due == today }, TODAY),
            Group("Later", entries.filter { it.task.due.let { due -> due != null && due > today } }, LATER),
            Group("No due date", entries.filter { it.task.due == null }),
        )
    }.filter { it.entries.isNotEmpty() }
}

private const val OVERDUE = "overdue"
private const val TODAY = "today"
private const val LATER = "later"

/**
 * The task as it is after the user moved it from one column of the board to another.
 *
 * The value of the source column is replaced by that of the target column; other projects and contexts stay.
 * For due date ranges, *today* sets today, *later* tomorrow, and the column without due date removes the date.
 *
 * @return `null` if the move makes no sense: within a column, or into the column of overdue tasks
 */
// [impl->dsn~board-move~1]
fun moveToGroup(task: Task, key: GroupKey, from: Group, to: Group, today: LocalDate): Task? {
    if (from.value == to.value) return null
    return when (key) {
        GroupKey.PRIORITY -> task.edit(priority = to.value?.single())
        GroupKey.PROJECT -> task.edit(projects = (task.projects - listOfNotNull(from.value).toSet() + listOfNotNull(to.value)).distinct())
        GroupKey.CONTEXT -> task.edit(contexts = (task.contexts - listOfNotNull(from.value).toSet() + listOfNotNull(to.value)).distinct())
        GroupKey.DUE -> when (to.value) {
            null -> task.withTag(Task.DUE, null)
            TODAY -> task.withTag(Task.DUE, today.toString())
            LATER -> task.withTag(Task.DUE, today.plus(DatePeriod(days = 1)).toString())
            else -> null
        }
    }
}
