package io.github.koppor.koppter.storage

import io.github.koppor.koppter.todotxt.BoardColumn

/**
 * The configuration that is kept next to the todo.txt file and synchronized with it.
 *
 * It is a YAML file. The key `board-columns` maps the title of each column the user defined to its filter expression:
 *
 * ```yaml
 * board-columns:
 *   "Now": "due:<=today -@waiting"
 *   Waiting: "@waiting"
 * ```
 */
// ponytail: reads the subset of YAML that it writes (one line per column, plain or quoted scalars, the escapes \" and \\);
// a YAML library when the file gets nested content.
// [impl->dsn~config-file~1]
object ConfigFile {
    // ponytail: one name for every todo.txt file, so two todo.txt files in one directory share it; derive it from the file name if that matters.
    const val NAME = "todo.txt-config.yml"

    private const val BOARD_COLUMNS = "board-columns:"
    private const val HEADER = "# Configuration of koppter for the todo.txt file in this directory"

    /** The columns in the order of the file. */
    fun boardColumns(config: String): List<BoardColumn> {
        val lines = config.lines()
        val start = lines.indexOfFirst { it.trimEnd() == BOARD_COLUMNS }
        if (start < 0) return emptyList()
        return lines.drop(start + 1).takeWhile(::inBlock).mapNotNull(::column)
    }

    /** The configuration with the columns replaced; everything else in it stays, except comments between the columns. */
    fun withBoardColumns(config: String, columns: List<BoardColumn>): String {
        // A title is a key and therefore unique.
        val block = columns.distinctBy { it.title }.map { "  ${quoted(it.title)}: ${quoted(it.expression)}" }
            .let { if (it.isEmpty()) it else listOf(BOARD_COLUMNS) + it }
        val lines = config.lines()
        val start = lines.indexOfFirst { it.trimEnd() == BOARD_COLUMNS }
        if (start < 0) {
            if (block.isEmpty()) return config
            return (if (config.isBlank()) "$HEADER\n" else config.trimEnd() + "\n\n") + block.joinToString("\n") + "\n"
        }
        val end = start + 1 + lines.drop(start + 1).takeWhile(::inBlock).dropLastWhile { !it.startsWith(" ") }.size
        return (lines.take(start) + block + lines.drop(end)).joinToString("\n")
    }

    private fun inBlock(line: String) = line.isBlank() || line.startsWith(" ") || line.startsWith("#")

    private fun column(line: String): BoardColumn? {
        val text = line.trim()
        if (text.isEmpty() || text.startsWith("#")) return null
        val (title, rest) = scalar(text, key = true) ?: return null
        if (!rest.startsWith(":")) return null
        val (expression, _) = scalar(rest.substring(1).trim(), key = false) ?: return null
        return BoardColumn(title, expression)
    }

    private val DOUBLE_QUOTED = Regex("^\"((?:[^\"\\\\]|\\\\.)*)\"")
    private val SINGLE_QUOTED = Regex("^'((?:[^']|'')*)'")
    private val ESCAPE = Regex("\\\\(.)")
    private val END_OF_PLAIN_KEY = Regex(":(\\s|$)|\\s#")
    private val END_OF_PLAIN_VALUE = Regex("\\s#")

    /** Reads the scalar at the start of the text; returns its value and the text after it. */
    private fun scalar(text: String, key: Boolean): Pair<String, String>? = when {
        text.startsWith("\"") -> DOUBLE_QUOTED.find(text)?.let { match ->
            match.groupValues[1].replace(ESCAPE) { it.groupValues[1] } to text.substring(match.value.length).trimStart()
        }
        text.startsWith("'") -> SINGLE_QUOTED.find(text)?.let { match ->
            match.groupValues[1].replace("''", "'") to text.substring(match.value.length).trimStart()
        }
        // In YAML, a number sign at the start is a comment: a filter that starts with a hashtag has to be quoted.
        text.startsWith("#") -> "" to ""
        else -> {
            // A plain scalar ends before a comment and, as a key, before the colon that separates it from the value.
            val end = (if (key) END_OF_PLAIN_KEY else END_OF_PLAIN_VALUE).find(text)?.range?.first ?: text.length
            text.substring(0, end).trimEnd() to text.substring(end).trimStart()
        }
    }

    // Always quoted, as a filter may start with a character that YAML reserves, such as `@`.
    private fun quoted(text: String) = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
