package io.github.koppor.koppter.sync

import io.github.koppor.koppter.todotxt.Task
import kotlinx.datetime.LocalDate

/** One version of the todo.txt file in the history, with the date it was committed. */
data class Snapshot(val date: LocalDate, val lines: List<String>)

/** The dates of a task as far as the history knows them. */
data class TaskDates(val added: LocalDate? = null, val completed: LocalDate? = null)

/**
 * Derives, for each line of the newest snapshot, when the task was added and when it was completed.
 *
 * A task is followed through the history by its body without the `pri:` tag, so that completing,
 * reopening and changing the priority do not break the trail. When a version removes as many
 * tasks as it adds, the removed and added ones are taken as edits, in order, and keep their dates.
 *
 * @param history oldest snapshot first
 * @return the dates by line of the newest snapshot
 */
// [impl->dsn~dates-from-history~1]
fun taskDates(history: List<Snapshot>): Map<String, TaskDates> {
    var known = mapOf<String, TaskDates>()
    for (snapshot in history) {
        val tasks = snapshot.lines.filter { it.isNotBlank() }.map(Task::parse)
        val keys = tasks.map(::identity)
        val gone = known.keys.filterNot { it in keys }
        val new = keys.filterNot { it in known }.distinct()
        val renamed = if (gone.size == new.size) new.zip(gone).toMap() else emptyMap()

        known = tasks.associate { task ->
            val key = identity(task)
            val before = known[key] ?: renamed[key]?.let { known[it] }
            key to TaskDates(
                added = before?.added ?: snapshot.date,
                completed = if (task.completed) before?.completed ?: snapshot.date else null,
            )
        }
    }
    val newest = history.lastOrNull() ?: return emptyMap()
    return newest.lines.filter { it.isNotBlank() }.associateWith { known.getValue(identity(Task.parse(it))) }
}

private fun identity(task: Task): String =
    task.body.split(' ').filterNot { it.isEmpty() || it.startsWith("${Task.PRIORITY}:") }.joinToString(" ")
