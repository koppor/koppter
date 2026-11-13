package io.github.koppor.koppter.todotxt

/**
 * The names under which priorities are shown, such as 1, 2, 3, 5 and 8 for A to E.
 *
 * The file keeps the letters of the todo.txt format, so that other programs read the priorities;
 * the names only change what the application shows and offers.
 */
// [impl->dsn~priority-names~1]
data class PriorityNames(val names: Map<Char, String> = emptyMap()) {
    /** The priorities offered for a task: those with a name, or A to F when none has one. */
    val choices: List<Char> get() = names.keys.sorted().ifEmpty { ('A'..'F').toList() }

    fun name(priority: Char): String = names[priority] ?: priority.toString()

    /** The typed line with a name in parentheses at its start replaced by the letter, so that `(5) Call` is a task with a priority. */
    fun toLetters(line: String): String {
        val name = line.takeIf { it.startsWith("(") }?.substringBefore(") ", "")?.drop(1) ?: return line
        val letter = names.entries.firstOrNull { it.value == name }?.key ?: return line
        return "($letter)" + line.substring(name.length + 2)
    }

    companion object {
        /** Reads pairs of letter and name; a pair whose letter is not one of A to Z or whose name is empty is left out. */
        fun of(entries: List<Pair<String, String>>) = PriorityNames(
            entries.mapNotNull { (letter, name) -> letter.singleOrNull()?.takeIf { it in 'A'..'Z' && name.isNotEmpty() }?.let { it to name } }.toMap(),
        )
    }
}
