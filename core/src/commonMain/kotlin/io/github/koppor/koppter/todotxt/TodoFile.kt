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

    /**
     * The file after the task of a line is put next to another task of the list; `null` if the order of the list does not allow that.
     *
     * The task takes the priority of the other one.
     * With the same due date and time, its line is moved directly before or behind the line of the other one, and is written as it was if its priority stays.
     * With another due date or time, the list orders the two tasks itself, so that only the priority changes.
     */
    // [impl->dsn~list-drag~1]
    fun moveTo(index: Int, target: Int, before: Boolean): TodoFile? {
        val task = Task.parse(lines[index])
        val other = Task.parse(lines[target])
        if (index == target || !task.canStepOver(other)) return null
        val line = if (task.priority == other.priority) lines[index] else task.edit(priority = other.priority).toLine()
        if (task.due != other.due || task.by != other.by) return copy(lines = lines.toMutableList().also { it[index] = line })
        val at = (if (target > index) target - 1 else target) + if (before) 0 else 1
        return copy(lines = lines.filterIndexed { i, _ -> i != index }.toMutableList().also { it.add(at, line) })
    }

    /**
     * The file after the task of a line steps over its neighbor in the list; `null` if the order of the list does not allow that.
     *
     * A neighbor with another priority is a border: the task takes that priority and stays on its side of the neighbor.
     * A neighbor with the same priority and due date changes places with it.
     */
    // [impl->dsn~list-step~1]
    fun step(index: Int, neighbor: Int, up: Boolean): TodoFile? =
        moveTo(index, neighbor, before = up == (Task.parse(lines[index]).priority == Task.parse(lines[neighbor]).priority))

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
