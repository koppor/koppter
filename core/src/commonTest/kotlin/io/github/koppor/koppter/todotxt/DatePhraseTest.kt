package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

// [utest->dsn~quick-add-dates~1]
class DatePhraseTest {
    private val friday = LocalDate(2026, 11, 13)

    private fun typed(line: String) = Task.parse(line).withDates(friday).toLine()

    @Test
    fun aDayOrAPeriodAtTheEndOfTheTextBecomesDates() {
        mapOf(
            "today" to "due:2026-11-13",
            "tomorrow" to "due:2026-11-14",
            "this week" to "due:2026-11-15",
            "next week" to "t:2026-11-16 due:2026-11-22",
            "This Month" to "due:2026-11-30",
            "next month" to "t:2026-12-01 due:2026-12-31",
            "weekend" to "t:2026-11-14 due:2026-11-15",
            "Wed" to "t:2026-11-18 due:2026-11-18",
            "on wednesday" to "t:2026-11-18 due:2026-11-18",
            "Fri" to "t:2026-11-13 due:2026-11-13",
            "by Wed" to "due:2026-11-18",
            "on Sep 15" to "t:2027-09-15 due:2027-09-15",
            "Sept 15" to "due:2027-09-15",
            "24 Dec" to "due:2026-12-24",
            "due by Dec 15th" to "due:2026-12-15",
            "due 2026-12-15" to "due:2026-12-15",
            "the week of Dec 9" to "t:2026-12-07 due:2026-12-13",
        ).forEach { (phrase, tags) -> assertEquals("Write the report $tags", typed("Write the report $phrase"), phrase) }
    }

    @Test
    fun aStartAndARangeBecomeTheThresholdDate() {
        mapOf(
            "in 4 days" to "t:2026-11-17",
            "start in two weeks" to "t:2026-11-27",
            "begin in a month" to "t:2026-12-13",
            "no earlier than Jan 18" to "t:2027-01-18",
            "delay until Jan 18" to "t:2027-01-18",
            "start on Jan 18" to "t:2027-01-18",
            "from Jan 18" to "t:2027-01-18",
            "from tomorrow to Wed" to "t:2026-11-14 due:2026-11-18",
            "from Dec 30 to Jan 2" to "t:2026-12-30 due:2027-01-02",
        ).forEach { (phrase, tags) -> assertEquals("Write the report $tags", typed("Write the report $phrase"), phrase) }
    }

    @Test
    fun aTimeBecomesTheFixedTimeOrTheDueTime() {
        mapOf(
            "tomorrow 3pm" to "due:2026-11-14 at:15:00",
            "15 Jul 14:00" to "due:2027-07-15 at:14:00",
            "on Wed at 9:30am" to "due:2026-11-18 at:9:30",
            "today 12 pm" to "due:2026-11-13 at:12:00",
            "at 9:30" to "at:9:30",
            "by Mon 14:00" to "due:2026-11-16 by:14:00",
        ).forEach { (phrase, tags) -> assertEquals("Dentist $tags", typed("Dentist $phrase"), phrase) }
    }

    @Test
    fun labelsMayFollowAndTypedDatesStayAndOtherWordsStayText() {
        assertEquals("(B) Buy milk +Home @shop dur:10m due:2026-11-14", typed("(B) Buy milk tomorrow +Home @shop dur:10m"))
        assertEquals("Call due:2026-11-18 t:2026-11-16", typed("Call due:2026-11-18 next week"))
        listOf(
            "Plan next week with Ana",
            "Buy tickets for the weekend.",
            "Call on Feb 30",
            "Meet at 25:00",
            "tomorrow",
            "Read chapter 15",
        ).forEach { assertEquals(it, typed(it)) }
    }
}
