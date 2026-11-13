package io.github.koppor.koppter.ui

import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.sync.TaskDates
import io.github.koppor.koppter.todotxt.Recurrence
import io.github.koppor.koppter.todotxt.Task
import kotlinx.datetime.LocalDate

/**
 * Edits one task.
 *
 * The first field holds the description only; the metadata has its own fields below it.
 * Below the fields, the dialog shows the todo.txt line that *Save* writes.
 */
// [impl->dsn~edit-dialog~1]
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditDialog(
    original: Task,
    isNew: Boolean,
    dates: TaskDates?,
    /** The text of the note of the task; `null` if the storage keeps no notes. */
    originalNote: String?,
    /** The file name for the note if the task has none yet. */
    newNoteName: String,
    knownProjects: List<String>,
    knownContexts: List<String>,
    knownHashtags: List<String>,
    onSave: (Task, note: String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    // The task as the line was last typed; the fields start anew from it.
    // [impl->dsn~edit-line~1]
    var base by remember { mutableStateOf(original) }
    /** The line while it is typed: what was typed stays, where the fields would write it in another form. */
    var line by remember { mutableStateOf<String?>(null) }
    var description by remember(base) { mutableStateOf(base.description) }
    var priority by remember(base) { mutableStateOf(base.priority) }
    var due by remember(base) { mutableStateOf(base.tag(Task.DUE).orEmpty()) }
    var threshold by remember(base) { mutableStateOf(base.tag(Task.THRESHOLD).orEmpty()) }
    var recurrence by remember(base) { mutableStateOf(base.tag(Task.RECURRENCE).orEmpty()) }
    var duration by remember(base) { mutableStateOf(base.tag(Task.DURATION).orEmpty()) }
    var at by remember(base) { mutableStateOf(base.tag(Task.AT).orEmpty()) }
    var by by remember(base) { mutableStateOf(base.tag(Task.BY).orEmpty()) }
    var contextSwitcher by remember(base) { mutableStateOf(base.tag(Task.CONTEXT_SWITCHER).orEmpty()) }
    var projects by remember(base) { mutableStateOf(base.projects) }
    var contexts by remember(base) { mutableStateOf(base.contexts) }
    var hashtags by remember(base) { mutableStateOf(base.hashtags) }
    var note by remember { mutableStateOf(originalNote.orEmpty()) }

    val dueValid = due.isBlank() || Task.parseDate(due.trim()) != null
    val thresholdValid = threshold.isBlank() || Task.parseDate(threshold.trim()) != null
    val recurrenceValid = recurrence.isBlank() || Recurrence.parse(recurrence.trim()) != null
    val durationValid = duration.isBlank() || Task.parseDuration(duration.trim()) != null
    val atValid = at.isBlank() || Task.parseTime(at.trim()) != null
    val byValid = by.isBlank() || Task.parseTime(by.trim()) != null
    val result = base
        .edit(description = description, priority = priority, projects = projects, contexts = contexts, hashtags = hashtags)
        .withTag(Task.DUE, due.trim())
        .withTag(Task.BY, by.trim())
        .withTag(Task.THRESHOLD, threshold.trim())
        .withTag(Task.RECURRENCE, recurrence.trim())
        .withTag(Task.DURATION, duration.trim())
        .withTag(Task.AT, at.trim())
        // A link pasted into the description counts only while the field is empty.
        .withTag(Task.CONTEXT_SWITCHER, Task.contextSwitcherId(contextSwitcher))
        .let { if (contextSwitcher.isBlank()) it.withContextSwitcherTags() else it }
        // [impl->dsn~notes~1]
        .let { if (originalNote == null) it else it.withTag(Task.NOTE, if (note.isBlank()) null else base.tag(Task.NOTE) ?: newNoteName) }
    val valid = dueValid && thresholdValid && recurrenceValid && durationValid && atValid && byValid && result.body.isNotBlank()

    val descriptionFocus = remember { FocusRequester() }
    val keyboard = LocalKeyboard.current
    LaunchedEffect(Unit) { if (keyboard) descriptionFocus.requestFocus() }

    val state = result.toLine() to note
    val initial = remember { state }

    GuardedDialog(changed = state != initial, onDismiss = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier.widthIn(max = 560.dp).testTag("edit-dialog")
                // [impl->dsn~keyboard-shortcuts~1]
                .onPreviewKeyEvent {
                    val save = it.type == KeyEventType.KeyDown && it.key == Key.Enter && (it.isCtrlPressed || it.isMetaPressed)
                    if (save && valid) onSave(result, if (originalNote == null) null else note)
                    save
                },
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.replace('\n', ' ') },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(descriptionFocus).testTag("edit-description"),
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val choices = (listOf<Char?>(null) + ('A'..'F')).let { if (priority in it) it else it + priority }
                    for (choice in choices) {
                        FilterChip(
                            selected = priority == choice,
                            onClick = { priority = choice },
                            label = { Text(choice?.toString() ?: "No priority") },
                            modifier = Modifier.testTag("edit-priority-${choice ?: "none"}"),
                        )
                    }
                }

                // A flow row: three fields side by side on the desktop, wrapped on a phone.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val field = Modifier.width(165.dp)
                    DateField("Due date", due, { due = it }, dueValid, field.testTag("edit-due"))
                    // [impl->dsn~due-time~1]
                    OutlinedTextField(
                        value = by,
                        onValueChange = { by = it },
                        label = { Text("Due time") },
                        placeholder = { Text("14:00") },
                        isError = !byValid,
                        singleLine = true,
                        modifier = field.testTag("edit-by"),
                    )
                    DateField("Threshold", threshold, { threshold = it }, thresholdValid, field.testTag("edit-threshold"))
                    OutlinedTextField(
                        value = recurrence,
                        onValueChange = { recurrence = it },
                        label = { Text("Recurrence") },
                        placeholder = { Text("w, 2w, +1m") },
                        isError = !recurrenceValid,
                        singleLine = true,
                        modifier = field.testTag("edit-recurrence"),
                    )
                    // [impl->dsn~fixed-time~1]
                    OutlinedTextField(
                        value = at,
                        onValueChange = { at = it },
                        label = { Text("Fixed time") },
                        placeholder = { Text("12:00") },
                        isError = !atValid,
                        singleLine = true,
                        modifier = field.testTag("edit-at"),
                    )
                    // [impl->dsn~duration~1]
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration") },
                        placeholder = { Text("45m, 1h30m") },
                        isError = !durationValid,
                        singleLine = true,
                        modifier = field.testTag("edit-duration"),
                    )
                }

                // [impl->dsn~context-switcher-tag~1]
                OutlinedTextField(
                    value = contextSwitcher,
                    onValueChange = { contextSwitcher = it },
                    label = { Text("ContextSwitcher task") },
                    placeholder = { Text("category/task or a copied link") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit-cs"),
                )

                NameList("Project", "+", projects, knownProjects, { projects = it }, "edit-project")
                NameList("Context", "@", contexts, knownContexts, { contexts = it }, "edit-context")
                // [impl->dsn~hashtags-in-ui~1]
                // "#123" in a line is a word of the description, not a hashtag.
                NameList("Hashtag", "#", hashtags, knownHashtags, { hashtags = it }, "edit-hashtag") { it.first().isLetter() }

                if (originalNote != null) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (Markdown)") },
                        minLines = 2,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("edit-note"),
                    )
                }

                Text("Line in todo.txt", style = MaterialTheme.typography.labelMedium)
                // [impl->dsn~edit-line~1]
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = line ?: result.toLine(),
                        onValueChange = { typed ->
                            line = typed.replace('\n', ' ')
                            base = Task.parse(line!!)
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(8.dp).fillMaxWidth().onFocusChanged { if (!it.isFocused) line = null }.testTag("edit-line"),
                    )
                }

                // [impl->dsn~dates-in-dialog~1]
                val history = listOfNotNull(dates?.added?.let { "Added $it" }, dates?.completed?.let { "Completed $it" })
                if (history.isNotEmpty()) {
                    Text(
                        history.joinToString(" · ") + " (from the git history)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("edit-dates"),
                    )
                }

                Row(Modifier.fillMaxWidth()) {
                    if (!isNew) {
                        TextButton(onClick = onDelete, modifier = Modifier.testTag("edit-delete")) { Text("Delete") }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("edit-cancel")) { Text("Cancel") }
                    Button(onClick = { onSave(result, if (originalNote == null) null else note) }, enabled = valid, modifier = Modifier.testTag("edit-save")) { Text("Save") }
                }
            }
        }
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, value: String, onChange: (String) -> Unit, valid: Boolean, modifier: Modifier) {
    var picking by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") },
        isError = !valid,
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { picking = true }) { Icon(Icons.Default.DateRange, contentDescription = "Choose $label") }
        },
        modifier = modifier,
    )
    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = Task.parseDate(value.trim())?.let { it.toEpochDays() * MILLIS_PER_DAY },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(LocalDate.fromEpochDays((it / MILLIS_PER_DAY).toInt()).toString()) }
                    picking = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { onChange(""); picking = false }) { Text("No date") }
            },
        ) {
            DatePicker(state)
        }
    }
}

