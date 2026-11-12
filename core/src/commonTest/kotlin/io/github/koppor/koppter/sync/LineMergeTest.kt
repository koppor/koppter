package io.github.koppor.koppter.sync

import kotlin.test.Test
import kotlin.test.assertEquals

// [utest->dsn~line-merge~1]
class LineMergeTest {
    private val base = listOf("a", "b", "c")

    @Test
    fun additionsOfBothSidesAreKept() {
        assertEquals(listOf("a", "b", "c", "theirs", "ours"), mergeLines(base, base + "ours", base + "theirs"))
    }

    @Test
    fun removalsOfBothSidesAreApplied() {
        assertEquals(listOf("b"), mergeLines(base, listOf("a", "b"), listOf("b", "c")))
    }

    @Test
    fun ourEditTakesThePlaceOfTheLine() {
        assertEquals(listOf("z", "a", "x b", "c"), mergeLines(base, listOf("a", "x b", "c"), listOf("z") + base))
    }

    @Test
    fun differentEditsOfTheSameLineAreBothKept() {
        assertEquals(listOf("a", "b theirs", "c", "b ours"), mergeLines(base, listOf("a", "b ours", "c"), listOf("a", "b theirs", "c")))
    }

    @Test
    fun theSameChangeOnBothSidesIsAppliedOnce() {
        assertEquals(listOf("a", "c", "new"), mergeLines(base, listOf("a", "c", "new"), listOf("a", "c", "new")))
    }

    @Test
    fun duplicateLinesAreCounted() {
        assertEquals(listOf("a", "a"), mergeLines(listOf("a", "a", "a"), listOf("a", "a"), listOf("a", "a", "a")))
    }

    @Test
    fun withoutOurChangesTheResultIsTheirs() {
        assertEquals(listOf("c", "z", "a"), mergeLines(base, base, listOf("c", "z", "a")))
    }
}
