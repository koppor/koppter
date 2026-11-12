package io.github.koppor.koppter.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import io.github.koppor.koppter.storage.Appearance
import io.github.koppor.koppter.storage.ColorTheme
import io.github.koppor.koppter.storage.CompanionFiles
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.SyncState
import io.github.koppor.koppter.storage.SyncStatus
import io.github.koppor.koppter.storage.SyncingStorage
import io.github.koppor.koppter.sync.TaskDates
import io.github.koppor.koppter.storage.TodoStorage
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

private class MemoryStorage(var text: String) : TodoStorage, CompanionFiles {
    val companions = mutableMapOf<String, String>()
    val changes = mutableListOf<String>()
    override fun readCompanion(path: String) = companions[path]
    override fun writeCompanion(path: String, text: String) {
        companions[path] = text
    }

    override fun read() = text
    override fun write(text: String, change: String) {
        this.text = text
        changes += change
    }
    override fun version(): Any? = text
}

@OptIn(ExperimentalTestApi::class)
class AppTest {
    private val storage = MemoryStorage(
        """
        (B) Schedule pickup +GarageSale @phone
        Post signs +GarageSale
        (A) Thank Mom @phone due:2026-11-20
        x Call Mom +Family @phone
        """.trimIndent() + "\n",
    )

