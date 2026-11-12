package io.github.koppor.koppter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.window.Dialog
import io.github.koppor.koppter.storage.Appearance
import io.github.koppor.koppter.storage.ColorTheme
import io.github.koppor.koppter.storage.ConfigFile
import io.github.koppor.koppter.storage.Settings
import kotlinx.coroutines.launch

/** Git access on a device that has no git and no SSH configuration of its own. */
interface GitSetupActions {
    /** The public SSH key of this device, to be registered with the git server. */
    fun publicKey(): String

    /** Clones the repository and returns the location of its todo.txt file. */
    suspend fun clone(url: String): String
}

// [impl->dsn~settings-dialog~1]
@Composable
fun SettingsDialog(
    settings: Settings,
    defaultTodoFile: String,
    chooseFile: (suspend () -> String?)?,
    git: GitSetupActions?,
    /** The definitions of the board columns; `null` if the storage cannot keep them. */
    boardColumns: String?,
    onSave: (Settings, boardColumns: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var todoFile by remember { mutableStateOf(settings.todoFile) }
    var writeDates by remember { mutableStateOf(settings.writeDates) }
    var topCount by remember { mutableStateOf(settings.topCount.toString()) }
    val topCountValue = topCount.trim().toIntOrNull()?.takeIf { it > 0 }
    var columns by remember { mutableStateOf(boardColumns) }
    var theme by remember { mutableStateOf(settings.theme) }
    var appearance by remember { mutableStateOf(settings.appearance) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.widthIn(max = 560.dp).testTag("settings-dialog")) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Settings", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = todoFile,
                    onValueChange = { todoFile = it },
                    label = { Text("todo.txt file") },
                    placeholder = { Text(defaultTodoFile) },
                    supportingText = { Text("Empty: $defaultTodoFile") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings-file"),
                )
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
                    OutlinedTextField(
                        value = text,
                        onValueChange = { columns = it },
                        label = { Text("Board columns by filter") },
                        supportingText = {
                            Text("One column per line as title = filter, for example: Today = due:<=today -@waiting. Kept in ${ConfigFile.NAME} next to the todo.txt file.")
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("settings-board-columns"),
                    )
                }
                Text("Colors", style = MaterialTheme.typography.titleMedium)
                RadioRow(ColorTheme.entries.map { it to it.title }, theme, tag = "settings-theme", onSelect = { theme = it })
                RadioRow(Appearance.entries.map { it to it.title }, appearance, tag = "settings-appearance", onSelect = { appearance = it })
                Text(
                    "A file in a git repository is committed after every change and synchronized with the remote repository.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (git != null) {
                    GitSection(git, onCloned = { todoFile = it })
                }
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            onSave(
                                settings.copy(
                                    todoFile = todoFile.trim(),
                                    writeDates = writeDates,
                                    topCount = topCountValue ?: settings.topCount,
                                    theme = theme,
                                    appearance = appearance,
                                ),
                                columns?.trim(),
                            )
                        },
                        enabled = topCountValue != null,
                        modifier = Modifier.testTag("settings-save"),
                    ) { Text("Save") }
                }
            }
        }
    }
}

/** The public key of the device and a field to clone a repository, whose todo.txt file becomes the file of the application. */
// [impl->dsn~git-setup-dialog~1]
@Composable
private fun GitSection(git: GitSetupActions, onCloned: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var cloning by remember { mutableStateOf(false) }
    val publicKey = remember { runCatching(git::publicKey).getOrElse { "No key: ${it.message}" } }

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
    OutlinedTextField(
        value = url,
        onValueChange = { url = it.trim() },
        label = { Text("Repository") },
        placeholder = { Text("git@github.com:user/todo.git") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("settings-git-url"),
    )
    OutlinedButton(
        enabled = url.isNotBlank() && !cloning,
        onClick = {
            cloning = true
            message = "Cloning"
            scope.launch {
                message = try {
                    onCloned(git.clone(url))
                    "Cloned. Save to use the todo.txt file of the repository."
                } catch (e: Exception) {
                    "Not cloned: ${e.message ?: e}"
                }
                cloning = false
            }
        },
        modifier = Modifier.testTag("settings-clone"),
    ) {
        Text("Clone")
    }
    if (message.isNotEmpty()) {
        Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("settings-clone-message"))
    }
}
