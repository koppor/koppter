package io.github.koppor.koppter.todotxt

/** A task together with its position in the file, which identifies it for updates. */
data class Entry(val index: Int, val task: Task)

/**
 * The content of a todo.txt file as its lines.
 *
 * The lines are the source of truth: a task is parsed from its line on demand, and only a line
 * that is changed through [replace] is rewritten. Lines the user did not touch, including blank
 * lines and lines in a form this application would not write itself, stay as they are.
 */
// [impl->dsn~todo-file-lines~1]
data class TodoFile(
    val lines: List<String> = emptyList(),
    val lineSeparator: String = "\n",
) {
    /** The tasks of all non-blank lines, in file order. */
    val entries: List<Entry>
        get() = lines.mapIndexedNotNull { index, line ->
            if (line.isBlank()) null else Entry(index, Task.parse(line))
        }

    fun add(task: Task): TodoFile = copy(lines = lines + task.toLine())

    /** Rewrites the line only if the task differs from what the line already says. */
    fun replace(index: Int, task: Task): TodoFile =
        if (Task.parse(lines[index]) == task) this
        else copy(lines = lines.toMutableList().also { it[index] = task.toLine() })

    fun remove(index: Int): TodoFile = copy(lines = lines.filterIndexed { i, _ -> i != index })

    fun serialize(): String = if (lines.isEmpty()) "" else lines.joinToString(lineSeparator, postfix = lineSeparator)

    companion object {
        fun parse(text: String): TodoFile {
            val separator = if (text.contains("\r\n")) "\r\n" else "\n"
            val lines = text.split(separator)
            // The final line separator terminates the last line; it does not start an empty one.
            return TodoFile(if (lines.last().isEmpty()) lines.dropLast(1) else lines, separator)
        }
    }
}
