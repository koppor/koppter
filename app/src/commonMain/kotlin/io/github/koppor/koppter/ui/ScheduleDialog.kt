package io.github.koppor.koppter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.todotxt.iceRecurLines
import kotlinx.datetime.LocalDate

private val EXAMPLES = """
    daily - (A) Check the mail
    weekly ; day Monday, Friday - Plan the week
    monthly ; day_of_month 1, -1 - Check the account
    monthly ; day_of_week Friday => -1 - Back up the photos
    @2026-11-05 weekly 2 ; day Thursday - Put out the paper bin
""".trimIndent()

/** Edits the schedule of `ice_recur.txt` and tells for each entry on which day it adds its task next. */
// [impl->dsn~ice-recur-editor~1]
@Composable
fun ScheduleDialog(schedule: String, today: LocalDate, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(schedule) }
    val lines = iceRecurLines(text)

    GuardedDialog(changed = text != schedule, onDismiss = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.widthIn(max = 560.dp).testTag("schedule-dialog")) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Scheduled tasks", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Tasks that are added on certain days, one entry per line as schedule - task. Kept in ice_recur.txt next to the todo.txt file.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Schedule") },
                    isError = lines.any { it.entry == null },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("schedule-text"),
                )
                for (line in lines) {
                    val next = line.entry?.next(today)
                    Text(
                        when {
                            line.entry == null -> "Line ${line.number} is not understood"
                            next == null -> "Line ${line.number}: no further day"
                            else -> "Line ${line.number}: next on ${next.dayOfWeek.name.take(3).lowercase().replaceFirstChar(Char::uppercase)} $next"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (line.entry == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("schedule-line-${line.number}"),
                    )
                }
                Text("Examples", style = MaterialTheme.typography.titleSmall)
                Text(EXAMPLES, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    // Lines that are not understood are skipped by the run, so they do not prevent saving.
                    Button(onClick = { onSave(text) }, modifier = Modifier.testTag("schedule-save")) { Text("Save") }
                }
            }
        }
    }
}