    private fun ComposeUiTest.start(settings: Settings = Settings()) {
        val controller = TodoController(settings, { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }
    }

    private fun ComposeUiTest.openTask(description: String) {
        onNodeWithText(description).performClick()
        onNodeWithTag("edit-dialog").assertExists()
    }

    // [utest->dsn~visible-tasks~1]
    // [utest->dsn~task-card~1]
    @Test
    fun showsOpenTasksWithDescriptionAndLabels() = runComposeUiTest {
        start()
        onAllNodesWithTag("task").assertCountEquals(3)
        onNodeWithText("Thank Mom").assertExists()
        onNodeWithText("Call Mom").assertDoesNotExist()
        // The description carries no metadata; the metadata is in the labels.
        onNodeWithText("Schedule pickup").assertExists()
        // A card merges the semantics of its content; the labels are found in the unmerged tree only.
        onAllNodes(hasText("+GarageSale") and hasAnyAncestor(hasTestTag("task-labels")), useUnmergedTree = true).assertCountEquals(2)
        onNode(hasText("2026-11-20") and hasAnyAncestor(hasTestTag("task-labels")), useUnmergedTree = true).assertExists()
    }

    // [utest->dsn~quick-add~1]
    @Test
    fun quickAddAppendsTheTypedLine() = runComposeUiTest {
        start()
        onNodeWithTag("quick-add").performTextInput("(C) Buy milk @shop")
        onNodeWithTag("quick-add").performKeyInput { pressKey(Key.Enter) }
        assertEquals("(C) Buy milk @shop", storage.text.trim().lines().last())
        onNodeWithText("Buy milk").assertExists()
    }

    // [utest->dsn~task-operations~1]
    @Test
    fun completingWritesNoDateByDefault() = runComposeUiTest {
        start()
        onAllNodesWithTag("task-done")[0].performClick()
        assertEquals("x Thank Mom @phone due:2026-11-20 pri:A", storage.text.lines()[2])
        onAllNodesWithTag("task").assertCountEquals(2)
    }

    // [utest->dsn~task-operations~1]
    @Test
    fun addingAndCompletingWriteDatesWhenSwitchedOn() = runComposeUiTest {
        start(Settings(writeDates = true))
        onNodeWithTag("quick-add").performTextInput("Buy milk")
        onNodeWithTag("quick-add").performKeyInput { pressKey(Key.Enter) }
        assertEquals("2026-11-12 Buy milk", storage.text.trim().lines().last())
        onAllNodesWithTag("task-done")[0].performClick()
        assertEquals("x 2026-11-12 Thank Mom @phone due:2026-11-20 pri:A", storage.text.lines()[2])
    }

    // [utest->dsn~edit-dialog~1]
    @Test
    fun theDialogShowsTheDescriptionWithoutMetadataAndTheResultingLine() = runComposeUiTest {
        start()
        openTask("Thank Mom")
        onNodeWithTag("edit-description").assertTextEquals("Description", "Thank Mom")
        onNodeWithTag("edit-line").assertTextEquals("(A) Thank Mom @phone due:2026-11-20")

        onNodeWithTag("edit-description").performTextReplacement("Thank Mom for the meatballs")
        onNodeWithTag("edit-priority-B").performClick()
        onNodeWithTag("edit-project-suggest-Family").performClick()
        onNodeWithTag("edit-line").assertTextEquals("(B) Thank Mom for the meatballs +Family @phone due:2026-11-20")

        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("(B) Thank Mom for the meatballs +Family @phone due:2026-11-20", storage.text.lines()[2])
        onNodeWithTag("edit-dialog").assertDoesNotExist()
    }

    // [utest->dsn~edit-dialog~1]
    @Test
    fun savingWithoutChangeLeavesTheFileUntouched() = runComposeUiTest {
        storage.text = "Call  +Family Mom   @phone\n"
        start()
        openTask("Call Mom")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Call  +Family Mom   @phone\n", storage.text)
    }

    // [utest->dsn~edit-dialog~1]
    @Test
    fun anInvalidDateBlocksSaving() = runComposeUiTest {
        start()
        openTask("Post signs")
        onNodeWithTag("edit-due").performTextInput("tomorrow")
        onNodeWithTag("edit-save").assertIsNotEnabled()
        onNodeWithTag("edit-due").performTextReplacement("2026-11-30")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Post signs +GarageSale due:2026-11-30", storage.text.lines()[1])
    }

    // [utest->dsn~edit-dialog~1]
    @Test
    fun deletesATaskFromTheDialog() = runComposeUiTest {
        start()
        openTask("Post signs")
        onNodeWithTag("edit-delete").performScrollTo().performClick()
        assertEquals(3, storage.text.trim().lines().size)
        onNodeWithText("Post signs").assertDoesNotExist()
    }

    // [utest->dsn~edit-dialog~1]
    @Test
    fun theNewTaskButtonOpensAnEmptyDialog() = runComposeUiTest {
        start()
        onNodeWithTag("new-task").performClick()
        onNodeWithTag("edit-save").assertIsNotEnabled()
        onNodeWithTag("edit-description").performTextInput("Water plants")
        onNodeWithTag("edit-context-add").performTextInput("home ")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Water plants @home", storage.text.trim().lines().last())
    }

    // [utest->dsn~filter-panel~1]
    @Test
    fun theFilterPanelRestrictsTheList() = runComposeUiTest {
        start()
        onNodeWithTag("filter-project-GarageSale").assertIsOff().performClick()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithTag("filter-project-GarageSale").assertIsOn()
        onNodeWithTag("filter-priority-B").performClick()
        onAllNodesWithTag("task").assertCountEquals(1)
        // A click on a selected label takes it out of the filter again.
        onNodeWithTag("filter-priority-B").performClick()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithTag("filter-clear").performClick()
        onAllNodesWithTag("task").assertCountEquals(3)
        onNodeWithTag("filter-completed-Yes").performClick()
        onAllNodesWithTag("task").assertCountEquals(1)
        onNodeWithText("Call Mom").assertExists()
    }

    // [utest->dsn~reload-on-external-change~1]
    @Test
    fun aChangeByAnotherProgramIsPickedUp() = runComposeUiTest {
        start()
        storage.text += "Added elsewhere\n"
        mainClock.advanceTimeBy(2_500)
        onNodeWithText("Added elsewhere").assertExists()
    }

    // [utest->dsn~settings-dialog~1]
    @Test
    fun theSettingsDialogSwitchesTheWritingOfDatesOn() = runComposeUiTest {
        start()
        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-write-dates").performClick()
        onNodeWithTag("settings-save").performClick()
        onAllNodesWithTag("task-done")[0].performClick()
        assertEquals("x 2026-11-12 Thank Mom @phone due:2026-11-20 pri:A", storage.text.lines()[2])
    }

    // [utest->dsn~context-switcher-tag~1]
    @Test
    fun theLabelOfAContextSwitcherTaskSwitchesToItAndAPastedLinkBecomesTheTag() = runComposeUiTest {
        val opened = mutableListOf<String>()
        val links = object : UriHandler {
            override fun openUri(uri: String) {
                if ("missing" in uri) error("no handler")
                opened += uri
            }
        }
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { CompositionLocalProvider(LocalUriHandler provides links) { App(controller, "todo.txt") } }

        onNodeWithTag("quick-add").performTextInput("Review the comments contextswitcher://task/jabref/fix-npe +JabRef")
        onNodeWithTag("quick-add").performKeyInput { pressKey(Key.Enter) }
        assertEquals("Review the comments +JabRef cs:jabref/fix-npe", storage.text.trim().lines().last())

        onNodeWithTag("label-cs", useUnmergedTree = true).performClick()
        assertEquals(listOf("contextswitcher://switch/jabref/fix-npe"), opened)
        // The click on the label does not open the task.
        onNodeWithTag("edit-dialog").assertDoesNotExist()

        // The edit dialog has a field for the task; it takes the id or a link and removes the tag when emptied.
        openTask("Review the comments")
        onNodeWithTag("edit-cs").assertTextContains("jabref/fix-npe")
        onNodeWithTag("edit-cs").performTextReplacement("contextswitcher://task/jabref/review")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Review the comments +JabRef cs:jabref/review", storage.text.trim().lines().last())
        openTask("Review the comments")
        onNodeWithTag("edit-cs").performTextReplacement("")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Review the comments +JabRef", storage.text.trim().lines().last())
        openTask("Review the comments")
        onNodeWithTag("edit-cs").performTextReplacement("jabref/fix-npe")
        onNodeWithTag("edit-save").performScrollTo().performClick()

        controller.add("Call cs:missing")
        onAllNodesWithTag("label-cs", useUnmergedTree = true)[1].performClick()
        onNodeWithText("Not opened: contextswitcher://switch/missing").assertExists()
    }

    // [utest->dsn~color-themes~1]
    @Test
    fun theSettingsDialogChoosesThePaletteAndItsVariant() = runComposeUiTest {
        var saved = Settings()
        val controller = TodoController(Settings(), { storage }, saveSettings = { saved = it }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }
        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-theme-Nord").assertIsSelected()
        onNodeWithTag("settings-theme-Everforest").performScrollTo().performClick()
        onNodeWithTag("settings-appearance-Dark").performScrollTo().performClick()
        onNodeWithTag("settings-save").performScrollTo().performClick()

        assertEquals(ColorTheme.EVERFOREST to Appearance.DARK, saved.theme to saved.appearance)
        assertEquals(saved, Settings.fromProperties(saved.toProperties()))
    }

    // [utest->dsn~sync-status~1]
    // [utest->dsn~dates-in-dialog~1]
    @Test
    fun aSynchronizingStorageShowsItsStatusAndTheDatesOfATask() = runComposeUiTest {
        val syncing = object : SyncingStorage, TodoStorage by storage {
            override var onStatus: (SyncStatus) -> Unit = {}
            var synchronizations = 0
            override fun sync() {
                synchronizations++
            }
            override fun dates() = mapOf("Post signs +GarageSale" to TaskDates(added = LocalDate(2026, 11, 1)))
        }
        val controller = TodoController(Settings(), { syncing }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }

        onNodeWithTag("sync-status").assertTextEquals("Synchronized")
        assertEquals(1, syncing.synchronizations)
        onNodeWithTag("sync-now").performClick()
        assertEquals(2, syncing.synchronizations)

        syncing.onStatus(SyncStatus(SyncState.FAILED, "no network"))
        onNodeWithTag("sync-status").assertTextEquals("Not synchronized: no network")

        openTask("Post signs")
        onNodeWithTag("edit-dates").assertTextEquals("Added 2026-11-01 (from the git history)")
    }

    // [utest->dsn~sync-status~1]
    @Test
    fun aPlainFileHasNoSynchronizationStatus() = runComposeUiTest {
        start()
        onNodeWithTag("sync-status").assertDoesNotExist()
    }

    // [utest->dsn~git-setup-dialog~1]
    @Test
    fun theSettingsShowThePublicKeyAndCloneARepository() = runComposeUiTest {
        val git = object : GitSetupActions {
            override fun publicKey() = "ecdsa-sha2-nistp256 AAAA koppter"
            override suspend fun clone(url: String) = if (url == "git@example.org:todo.git") "/data/repository/todo.txt" else error("no such repository")
        }
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt", git = git) }

        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-public-key").assertTextEquals("ecdsa-sha2-nistp256 AAAA koppter")
        onNodeWithTag("settings-git-url").performScrollTo().performTextInput("git@example.org:other.git")
        onNodeWithTag("settings-clone").performScrollTo().performClick()
        onNodeWithTag("settings-clone-message").assertTextEquals("Not cloned: no such repository")

        onNodeWithTag("settings-git-url").performTextReplacement("git@example.org:todo.git")
        onNodeWithTag("settings-clone").performScrollTo().performClick()
        onNodeWithTag("settings-save").performScrollTo().performClick()
        assertEquals("/data/repository/todo.txt", controller.settings.todoFile)
    }

    // [utest->dsn~recurrence-on-completion~1]
    @Test
    fun completingARecurringTaskAddsItsNextOccurrence() = runComposeUiTest {
        storage.text = "Water plants due:2026-11-10 rec:1w\n"
        start()
        onNodeWithTag("task-done").performClick()
        assertEquals("x Water plants due:2026-11-10 rec:1w\nWater plants due:2026-11-19 rec:1w\n", storage.text)
        onAllNodesWithTag("task").assertCountEquals(1)
    }

    // [utest->dsn~top-tasks~1]
    // [utest->dsn~view-options~1]
    @Test
    fun theTopViewShowsTheFirstTasksOnly() = runComposeUiTest {
        start(Settings(topCount = 2))
        onNodeWithTag("top").assertTextEquals("Top 2").performClick()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithText("Thank Mom").assertExists()
        onNodeWithText("Schedule pickup").assertExists()
        onNodeWithTag("top").performClick()
        onAllNodesWithTag("task").assertCountEquals(3)
    }

    // [utest->dsn~board~1]
    // [utest->dsn~view-options~1]
    @Test
    fun theBoardShowsOneColumnPerGroup() = runComposeUiTest {
        start()
        onNodeWithTag("board-switch").performClick()
        onAllNodesWithTag("board-column").assertCountEquals(3)
        onNodeWithText("A (1)").assertExists()
        onNodeWithText("No priority (1)").assertExists()

        onNodeWithTag("group-PROJECT").performClick()
        onAllNodesWithTag("board-column").assertCountEquals(2)
        onNodeWithText("+GarageSale (2)").assertExists()
        onNodeWithText("No project (1)").assertExists()

        // A task on the board is completed and edited like a task in the list.
        onNodeWithText("Thank Mom").performClick()
        onNodeWithTag("edit-dialog").assertExists()
    }

    // [utest->dsn~notes~1]
    @Test
    fun aNoteIsKeptInAFileNamedByATag() = runComposeUiTest {
        start()
        openTask("Post signs")
        onNodeWithTag("edit-note").performScrollTo().performTextInput("Ask the neighbors first")
        onNodeWithTag("edit-save").performScrollTo().performClick()

        val line = storage.text.lines()[1]
        val name = Regex("^Post signs \\+GarageSale note:([0-9a-z]{3}\\.txt)$").matchEntire(line)!!.groupValues[1]
        assertEquals("Ask the neighbors first", storage.companions["notes/$name"])

        openTask("Post signs")
        onNodeWithTag("edit-note").assertTextEquals("Note", "Ask the neighbors first")
        onNodeWithTag("edit-note").performScrollTo().performTextReplacement("Ask the neighbors first, then the school")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals(line, storage.text.lines()[1])
        assertEquals("Ask the neighbors first, then the school", storage.companions["notes/$name"])
        assertEquals("Edit note: Post signs", storage.changes.last())
    }

    // [utest->dsn~ice-recur-run~1]
    @Test
    fun scheduledTasksAreAddedOncePerDay() = runComposeUiTest {
        storage.companions["ice_recur.txt"] = "daily - (A) Check mail\nweekly ; day Friday - Take out trash\n"
        start()
        mainClock.advanceTimeBy(2_500)
        assertEquals("(A) Check mail", storage.text.trim().lines().last())
        assertEquals("Add recurring tasks", storage.changes.last())

        onNodeWithText("Check mail").assertExists()
        onAllNodesWithTag("task-done")[0].performClick()
        mainClock.advanceTimeBy(2_500)
        assertEquals(1, storage.text.lines().count { "Check mail" in it })
    }

    // [utest->dsn~ice-recur-run~1]
    @Test
    fun scheduledTasksOfDaysWithoutARunAreAdded() = runComposeUiTest {
        // Today is Thursday; the last run was on Sunday.
        storage.companions["ice_recur.txt"] = "weekly ; day Monday - Plan the week\nweekly ; day Saturday - Clean the kitchen\n"
        start(Settings(iceRecurLastRun = "2026-11-08", writeDates = true))
        mainClock.advanceTimeBy(2_500)

        assertEquals("2026-11-09 Plan the week", storage.text.trim().lines().last())
        assertEquals(0, storage.text.lines().count { "kitchen" in it })
    }

    // [utest->dsn~ice-recur-editor~1]
    @Test
    fun theScheduleIsEditedAndANewEntryAddsItsTaskOfToday() = runComposeUiTest {
        storage.companions["ice_recur.txt"] = "weekly ; day Thursday - Put out the bin\n"
        start(Settings(iceRecurLastRun = "2026-11-12"))
        onNodeWithTag("open-schedule").performClick()
        onNodeWithTag("schedule-line-1").assertTextEquals("Line 1: next on Thu 2026-11-12")

        onNodeWithTag("schedule-text").performTextReplacement("weekly ; day Thursday - Put out the bin\ndaily - Check the mail\nsometimes - Dream\nweekly ; day Monday - Plan the week")
        onNodeWithTag("schedule-line-3").assertTextEquals("Line 3 is not understood")
        onNodeWithTag("schedule-line-4").assertTextEquals("Line 4: next on Mon 2026-11-16")
        onNodeWithTag("schedule-save").performScrollTo().performClick()

        assertEquals("weekly ; day Thursday - Put out the bin\ndaily - Check the mail\nsometimes - Dream\nweekly ; day Monday - Plan the week\n", storage.companions["ice_recur.txt"])
        assertEquals("Edit scheduled tasks", storage.changes.last())
        // Only the new entry that occurs today adds its task; the entry that was there ran already.
        assertEquals("Check the mail", storage.text.trim().lines().last())
        assertEquals(0, storage.text.lines().count { "bin" in it || "Plan" in it })
        onNodeWithText("Check the mail").assertExists()
    }

    // [utest->dsn~board-drag~1]
    @Test
    fun draggingACardToAnotherColumnChangesTheTask() = runComposeUiTest {
        start()
        onNodeWithTag("board-switch").performClick()
        // The columns are A, B and "No priority", 328 pixels apart; the card of column B goes to column A.
        onNodeWithText("Schedule pickup").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(-30f, 0f))
            moveBy(Offset(-300f, 0f))
        }
        onNodeWithTag("board-dragged").assertExists()
        // The description now exists twice, in the card and at the pointer, so the board takes the release.
        onNodeWithTag("board").performMouseInput { release() }

