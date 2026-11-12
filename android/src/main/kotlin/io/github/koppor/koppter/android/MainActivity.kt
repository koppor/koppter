package io.github.koppor.koppter.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import io.github.koppor.koppter.sync.GitSetup
import io.github.koppor.koppter.ui.App
import io.github.koppor.koppter.ui.GitSetupActions
import io.github.koppor.koppter.ui.TodoController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    private val gitSetup by lazy { GitSetup(filesDir.toPath().resolve("git")) }

    // [impl->dsn~git-setup-dialog~1]
    private val gitActions = object : GitSetupActions {
        override fun publicKey(): String = gitSetup.publicKey()

        override suspend fun clone(url: String): String = withContext(Dispatchers.IO) { gitSetup.clone(url).toString() }
    }

    private lateinit var controller: TodoController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        controller = todoController(gitSetup)
        publishShareTargets(controller.shareActions)
        setContent { App(controller, "todo.txt in the storage of the application", chooseFile = ::chooseFile, git = gitActions) }
    }

    // The share actions may have been edited in the settings.
    override fun onStop() {
        super.onStop()
        publishShareTargets(controller.shareActions)
    }
}
