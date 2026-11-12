package io.github.koppor.koppter.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.input.key.Key
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.TodoStorage
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

private class MemoryStorage(var text: String) : TodoStorage {
    override fun read() = text
    override fun write(text: String) {
        this.text = text
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

        onNodeWithTag("edit-save").performClick()
        assertEquals("(B) Thank Mom for the meatballs +Family @phone due:2026-11-20", storage.text.lines()[2])
        onNodeWithTag("edit-dialog").assertDoesNotExist()
    }

    // [utest->dsn~edit-dialog~1]
    @Test
    fun savingWithoutChangeLeavesTheFileUntouched() = runComposeUiTest {
        storage.text = "Call  +Family Mom   @phone\n"
        start()
        openTask("Call Mom")
        onNodeWithTag("edit-save").performClick()
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
        onNodeWithTag("edit-save").performClick()
        assertEquals("Post signs +GarageSale due:2026-11-30", storage.text.lines()[1])
    }

    // [utest->dsn~edit-dialog~1]
    @Test
    fun deletesATaskFromTheDialog() = runComposeUiTest {
        start()
        openTask("Post signs")
        onNodeWithTag("edit-delete").performClick()
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
        onNodeWithTag("edit-save").performClick()
        assertEquals("Water plants @home", storage.text.trim().lines().last())
    }

    // [utest->dsn~filter-panel~1]
    @Test
    fun theFilterPanelRestrictsTheList() = runComposeUiTest {
        start()
        onNodeWithTag("filter-project-GarageSale").performClick()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithTag("filter-priority-B").performClick()
        onAllNodesWithTag("task").assertCountEquals(1)
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
}