        assertEquals("(A) Schedule pickup +GarageSale @phone", storage.text.lines()[0])
        assertEquals("Move task to A: Schedule pickup", storage.changes.last())
        onNodeWithText("A (2)").assertExists()
        onNodeWithTag("board-dragged").assertDoesNotExist()
        onNodeWithTag("edit-dialog").assertDoesNotExist()
    }

    // [utest->dsn~board-drag~1]
    @Test
    fun aCardIsDraggedWithTheFingerAfterALongPress() = runComposeUiTest {
        start()
        onNodeWithTag("board-switch").performClick()
        onNodeWithText("Schedule pickup").performTouchInput {
            down(center)
            advanceEventTime(1000)
            moveBy(Offset(-330f, 0f))
            up()
        }

        assertEquals("(A) Schedule pickup +GarageSale @phone", storage.text.lines()[0])
        onNodeWithTag("edit-dialog").assertDoesNotExist()
    }

    // [utest->dsn~board-defined-columns~1]
    // [utest->dsn~settings-dialog~1]
    @Test
    fun columnsDefinedInTheSettingsFormTheBoard() = runComposeUiTest {
        start()
        onNodeWithTag("board-switch").performClick()
        onNodeWithTag("group-FILTER").assertDoesNotExist()

        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-board-columns").performTextInput("Phone = @phone\nSoon = due:<2w\nNothing = +Nowhere")
        onNodeWithTag("settings-save").performClick()
        assertEquals(
            "# Configuration of koppter for the todo.txt file in this directory\n" +
                "board-columns:\n  \"Phone\": \"@phone\"\n  \"Soon\": \"due:<2w\"\n  \"Nothing\": \"+Nowhere\"\n",
            storage.companions["koppter.yml"],
        )
        assertEquals("Edit board columns", storage.changes.last())
        onNodeWithTag("group-FILTER").performClick()

        onNodeWithText("Phone (2)").assertExists()
        onNodeWithText("Soon (1)").assertExists()
        onNodeWithText("Nothing (0)").assertExists()
        onNodeWithText("A (1)").assertDoesNotExist()
    }

    // [utest->dsn~board-defined-columns~1]
    @Test
    fun columnsThatAnotherDeviceDefinedAppear() = runComposeUiTest {
        start()
        onNodeWithTag("board-switch").performClick()
        storage.companions["koppter.yml"] = "board-columns:\n  Phone: \"@phone\"\n"
        mainClock.advanceTimeBy(5_000)

        onNodeWithTag("group-FILTER").performClick()
        onNodeWithText("Phone (2)").assertExists()
    }

    // [utest->dsn~board-move-to-filter~1]
    @Test
    fun draggingACardToAColumnDefinedByAFilterMakesTheTaskMatchTheFilter() = runComposeUiTest {
        storage.companions["koppter.yml"] = "board-columns:\n  Phone: \"@phone\"\n  Garage: +GarageSale\n"
        start()
        onNodeWithTag("board-switch").performClick()
        onNodeWithTag("group-FILTER").performClick()
        onNodeWithText("Garage (2)").assertExists()

        onNodeWithText("Thank Mom").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(30f, 0f))
            moveBy(Offset(300f, 0f))
        }
        onNodeWithTag("board").performMouseInput { release() }

        assertEquals("(A) Thank Mom +GarageSale @phone due:2026-11-20", storage.text.lines()[2])
        assertEquals("Move task to Garage: Thank Mom", storage.changes.last())
        onNodeWithText("Garage (3)").assertExists()
        // The task matches the filter of its former column as before.
        onNodeWithText("Phone (2)").assertExists()
    }

    // [utest->dsn~hashtags-in-ui~1]
    @Test
    fun hashtagsAreLabelsFilterCriteriaAndEditableInTheDialog() = runComposeUiTest {
        start()
        onNodeWithTag("filter-hashtag-urgent").assertDoesNotExist()

        openTask("Post signs")
        onNodeWithTag("edit-hashtag-add").performTextInput("123 ")
        onNodeWithTag("edit-hashtag-add").performTextInput("#urgent ")
        onNodeWithTag("edit-line").assertTextEquals("Post signs +GarageSale #urgent")
        // The dialog is higher than the window of the test.
        onNodeWithTag("edit-save").performScrollTo().performClick()

        assertEquals("Post signs +GarageSale #urgent", storage.text.lines()[1])
        // On the card and in the filter panel.
        onAllNodesWithText("#urgent", useUnmergedTree = true).assertCountEquals(2)
        onNodeWithTag("filter-hashtag-urgent").performClick()
        onAllNodesWithTag("task").assertCountEquals(1)

        onNodeWithTag("board-switch").performClick()
        onNodeWithTag("group-HASHTAG").performClick()
        onNodeWithText("#urgent (1)").assertExists()
    }
}
