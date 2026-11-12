package io.github.koppor.koppter.ui

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.TodoStorage
import kotlinx.datetime.LocalDate
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** Renders the main screen and the edit dialog to `build/screenshots`, for the documentation and for a look at layout changes. */
@OptIn(ExperimentalTestApi::class)
class ScreenshotTest {
    private val storage = object : TodoStorage {
        override fun read() = """
            (B) Schedule Goodwill pickup +GarageSale @phone
            Post signs around the neighborhood +GarageSale
            (A) Thank Mom for the meatballs @phone due:2026-11-10
            Test due:2026-12-12 t:2026-11-01 +test @Stuttgart hallo du rec:1m
            (C) Document task format +TodoTxt due:2026-11-12
            Email SoAndSo at soandso@example.com
            x Call Mom +Family @phone
        """.trimIndent() + "\n"

        override fun write(text: String, change: String) = Unit
        override fun version(): Any? = null
    }

    @Test
    fun rendersMainScreenAndEditDialog() = runDesktopComposeUiTest(width = 1100, height = 700) {
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }
        save("main.png")
        onNodeWithText("Test hallo du").performClick()
        save("edit.png")
    }

    private fun androidx.compose.ui.test.DesktopComposeUiTest.save(name: String) {
        waitForIdle()
        val file = File("build/screenshots/$name")
        file.parentFile.mkdirs()
        ImageIO.write(captureToImage().toAwtImage(), "png", file)
        assertTrue(file.length() > 0)
    }
}
