package io.github.koppor.koppter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.storage.Appearance
import io.github.koppor.koppter.storage.ColorTheme
import io.github.koppor.koppter.storage.ConfigFile
import io.github.koppor.koppter.todotxt.PriorityNames
import io.github.koppor.koppter.storage.Settings
import kotlinx.coroutines.launch

/** Git access that the application brings along, for a repository that is not cloned yet. */
interface GitSetupActions {
    /**
     * The public SSH key of this device, to be registered with the git server;
     * `null` where the application uses the SSH configuration of the user.
     */
    fun publicKey(): String?

    /** Clones the repository and returns the location of its todo.txt file. */
    suspend fun clone(url: String): String
}

/** Whether the text names a git repository on a server rather than a file: a URL, or the form `user@host:path` of SSH. */
internal fun isGitAddress(text: String): Boolean = Regex("(ssh|git|https?)://.+|[\\w.-]+@[\\w.-]+:.+").matches(text)

/** Scales offered for the names of the priorities: the letters of the format, numbers, and steps of the Fibonacci sequence. */
private val PRIORITY_PRESETS = listOf(
    emptyList(),
    listOf("1", "2", "3", "4", "5"),
    listOf("1", "2", "3", "5", "8"),
    listOf("1", "2", "3", "5", "8", "13", "21", "34"),
)

// [impl->dsn~settings-dialog~1]
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    settings: Settings,
    defaultTodoFile: String,
    chooseFile: (suspend () -> String?)?,
    git: GitSetupActions?,
    /** The definitions of the board columns; `null` if the storage cannot keep them. */
    boardColumns: String?,
    /** The share actions; `null` if the storage cannot keep them. */
    shareActions: String?,
    /** The names of the priorities; `null` if the storage cannot keep them. */
    priorityNames: String?,
    /** Called when the user chooses colors: they apply at once, so that the user sees them behind the dialog. */
    onColors: (ColorTheme, Appearance) -> Unit,
    onSave: (Settings, boardColumns: String?, shareActions: String?, priorityNames: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var todoFile by remember { mutableStateOf(settings.todoFile) }
    var writeDates by remember { mutableStateOf(settings.writeDates) }
    var topCount by remember { mutableStateOf(settings.topCount.toString()) }
    val topCountValue = topCount.trim().toIntOrNull()?.takeIf { it > 0 }
    var columns by remember { mutableStateOf(boardColumns) }
    var actions by remember { mutableStateOf(shareActions) }
    var priorities by remember { mutableStateOf(priorityNames) }
    var cloning by remember { mutableStateOf(false) }
    var cloneMessage by remember { mutableStateOf("") }

    val state = listOf(todoFile, writeDates, topCount, columns, actions, priorities)
    val initial = remember { state }

    GuardedDialog(changed = state != initial, onDismiss = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.widthIn(max = 560.dp).testTag("settings-dialog")) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Settings", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = todoFile,
                    onValueChange = { todoFile = it },
                    label = { Text(if (git == null) "todo.txt file" else "todo.txt file or address of a git repository") },
                    placeholder = { Text(defaultTodoFile) },
                    supportingText = {
                        Text(
                            "Empty: $defaultTodoFile" +
                                if (git == null) "" else "\nAn address such as git@github.com:user/todo.git is cloned on Save, and the todo.txt file of the clone is used.",
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings-file"),
                )
                if (cloneMessage.isNotEmpty()) {
                    Text(cloneMessage, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("settings-clone-message"))
                }
                if (chooseFile != null) {
                    OutlinedButton(onClick = { scope.launch { chooseFile()?.let { todoFile = it } } }, modifier = Modifier.testTag("settings-choose-file")) {
                        Text("Choose file")
                    }
                }
                Row(
                    Modifier.fillMaxWidth().toggleable(value = writeDates, onValueChange = { writeDates = it }, role = Role.Checkbox)
                        .testTag("settings-write-dates"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = writeDates, onCheckedChange = null, modifier = Modifier.padding(6.dp))
                    Text("Write creation and completion dates into the file")
                }
                OutlinedTextField(
                    value = topCount,
                    onValueChange = { topCount = it },
                    label = { Text("Number of tasks in the Top view") },
                    isError = topCountValue == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings-top-count"),
                )
                columns?.let { text ->
                    ConfigField(
                        "Board columns by filter", text, { columns = it }, "Today = due:<=today -@waiting\nWaiting = @waiting",
                        "One column per line as title = filter.", "settings-board-columns",
                    )
                }
                // [impl->dsn~priority-names~1]
                priorities?.let { text ->
                    ConfigField(
                        "Names of the priorities", text, { priorities = it }, "A = 1\nB = 2\nC = 3",
                        "One priority per line as letter = name. The tasks show the names, and a task is given one of these priorities; the file keeps the letters.",
                        "settings-priorities",
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PRIORITY_PRESETS.forEachIndexed { index, names ->
                            AssistChip(
                                onClick = { priorities = PriorityNames.lines(names) },
                                label = { Text(if (names.isEmpty()) "Letters" else names.joinToString(", ")) },
                                modifier = Modifier.testTag("settings-priorities-preset-$index"),
                            )
                        }
                    }
                }
                // [impl->dsn~share-actions~1]
                actions?.let { text ->
                    ConfigField(
                        "Share actions", text, { actions = it }, "reading = {} @research>reading due:2w dur:1h",
                        "What sharing a text to koppter on Android adds: one action per line as title = task; {} stands for the shared text.",
                        "settings-share-actions",
                    )
                }
                Text("Colors", style = MaterialTheme.typography.titleMedium)
                RadioRow(ColorTheme.entries.map { it to it.title }, settings.theme, tag = "settings-theme", onSelect = { onColors(it, settings.appearance) })
                RadioRow(Appearance.entries.map { it to it.title }, settings.appearance, tag = "settings-appearance", onSelect = { onColors(settings.theme, it) })
                Text(
                    "A file in a git repository is committed after every change and synchronized with the remote repository.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (git != null) {
                    GitSection(git)
                }
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    fun save(file: String) = onSave(
                        settings.copy(todoFile = file, writeDates = writeDates, topCount = topCountValue ?: settings.topCount),
                        columns?.trim(),
                        actions?.trim(),
                        priorities?.trim(),
                    )
                    Button(
                        onClick = {
                            val file = todoFile.trim()
                            if (git != null && isGitAddress(file)) {
                                cloning = true
                                cloneMessage = "Cloning"
                                scope.launch {
                                    try {
                                        save(git.clone(file))
                                    } catch (e: Exception) {
                                        cloneMessage = "Not cloned: ${e.message ?: e}"
                                    }
                                    cloning = false
                                }
                            } else {
                                save(file)
                            }
                        },
                        enabled = topCountValue != null && !cloning,
                        modifier = Modifier.testTag("settings-save"),
                    ) { Text("Save") }
                }
            }
        }
    }
}

