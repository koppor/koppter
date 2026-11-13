package io.github.koppor.koppter.todotxt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

// [utest->dsn~todo-file-lines~1]
class TodoFileTest {
    private val text = "(A)  Call   Mom\n\nx done  thing\n"

    @Test
    fun anUntouchedFileIsWrittenBackUnchanged() {
        assertEquals(text, TodoFile.parse(text).serialize())
        assertEquals("a\r\nb\r\n", TodoFile.parse("a\r\nb\r\n").serialize())
        assertEquals("", TodoFile.parse("").serialize())
    }

    @Test
    fun blankLinesAreNotTasksAndKeepTheirPosition() {
        val file = TodoFile.parse(text)
        assertEquals(listOf(0, 2), file.entries.map { it.index })
    }

    @Test
    fun replacingRewritesOnlyTheChangedLine() {
        val file = TodoFile.parse(text)
        val entry = file.entries.first()
        assertSame(file, file.replace(entry.index, entry.task))
        val changed = file.replace(entry.index, entry.task.edit(priority = 'B'))
        assertEquals("(B) Call Mom\n\nx done  thing\n", changed.serialize())
    }

    @Test
    fun addsAndRemovesLines() {
        val file = TodoFile.parse("a\nb\n").add(Task.parse("c")).remove(0)
        assertEquals("b\nc\n", file.serialize())
    }

    @Test
    fun aFileWithoutFinalLineSeparatorGetsOne() {
        assertEquals("a\nb\n", TodoFile.parse("a\nb").serialize())
    }

    // [utest->dsn~list-step~1]
    @Test
    fun aStepChangesPlacesWithinAPriorityAndTakesThePriorityAtItsBorder() {
        val file = TodoFile.parse("(A) Report\n\n(B) Shelf  @home\n(B) Photos\nLetter\n(B) Taxes due:2026-11-15\nx Done\n")
        fun TodoFile.stepped(index: Int, neighbor: Int, up: Boolean) = step(index, neighbor, up)?.serialize()

        // Same priority and due date: the lines change places, as they are, and other lines stay where they are.
        assertEquals("(A) Report\n\n(B) Photos\n(B) Shelf  @home\nLetter\n(B) Taxes due:2026-11-15\nx Done\n", file.stepped(3, 2, up = true))
        assertEquals("(A) Report\n\n(B) Photos\n(B) Shelf  @home\nLetter\n(B) Taxes due:2026-11-15\nx Done\n", file.stepped(2, 3, up = false))
        // Another priority: the task takes it and becomes the last of that priority, or the first on the way down.
        assertEquals("(A) Report\n(A) Shelf @home\n\n(B) Photos\nLetter\n(B) Taxes due:2026-11-15\nx Done\n", file.stepped(2, 0, up = true))
        assertEquals("(A) Report\n\n(B) Shelf  @home\n(B) Photos\n(B) Letter\n(B) Taxes due:2026-11-15\nx Done\n", file.stepped(4, 3, up = true))
        assertEquals("(A) Report\n\n(B) Shelf  @home\nPhotos\nLetter\n(B) Taxes due:2026-11-15\nx Done\n", file.stepped(3, 4, up = false))
        // Another due date: at a border, only the priority changes; within a priority, the due date decides and there is no step.
        assertEquals("(B) Report\n\n(B) Shelf  @home\n(B) Photos\nLetter\n(B) Taxes due:2026-11-15\nx Done\n", file.stepped(0, 5, up = false))
        assertNull(file.stepped(2, 5, up = true))
        assertNull(file.stepped(4, 6, up = false))
    }
}
