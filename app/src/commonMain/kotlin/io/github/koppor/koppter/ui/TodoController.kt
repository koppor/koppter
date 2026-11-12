package io.github.koppor.koppter.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.SyncState
import io.github.koppor.koppter.storage.SyncStatus
import io.github.koppor.koppter.storage.SyncingStorage
import io.github.koppor.koppter.storage.TodoStorage
import io.github.koppor.koppter.sync.TaskDates
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

    /** The state of the synchronization; `null` if the storage does not synchronize. */
    var syncStatus by mutableStateOf<SyncStatus?>(null)
        private set

    private var storage = storageFor(settings)
    private var loadedVersion: Any? = null

    init {
        open()
    }

    /** Loads the file and, for a synchronizing storage, starts the first synchronization. */
    // [impl->dsn~sync-status~1]
    private fun open() {
        load()
        val syncing = storage as? SyncingStorage
        syncStatus = syncing?.let { SyncStatus(SyncState.IDLE) }
        syncing?.onStatus = { status ->
            // A storage that was replaced in the settings may still report.
            if (storage === syncing) syncStatus = status
        }
        sync()
    }

    fun sync() = guarded { (storage as? SyncingStorage)?.sync() }

    /** When the task was added and completed according to the history of the storage. */
    fun datesOf(task: Task): TaskDates? = try {
        (storage as? SyncingStorage)?.dates()?.get(task.toLine())
    } catch (_: Exception) {
        null
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
        write(file.add(task.created(today(), settings.writeDates)), "Add task: ${task.description}")
    }

    fun add(line: String) = add(Task.parse(line))

    fun toggle(entry: Entry) {
        val index = locate(entry) ?: return
        if (entry.task.completed) {
            write(file.replace(index, entry.task.reopen()), "Reopen task: ${entry.task.description}")
        } else {
            write(file.replace(index, entry.task.complete(today(), settings.writeDates)), "Complete task: ${entry.task.description}")
        }
    }

    fun replace(entry: Entry, task: Task) {
        val index = locate(entry) ?: return
        write(file.replace(index, task), "Edit task: ${task.description}")
    }

    fun delete(entry: Entry) {
        val index = locate(entry) ?: return
        write(file.remove(index), "Delete task: ${entry.task.description}")
    }

    fun updateSettings(new: Settings) {
        if (new == settings) return
        val fileChanged = new.todoFile != settings.todoFile
        settings = new
        guarded { saveSettings(new) }
        if (fileChanged) {
            storage = storageFor(new)
            open()
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

    private fun write(new: TodoFile, change: String) {
        if (new == file) return
        guarded {
            storage.write(new.serialize(), change)
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
