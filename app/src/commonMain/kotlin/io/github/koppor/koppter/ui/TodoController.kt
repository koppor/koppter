package io.github.koppor.koppter.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.koppor.koppter.storage.CompanionFiles
import io.github.koppor.koppter.plan.CalendarTxt
import io.github.koppor.koppter.plan.Ics
import io.github.koppor.koppter.plan.PlanState
import io.github.koppor.koppter.plan.Planned
import io.github.koppor.koppter.plan.Span
import io.github.koppor.koppter.plan.TextBlock
import io.github.koppor.koppter.plan.busyTimes
import io.github.koppor.koppter.plan.plan
import io.github.koppor.koppter.storage.ConfigFile
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
import io.github.koppor.koppter.todotxt.PriorityNames
import io.github.koppor.koppter.todotxt.Query
import io.github.koppor.koppter.todotxt.ShareAction
import io.github.koppor.koppter.todotxt.groupByColumns
import io.github.koppor.koppter.todotxt.parseBoardColumns
import io.github.koppor.koppter.todotxt.groupEntries
import io.github.koppor.koppter.todotxt.iceRecurOccurrences
import io.github.koppor.koppter.todotxt.iceRecurTasks
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.hierarchyOrder
import io.github.koppor.koppter.todotxt.withAncestors
import io.github.koppor.koppter.todotxt.TodoFile
import io.github.koppor.koppter.todotxt.listOrder
import io.github.koppor.koppter.todotxt.moveToGroup
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.random.Random

/**
 * The state of the application and the operations on it.
 *
 * Every change is written to the storage at once; there is no unsaved state.
 */
// ponytail: file access runs on the caller's thread; move it off the UI thread when a storage is slow (network).
/** What the main screen shows of the tasks. */
enum class View(val title: String) { LIST("List"), BOARD("Board"), OVERVIEW("Overview"), AGENDA("Agenda"), WEEK("Week"), INBOX("Inbox") }

