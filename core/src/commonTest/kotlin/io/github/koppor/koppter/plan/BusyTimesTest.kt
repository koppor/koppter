package io.github.koppor.koppter.plan

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

// [utest->dsn~busy-times~1]
class BusyTimesTest {
    private val berlin = TimeZone.of("Europe/Berlin")

    // A Thursday.
    private val from = LocalDate(2026, 11, 12)
    private val to = LocalDate(2026, 12, 31)

    private fun busy(vararg events: String): Map<String, List<String>> {
        val ics = "BEGIN:VCALENDAR\r\n" + events.joinToString("") { "BEGIN:VEVENT\r\n${it.trimIndent().replace("\n", "\r\n")}\r\nEND:VEVENT\r\n" } + "END:VCALENDAR\r\n"
        return busyTimes(ics, from, to, berlin).entries.sortedBy { it.key }.associate { (date, spans) ->
            date.toString() to spans.map { "${it.start / 60}:${(it.start % 60).toString().padStart(2, '0')}-${it.end / 60}:${(it.end % 60).toString().padStart(2, '0')}" }
        }
    }

    @Test
    fun eventsTakeTheirTimeInTheZoneOfTheUser() {
        assertEquals(
            mapOf(
                // UTC is an hour behind Berlin in winter; the zone of New York six hours; a zone without a known name is the own one.
                "2026-11-12" to listOf("9:00-10:00", "15:00-16:30"),
                "2026-11-13" to listOf("8:00-8:45", "14:00-15:00"),
                // Over midnight, and overlapping events joined.
                "2026-11-16" to listOf("10:00-12:00", "23:00-24:00"),
                "2026-11-17" to listOf("0:00-1:00"),
            ),
            busy(
                "DTSTART:20261112T080000Z\nDTEND:20261112T090000Z",
                "DTSTART;TZID=America/New_York:20261112T090000\nDTEND;TZID=America/New_York:20261112T103000",
                "DTSTART;TZID=W. Europe Standard Time:20261113T080000\nDURATION:PT45M",
                "DTSTART:20261113T140000\nDTEND:20261113T150000\nSUMMARY:A title that is\n  folded",
                "DTSTART:20261116T100000\nDTEND:20261116T113000",
                "DTSTART:20261116T110000\nDTEND:20261116T120000",
                "DTSTART:20261116T230000\nDTEND:20261117T010000",
            ),
        )
    }

    @Test
    fun eventsThatTakeNoTimeAreLeftOut() {
        assertEquals(
            emptyMap(),
            busy(
                "DTSTART;VALUE=DATE:20261112\nDTEND;VALUE=DATE:20261113",
                "DTSTART:20261112T080000Z\nDTEND:20261112T090000Z\nSTATUS:CANCELLED",
                "DTSTART:20261112T080000Z\nDTEND:20261112T090000Z\nTRANSP:TRANSPARENT",
                "DTSTART:20261112T080000Z\nDTEND:20261112T090000Z\nX-MICROSOFT-CDO-BUSYSTATUS:FREE",
                // A block of the own plan that was imported into the calendar.
                "UID:20261112-480-1f@koppter\nDTSTART:20261112T080000\nDTEND:20261112T090000",
                "DTSTART:20261111T080000\nDTEND:20261111T090000",
                "DTSTART:20270104T080000\nDTEND:20270104T090000",
            ),
        )
    }

    @Test
    fun repeatingEventsAreFollowed() {
        val days = busy(
            // Every second week on Monday and Wednesday since October, without one Wednesday.
            "DTSTART:20261005T090000\nDTEND:20261005T100000\nRRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE;UNTIL=20261205T000000Z\nEXDATE:20261118T090000",
            // Daily, three times.
            "DTSTART:20261112T120000\nDTEND:20261112T123000\nRRULE:FREQ=DAILY;COUNT=3",
            // The last Friday and the second Tuesday of each month.
            "DTSTART:20260130T140000\nDTEND:20260130T150000\nRRULE:FREQ=MONTHLY;BYDAY=-1FR",
            "DTSTART:20260113T160000\nDTEND:20260113T170000\nRRULE:FREQ=MONTHLY;BYDAY=2TU",
            // On the 15th of every month, and every year on 24 December.
            "DTSTART:20260115T180000\nDTEND:20260115T190000\nRRULE:FREQ=MONTHLY",
            "DTSTART:20201224T200000\nDTEND:20201224T210000\nRRULE:FREQ=YEARLY",
        )
        assertEquals(
            mapOf(
                "2026-11-12" to listOf("12:00-12:30"),
                "2026-11-13" to listOf("12:00-12:30"),
                "2026-11-14" to listOf("12:00-12:30"),
                "2026-11-15" to listOf("18:00-19:00"),
                "2026-11-16" to listOf("9:00-10:00"),
                "2026-11-27" to listOf("14:00-15:00"),
                "2026-11-30" to listOf("9:00-10:00"),
                "2026-12-02" to listOf("9:00-10:00"),
                "2026-12-08" to listOf("16:00-17:00"),
                "2026-12-15" to listOf("18:00-19:00"),
                "2026-12-24" to listOf("20:00-21:00"),
                "2026-12-25" to listOf("14:00-15:00"),
            ),
            days,
        )
    }
}
