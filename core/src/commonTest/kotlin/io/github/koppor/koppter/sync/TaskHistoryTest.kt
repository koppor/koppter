package io.github.koppor.koppter.sync

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

// [utest->dsn~dates-from-history~1]
class TaskHistoryTest {
    private fun day(day: Int) = LocalDate(2026, 11, day)

    @Test
    fun aTaskIsAddedWhenItFirstAppearsAndCompletedWhenItIsMarked() {
        val dates = taskDates(
            listOf(
                Snapshot(day(1), listOf("(A) Call Mom")),
                Snapshot(day(3), listOf("(A) Call Mom", "Buy milk")),
                Snapshot(day(5), listOf("x Call Mom pri:A", "Buy milk")),
            ),
        )
        assertEquals(TaskDates(added = day(1), completed = day(5)), dates["x Call Mom pri:A"])
        assertEquals(TaskDates(added = day(3)), dates["Buy milk"])
    }

    @Test
    fun reopeningForgetsTheCompletion() {
        val dates = taskDates(
            listOf(
                Snapshot(day(1), listOf("Call Mom")),
                Snapshot(day(2), listOf("x Call Mom")),
                Snapshot(day(3), listOf("Call Mom")),
            ),
        )
        assertEquals(TaskDates(added = day(1)), dates["Call Mom"])
    }

    @Test
    fun anEditedTaskKeepsItsDates() {
        val dates = taskDates(
            listOf(
                Snapshot(day(1), listOf("Call Mom", "Buy milk")),
                Snapshot(day(4), listOf("Call Mom +Family due:2026-11-20", "Buy milk")),
            ),
        )
        assertEquals(TaskDates(added = day(1)), dates["Call Mom +Family due:2026-11-20"])
    }

    @Test
    fun aTaskThatReplacesSeveralOthersIsNew() {
        val dates = taskDates(
            listOf(
                Snapshot(day(1), listOf("Call Mom", "Buy milk")),
                Snapshot(day(4), listOf("Clean up")),
            ),
        )
        assertEquals(TaskDates(added = day(4)), dates["Clean up"])
    }

    @Test
    fun anEmptyHistoryKnowsNothing() {
        assertEquals(emptyMap(), taskDates(emptyList()))
    }
}
