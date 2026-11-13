package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

// [utest->dsn~overview~1]
class UrgencyTest {
    private val today = LocalDate(2026, 11, 12)

    @Test
    fun theUrgencyTellsTheDaysLeftAndThePartOfTheTimeThatHasPassed() {
        val half = Task.parse("2026-11-02 Vacuum due:2026-11-22").urgency(today)!!
        assertEquals(Urgency(daysLeft = 10, elapsed = 0.5f), half)
        assertEquals("Due in 10 days", half.text)
        assertEquals(UrgencyState.FINE, half.state)

        // The threshold date is the start if the task has one; without a start, the task starts two weeks before it is due.
        assertEquals(0.5f, Task.parse("2026-01-01 Vacuum t:2026-11-10 due:2026-11-14").urgency(today)!!.elapsed)
        assertEquals(0.5f, Task.parse("Vacuum due:2026-11-19").urgency(today)!!.elapsed)

        val overdue = Task.parse("Vacuum due:2026-11-07").urgency(today)!!
        assertEquals(Urgency(daysLeft = -5, elapsed = 1f), overdue)
        assertEquals("5 days overdue", overdue.text)
        assertEquals(UrgencyState.OVERDUE, overdue.state)
        assertEquals("Due today", Task.parse("Vacuum due:2026-11-12").urgency(today)!!.text)

        assertEquals(null, Task.parse("Vacuum").urgency(today))
        assertEquals(null, Task.parse("x Vacuum due:2026-11-07").urgency(today))
    }

    @Test
    fun aContextOfTheOutermostLevelHasTheStateOfItsMostUrgentTask() {
        val file = TodoFile.parse(
            """
            Read the survey @research>reading due:2026-11-30
            Review a paper @research due:2026-11-10
            (A) Plan the experiment @research
            Call the landlord @phone due:2026-11-12
            Water the plants @home
            x Sweep the floor @home due:2026-11-01
            Buy a present due:2026-12-24
            """.trimIndent(),
        )
        val states = contextStates(file.entries, today)

        assertEquals(listOf("home", "phone", "research", null), states.map { it.context })
        assertEquals(listOf(UrgencyState.FINE, UrgencyState.DUE, UrgencyState.OVERDUE, UrgencyState.FINE), states.map { it.state })
        // The most urgent task first, a task without due date last.
        assertEquals(listOf("Review a paper", "Read the survey", "Plan the experiment"), states[2].entries.map { it.task.description })
        assertEquals(1, states[2].overdue)
    }
}
