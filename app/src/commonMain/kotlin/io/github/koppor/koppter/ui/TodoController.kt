package io.github.koppor.koppter.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.koppor.koppter.storage.CompanionFiles
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.SyncState
import io.github.koppor.koppter.storage.SyncStatus
import io.github.koppor.koppter.storage.SyncingStorage
import io.github.koppor.koppter.storage.TodoStorage
import io.github.koppor.koppter.sync.TaskDates
import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.Filter
import io.github.koppor.koppter.todotxt.Group
import io.github.koppor.koppter.todotxt.BoardColumn
import io.github.koppor.koppter.todotxt.GroupKey
import io.github.koppor.koppter.todotxt.groupByColumns
import io.github.koppor.koppter.todotxt.parseBoardColumns
import io.github.koppor.koppter.todotxt.groupEntries
import io.github.koppor.koppter.todotxt.iceRecurTasks
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.TodoFile
import io.github.koppor.koppter.todotxt.listOrder
import io.github.koppor.koppter.todotxt.moveToGroup
import kotlinx.datetime.LocalDate
import kotlin.random.Random

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

    /**
     * Restricts the list to its first tasks, the ones to do next.
     * With priority A and a due date, these are the most important tasks; there is no notation of its own for them.
     */
    // [impl->dsn~most-important-tasks~1]
    var topOnly by mutableStateOf(false)

    /** Shows the tasks as a board instead of as a list. */
    var board by mutableStateOf(false)

    /** The attribute whose values are the columns of the board; `null` for the columns the user defined. */
    var groupKey by mutableStateOf<GroupKey?>(GroupKey.PRIORITY)

    /** The columns the user defined in the settings. */
    val boardColumns: List<BoardColumn> get() = parseBoardColumns(settings.boardColumns)

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
            // [impl->dsn~top-tasks~1]
            .let { if (topOnly) it.take(settings.topCount) else it }

    /** The visible tasks as the columns of the board. */
    // [impl->dsn~board~1]
    // [impl->dsn~board-defined-columns~1]
    val groups: List<Group>
        get() = groupKey?.let { groupEntries(visible, it, today()) } ?: groupByColumns(visible, boardColumns, today())

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
        addScheduledTasks()
    }

    /**
     * Adds the tasks that `ice_recur.txt` schedules for today, once per day and device.
     * It waits for a running synchronization, so that tasks another device added today are known.
     */
    // [impl->dsn~ice-recur-run~1]
    private fun addScheduledTasks() {
        val today = today()
        if (settings.iceRecurLastRun == today.toString() || syncStatus?.state == SyncState.RUNNING) return
        guarded {
            val schedule = (storage as? CompanionFiles)?.readCompanion(ICE_RECUR_FILE) ?: return@guarded
            // A task that was added today counts as present even if it is completed already.
            val addedToday = (storage as? SyncingStorage)?.dates().orEmpty().filterValues { it.added == today }.keys
            val existing = file.lines.filter { it.isNotBlank() }.map { line -> Task.parse(line).let { if (line in addedToday) it.reopen() else it } }
            val tasks = iceRecurTasks(schedule, today, existing)
            if (tasks.isNotEmpty()) {
                write(tasks.fold(file) { current, task -> current.add(task.created(today, settings.writeDates)) }, "Add recurring tasks")
            }
            updateSettings(settings.copy(iceRecurLastRun = today.toString()))
        }
    }

    /** The text of the note of the task; empty if it has none, `null` if the storage keeps no notes. */
    // [impl->dsn~notes~1]
    fun noteOf(task: Task): String? = try {
        (storage as? CompanionFiles)?.let { files -> task.tag(Task.NOTE)?.let { files.readCompanion("$NOTES_DIRECTORY/$it") }.orEmpty() }
    } catch (_: Exception) {
        null
    }

    /** A file name for the note of a task that has none yet. */
    fun newNoteName(): String = Random.nextInt(36 * 36 * 36).toString(36).padStart(3, '0') + ".txt"

    /** Writes the note into the file that the `note:` tag of the task names; returns whether the text changed. */
    private fun saveNote(task: Task, note: String?): Boolean {
        val name = task.tag(Task.NOTE) ?: return false
        if (note == null || note == noteOf(task)) return false
        (storage as? CompanionFiles)?.writeCompanion("$NOTES_DIRECTORY/$name", note) ?: return false
        return true
    }

    // [impl->dsn~task-operations~1]
    fun add(task: Task, note: String? = null) {
        if (task.body.isBlank()) return
        guarded { saveNote(task, note) }
        write(file.add(task.created(today(), settings.writeDates)), "Add task: ${task.description}")
    }

    fun add(line: String) = add(Task.parse(line))

    fun toggle(entry: Entry) {
        val index = locate(entry) ?: return
        if (entry.task.completed) {
            write(file.replace(index, entry.task.reopen()), "Reopen task: ${entry.task.description}")
        } else {
            var new = file.replace(index, entry.task.complete(today(), settings.writeDates))
            // [impl->dsn~recurrence-on-completion~1]
            entry.task.nextOccurrence(today())?.let { new = new.add(it.created(today(), settings.writeDates)) }
            write(new, "Complete task: ${entry.task.description}")
        }
    }

    fun replace(entry: Entry, task: Task, note: String? = null) {
        val index = locate(entry) ?: return
        var noteChanged = false
        guarded { noteChanged = saveNote(task, note) }
        val new = file.replace(index, task)
        // A changed note alone is a change to record, although the todo.txt file stays the same.
        if (new == file && noteChanged) guarded { storage.write(file.serialize(), "Edit note: ${task.description}") }
        write(new, "Edit task: ${task.description}")
    }

    /** Applies the move of a task from one column of the board to another. */
    // [impl->dsn~board-move~1]
    fun move(entry: Entry, from: Group, to: Group) {
        val task = moveToGroup(entry.task, groupKey ?: return, from, to, today()) ?: return
        val index = locate(entry) ?: return
        write(file.replace(index, task), "Move task to ${to.title}: ${task.description}")
    }

    fun delete(entry: Entry) {
        val index = locate(entry) ?: return
        write(file.remove(index), "Delete task: ${entry.task.description}")
    }

    fun updateSettings(new: Settings) {
        if (new == settings) return
        val fileChanged = new.todoFile != settings.todoFile
        settings = new
        if (boardColumns.isEmpty() && groupKey == null) groupKey = GroupKey.PRIORITY
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

    private companion object {
        const val ICE_RECUR_FILE = "ice_recur.txt"
        const val NOTES_DIRECTORY = "notes"
    }

    private fun guarded(action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            error = e.message ?: e.toString()
        }
    }
}
