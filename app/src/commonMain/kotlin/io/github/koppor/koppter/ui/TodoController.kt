package io.github.koppor.koppter.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.TodoStorage
import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.Filter
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.TodoFile
import io.github.koppor.koppter.todotxt.listOrder
import kotlinx.datetime.LocalDate

/**
 * The state of the application and the operations on it.
 *
 * Every change is written to the storage at once; there is no unsaved state.
 */
// ponytail: file access runs on the caller's thread; move it off the UI thread when a storage is slow (network).
class TodoController(
    settings: Settings,
    private val storageFor: (Settings) -> TodoStorage,
    private val saveSettings: (Settings) -> Unit = {},
    val today: () -> LocalDate,
) {
    var settings by mutableStateOf(settings)
        private set
    var file by mutableStateOf(TodoFile())
        private set
    var filter by mutableStateOf(Filter())
    var error by mutableStateOf<String?>(null)

    private var storage = storageFor(settings)
    private var loadedVersion: Any? = null

    init {
        load()
    }

    /** The tasks to show, filtered and in list order. */
    // [impl->dsn~visible-tasks~1]
    val visible: List<Entry>
        get() = file.entries.filter { filter.matches(it.task, today()) }.sortedWith(listOrder)

    val projects: List<String> get() = file.entries.flatMap { it.task.projects }.distinct().sorted()
    val contexts: List<String> get() = file.entries.flatMap { it.task.contexts }.distinct().sorted()
    val priorities: List<Char> get() = file.entries.mapNotNull { it.task.priority }.distinct().sorted()

    fun load() = guarded {
        file = TodoFile.parse(storage.read())
        loadedVersion = storage.version()
    }

    /** Reloads if another program changed the file. */
    // [impl->dsn~reload-on-external-change~1]
    fun reloadIfChanged() {
        val version = try {
            storage.version()
        } catch (_: Exception) {
            return
        }
        if (version != loadedVersion) load()
    }

    // [impl->dsn~task-operations~1]
    fun add(task: Task) {
        if (task.body.isBlank()) return
        write(file.add(task.created(today(), settings.writeDates)))
    }

    fun add(line: String) = add(Task.parse(line))

    fun toggle(entry: Entry) {
        val task = if (entry.task.completed) entry.task.reopen() else entry.task.complete(today(), settings.writeDates)
        replace(entry, task)
    }

    fun replace(entry: Entry, task: Task) {
        val index = locate(entry) ?: return
        write(file.replace(index, task))
    }

    fun delete(entry: Entry) {
        val index = locate(entry) ?: return
        write(file.remove(index))
    }

    fun updateSettings(new: Settings) {
        if (new == settings) return
        val fileChanged = new.todoFile != settings.todoFile
        settings = new
        guarded { saveSettings(new) }
        if (fileChanged) {
            storage = storageFor(new)
            load()
        }
    }

    /**
     * The current line index of the task the user acted on.
     * The file may have been reloaded since the entry was shown, so the index is checked against the content.
     */
    private fun locate(entry: Entry): Int? {
        val current = file.entries
        val found = current.firstOrNull { it == entry } ?: current.firstOrNull { it.task == entry.task }
        if (found == null) error = "The task was changed by another program. Nothing was written."
        return found?.index
    }

    private fun write(new: TodoFile) {
        if (new == file) return
        guarded {
            storage.write(new.serialize())
            file = new
            loadedVersion = storage.version()
        }
    }

    private fun guarded(action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            error = e.message ?: e.toString()
        }
    }
}
