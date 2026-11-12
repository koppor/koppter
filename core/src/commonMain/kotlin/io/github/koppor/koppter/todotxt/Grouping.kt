package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate

/** The attribute whose values form the columns of the board. */
enum class GroupKey(val label: String) {
    PRIORITY("Priority"),
    PROJECT("Project"),
    CONTEXT("Context"),
    DUE("Due date"),
}

/** One column of the board. */
data class Group(val title: String, val entries: List<Entry>)

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
            .map { value -> Group(prefix + value, entries.filter { value in values(it.task) }) }
        return named + Group(none, entries.filter { values(it.task).isEmpty() })
    }
    return when (key) {
        GroupKey.PRIORITY -> byValues("No priority", "") { listOfNotNull(it.priority?.toString()) }
        GroupKey.PROJECT -> byValues("No project", "+") { it.projects }
        GroupKey.CONTEXT -> byValues("No context", "@") { it.contexts }
        GroupKey.DUE -> listOf(
            Group("Overdue", entries.filter { it.task.due.let { due -> due != null && due < today } }),
            Group("Today", entries.filter { it.task.due == today }),
            Group("Later", entries.filter { it.task.due.let { due -> due != null && due > today } }),
            Group("No due date", entries.filter { it.task.due == null }),
        )
    }.filter { it.entries.isNotEmpty() }
}
