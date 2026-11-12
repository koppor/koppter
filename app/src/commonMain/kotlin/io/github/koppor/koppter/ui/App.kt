package io.github.koppor.koppter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.storage.SyncState
import io.github.koppor.koppter.storage.SyncStatus
import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.GroupKey
import io.github.koppor.koppter.todotxt.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Width from which the filter panel stands beside the list instead of sliding over it. */
private val WIDE = 840.dp

private const val RELOAD_INTERVAL_MILLIS = 2_000L

/**
 * @param chooseFile lets the user pick the todo.txt file with the file dialog of the platform and returns its location,
 *   or `null` if the user cancelled.
 * @param git offered in the settings on platforms where the application has to bring its own git access
 */
@Composable
fun App(
    controller: TodoController,
    defaultTodoFile: String,
    chooseFile: (suspend () -> String?)? = null,
    git: GitSetupActions? = null,
) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        MainScreen(controller, defaultTodoFile, chooseFile, git)
    }
}

/** The task whose edit dialog is open; `entry` is null for a task that is not in the file yet. */
private data class Editing(val entry: Entry?)

// [impl->dsn~main-screen~1]
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    controller: TodoController,
    defaultTodoFile: String,
    chooseFile: (suspend () -> String?)?,
    git: GitSetupActions?,
) {
    var editing by remember { mutableStateOf<Editing?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var schedule by remember { mutableStateOf<String?>(null) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(controller) {
        while (true) {
            delay(RELOAD_INTERVAL_MILLIS)
            controller.reloadIfChanged()
        }
    }
    LaunchedEffect(controller.error) {
        controller.error?.let {
            snackbar.showSnackbar(it)
            controller.error = null
        }
    }

    val filterPanel: @Composable (Modifier) -> Unit = { modifier ->
        FilterPanel(
            filter = controller.filter,
            onChange = { controller.filter = it },
            priorities = controller.priorities,
            projects = controller.projects,
            contexts = controller.contexts,
            hashtags = controller.hashtags,
            modifier = modifier,
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= WIDE
        ModalNavigationDrawer(
            drawerState = drawer,
            gesturesEnabled = !wide,
            drawerContent = { if (!wide) ModalDrawerSheet { filterPanel(Modifier) } },
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("koppter") },
                        navigationIcon = {
                            if (!wide) {
                                IconButton(onClick = { scope.launch { drawer.open() } }, modifier = Modifier.testTag("open-filter")) {
                                    Icon(Icons.Default.Menu, contentDescription = "Filter")
                                }
                            }
                        },
                        actions = {
                            controller.syncStatus?.let { SyncIndicator(it, onSync = controller::sync) }
                            if (controller.canSchedule) {
                                IconButton(onClick = { schedule = controller.schedule() }, modifier = Modifier.testTag("open-schedule")) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Scheduled tasks")
                                }
                            }
                            IconButton(onClick = { showSettings = true }, modifier = Modifier.testTag("open-settings")) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        },
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { editing = Editing(null) }, modifier = Modifier.testTag("new-task")) {
                        Icon(Icons.Default.Add, contentDescription = "New task")
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                Row(Modifier.padding(padding).fillMaxSize()) {
                    if (wide) {
                        filterPanel(Modifier.width(280.dp).fillMaxHeight())
                        VerticalDivider()
                    }
                    Column(Modifier.weight(1f)) {
                        QuickAdd(onAdd = controller::add)
                        ViewOptions(controller)
                        val emptyText = if (controller.file.entries.isEmpty()) "No tasks" else "No task matches the filter"
                        if (controller.board) {
                            Board(
                                controller.groups, controller.today(), controller::toggle, onOpen = { editing = Editing(it) },
                                canMove = { entry, from, to -> controller.moved(entry, from, to) != null },
                                onMove = controller::move,
                                emptyText = emptyText,
                            )
                        } else {
                            TaskList(controller.visible, controller.today(), controller::toggle, { editing = Editing(it) }, emptyText)
                        }
                    }
                }
            }
        }
    }

    editing?.let { (entry) ->
        EditDialog(
            original = entry?.task ?: Task(body = ""),
            isNew = entry == null,
            dates = remember(entry) { entry?.let { controller.datesOf(it.task) } },
            originalNote = remember(entry) { controller.noteOf(entry?.task ?: Task(body = "")) },
            newNoteName = remember(entry) { controller.newNoteName() },
            knownProjects = controller.projects,
            knownContexts = controller.contexts,
            knownHashtags = controller.hashtags,
            onSave = { task, note ->
                if (entry == null) controller.add(task, note) else controller.replace(entry, task, note)
                editing = null
            },
            onDelete = {
                entry?.let(controller::delete)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
    schedule?.let {
        ScheduleDialog(
            schedule = it,
            today = controller.today(),
            onSave = { text ->
                controller.updateSchedule(text)
                schedule = null
            },
            onDismiss = { schedule = null },
        )
    }
    if (showSettings) {
        SettingsDialog(
            settings = controller.settings,
            defaultTodoFile = defaultTodoFile,
            chooseFile = chooseFile,
            git = git,
            boardColumns = controller.boardColumnsText,
            onSave = { settings, boardColumns ->
                // The columns belong to the file that was open when the dialog was opened.
                boardColumns?.let(controller::updateBoardColumns)
                controller.updateSettings(settings)
                showSettings = false
            },
            onDismiss = { showSettings = false },
        )
    }
}

/** The switches above the tasks: only the top tasks, list or board, and what forms the columns of the board. */
// [impl->dsn~view-options~1]
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ViewOptions(controller: TodoController) {
    FlowRow(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = controller.topOnly,
            onClick = { controller.topOnly = !controller.topOnly },
            label = { Text("Top ${controller.settings.topCount}") },
            modifier = Modifier.testTag("top"),
        )
        FilterChip(
            selected = controller.board,
            onClick = { controller.board = !controller.board },
            label = { Text("Board") },
            modifier = Modifier.testTag("board-switch"),
        )
        if (controller.board) {
            for (key in GroupKey.entries) {
                FilterChip(
                    selected = controller.groupKey == key,
                    onClick = { controller.groupKey = key },
                    label = { Text("by ${key.label.lowercase()}") },
                    modifier = Modifier.testTag("group-${key.name}"),
                )
            }
            if (controller.boardColumns.isNotEmpty()) {
                FilterChip(
                    selected = controller.groupKey == null,
                    onClick = { controller.groupKey = null },
                    label = { Text("by filter") },
                    modifier = Modifier.testTag("group-FILTER"),
                )
            }
        }
    }
}

/** Shows whether the file is synchronized; a click synchronizes now. */
// [impl->dsn~sync-status~1]
@Composable
private fun SyncIndicator(status: SyncStatus, onSync: () -> Unit) {
    val (text, color) = when (status.state) {
        SyncState.IDLE -> "Synchronized" to MaterialTheme.colorScheme.onSurfaceVariant
        SyncState.RUNNING -> "Synchronizing" to MaterialTheme.colorScheme.onSurfaceVariant
        SyncState.FAILED -> "Not synchronized: ${status.message}" to MaterialTheme.colorScheme.error
    }
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 360.dp).testTag("sync-status"),
    )
    IconButton(onClick = onSync, enabled = status.state != SyncState.RUNNING, modifier = Modifier.testTag("sync-now")) {
        Icon(Icons.Default.Refresh, contentDescription = "Synchronize now")
    }
}

/** A line that adds a task from text in todo.txt syntax, for input without opening the dialog. */
// [impl->dsn~quick-add~1]
@Composable
private fun QuickAdd(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    fun submit() {
        onAdd(text)
        text = ""
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.replace("\n", "") },
        placeholder = { Text("Add a task, such as: (A) Call Mom +Family @phone due:2026-12-24") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("quick-add")
            // The Enter key of a hardware keyboard does not trigger the IME action on the desktop.
            .onPreviewKeyEvent {
                if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                    submit()
                    true
                } else {
                    false
                }
            },
    )
}
