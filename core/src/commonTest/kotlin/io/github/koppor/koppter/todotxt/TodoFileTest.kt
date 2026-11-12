package io.github.koppor.koppter.todotxt

import kotlin.test.Test
import kotlin.test.assertEquals
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
}
