package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate

enum class Completion { ALL, COMPLETED, OPEN }

/** Separates the levels of a hierarchical context: `research>reading` lies below `research`. */
// [impl->dsn~context-hierarchy~1]
const val LEVEL_SEPARATOR = '>'

/** Whether the context is [ancestor] or lies below it. */
fun String.isWithin(ancestor: String): Boolean = this == ancestor || startsWith("$ancestor$LEVEL_SEPARATOR")

/** The context with its ancestors, the outermost first: `a`, `a>b`, `a>b>c`. */
fun String.withAncestors(): List<String> = split(LEVEL_SEPARATOR).runningReduce { path, level -> "$path$LEVEL_SEPARATOR$level" }

/** The last level of the context, which is what a label shows. */
val String.lastLevel: String get() = substringAfterLast(LEVEL_SEPARATOR)

/** Sorts a context directly before the contexts below it. */
val hierarchyOrder: Comparator<String> = compareBy { it.replace(LEVEL_SEPARATOR, '\u0000') }

/**
 * Restricts the task list.
 *
 * The criteria are combined with *and*; within [priorities], [projects], [contexts] and [hashtags] one match
 * suffices. An empty set does not restrict. A context also matches the contexts below it.
 */
// [impl->dsn~filter~1]
data class Filter(
    val completion: Completion = Completion.OPEN,
    val text: String = "",
    val priorities: Set<Char> = emptySet(),
    val projects: Set<String> = emptySet(),
    val contexts: Set<String> = emptySet(),
    val hashtags: Set<String> = emptySet(),
    /** Hides tasks whose threshold date (`t:`) lies after today. */
    val hideFuture: Boolean = true,
) {
    fun matches(task: Task, today: LocalDate): Boolean =
        when (completion) {
            Completion.ALL -> true
            Completion.COMPLETED -> task.completed
            Completion.OPEN -> !task.completed
        } &&
            (text.isBlank() || task.toLine().contains(text.trim(), ignoreCase = true)) &&
            (priorities.isEmpty() || task.priority in priorities) &&
            (projects.isEmpty() || task.projects.any { it in projects }) &&
            (contexts.isEmpty() || task.contexts.any { context -> contexts.any(context::isWithin) }) &&
            (hashtags.isEmpty() || task.hashtags.any { it in hashtags }) &&
            !(hideFuture && !task.completed && task.threshold.let { it != null && it > today })

    val isDefault: Boolean get() = this == Filter()
}

/**
 * The order of the task list: open before completed, then by priority (none last),
 * then by due date (none last) and the time by which they are due on that day (none last), then by position in the file.
 */
// [impl->dsn~sort-order~1]
val listOrder: Comparator<Entry> =
    compareBy<Entry> { it.task.completed }
        .thenBy { it.task.priority ?: Char.MAX_VALUE }
        .thenBy { it.task.due ?: LocalDate(9999, 12, 31) }
        // [impl->dsn~due-time~1]
        .thenBy { it.task.by ?: (24 * 60) }
        .thenBy { it.index }
