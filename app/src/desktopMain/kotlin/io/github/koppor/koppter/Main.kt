package io.github.koppor.koppter

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.koppor.koppter.storage.FileStorage
import io.github.koppor.koppter.storage.SettingsFile
import io.github.koppor.koppter.ui.App
import io.github.koppor.koppter.ui.TodoController
import kotlinx.datetime.LocalDate
import java.nio.file.Path
import kotlin.io.path.Path

/** The directory for the settings: `%APPDATA%\koppter` on Windows, `$XDG_CONFIG_HOME/koppter` or `~/.config/koppter` elsewhere. */
// [impl->dsn~desktop-locations~1]
private fun configDirectory(): Path {
    val home = Path(System.getProperty("user.home"))
    val base = System.getenv("APPDATA")?.takeIf { System.getProperty("os.name").startsWith("Windows") }
        ?: System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
    return (base?.let(::Path) ?: home.resolve(".config")).resolve("koppter")
}

/** Shows the file dialog of the operating system; a name typed there may be that of a file that does not exist yet. */
// [impl->dsn~choose-file~1]
private fun chooseFile(owner: java.awt.Frame): String? {
    val dialog = java.awt.FileDialog(owner, "todo.txt file", java.awt.FileDialog.LOAD)
    dialog.isVisible = true
    return dialog.file?.let { Path(dialog.directory, it).toString() }
}

fun main() {
    val defaultTodoFile = Path(System.getProperty("user.home")).resolve("todo.txt")
    val settingsFile = SettingsFile(configDirectory().resolve("settings.properties"))
    val controller = TodoController(
        settings = settingsFile.load(),
        storageFor = { FileStorage(it.todoFile.takeIf(String::isNotBlank)?.let(::Path) ?: defaultTodoFile) },
        saveSettings = settingsFile::save,
        today = { java.time.LocalDate.now().let { LocalDate(it.year, it.monthValue, it.dayOfMonth) } },
    )
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "koppter",
            state = rememberWindowState(width = 1100.dp, height = 760.dp),
        ) {
            App(controller, defaultTodoFile.toString(), chooseFile = { chooseFile(window) })
        }
    }
}