/** A field for a block of the configuration file: lines of the form `key = value`, with an example shown while it is empty. */
@Composable
private fun ConfigField(title: String, value: String, onChange: (String) -> Unit, example: String, help: String, tag: String) {
    val style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
    Column {
        // The title stands above the field: as a label in it, it would hide the example until the field has the focus.
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            // Paler than the placeholder of Material, which is hard to tell from lines that were typed.
            placeholder = { Text(example, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            supportingText = {
                Text("$help ${if (value.isEmpty()) "The pale lines are an example. " else ""}Kept in ${ConfigFile.NAME} next to the todo.txt file.")
            },
            textStyle = style,
            minLines = maxOf(2, example.lines().size),
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
    }
}

/** The public key of the device, where the application has a key of its own. */
// [impl->dsn~git-setup-dialog~1]
@Composable
private fun GitSection(git: GitSetupActions) {
    val publicKey = remember { runCatching(git::publicKey).getOrElse { "No key: ${it.message}" } } ?: return

    Text("Git repository", style = MaterialTheme.typography.titleMedium)
    Text("Public SSH key of this device. Register it with the git server, for GitHub as a deploy key with write access.", style = MaterialTheme.typography.bodySmall)
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        SelectionContainer {
            Text(
                publicKey,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp).testTag("settings-public-key"),
            )
        }
    }
}