class TodoController(
    settings: Settings,
    private val storageFor: (Settings) -> TodoStorage,
    private val saveSettings: (Settings) -> Unit = {},
    val today: () -> LocalDate,
    /** The minutes of today that have passed. */
    private val minuteOfDay: () -> Int = { 0 },
    /** Reads the file at an address of the web; `null` if the platform cannot. */
    private val fetch: (suspend (String) -> String)? = null,
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
    var view by mutableStateOf(View.LIST)

    val board: Boolean get() = view == View.BOARD

    /** The attribute whose values are the columns of the board; `null` for the columns the user defined. */
    var groupKey by mutableStateOf<GroupKey?>(GroupKey.PRIORITY)

    /** The configuration file next to the todo.txt file; `null` if the storage keeps no files next to the todo.txt file. */
    private var config by mutableStateOf<String?>(null)

    /** The blocks of `calendar.txt`; `null` without that file. */
    var planBlocks by mutableStateOf<List<TextBlock>?>(null)
        private set

    /** When the tasks are planned from today on, by the text of their blocks. */
    private val planned: Map<String, Planned>? get() = planBlocks?.let { CalendarTxt.planned(it, today()) }

    /** The open task that a block of the plan is for; `null` if the task was completed, deleted or edited since. */
    fun entryOf(block: TextBlock): Entry? = file.entries.firstOrNull { !it.task.completed && CalendarTxt.text(it.task) == block.text }

    /** The open tasks that are due today or earlier and have no block today. */
    // [impl->dsn~agenda~1]
    val dueUnplanned: List<Entry>
        get() {
            val today = today()
            val plannedToday = planBlocks.orEmpty().filter { it.date == today }.map { it.text }.toSet()
            return file.entries.filter { entry ->
                !entry.task.completed && entry.task.due.let { it != null && it <= today } && CalendarTxt.text(entry.task) !in plannedToday
            }.sortedWith(listOrder)
        }

    /** The visible open tasks without context, which wait for one. */
    // [impl->dsn~inbox~1]
    val inbox: List<Entry> get() = visible.filter { !it.task.completed && it.task.contexts.isEmpty() }

    /** Gives a task of the inbox its context. */
    fun assignContext(entry: Entry, context: String) = replace(entry, entry.task.edit(contexts = listOf(context)))

    /** The columns the user defined, as the configuration states them. */
    private val definedColumns: List<BoardColumn> get() = ConfigFile.boardColumns(config.orEmpty())

    /** The columns the user defined; their filters name a priority by its letter, also where the configuration uses its name. */
    // [impl->dsn~priority-names~1]
    val boardColumns: List<BoardColumn> get() = definedColumns.map { it.copy(expression = priorityNames.toLettersInFilter(it.expression)) }

    /** The columns the user defined, one per line as `title = filter expression`; `null` if the storage cannot keep them. */
    val boardColumnsText: String? get() = config?.let { definedColumns.joinToString("\n") { "${it.title} = ${it.expression}" } }

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
        get() = groupKey?.let { key ->
            val groups = groupEntries(visible, key, today())
            if (key != GroupKey.PRIORITY) groups
            else groups.map { group -> group.value?.singleOrNull()?.let { group.copy(title = priorityNames.name(it)) } ?: group }
        } ?: groupByColumns(visible, boardColumns, today())

    val projects: List<String> get() = file.entries.flatMap { it.task.projects }.distinct().sorted()
    /** The contexts of the file and the contexts above them, a context directly before those below it. */
    val contexts: List<String> get() = file.entries.flatMap { it.task.contexts }.flatMap { it.withAncestors() }.distinct().sortedWith(hierarchyOrder)
    val hashtags: List<String> get() = file.entries.flatMap { it.task.hashtags }.distinct().sorted()
    val priorities: List<Char> get() = file.entries.mapNotNull { it.task.priority }.distinct().sorted()

    fun load() {
        guarded {
            file = TodoFile.parse(storage.read())
            loadedVersion = storage.version()
        }
        loadConfig()
    }

    /** Reads the configuration file; a synchronization may change it while the todo.txt file stays the same. */
    // [impl->dsn~board-defined-columns~1]
    private fun loadConfig() {
        config = try {
            (storage as? CompanionFiles)?.let { it.readCompanion(ConfigFile.NAME).orEmpty() }
        } catch (_: Exception) {
            config
        }
        if (boardColumns.isEmpty() && groupKey == null) groupKey = GroupKey.PRIORITY
        planBlocks = try {
            (storage as? CompanionFiles)?.readCompanion(CalendarTxt.NAME)?.let(CalendarTxt::blocks)
        } catch (_: Exception) {
            planBlocks
        }
    }

    /** Replaces the columns the user defined by those in the text, one per line as `title = filter expression`. */
    fun updateBoardColumns(text: String) {
        val columns = parseBoardColumns(text)
        if (columns == definedColumns) return
        writeConfig(ConfigFile.withBoardColumns(config.orEmpty(), columns), "Edit board columns")
    }

    private fun writeConfig(text: String, message: String) {
        guarded {
            (storage as? CompanionFiles)?.writeCompanion(ConfigFile.NAME, text) ?: return@guarded
            // The storage records the files next to the todo.txt file with a change of that file.
            storage.write(file.serialize(), message)
            loadedVersion = storage.version()
        }
        loadConfig()
    }

    /** The names under which the priorities are shown. */
    // [impl->dsn~priority-names~1]
    val priorityNames: PriorityNames get() = PriorityNames.of(ConfigFile.entries(config.orEmpty(), ConfigFile.PRIORITIES))

    /** The names of the priorities, one per line as `letter = name`; `null` if the storage keeps no configuration file. */
    val priorityNamesText: String? get() = config?.let { ConfigFile.toLines(ConfigFile.entries(it, ConfigFile.PRIORITIES)) }

    fun updatePriorityNames(text: String) {
        val names = ConfigFile.parseLines(text)
        if (names == ConfigFile.entries(config.orEmpty(), ConfigFile.PRIORITIES)) return
        writeConfig(ConfigFile.withEntries(config.orEmpty(), ConfigFile.PRIORITIES, names), "Edit names of priorities")
    }

    /** The state of the plan of an open task and a text that tells its start; `null` if nothing was planned yet. */
    // [impl->dsn~plan-state~1]
    fun planOf(task: Task): Pair<PlanState, String>? {
        val plan = planned?.takeIf { !task.completed } ?: return null
        val own = plan[CalendarTxt.text(task)]
        // A task that is not due for work yet is not late for not being planned.
        if (own == null && task.threshold.let { it != null && it > today() }) return null
        return CalendarTxt.state(task, own) to (own?.let { "Planned for ${CalendarTxt.startText(it)}" } ?: "Not planned")
    }

    /** The configuration for planning as the dialog shows it; `null` if the storage keeps no files next to the todo.txt file. */
    val planSettings: PlanSettings?
        get() = config?.let { text ->
            PlanSettings(
                ConfigFile.toLines(ConfigFile.entries(text, ConfigFile.TIME_MAPS)),
                ConfigFile.toLines(ConfigFile.entries(text, ConfigFile.CONTEXT_TIME_MAPS)),
                ConfigFile.toLines(ConfigFile.entries(text, ConfigFile.BARRIERS)),
                ConfigFile.toLines(ConfigFile.entries(text, ConfigFile.LIMITS)),
            )
        }

    /** The addresses of the calendars with busy times; `null` if the platform cannot read them. */
    val calendars: String? get() = settings.calendars.takeIf { fetch != null }

    /**
     * Saves the addresses of the calendars, reads their busy times for a year from today and plans with them, see [plan].
     * A calendar that cannot be read is reported and left out.
     */
    // [impl->dsn~busy-times~1]
    suspend fun planWithCalendars(settings: PlanSettings, calendars: String?): PlanResult? {
        val fetch = fetch
        if (calendars == null || fetch == null) return plan(settings)
        updateSettings(this.settings.copy(calendars = calendars.trim()))
        val busy = mutableMapOf<LocalDate, List<Span>>()
        val problems = mutableListOf<String>()
        for (address in calendars.lines().map(String::trim).filter(String::isNotEmpty)) {
            try {
                busyTimes(fetch(address), today(), today().plus(DatePeriod(years = 1)), TimeZone.currentSystemDefault())
                    .forEach { (date, spans) -> busy[date] = busy[date].orEmpty() + spans }
            } catch (e: Exception) {
                // The address is a secret: only its host is shown.
                problems += "Calendar of ${address.substringAfter("://").substringBefore('/')} was not read: ${e.message ?: e}"
            }
        }
        return plan(settings, busy, problems)
    }

    /**
     * Saves the configuration for planning, plans the open tasks from now on and writes the plan to `calendar.txt`.
     * The todo.txt file is not changed: the plan is derived from it.
     *
     * @param busy the times that are taken, by day
     */
    // [impl->dsn~plan-dialog~1]
    fun plan(settings: PlanSettings, busy: Map<LocalDate, List<Span>> = emptyMap(), problems: List<String> = emptyList()): PlanResult? {
        var result: PlanResult? = null
        guarded {
            val files = storage as? CompanionFiles ?: return@guarded
            val newConfig = listOf(
                ConfigFile.TIME_MAPS to settings.timeMaps,
                ConfigFile.CONTEXT_TIME_MAPS to settings.contextMaps,
                ConfigFile.BARRIERS to settings.barriers,
                ConfigFile.LIMITS to settings.limits,
            ).fold(config.orEmpty()) { text, (key, lines) -> ConfigFile.withEntries(text, key, ConfigFile.parseLines(lines)) }
            if (newConfig != config.orEmpty()) files.writeCompanion(ConfigFile.NAME, newConfig)
            val planConfig = ConfigFile.planConfig(newConfig)
            // A block does not start at an odd minute.
            // ponytail: a year ahead, as far as the widest named barrier; longer when a barrier with a date lies behind.
            val upcoming = iceRecurOccurrences(files.readCompanion(ICE_RECUR_FILE).orEmpty(), today().plus(DatePeriod(days = 1))..today().plus(DatePeriod(days = 366)))
            val plan = plan(file.entries, planConfig, busy, today(), (minuteOfDay() + 14) / 15 * 15, upcoming)
            files.writeCompanion(CalendarTxt.NAME, CalendarTxt.write(files.readCompanion(CalendarTxt.NAME).orEmpty(), plan.blocks, today()))
            // [impl->dsn~ics-export~1]
            files.writeCompanion(Ics.NAME, Ics.write(plan.blocks, today()))
            // The storage records the files next to the todo.txt file with a change of that file.
            storage.write(file.serialize(), "Update the plan")
            loadedVersion = storage.version()
            // By the text, as each day of a schedule is a task of its own.
            val tasks = plan.blocks.filterNot { it.travel }.map { it.text }.distinct().size
            result = PlanResult(
                if (plan.blocks.isEmpty()) "Nothing was planned." else "Planned $tasks tasks until ${plan.blocks.last().date}.",
                problems + planConfig.problems + plan.problems,
                plan.unplaced.map { entry ->
                    "${entry.task.description}${entry.task.contexts.firstOrNull()?.let { " @$it" }.orEmpty()} (barrier ${planConfig.barrierOf(entry.task, today())})"
                },
            )
        }
        loadConfig()
        return result
    }

    // [impl->dsn~share-actions~1]
    val shareActions: List<ShareAction> get() = ConfigFile.shareActions(config.orEmpty())

    /** The share actions as the settings show them; `null` if the storage keeps no configuration file. */
    val shareActionsText: String? get() = config?.let { shareActions.joinToString("\n") { "${it.title} = ${it.template}" } }

    /** Replaces the share actions by those in the text, one per line as `title = template`. */
    fun updateShareActions(text: String) {
        val actions = ShareAction.parse(text)
        if (actions == shareActions) return
        writeConfig(ConfigFile.withShareActions(config.orEmpty(), actions), "Edit share actions")
    }

    /**
     * Adds the text that another application shared as a task.
     * [action] is the title of the share action to apply; without it or if there is none with this title, the first one is applied.
     * Returns what to tell the user.
     */
    fun share(text: String, action: String? = null): String {
        val chosen = shareActions.firstOrNull { it.title == action } ?: shareActions.firstOrNull()
        val task = chosen?.task(text, today()) ?: Task(body = text.trim().replace(Regex("\\s+"), " "))
        error = null
        add(task)
        return error ?: "Added${chosen?.let { " (${it.title})" }.orEmpty()}: ${task.description}"
    }

    /** Reloads if another program changed the file. */
    // [impl->dsn~reload-on-external-change~1]
    fun reloadIfChanged() {
        val version = try {
            storage.version()
        } catch (_: Exception) {
            return
        }
        if (version != loadedVersion) load() else loadConfig()
        addScheduledTasks()
    }

    /**
     * Adds the tasks that `ice_recur.txt` schedules for today and for the days since the last run, once per day and device.
     * It waits for a running synchronization, so that tasks another device added in the meantime are known.
     */
    // [impl->dsn~ice-recur-run~1]
    private fun addScheduledTasks() {
        val today = today()
        if (settings.iceRecurLastRun == today.toString() || syncStatus?.state == SyncState.RUNNING) return
        guarded {
            val schedule = (storage as? CompanionFiles)?.readCompanion(ICE_RECUR_FILE) ?: return@guarded
            // The days since the last run, so that the tasks of a day on which the application was not open are not lost.
            val from = Task.parseDate(settings.iceRecurLastRun)?.plus(DatePeriod(days = 1))?.takeIf { it <= today } ?: today
            // A task that was added on these days, by any device, counts as present even if it is completed already.
            val addedSince = (storage as? SyncingStorage)?.dates().orEmpty().filterValues { it.added.let { added -> added != null && added >= from } }.keys
            val existing = file.lines.filter { it.isNotBlank() }.map { line -> Task.parse(line).let { if (line in addedSince) it.reopen() else it } }
            val tasks = iceRecurTasks(schedule, from..today, existing)
            if (tasks.isNotEmpty()) {
                write(tasks.fold(file) { current, (day, task) -> current.add(task.created(day, settings.writeDates)) }, "Add recurring tasks")
            }
            updateSettings(settings.copy(iceRecurLastRun = today.toString()))
        }
    }

    /** Whether the storage keeps files next to the todo.txt file, of which the schedule is one. */
    val canSchedule: Boolean get() = config != null

    /** The schedule of `ice_recur.txt`; empty if the file does not exist, `null` if it cannot be read. */
    fun schedule(): String? = try {
        (storage as? CompanionFiles)?.let { it.readCompanion(ICE_RECUR_FILE).orEmpty().replace("\r\n", "\n") }
    } catch (e: Exception) {
        error = e.message ?: e.toString()
        null
    }

    /** Writes the schedule and adds the tasks that its new and changed entries have for today. */
    // [impl->dsn~ice-recur-editor~1]
    fun updateSchedule(text: String) {
        val old = schedule() ?: return
        val new = text.trimEnd().let { if (it.isEmpty()) "" else "$it\n" }
        if (new == old) return
        guarded {
            (storage as? CompanionFiles)?.writeCompanion(ICE_RECUR_FILE, new) ?: return@guarded
            // The daily run does not come back to today, so an entry that was not there takes effect now.
            val today = today()
            val known = old.lines().toSet()
            val tasks = iceRecurTasks(new.lines().filterNot { it in known }.joinToString("\n"), today..today, file.entries.map { it.task })
            val added = tasks.fold(file) { current, (day, task) -> current.add(task.created(day, settings.writeDates)) }
            // The storage records the files next to the todo.txt file with a change of that file.
            storage.write(added.serialize(), "Edit scheduled tasks")
            file = added
            loadedVersion = storage.version()
        }
    }

    /** The text of the note of the task; empty if it has none, `null` if the storage keeps no notes. */
    // [impl->dsn~notes~1]
    fun noteOf(task: Task): String? = try {
        (storage as? CompanionFiles)?.let { files -> task.tag(Task.NOTE)?.let { files.readCompanion("$NOTES_DIRECTORY/$it") }.orEmpty() }
    } catch (_: Exception) {
        null
    }

    /** A file name for the note of a task that has none yet; Markdown, so that programs that show the file format it. */
    fun newNoteName(): String = Random.nextInt(36 * 36 * 36).toString(36).padStart(3, '0') + ".md"

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

    fun add(line: String) = add(Task.parse(priorityNames.toLetters(line)).withDates(today()).withContextSwitcherTags())

    /** The line that [add] writes for the typed one, if words of it become dates; `null` otherwise. */
    fun datesPreview(line: String): String? = Task.parse(priorityNames.toLetters(line)).let { task -> task.withDates(today()).takeIf { it != task }?.toLine() }

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

    /**
     * The task as it is after a move from one column of the board to another; `null` if that move is not possible.
     * For a column the user defined, the task is changed as little as possible so that it matches the filter of the column.
     */
    // [impl->dsn~board-move~1]
    // [impl->dsn~board-move-to-filter~1]
    fun moved(entry: Entry, from: Group, to: Group): Task? {
        val key = groupKey
        val task = when {
            key != null -> moveToGroup(entry.task, key, from, to, today())
            from.title == to.title -> null
            else -> boardColumns.firstOrNull { it.title == to.title }?.let { Query.parse(it.expression).applyTo(entry.task, today()) }
        }
        return task?.takeIf { it != entry.task }
    }

    /**
     * Applies the move of a task from one column of the board to another, or within its column.
     * With [at], the task is also put before or behind that task of the column.
     * Within its column, it takes the priority of that task, as [drop] does it.
     */
    // [impl->dsn~board-drag-position~1]
    fun move(entry: Entry, from: Group, to: Group, at: Entry? = null, before: Boolean = false) {
        val task = moved(entry, from, to)
        if (task == null && (from.title != to.title || at == null)) return
        val index = locate(entry) ?: return
        var new = task?.let { file.replace(index, it) } ?: file
        // A move to another column changes what the column stands for and nothing else: the task gets its place only among tasks of its priority.
        if (at != null && (task == null || task.priority == at.task.priority)) locate(at)?.let { other -> new.moveTo(index, other, before)?.let { new = it } }
        write(new, if (task == null) "Move task: ${entry.task.description}" else "Move task to ${to.title}: ${task.description}")
    }

    /** Puts the task before or behind another one of the list: see [TodoFile.moveTo]. */
    // [impl->dsn~list-drag~1]
    fun drop(entry: Entry, target: Entry, before: Boolean) {
        val index = locate(entry) ?: return
        val other = locate(target) ?: return
        write(file.moveTo(index, other, before) ?: return, "Move task: ${entry.task.description}")
    }

    /** Lets the task step over its neighbor in the list: see [TodoFile.step]. */
    // [impl->dsn~list-step~1]
    fun step(entry: Entry, neighbor: Entry, up: Boolean) {
        val index = locate(entry) ?: return
        val other = locate(neighbor) ?: return
        write(file.step(index, other, up) ?: return, "Move task ${if (up) "up" else "down"}: ${entry.task.description}")
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
