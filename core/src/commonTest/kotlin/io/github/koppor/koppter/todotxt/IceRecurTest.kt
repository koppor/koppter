package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IceRecurTest {
    private fun days(line: String, year: Int, month: Int): List<Int> {
        val entry = IceRecurEntry.parse(line)!!
        return (1..31).filter { day -> runCatching { LocalDate(year, month, day) }.getOrNull()?.let(entry::occursOn) ?: false }
    }

    // [utest->dsn~ice-recur-schedule~1]
    @Test
    fun dailySchedules() {
        assertEquals((1..30).toList(), days("daily - (A) Every day", 2026, 11))
        assertEquals(listOf(10, 12, 14, 16, 18, 20), days("@2026-11-10 @@2026-11-20 daily 2 - Every other day", 2026, 11))
    }

    // [utest->dsn~ice-recur-schedule~1]
    @Test
    fun weeklySchedules() {
        // November 2026 starts on a Sunday.
        assertEquals(listOf(1, 6, 8, 13, 15, 20, 22, 27, 29), days("weekly ; day Friday, Sunday - Twice a week", 2026, 11))
        assertEquals(listOf(3, 5, 10, 12, 17, 19, 24, 26), days("weekly ; day 2, 4 - Tuesdays and Thursdays", 2026, 11))
        assertEquals(listOf(5, 19), days("@2026-11-02 Weekly 2 ; day Thursday - Every second Thursday", 2026, 11))
        assertEquals(listOf(4, 11, 18, 25), days("@2026-11-04 weekly - On the weekday of the start", 2026, 11))
    }

    // [utest->dsn~ice-recur-schedule~1]
    @Test
    fun monthlySchedules() {
        assertEquals(listOf(11, 13), days("monthly ; day_of_month 11, 13 - Twice a month", 2026, 11))
        assertEquals(listOf(1, 28), days("monthly ; day_of_month 1, -3 - First and third to last", 2026, 11))
        assertEquals(listOf(3, 24), days("monthly ; day_of_week Tuesday => 1, -1 - First and last Tuesday", 2026, 11))
        assertEquals(emptyList(), days("@2026-10-01 monthly 2 ; day_of_month 15 - Every second month", 2026, 11))
        assertEquals(listOf(15), days("@2026-10-01 monthly 2 ; day_of_month 15 - Every second month", 2026, 12))
        assertEquals(listOf(1), days("@2026-03-01 Monthly 3 - Every quarter", 2026, 12))
    }

    // [utest->dsn~ice-recur-schedule~1]
    @Test
    fun yearlySchedules() {
        assertEquals(listOf(4), days("@2016-01-04 Yearly - Anniversary", 2027, 1))
        assertEquals(listOf(16), days("yearly ; day_of_year 16 - Sixteenth day", 2026, 1))
        assertEquals(listOf(31), days("yearly 4 ; day_of_year -1 - Last day of every fourth year", 2026, 12))
        assertEquals(listOf(1), days("yearly ; month_of_year March, April - Spring", 2026, 4))
        assertEquals(emptyList(), days("yearly ; month_of_year March, April - Spring", 2026, 5))
    }

    // [utest->dsn~ice-recur-schedule~1]
    @Test
    fun linesThatAreNotSchedulesAreSkipped() {
        assertNull(IceRecurEntry.parse(""))
        assertNull(IceRecurEntry.parse("# comment"))
        assertNull(IceRecurEntry.parse("hourly - Too often"))
        assertNull(IceRecurEntry.parse("weekly ; day Someday - Never"))
        assertNull(IceRecurEntry.parse("daily without separator"))
    }

    // [utest->dsn~ice-recur-run~1]
    @Test
    fun addsTheTasksOfTodayThatAreNotOpenAlready() {
        val schedule = """
            daily - (A) Check mail
            weekly ; day Thursday - Water plants +home
            weekly ; day Friday - Take out trash
        """.trimIndent()
        val thursday = LocalDate(2026, 11, 12)
        val existing = listOf(Task.parse("(A) 2026-11-11 Check mail"), Task.parse("x Water plants +home"))
        assertEquals(listOf(thursday to "Water plants +home"), iceRecurTasks(schedule, thursday..thursday, existing).map { (day, task) -> day to task.toLine() })
    }

    // [utest->dsn~ice-recur-run~1]
    @Test
    fun daysWithoutARunAreCaughtUpAndEachTaskIsAddedOnce() {
        val schedule = """
            daily - Check mail
            weekly ; day Monday - (B) Plan the week
            weekly ; day Saturday - Clean the kitchen
        """.trimIndent()
        val friday = LocalDate(2026, 11, 6)
        val thursday = LocalDate(2026, 11, 12)

        assertEquals(
            listOf(friday to "Check mail", LocalDate(2026, 11, 7) to "Clean the kitchen", LocalDate(2026, 11, 9) to "(B) Plan the week"),
            iceRecurTasks(schedule, friday..thursday, emptyList()).map { (day, task) -> day to task.toLine() },
        )
    }

    // [utest->dsn~ice-recur-run~1]
    @Test
    fun aTaskThatDiffersInPriorityDatesOrChangingTagsExistsAlready() {
        val today = LocalDate(2026, 11, 12)
        fun added(entry: String, existing: String) = iceRecurTasks("daily - $entry", today..today, listOf(Task.parse(existing))).size

        assertEquals(0, added("(B) Pay the rent +Home", "(A) 2026-11-01 Pay the rent +Home due:2026-11-15 t:2026-11-10 note:abc.txt"))
        // Projects and contexts are part of the text.
        assertEquals(1, added("Pay the rent +Home", "Pay the rent"))
        assertEquals(1, added("Pay the rent", "Pay the rent rec:1m"))
    }
}
