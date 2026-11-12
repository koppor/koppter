package io.github.koppor.koppter.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.SettingsFile
import io.github.koppor.koppter.storage.TodoStorage
import io.github.koppor.koppter.sync.GitSetup
import io.github.koppor.koppter.todotxt.ShareAction
import io.github.koppor.koppter.ui.TodoController
import kotlinx.datetime.LocalDate

/**
 * Receives text that another application shares and adds it as a task, without showing a screen.
 * A share action chosen in the share sheet arrives as the id of its shortcut; the plain entry of the application applies the first action.
 */
// [impl->dsn~android-share-target~1]
class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = listOfNotNull(intent.getStringExtra(Intent.EXTRA_SUBJECT), intent.getStringExtra(Intent.EXTRA_TEXT))
            // Some applications repeat the subject in the text.
            .let { (it.dropLast(1).filterNot { subject -> subject in it.last() } + it.takeLast(1)).joinToString(" ") }
        val action = intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)
        // The share sheet ranks the targets by their use.
        action?.let { getSystemService(ShortcutManager::class.java)?.reportShortcutUsed(it) }
        val message = if (text.isBlank()) {
            "Nothing to add"
        } else {
            try {
                // ponytail: the push runs on a background thread of a process that the system may stop after this activity;
                // the commit is made before, and the next start of the application pushes it. A foreground service if that shows.
                todoController(GitSetup(filesDir.toPath().resolve("git"))).share(text, action)
            } catch (e: Exception) {
                "Not added: ${e.message ?: e}"
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}

// [impl->dsn~android-locations~1]
internal fun Context.storageFor(settings: Settings, gitSetup: GitSetup): TodoStorage = when {
    settings.todoFile.isBlank() -> gitSetup.storageFor(filesDir.toPath().resolve("todo.txt"))
    settings.todoFile.startsWith("content:") -> DocumentStorage(contentResolver, Uri.parse(settings.todoFile))
    else -> gitSetup.storageFor(java.nio.file.Paths.get(settings.todoFile))
}

internal fun Context.todoController(gitSetup: GitSetup): TodoController {
    val settingsFile = SettingsFile(filesDir.toPath().resolve("settings.properties"))
    return TodoController(
        settings = settingsFile.load(),
        storageFor = { storageFor(it, gitSetup) },
        saveSettings = settingsFile::save,
        today = { java.time.LocalDate.now().let { LocalDate(it.year, it.monthValue, it.dayOfMonth) } },
        minuteOfDay = { java.time.LocalTime.now().toSecondOfDay() / 60 },
    )
}

/**
 * Publishes one shortcut per share action, which the share sheet of Android 10 and later offers as a target of its own.
 * Their number is limited by the system; the first actions win.
 */
// [impl->dsn~android-share-target~1]
internal fun Context.publishShareTargets(actions: List<ShareAction>) {
    val manager = getSystemService(ShortcutManager::class.java) ?: return
    try {
        manager.dynamicShortcuts = actions.distinctBy { it.title }.take(manager.maxShortcutCountPerActivity).map { action ->
            ShortcutInfo.Builder(this, action.title)
                .setShortLabel(action.title)
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                // What the shortcut does when it is chosen in the launcher: it opens the application.
                .setIntent(Intent(Intent.ACTION_MAIN, null, this, MainActivity::class.java))
                .setCategories(setOf("io.github.koppor.koppter.android.category.SHARE"))
                .apply { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setLongLived(true) }
                .build()
        }
    } catch (_: RuntimeException) {
        // The launcher may refuse shortcuts; sharing through the entry of the application still works.
    }
}
