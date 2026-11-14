package io.github.koppor.koppter

import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.koppor.koppter.plan.fetchCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.koppor.koppter.storage.SettingsFile
import io.github.koppor.koppter.sync.GitSetup
import io.github.koppor.koppter.sync.storageForFile
import io.github.koppor.koppter.ui.App
import io.github.koppor.koppter.ui.GitSetupActions
import io.github.koppor.koppter.ui.TodoController
import kotlinx.datetime.LocalDate
import net.harawata.appdirs.AppDirsFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

// [impl->adr~desktop-locations-from-appdirs~1]
private val directories = AppDirsFactory.getInstance()

/** The directory for the settings: `%APPDATA%\koppter` on Windows, `$XDG_CONFIG_HOME/koppter` or `~/.config/koppter` on Linux. */
// [impl->dsn~desktop-locations~1]
private fun configDirectory(): Path = Path(directories.getUserConfigDir("koppter", null, null, true))

/** The directory for files that the application creates: `%APPDATA%\koppter` on Windows, `$XDG_DATA_HOME/koppter` or `~/.local/share/koppter` on Linux. */
private fun dataDirectory(): Path = Path(directories.getUserDataDir("koppter", null, null, true))

/** Shows the file dialog of the operating system; a name typed there may be that of a file that does not exist yet. */
// [impl->dsn~choose-file~1]
private fun chooseFile(owner: java.awt.Frame): String? {
    val dialog = java.awt.FileDialog(owner, "todo.txt file", java.awt.FileDialog.LOAD)
    dialog.isVisible = true
    return dialog.file?.let { Path(dialog.directory, it).toString() }
}

fun main() {
    // A file in the home directory, where earlier versions kept it, stays in use.
    val defaultTodoFile = Path(System.getProperty("user.home")).resolve("todo.txt").takeIf { it.exists() } ?: dataDirectory().resolve("todo.txt")
    // [impl->dsn~git-setup-dialog~1]
    val git = object : GitSetupActions {
        override fun publicKey(): String? = null
        override suspend fun clone(url: String) = withContext(Dispatchers.IO) { GitSetup(dataDirectory(), ownKey = false).clone(url).toString() }
    }
    val settingsFile = SettingsFile(configDirectory().resolve("settings.properties"))
    val controller = TodoController(
        settings = settingsFile.load(),
        storageFor = { storageForFile(it.todoFile.takeIf(String::isNotBlank)?.let(::Path) ?: defaultTodoFile) },
        saveSettings = settingsFile::save,
        today = { java.time.LocalDate.now().let { LocalDate(it.year, it.monthValue, it.dayOfMonth) } },
        minuteOfDay = { java.time.LocalTime.now().toSecondOfDay() / 60 },
        fetch = { address -> withContext(Dispatchers.IO) { fetchCalendar(address) } },
    )
    // [impl->dsn~application-icon~1]
    val icon = BitmapPainter(object {}.javaClass.getResourceAsStream("/koppter.png")!!.readBytes().decodeToImageBitmap())
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "koppter",
            icon = icon,
            state = rememberWindowState(width = 1100.dp, height = 760.dp),
        ) {
            App(controller, defaultTodoFile.toString(), chooseFile = { chooseFile(window) }, git = git)
        }
    }
}
