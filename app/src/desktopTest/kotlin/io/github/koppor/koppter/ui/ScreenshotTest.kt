package io.github.koppor.koppter.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import io.github.koppor.koppter.storage.Appearance
import io.github.koppor.koppter.storage.ColorTheme
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
            "koppter.yml" to """
                board-columns:
                  Now: due:<=today -@waiting
                  Waiting: "@waiting"
                  This week: due:<1w
                  Important: (>C)
                time-maps:
                  General: Mon-Fri 08:00-12:00 13:00-17:00
                  Focus: General 08:00-10:00
                  Evenings: Mon-Thu 19:00-21:00, Sat 10:00-12:00
                context-time-maps:
                  "@computer": Focus
                  "@home": Evenings
                barriers:
                  default: quarter
                  "+Vacation": 2026-11-30
            """.trimIndent(),
            // As "Save and plan" writes it on the day of the screenshots at noon; the bike was added afterwards.
            "calendar.txt" to """
                2026-11-12 w46 Thu  13:00-13:30 Call the landlord about the heating @phone. 13:30-14:00 Schedule Goodwill pickup +GarageSale @phone. 14:00-14:30 Post signs around the neighborhood +GarageSale @errands. 14:30-15:00 Renew the library card @errands>city. 19:00-19:30 Water the plants @home
                2026-11-13 w46 Fri  08:00-08:30 Submit the travel expense report +Work @computer. 08:30-10:00 Prepare the slides for the team meeting +Work @computer
                2026-11-14 w46 Sat
                2026-11-15 w46 Sun
                2026-11-16 w47 Mon  08:00-08:30 Prepare the slides for the team meeting +Work @computer. 08:30-09:00 Book the train to Hamburg +Vacation @computer @waiting. 09:00-09:30 Back up the photos @computer>backup
            """.trimIndent() + "\n",
            // No entry occurs on the day of the screenshots, so the list stays as the file has it.
            "ice_recur.txt" to """
                # Work
                weekly ; day Monday - (B) Plan the week +Work
                monthly ; day_of_month -1 - Send the time sheet +Work @computer

                # Household
                @2026-11-05 weekly 2 ; day Thursday - Put out the paper bin @home
                monthly ; day_of_week Saturday => 1 - Clean the bike @home
                @2026-04-15 yearly - (A) Renew the insurance #money
            """.trimIndent() + "\n",
            "notes/k3f.txt" to "Agenda: results of the survey, plan for December.\nAsk for the room with the projector.",
        )

        override var onStatus: (SyncStatus) -> Unit = {}
        override fun sync() = Unit
        override fun dates() = mapOf(
            "(B) Prepare the slides for the team meeting +Work @computer #meeting due:2026-11-17 dur:2h cs:work/team-meeting note:k3f.txt" to TaskDates(added = LocalDate(2026, 11, 5)),
        )

        override fun readCompanion(path: String) = companions[path]
        override fun writeCompanion(path: String, text: String) = Unit

        override fun read() = """
            (A) Call the landlord about the heating @phone due:2026-11-12
            (A) Submit the travel expense report +Work @computer #money due:2026-11-10
            (B) Schedule Goodwill pickup +GarageSale @phone due:2026-11-14
            (B) Prepare the slides for the team meeting +Work @computer #meeting due:2026-11-17 dur:2h cs:work/team-meeting note:k3f.txt
            (C) Post signs around the neighborhood +GarageSale @errands
            (C) Renew the library card @errands>city #someday due:2026-11-20
            Water the plants @home due:2026-11-12 rec:1w
            Book the train to Hamburg +Vacation @computer @waiting
            Compare prices for a new bike +Vacation #money #someday
            Back up the photos @computer>backup due:2026-12-01 rec:+1m
            x Thank Mom for the meatballs +Family @phone
        """.trimIndent() + "\n"

        override fun write(text: String, change: String) = Unit
        override fun version(): Any? = null
    }

    private val git = object : GitSetupActions {
        override fun publicKey() = "ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBExampleKeyOfTheDemo= koppter"
        override suspend fun clone(url: String) = "todo.txt"
    }

    private fun DesktopComposeUiTest.start(settings: Settings = Settings()) {
        val controller = TodoController(settings, { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt", git = git) }
    }

    @Test
    fun rendersListAndDialogs() = runDesktopComposeUiTest(width = 1100, height = 1060) {
        start()
        save("list.png")
        onNodeWithTag("filter-project-GarageSale").performClick()
        onNodeWithTag("filter-project-Work").performClick()
        onNodeWithTag("filter-context-phone").performClick()
        save("filter.png")
        onNodeWithTag("filter-clear").performClick()
        onNodeWithText("Prepare the slides for the team meeting").performClick()
        save("edit.png")
        onNodeWithTag("edit-cancel").performScrollTo().performClick()
        onNodeWithTag("open-settings").performClick()
        save("settings.png")
    }

    @Test
    fun rendersOverview() = runDesktopComposeUiTest(width = 1100, height = 620) {
        start()
        onNodeWithTag("overview-switch").performClick()
        save("overview.png")
        onNodeWithTag("overview-computer").performClick()
        save("overview-context.png")
    }

    @Test
    fun rendersPlan() = runDesktopComposeUiTest(width = 1100, height = 1060) {
        start()
        onNodeWithTag("open-plan").performClick()
        onNodeWithTag("plan-update").performScrollTo().performClick()
        save("plan.png")
    }

    @Test
    fun rendersSchedule() = runDesktopComposeUiTest(width = 1100, height = 1060) {
        start()
        onNodeWithTag("open-schedule").performClick()
        save("schedule.png")
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
    fun rendersNordDark() = runDesktopComposeUiTest(width = 1100, height = 620) {
        start(Settings(appearance = Appearance.DARK))
        save("nord-dark.png")
    }

    @Test
    fun rendersEverforestLight() = runDesktopComposeUiTest(width = 1100, height = 620) {
        start(Settings(theme = ColorTheme.EVERFOREST))
        save("everforest-light.png")
    }

    @Test
    fun rendersEverforestDark() = runDesktopComposeUiTest(width = 1100, height = 620) {
        start(Settings(theme = ColorTheme.EVERFOREST, appearance = Appearance.DARK))
        save("everforest-dark.png")
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
