package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// [utest->dsn~recurrence-syntax~1]
class RecurrenceTest {
    @Test
    fun parsesIntervals() {
        assertEquals(Recurrence(1, Recurrence.Unit.MONTH), Recurrence.parse("1m"))
        assertEquals(Recurrence(2, Recurrence.Unit.WEEK, strict = true), Recurrence.parse("+2w"))
        assertEquals("+2w", Recurrence.parse("+2w").toString())
    }

    @Test
    fun rejectsWhatIsNotAnInterval() {
        assertNull(Recurrence.parse("monthly"))
        assertNull(Recurrence.parse("0d"))
        assertNull(Recurrence.parse("1h"))
    }

    @Test
    fun computesTheNextDate() {
        val friday = LocalDate(2026, 11, 13)
        assertEquals(LocalDate(2026, 11, 14), Recurrence.parse("1d")!!.next(friday))
        assertEquals(LocalDate(2026, 11, 16), Recurrence.parse("1b")!!.next(friday))
        assertEquals(LocalDate(2026, 11, 27), Recurrence.parse("2w")!!.next(friday))
        assertEquals(LocalDate(2027, 2, 28), Recurrence.parse("1m")!!.next(LocalDate(2027, 1, 31)))
        assertEquals(LocalDate(2027, 11, 13), Recurrence.parse("1y")!!.next(friday))
    }
}