/** The projects, contexts or hashtags of the task as removable chips, the known names as suggestions, and a field of the size of a chip to add one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NameList(
    kind: String,
    sign: String,
    names: List<String>,
    known: List<String>,
    onChange: (List<String>) -> Unit,
    tag: String,
    /** Whether the name is one that the todo.txt line keeps as this kind of metadata. */
    valid: (String) -> Boolean = { true },
) {
    var input by remember { mutableStateOf("") }
    fun add(name: String) {
        val clean = name.trim().removePrefix(sign).replace(Regex("\\s+"), "-")
        if (clean.isNotEmpty() && clean !in names && valid(clean)) onChange(names + clean)
        input = ""
    }
    // The names of the task first, then the known names that can be added with one click.
    val suggestions = known.filter { it !in names && it.startsWith(input.trim().removePrefix(sign), ignoreCase = true) }.take(8)
    val colors = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (name in names) {
            InputChip(
                selected = true,
                onClick = { onChange(names - name) },
                label = { Text("$sign$name") },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove $kind $name") },
                modifier = Modifier.testTag("$tag-$name"),
            )
        }
        for (name in suggestions) {
            SuggestionChip(onClick = { add(name) }, label = { Text("$sign$name") }, modifier = Modifier.testTag("$tag-suggest-$name"))
        }
        BasicTextField(
            value = input,
            onValueChange = { if (it.endsWith(' ')) add(it) else input = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.labelLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add(input) }),
            modifier = Modifier.align(Alignment.CenterVertically).width(150.dp).height(32.dp)
                .border(1.dp, colors.outline, MaterialTheme.shapes.small).testTag("$tag-add"),
            decorationBox = { field ->
                Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (input.isEmpty()) Text("Add ${kind.lowercase()}…", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    field()
                }
            },
        )
    }
}
