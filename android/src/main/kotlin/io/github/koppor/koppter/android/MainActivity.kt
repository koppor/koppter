package io.github.koppor.koppter.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import io.github.koppor.koppter.storage.FileStorage
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.SettingsFile
import io.github.koppor.koppter.storage.TodoStorage
import io.github.koppor.koppter.ui.App
import io.github.koppor.koppter.ui.TodoController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.LocalDate

class MainActivity : ComponentActivity() {
    private var picked: CompletableDeferred<String?>? = null

    // The permission is persisted, as the file is read again after every restart of the application.
    // [impl->dsn~choose-file~1]
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        picked?.complete(uri?.toString())
    }

    private suspend fun chooseFile(): String? {
        val result = CompletableDeferred<String?>()
        picked = result
        // A todo.txt file is not always reported as text/plain by a document provider.
        picker.launch(arrayOf("*/*"))
        return result.await()
    }

    // [impl->dsn~android-locations~1]
    private fun storageFor(settings: Settings): TodoStorage = when {
        settings.todoFile.isBlank() -> FileStorage(filesDir.toPath().resolve("todo.txt"))
        settings.todoFile.startsWith("content:") -> DocumentStorage(contentResolver, Uri.parse(settings.todoFile))
        else -> FileStorage(java.nio.file.Paths.get(settings.todoFile))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsFile = SettingsFile(filesDir.toPath().resolve("settings.properties"))
        val controller = TodoController(
            settings = settingsFile.load(),
            storageFor = ::storageFor,
            saveSettings = settingsFile::save,
            today = { java.time.LocalDate.now().let { LocalDate(it.year, it.monthValue, it.dayOfMonth) } },
        )
        setContent { App(controller, "todo.txt in the storage of the application", chooseFile = ::chooseFile) }
    }
}
