package io.github.koppor.koppter.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import io.github.koppor.koppter.storage.CompanionFiles
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.SyncStatus
import io.github.koppor.koppter.storage.SyncingStorage
import io.github.koppor.koppter.sync.TaskDates
import kotlinx.datetime.LocalDate
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders the screens to `build/screenshots`, for a look at layout changes and for the documentation:
 * `docs/screenshots` holds copies, see `CLAUDE.md`.
 *
 * The tasks are invented; the storage stands in for a git repository so that the screens show synchronization and dates.
 */
@OptIn(ExperimentalTestApi::class)
class ScreenshotTest {
    private val storage = object : SyncingStorage, CompanionFiles {
        private val companions = mapOf(
            "todo.txt-config.yml" to """
                board-columns:
                  Now: due:<=today -@waiting
                  Waiting: "@waiting"
                  This week: due:<1w
                  Important: (>C)
            """.trimIndent(),
            "notes/k3f.txt" to "Agenda: results of the survey, plan for December.\nAsk for the room with the projector.",
        )

        override var onStatus: (SyncStatus) -> Unit = {}
        override fun sync() = Unit
        override fun dates() = mapOf(
            "(B) Prepare the slides for the team meeting +Work @computer #meeting due:2026-11-17 note:k3f.txt" to TaskDates(added = LocalDate(2026, 11, 5)),
        )

        override fun readCompanion(path: String) = companions[path]
        override fun writeCompanion(path: String, text: String) = Unit

        override fun read() = """
            (A) Call the landlord about the heating @phone due:2026-11-12
            (A) Submit the travel expense report +Work @computer #money due:2026-11-10
            (B) Schedule Goodwill pickup +GarageSale @phone due:2026-11-14
            (B) Prepare the slides for the team meeting +Work @computer #meeting due:2026-11-17 note:k3f.txt
            (C) Post signs around the neighborhood +GarageSale @errands
            (C) Renew the library card @errands #someday due:2026-11-20
            Water the plants @home due:2026-11-12 rec:1w
            Book the train to Hamburg +Vacation @computer @waiting
            Compare prices for a new bike +Vacation #money #someday
            Back up the photos @computer due:2026-12-01 rec:+1m
            x Thank Mom for the meatballs +Family @phone
        """.trimIndent() + "\n"

        override fun write(text: String, change: String) = Unit
        override fun version(): Any? = null
    }

    private val git = object : GitSetupActions {
        override fun publicKey() = "ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBExampleKeyOfTheDemo= koppter"
        override suspend fun clone(url: String) = "todo.txt"
    }

    private fun DesktopComposeUiTest.start() {
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt", git = git) }
    }

    @Test
    fun rendersListAndDialogs() = runDesktopComposeUiTest(width = 1100, height = 980) {
        start()
        save("list.png")
        onNodeWithTag("filter-project-GarageSale").performClick()
        onNodeWithTag("filter-project-Work").performClick()
        onNodeWithTag("filter-context-phone").performClick()
        save("filter.png")
        onNodeWithTag("filter-clear").performClick()
        onNodeWithText("Prepare the slides for the team meeting").performClick()
        save("edit.png")
        onNodeWithTag("edit-cancel").performClick()
        onNodeWithTag("open-settings").performClick()
        save("settings.png")
    }

    @Test
    fun rendersBoard() = runDesktopComposeUiTest(width = 1400, height = 620) {
        start()
        onNodeWithTag("board-switch").performClick()
        // A card on its way from column B to column A.
        onNodeWithText("Schedule Goodwill pickup").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(-30f, 0f))
            moveBy(Offset(-200f, 60f))
        }
        save("board-priority.png")
        // Back to its column, so that the task stays as it is for the other boards.
        onNodeWithTag("board").performMouseInput {
            moveBy(Offset(230f, -60f))
            release()
        }
        onNodeWithTag("group-PROJECT").performClick()
        save("board-project.png")
        onNodeWithTag("group-FILTER").performClick()
        save("board-filter.png")
    }

    @Test
    fun rendersNarrowWindow() = runDesktopComposeUiTest(width = 412, height = 760) {
        start()
        save("narrow.png")
    }

    private fun DesktopComposeUiTest.save(name: String) {
        waitForIdle()
        val file = File("build/screenshots/$name")
        file.parentFile.mkdirs()
        ImageIO.write(captureToImage().toAwtImage(), "png", file)
        assertTrue(file.length() > 0)
    }
}
