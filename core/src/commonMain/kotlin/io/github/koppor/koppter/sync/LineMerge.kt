package io.github.koppor.koppter.sync

/**
 * Three-way merge of a todo.txt file.
 *
 * A todo.txt file is a collection of independent lines, so two sides never conflict:
 * the result is *theirs* with the changes of *ours* since [base] applied to it.
 * Lines we removed are removed, lines we added are appended.
 * If we removed as many lines as we added, the changes are taken as edits, and each new line takes the place of the old one.
 * If both sides changed the same line differently, both versions are kept, so that nothing is lost.
 */
// [impl->dsn~line-merge~1]
fun mergeLines(base: List<String>, ours: List<String>, theirs: List<String>): List<String> {
    val removedByUs = base.minusEach(ours)
    val addedByUs = ours.minusEach(base)
    val addedByThem = theirs.minusEach(base).toMutableList()
    val edits = removedByUs.size == addedByUs.size

    val result = theirs.toMutableList()
    val appended = mutableListOf<String>()
    removedByUs.forEachIndexed { i, removed ->
        val position = result.indexOf(removed)
        val replacement = if (edits) addedByUs[i].takeIf { !addedByThem.remove(it) } else null
        when {
            position < 0 -> replacement?.let(appended::add)
            replacement != null -> result[position] = replacement
            else -> result.removeAt(position)
        }
    }
    if (!edits) {
        // Both sides added the same line: keep it once.
        addedByUs.filterNotTo(appended) { addedByThem.remove(it) }
    }
    return result + appended
}

/** Multiset difference that keeps the order: each element of [other] removes one equal element of the receiver. */
private fun List<String>.minusEach(other: List<String>): List<String> {
    val remaining = other.groupingBy { it }.eachCount().toMutableMap()
    return filter { line ->
        val count = remaining[line] ?: 0
        if (count > 0) remaining[line] = count - 1
        count == 0
    }
}
