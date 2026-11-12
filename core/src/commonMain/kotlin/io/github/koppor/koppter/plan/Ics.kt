package io.github.koppor.koppter.plan

import kotlinx.datetime.LocalDate

/**
 * The plan as an iCalendar file, for calendar programs that import one.
 *
 * The blocks are tentative, and their times are local without a time zone: the calendar that imports them shows them at these times.
 */
// [impl->dsn~ics-export~1]
object Ics {
    const val NAME = "calendar.ics"

    fun write(blocks: List<Block>, today: LocalDate): String = buildString {
        fun line(text: String) {
            // A line has at most 75 octets; longer ones continue on the next line after a blank.
            // ponytail: cut by characters with a margin for characters of several octets, not by octets.
            append(text.chunked(60).joinToString("\r\n ")).append("\r\n")
        }
        line("BEGIN:VCALENDAR")
        line("VERSION:2.0")
        line("PRODID:-//koppter//plan//EN")
        for (block in blocks) {
            val text = CalendarTxt.text(block.entry.task)
            val day = block.date.toString().replace("-", "")
            line("BEGIN:VEVENT")
            // The same block has the same id in every export, so that a calendar that knows the id replaces the event.
            line("UID:$day-${block.span.start}-${text.hashCode().toUInt().toString(16)}@koppter")
            line("DTSTAMP:${today.toString().replace("-", "")}T000000Z")
            line("DTSTART:${day}T${time(block.span.start)}")
            line("DTEND:${day}T${time(block.span.end)}")
            line("SUMMARY:" + text.replace(Regex("[\\\\;,]")) { "\\" + it.value })
            line("STATUS:TENTATIVE")
            line("X-MICROSOFT-CDO-BUSYSTATUS:TENTATIVE")
            line("END:VEVENT")
        }
        line("END:VCALENDAR")
    }

    private fun time(minutes: Int) = "${(minutes / 60).toString().padStart(2, '0')}${(minutes % 60).toString().padStart(2, '0')}00"
}
