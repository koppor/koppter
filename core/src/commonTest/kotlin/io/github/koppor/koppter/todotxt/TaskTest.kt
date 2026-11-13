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
        val task = Task.parse("Water due:2026-12-12 t:2026-11-19 +Garden @home the plants rec:m")
        assertEquals("Water the plants", task.description)
        assertEquals(listOf("Garden"), task.projects)
        assertEquals(listOf("home"), task.contexts)
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
    fun completingKeepsThePriorityAsTagAndReopeningRestoresIt() {
        val completed = Task.parse("(A) Call Mom +Family").complete(today, writeDate = false)
        assertEquals("x Call Mom +Family pri:A", completed.toLine())
        assertEquals("(A) Call Mom +Family", completed.reopen().toLine())
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

    // [utest->dsn~recurrence-on-completion~1]
    @Test
    fun aPlainRecurrenceCountsFromToday() {
        val task = Task.parse("(B) Water plants due:2026-11-01 rec:1w")
        assertEquals("(B) Water plants due:2026-11-19 rec:1w", task.nextOccurrence(today)!!.toLine())
    }

    // [utest->dsn~recurrence-on-completion~1]
    @Test
    fun aStrictRecurrenceCountsFromTheDueDate() {
        val task = Task.parse("Pay rent due:2026-11-01 rec:+1m")
        assertEquals("Pay rent due:2026-12-01 rec:+1m", task.nextOccurrence(today)!!.toLine())
    }

    // [utest->dsn~recurrence-on-completion~1]
    @Test
    fun theDistanceBetweenThresholdAndDueDateIsKept() {
        val task = Task.parse("Tax return t:2026-11-01 due:2026-11-11 rec:+1y")
        assertEquals("Tax return t:2027-11-01 due:2027-11-11 rec:+1y", task.nextOccurrence(today)!!.toLine())
        assertEquals("Check mail t:2026-11-13 rec:1d", Task.parse("Check mail t:2026-11-10 rec:1d").nextOccurrence(today)!!.toLine())
    }

    // [utest->dsn~recurrence-on-completion~1]
    @Test
    fun aRecurringTaskWithoutDatesGetsADueDateAndOthersDoNotRecur() {
        assertEquals("Stretch rec:1d due:2026-11-13", Task.parse("Stretch rec:1d").nextOccurrence(today)!!.toLine())
        assertNull(Task.parse("Call Mom").nextOccurrence(today))
    }

    // [utest->dsn~recurrence-on-completion~1]
    @Test
    fun theNextOccurrenceOfACompletedTaskIsOpenWithItsPriority() {
        val completed = Task.parse("(A) 2026-11-01 Water plants rec:1w").complete(today, writeDate = true)
        assertEquals("(A) Water plants rec:1w due:2026-11-19", completed.nextOccurrence(today)!!.toLine())
    }

    // [utest->dsn~hashtags~1]
    @Test
    fun hashtagsAreMetadataAndStartWithALetter() {
        val task = Task.parse("Fix #123 in C# for #release +App #Release #release #2go # x#y")

        assertEquals(listOf("release", "Release"), task.hashtags)
        assertEquals("Fix #123 in C# for #2go # x#y", task.description)
    }

    // [utest->dsn~hashtags~1]
    // [utest->dsn~canonical-order-on-edit~1]
    @Test
    fun editingWritesHashtagsBetweenContextsAndTags() {
        val task = Task.parse("Call #family Mom due:2026-11-20 @phone +Home")

        assertEquals("Call Mom +Home @phone #family #urgent due:2026-11-20", task.edit(hashtags = task.hashtags + "urgent").toLine())
        assertEquals("Call Mom +Home @phone due:2026-11-20", task.edit(hashtags = emptyList()).toLine())
        assertEquals(task, task.edit(hashtags = listOf("family")))
    }

    // [utest->dsn~duration~1]
    @Test
    fun theDurationIsReadFromItsTag() {
        assertEquals(90, Task.parse("Read the survey dur:1h30m").duration?.inWholeMinutes)
        assertEquals(45, Task.parse("Read the survey dur:45m").duration?.inWholeMinutes)
        assertEquals(120, Task.parse("Read the survey dur:2h").duration?.inWholeMinutes)
        for (none in listOf("Read the survey", "Read dur:long", "Read dur:0m", "Read dur:-1h", "Read dur:90")) {
            assertEquals(null, Task.parse(none).duration, none)
        }
    }

    // [utest->dsn~context-switcher-tag~1]
    @Test
    fun aLinkOfContextSwitcherBecomesATagThatGivesTheLinkToSwitch() {
        val task = Task.parse("(B) Review contextswitcher://task/jabref/fix-npe the comments +JabRef due:2026-11-20").withContextSwitcherTags()

        assertEquals("(B) Review the comments +JabRef due:2026-11-20 cs:jabref/fix-npe", task.toLine())
        assertEquals("Review the comments", task.description)
        assertEquals("contextswitcher://switch/jabref/fix-npe", Task.contextSwitcherLink(task.tag(Task.CONTEXT_SWITCHER)!!))
        assertEquals(task, task.withContextSwitcherTags())
        assertEquals(null, Task.parse("Review cs: https://example.org/x").withContextSwitcherTags().tag(Task.CONTEXT_SWITCHER))
        assertEquals("jabref/fix-npe", Task.contextSwitcherId(" contextswitcher://switch/jabref/fix-npe "))
        assertEquals("My%20Category/task", Task.contextSwitcherId("My Category/task"))
    }
}
