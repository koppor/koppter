package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TaskTest {
    private val today = LocalDate(2026, 11, 12)

    // [utest->dsn~task-parser~1]
    @Test
    fun parsesPlainTask() {
        val task = Task.parse("Call Mom")
        assertEquals(Task(body = "Call Mom"), task)
        assertEquals("Call Mom", task.description)
    }

    // [utest->dsn~task-parser~1]
    @Test
    fun parsesPriorityAndCreationDate() {
        val task = Task.parse("(A) 2026-11-10 Call Mom")
        assertEquals('A', task.priority)
        assertEquals(LocalDate(2026, 11, 10), task.creationDate)
        assertEquals("Call Mom", task.body)
    }

    // [utest->dsn~task-parser~1]
    @Test
    fun parsesCompletedTaskWithBothDates() {
        val task = Task.parse("x 2026-11-12 2026-11-10 Call Mom")
        assertTrue(task.completed)
        assertEquals(LocalDate(2026, 11, 12), task.completionDate)
        assertEquals(LocalDate(2026, 11, 10), task.creationDate)
    }

    // [utest->dsn~task-parser~1]
    @Test
    fun aSingleDateOfACompletedTaskIsTheCompletionDate() {
        val task = Task.parse("x 2026-11-12 Call Mom")
        assertEquals(LocalDate(2026, 11, 12), task.completionDate)
        assertNull(task.creationDate)
    }

    // [utest->dsn~task-parser~1]
    @Test
    fun aLowercasePriorityAndAnInvalidDateBelongToTheBody() {
        assertEquals(Task(body = "(a) Call Mom"), Task.parse("(a) Call Mom"))
        assertEquals(Task(body = "2026-13-40 Call Mom"), Task.parse("2026-13-40 Call Mom"))
        assertEquals(Task(body = "xylophone"), Task.parse("xylophone"))
    }

    // [utest->dsn~task-model~1]
    @Test
    fun separatesDescriptionFromMetadata() {
        val task = Task.parse("Test due:2026-12-12 t:2026-11-19 +test @Stuttgart hallo du rec:1m")
        assertEquals("Test hallo du", task.description)
        assertEquals(listOf("test"), task.projects)
        assertEquals(listOf("Stuttgart"), task.contexts)
        assertEquals(LocalDate(2026, 12, 12), task.due)
        assertEquals(LocalDate(2026, 11, 19), task.threshold)
        assertEquals(Recurrence(1, Recurrence.Unit.MONTH), task.recurrence)
    }

    // [utest->dsn~task-model~1]
    @Test
    fun urlsTimesAndLoneSignsAreWordsNotMetadata() {
        val task = Task.parse("Read https://example.org at 12:30 + @ a:")
        assertEquals("Read https://example.org at 12:30 + @ a:", task.description)
        assertEquals(emptyList(), task.tags)
    }

    // [utest->dsn~task-serializer~1]
    @Test
    fun serializesWhatItParsed() {
        for (line in listOf(
            "Call Mom",
            "(A) Call Mom +Family @phone",
            "(B) 2026-11-10 Call Mom due:2026-11-20",
            "x 2026-11-12 2026-11-10 Call Mom pri:A",
            "x Call Mom",
        )) {
            assertEquals(line, Task.parse(line).toLine())
        }
    }

    // [utest->dsn~completion-dates~1]
    @Test
    fun completingWritesNoDateByDefault() {
        assertEquals("x Call Mom", Task.parse("Call Mom").complete(today, writeDate = false).toLine())
    }

    // [utest->dsn~completion-dates~1]
    @Test
    fun completingWritesTheDateWhenSwitchedOn() {
        assertEquals("x 2026-11-12 Call Mom", Task.parse("Call Mom").complete(today, writeDate = true).toLine())
    }

    // [utest->dsn~completion-dates~1]
    @Test
    fun completingATaskWithCreationDateAlwaysWritesTheCompletionDate() {
        assertEquals(
            "x 2026-11-12 2026-11-10 Call Mom",
            Task.parse("2026-11-10 Call Mom").complete(today, writeDate = false).toLine(),
        )
    }

    // [utest->dsn~completion-dates~1]
    @Test
    fun completingKeepsThePriorityAsTagAndUncompletingRestoresIt() {
        val completed = Task.parse("(A) Call Mom +Family").complete(today, writeDate = false)
        assertEquals("x Call Mom +Family pri:A", completed.toLine())
        assertEquals("(A) Call Mom +Family", completed.uncomplete().toLine())
    }

    // [utest->dsn~canonical-order-on-edit~1]
    @Test
    fun editingWritesMetadataAfterTheDescription() {
        val task = Task.parse("(B) Call +Family Mom @phone due:2026-11-20")
        val edited = task.edit(description = "Call Mom today", priority = 'A', contexts = listOf("phone", "home"))
        assertEquals("(A) Call Mom today +Family @phone @home due:2026-11-20", edited.toLine())
    }

    // [utest->dsn~canonical-order-on-edit~1]
    @Test
    fun editingWithoutChangeKeepsTheTask() {
        val task = Task.parse("Call +Family Mom @phone")
        assertSame(task, task.edit())
        assertSame(task, task.withTag(Task.DUE, null))
    }

    // [utest->dsn~canonical-order-on-edit~1]
    @Test
    fun settingATagReplacesTheTagWithTheSameKey() {
        val task = Task.parse("Call Mom due:2026-11-20 +Family")
        assertEquals("Call Mom +Family due:2026-11-25", task.withTag(Task.DUE, "2026-11-25").toLine())
        assertEquals("Call Mom +Family", task.withTag(Task.DUE, null).toLine())
    }

    // [utest->dsn~canonical-order-on-edit~1]
    @Test
    fun aTagKeepsItsPositionWhenItsValueChanges() {
        val task = Task.parse("Call Mom t:2026-11-01 due:2026-11-20")
        assertEquals("Call Mom t:2026-11-05 due:2026-11-20", task.withTag(Task.THRESHOLD, "2026-11-05").toLine())
        assertSame(task, task.withTag(Task.THRESHOLD, "2026-11-01"))
    }

    // [utest->dsn~creation-date~1]
    @Test
    fun aNewTaskGetsACreationDateOnlyWhenSwitchedOn() {
        assertEquals("(A) Call Mom", Task.parse("(A) Call Mom").created(today, writeDate = false).toLine())
        assertEquals("(A) 2026-11-12 Call Mom", Task.parse("(A) Call Mom").created(today, writeDate = true).toLine())
        assertEquals("2026-11-01 Call Mom", Task.parse("2026-11-01 Call Mom").created(today, writeDate = true).toLine())
    }
}
