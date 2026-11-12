package io.github.koppor.koppter.plan

import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.TodoFile
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PlannerTest {
    // A Thursday.
    private val today = LocalDate(2026, 11, 12)
    private val friday = LocalDate(2026, 11, 13)
    private val saturday = LocalDate(2026, 11, 14)
    private val monday = LocalDate(2026, 11, 16)

    private val maps = TimeMaps(
        listOf(
            "General" to "Mon-Fri 08:00-12:00 13:00-18:00",
            "Morning-Focus" to "General 07:00-11:00",
            "Evenings" to "Mon-Thu 19:00-21:00, Sat 10:00-12:00",
        ),
    )

    private fun span(from: String, to: String) = Span(minutes(from), minutes(to))

    private fun minutes(time: String) = time.substringBefore(':').toInt() * 60 + time.substringAfter(':').toInt()

    // [utest->dsn~time-maps~1]
    @Test
    fun aTimeMapNamesTheTimesOfTheWeekAndIsRestrictedToTheMapItNames() {
        assertEquals(listOf(span("08:00", "12:00"), span("13:00", "18:00")), maps.spans("General", today))
        assertEquals(emptyList(), maps.spans("General", saturday))
        // The focus time starts at seven, but the general map starts at eight.
        assertEquals(listOf(span("08:00", "11:00")), maps.spans("Morning-Focus", today))
        assertEquals(listOf(span("19:00", "21:00")), maps.spans("Evenings", today))
        assertEquals(emptyList(), maps.spans("Evenings", friday))
        assertEquals(listOf(span("10:00", "12:00")), maps.spans("Evenings", saturday))
        // A task without map and a map that does not exist use the first map.
        assertEquals(maps.spans("General", today), maps.spans(null, today))
        assertEquals(maps.spans("General", today), maps.spans("Gone", today))
        assertEquals(emptyList(), maps.problems)
    }

    // [utest->dsn~time-maps~1]
    @Test
    fun withoutMapsTheTimesAreOfficeHoursAndWhatIsNotUnderstoodIsReported() {
        val none = TimeMaps(emptyList())
        assertEquals(listOf(span("09:00", "17:00")), none.spans(null, today))
        assertEquals(emptyList(), none.spans(null, saturday))

        val odd = TimeMaps(listOf("Weekend" to "Sat-Sun", "Odd" to "Mon 8-12 Funday", "Night" to "Fri-Mon 22:00-24:00"))
        assertEquals(listOf(Span(0, 24 * 60)), odd.spans("Weekend", saturday))
        assertEquals(listOf("Odd: \"8-12\" is neither days nor times", "Odd: \"Funday\" is neither days nor times"), odd.problems)
        // A range of days may run over the end of the week.
        assertEquals(listOf(span("22:00", "24:00")), odd.spans("Night", saturday))
        assertEquals(emptyList(), odd.spans("Night", today))
    }

    private fun plan(todo: String, config: PlanConfig = PlanConfig(maps), busy: Map<LocalDate, List<Span>> = emptyMap(), now: String = "07:00") =
        plan(TodoFile.parse(todo.trimIndent()).entries, config, busy, today, minutes(now))

    private fun Plan.lines() = blocks.map { "${it.date} ${it.span.start / 60}:${(it.span.start % 60).toString().padStart(2, '0')} ${it.span.length} ${it.entry.task.description}" }

    // [utest->dsn~planner~1]
    @Test
    fun tasksGetTheEarliestFreeTimesInTheOrderOfTheirPriority() {
        val plan = plan(
            """
            Write the report dur:2h
            (A) Call the landlord dur:30m
            x Done dur:1h
            Answer the mail
            Review the paper dur:1h t:2026-11-16
            """,
            now = "09:10",
        )
        assertEquals(
            listOf(
                // Not before the time of today that has passed, and on the quarter of an hour; priority A first, then the order of the file.
                "2026-11-12 9:15 30 Call the landlord",
                "2026-11-12 9:45 120 Write the report",
                // A task without duration takes half an hour; the 15 minutes before noon are too short for it.
                "2026-11-12 13:00 30 Answer the mail",
                // Not before its threshold date.
                "2026-11-16 8:00 60 Review the paper",
            ),
            plan.lines(),
        )
        assertEquals(emptyList(), plan.unplaced)
    }

    // [utest->dsn~planner~1]
    @Test
    fun aTaskIsPlannedInTheMapOfItsContextAroundTakenTimesAndCutIfNeeded() {
        val config = PlanConfig(maps, contextMaps = mapOf("research" to "Morning-Focus", "home" to "Evenings"))
        val plan = plan(
            """
            Read the survey @research>reading dur:4h
            Water the plants @home dur:30m
            Sort the mail dur:1h
            """,
            config,
            busy = mapOf(today to listOf(span("09:00", "10:00"))),
        )
        assertEquals(
            listOf(
                // The context below research has its map; a meeting takes an hour, and the rest of the task moves to the next mornings.
                "2026-11-12 8:00 60 Read the survey",
                "2026-11-12 10:00 60 Read the survey",
                // The first free time of the general map; the focus time is taken.
                "2026-11-12 11:00 60 Sort the mail",
                "2026-11-12 19:00 30 Water the plants",
                "2026-11-13 8:00 120 Read the survey",
            ),
            plan.lines(),
        )
    }

    // [utest->dsn~planner~1]
    // [utest->dsn~plan-configuration~1]
    @Test
    fun aTaskThatDoesNotFitBeforeItsBarrierIsNotPlanned() {
        val config = PlanConfig(
            maps,
            barriers = listOf("default" to "year", "+Thesis" to "2026-11-13", "@errands" to "week", "+Odd" to "soon"),
        )
        val plan = plan(
            """
            Write the chapter +Thesis dur:20h
            Outline the talk +Thesis dur:1h
            Buy paint @errands>city dur:1h
            """,
            config,
        )
        // Two days hold 18 hours of the general map: the chapter does not fit and leaves the time to the others.
        assertEquals(listOf("Write the chapter"), plan.unplaced.map { it.task.description })
        assertEquals(listOf("2026-11-12 8:00 60 Outline the talk", "2026-11-12 9:00 60 Buy paint"), plan.lines())

        assertEquals(LocalDate(2026, 11, 13), config.barrierOf(Task.parse("Write +Thesis @errands"), today))
        assertEquals(LocalDate(2026, 11, 15), config.barrierOf(Task.parse("Buy paint @errands>city"), today))
        assertEquals(LocalDate(2026, 12, 31), config.barrierOf(Task.parse("Anything"), today))
        // Without a default, the barrier is the end of the quarter.
        assertEquals(LocalDate(2026, 12, 31), PlanConfig().barrierOf(Task.parse("Anything"), today))
        assertEquals(LocalDate(2026, 3, 31), PlanConfig().barrierOf(Task.parse("Anything"), LocalDate(2026, 2, 1)))
        assertEquals(LocalDate(2026, 11, 30), PlanConfig.barrierDate("month", today))
        assertEquals(listOf("+Odd: \"soon\" is no barrier"), config.problems)
        assertEquals(listOf("@home: there is no time map \"Gone\""), PlanConfig(maps, mapOf("home" to "Gone")).problems)
    }

    // [utest->dsn~plan-limits~1]
    @Test
    fun aTaskIsNotCutIntoPartsShorterThanTheBlockOfItsProjectOrContext() {
        val config = PlanConfig(maps, limits = listOf("default" to "block 2h", "@chores" to "block 30m", "+Thesis" to "block 3h", "@odd" to "block soon, max 1h, 2h"))
        val plan = plan(
            """
            (A) Write the chapter +Thesis dur:7h
            (B) Sort the mail dur:3h
            (C) Tidy the desk @chores>office dur:1h
            (D) Think dur:3h
            """,
            config,
            // Free today: 08:00-09:30, 10:00-12:00 and the afternoon.
            busy = mapOf(today to listOf(span("09:30", "10:00"))),
        )
        assertEquals(
            listOf(
                "2026-11-12 8:00 60 Tidy the desk",
                // Three hours at a stretch are free in the afternoon only; four of the five hours, so that a block of three is left.
                "2026-11-12 13:00 240 Write the chapter",
                "2026-11-13 8:00 180 Write the chapter",
                // Without a block of its own, the default one of two hours: three hours are not cut into two and one.
                "2026-11-13 13:00 180 Sort the mail",
                "2026-11-16 8:00 180 Think",
            ),
            plan.lines(),
        )
        assertEquals(60, PlanConfig().blockOf(Task.parse("Anything")))
        // Of several blocks, the longest counts.
        assertEquals(180, config.blockOf(Task.parse("Write +Thesis @chores")))
        assertEquals(listOf("@odd: \"block soon\" is no limit", "@odd: \"max 1h\" is no limit", "@odd: \"2h\" is no limit"), config.problems)
    }

    // [utest->dsn~planner~1]
    @Test
    fun afterAMeetingABlockStartsOnTheQuarterOfAnHourAndAfterABlockAtOnce() {
        val plan = plan(
            """
            (A) Call the landlord dur:50m
            (B) Answer the mail dur:20m
            """,
            busy = mapOf(today to listOf(span("08:00", "09:55"))),
        )
        assertEquals(listOf("2026-11-12 10:00 50 Call the landlord", "2026-11-12 10:50 20 Answer the mail"), plan.lines())
    }

    // [utest->dsn~plan-limits~1]
    @Test
    fun notMoreThanTheBudgetOfADayAndAWeekIsPlanned() {
        val config = PlanConfig(maps, limits = listOf("@chores" to "max 90m/day, max 2h/week", "default" to "max 4h/day"))
        val plan = plan(
            """
            (A) Clean the bike @chores dur:3h
            (B) Sort the mail @chores>office dur:2h
            (C) Write the report dur:6h
            """,
            config,
        )
        assertEquals(
            listOf(
                // An hour and a half of chores a day.
                "2026-11-12 8:00 90 Clean the bike",
                // Four hours a day in all.
                "2026-11-12 9:30 150 Write the report",
                // Two hours of chores a week: the half hour that is left of them on Friday is shorter than a block.
                "2026-11-13 8:00 210 Write the report",
                "2026-11-16 8:00 90 Clean the bike",
                // The context below counts as well, so the mail waits for the week after; an hour each day, as half an hour would be left otherwise.
                "2026-11-23 8:00 60 Sort the mail",
                "2026-11-24 8:00 60 Sort the mail",
            ),
            plan.lines(),
        )
    }

    // [utest->dsn~plan-limits~1]
    @Test
    fun theMinimumOfAWeekIsPlannedBeforeTasksWithAHigherPriority() {
        val todo = """
            (A) Write the report dur:20h
            (B) Prepare the course dur:20h
            Outline the paper @paper>first dur:3h
        """
        // Without a minimum, the paper waits until the others are planned: 18 hours fit into the first week.
        assertEquals("2026-11-18", plan(todo, PlanConfig(maps, barriers = listOf("default" to "year"))).blocks.first { it.entry.task.description == "Outline the paper" }.date.toString())

        val plan = plan(todo, PlanConfig(maps, barriers = listOf("default" to "year"), limits = listOf("@paper" to "min 1h/week")))
        assertEquals(
            // An hour in each week, up to the week in which the paper would have been done anyway; there, it is done.
            listOf("2026-11-12 8:00 60 Outline the paper", "2026-11-16 8:00 60 Outline the paper", "2026-11-18 15:00 60 Outline the paper"),
            plan.lines().filter { it.endsWith("Outline the paper") },
        )
        assertEquals(emptyList(), plan.unplaced)
    }

    // [utest->dsn~dependencies~1]
    @Test
    fun aTaskIsPlannedAfterTheTasksItWaitsFor() {
        val plan = plan(
            """
            (A) Send the paper id:1 dur:30m
            Write the paper p:1 dur:2h id:2
            Collect the data p:2 dur:1h
            x Find the venue p:1
            (B) Call the landlord dur:1h
            Odd id:7 p:8
            Odder id:8 p:7
            """,
        )
        assertEquals(
            listOf(
                // The tasks that the first one waits for come with it, in the order of their dependencies; a completed one is not waited for.
                "2026-11-12 8:00 60 Collect the data",
                "2026-11-12 9:00 120 Write the paper",
                "2026-11-12 11:00 30 Send the paper",
                // Tasks that wait for each other are planned all the same; the half hour before noon is too short for the call.
                "2026-11-12 11:30 30 Odder",
                "2026-11-12 13:00 60 Call the landlord",
                "2026-11-12 14:00 30 Odd",
            ),
            plan.lines(),
        )
        assertEquals(listOf("\"Odder\" and \"Odd\" wait for each other"), plan.problems)
    }

    // [utest->dsn~planner~1]
    @Test
    fun aTaskWhoseThresholdLiesBehindItsBarrierIsLeftOutWithTheTasksThatWaitForIt() {
        val plan = plan(
            """
            Learn to sail t:2027-06-01 p:1
            Buy a boat id:1
            Call the landlord t:2026-11-16
            """,
        )
        assertEquals(listOf("2026-11-16 8:00 30 Call the landlord"), plan.lines())
        assertEquals(emptyList(), plan.unplaced)
    }

    // [utest->dsn~dependencies~1]
    @Test
    fun aTaskThatWaitsForATaskThatDoesNotFitIsNotPlannedEither() {
        val plan = plan(
            """
            Send the thesis id:1 dur:30m
            Write the thesis p:1 dur:200h +Thesis
            Review the thesis p:1 dur:1h t:2026-11-16
            """,
            PlanConfig(maps, barriers = listOf("+Thesis" to "2026-11-20")),
        )
        assertEquals(listOf("Write the thesis", "Send the thesis"), plan.unplaced.map { it.task.description })
        assertEquals(listOf("2026-11-16 8:00 60 Review the thesis"), plan.lines())
    }

    // [utest->dsn~calendar-txt~1]
    @Test
    fun thePlanIsWrittenAsOneLinePerDayAndThePastStays() {
        val plan = plan(
            """
            (A) Call the landlord @phone dur:30m due:2026-11-12
            Read the survey. Twice +Reading @research>reading dur:1h due:2026-11-20
            Review the paper dur:1h t:2026-11-16
            """,
        )
        val existing = "2026  The year of the thesis\n2026-11-11 w46 Wed  09:00-10:00 Old block\n2026-11-12 w46 Thu  08:00-09:00 Replaced\n2026-11-20 w47 Fri  08:00-09:00 Replaced too\n"
        val calendar = CalendarTxt.write(existing, plan.blocks, today)
        assertEquals(
            """
            2026  The year of the thesis
            2026-11-11 w46 Wed  09:00-10:00 Old block
            2026-11-12 w46 Thu  08:00-08:30 Call the landlord @phone. 08:30-09:30 Read the survey. Twice +Reading @research>reading
            2026-11-13 w46 Fri
            2026-11-14 w46 Sat
            2026-11-15 w46 Sun
            2026-11-16 w47 Mon  08:00-09:00 Review the paper
            """.trimIndent() + "\n",
            calendar,
        )
        // Writing the same plan again changes nothing.
        assertEquals(calendar, CalendarTxt.write(calendar, plan.blocks, today))

        val planned = CalendarTxt.planned(calendar, today)
        assertEquals(setOf("Call the landlord @phone", "Read the survey. Twice +Reading @research>reading", "Review the paper"), planned.keys)
        assertEquals(Planned(monday, 8 * 60, monday), planned["Review the paper"])
        assertEquals("Mon 2026-11-16 08:00", CalendarTxt.startText(planned.getValue("Review the paper")))

        // The blocks of all days, for the views of the plan.
        assertEquals(
            listOf("2026-11-11 09:00-10:00 Old block", "2026-11-12 08:00-08:30 Call the landlord @phone"),
            CalendarTxt.blocks(calendar).take(2).map { "${it.date} ${CalendarTxt.timesText(it.span)} ${it.text}" },
        )
        assertEquals("Mon 2026-11-16", CalendarTxt.dayText(monday))

        val landlord = Task.parse("(A) Call the landlord @phone dur:30m due:2026-11-12")
        assertEquals(PlanState.TIGHT, CalendarTxt.state(landlord, planned[CalendarTxt.text(landlord)]))
        assertEquals(PlanState.FITS, CalendarTxt.state(Task.parse("Review the paper"), planned["Review the paper"]))
        assertEquals(PlanState.LATE, CalendarTxt.state(Task.parse("Review the paper due:2026-11-13"), planned["Review the paper"]))
        assertEquals(PlanState.LATE, CalendarTxt.state(Task.parse("New task"), planned["New task"]))

        assertEquals(53, CalendarTxt.week(LocalDate(2027, 1, 1)))
        assertEquals(1, CalendarTxt.week(LocalDate(2024, 12, 30)))
    }

    // [utest->dsn~ics-export~1]
    @Test
    fun thePlanIsExportedAsTentativeEventsWithLocalTimes() {
        val blocks = plan("Read the survey; twice, with a title that is long enough to need a second line in the file @research dur:1h").blocks
        val ics = Ics.write(blocks, today)

        assertEquals(
            listOf(
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:-//koppter//plan//EN",
                "BEGIN:VEVENT",
                "UID:20261112-480-${"Read the survey; twice, with a title that is long enough to need a second line in the file @research".hashCode().toUInt().toString(16)}@koppter",
                "DTSTAMP:20261112T000000Z",
                "DTSTART:20261112T080000",
                "DTEND:20261112T090000",
                "SUMMARY:Read the survey\\; twice\\, with a title that is long ",
                " enough to need a second line in the file @research",
                "STATUS:TENTATIVE",
                "X-MICROSOFT-CDO-BUSYSTATUS:TENTATIVE",
                "END:VEVENT",
                "END:VCALENDAR",
                "",
            ),
            ics.split("\r\n"),
        )
        // The same plan gives the same file.
        assertEquals(ics, Ics.write(blocks, today))
    }
}
