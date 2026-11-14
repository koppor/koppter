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
        val letter = letter(name) ?: return line
        return "($letter)" + line.substring(name.length + 2)
    }

    /**
     * The filter expression with the names of priorities replaced by their letters, so that `(>3)` is read as `(>C)`.
     * A term with a letter stays as it is, unless the letter is the name of another priority.
     */
    fun toLettersInFilter(expression: String): String = expression.split(' ').joinToString(" ") { word ->
        FILTER_TERM.matchEntire(word)?.let { term -> letter(term.groupValues[2])?.let { term.groupValues[1] + it + ")" } } ?: word
    }

    private fun letter(name: String): Char? = names.entries.firstOrNull { it.value == name }?.key

    companion object {
        private val FILTER_TERM = Regex("^(-?\\((?:<=|>=|<|>|=)?)(.+)\\)$")

        /** The text that names the priorities from A on by the given names, one per line as `letter = name`. */
        fun lines(names: List<String>): String = names.mapIndexed { index, name -> "${'A' + index} = $name" }.joinToString("\n")

        /** Reads pairs of letter and name; a pair whose letter is not one of A to Z or whose name is empty is left out. */
        fun of(entries: List<Pair<String, String>>) = PriorityNames(
            entries.mapNotNull { (letter, name) -> letter.singleOrNull()?.takeIf { it in 'A'..'Z' && name.isNotEmpty() }?.let { it to name } }.toMap(),
        )
    }
}
