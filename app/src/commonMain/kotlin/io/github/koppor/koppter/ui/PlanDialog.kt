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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.plan.CalendarTxt
import io.github.koppor.koppter.storage.ConfigFile
import kotlinx.coroutines.launch

/** The parts of the configuration for planning, each as lines of the form `name = value`. */
data class PlanSettings(val timeMaps: String, val contextMaps: String, val barriers: String, val limits: String = "")

/** What a run of the planner did, as texts for the user. */
data class PlanResult(val summary: String, val problems: List<String>, val unplaced: List<String>)

/**
 * Edits the time maps, the time maps of the contexts and the barriers, and plans the open tasks into `calendar.txt`.
 * The dialog stays open after planning and tells what was planned and which tasks do not fit.
 */
// [impl->dsn~plan-dialog~1]
@Composable
fun PlanDialog(
    settings: PlanSettings,
    /** The addresses of the calendars with busy times; `null` if the platform cannot read them. */
    calendars: String?,
    onPlan: suspend (PlanSettings, calendars: String?) -> PlanResult?,
    onDismiss: () -> Unit,
) {
    var timeMaps by remember { mutableStateOf(settings.timeMaps) }
    var contextMaps by remember { mutableStateOf(settings.contextMaps) }
    var barriers by remember { mutableStateOf(settings.barriers) }
    var limits by remember { mutableStateOf(settings.limits) }
    var addresses by remember { mutableStateOf(calendars) }
    var result by remember { mutableStateOf<PlanResult?>(null) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    @Composable
    fun field(label: String, value: String, onChange: (String) -> Unit, help: String, tag: String) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            // In the small style also while the field is empty: at the size of the content, the label reads as content.
            label = { Text("$label (optional)", style = MaterialTheme.typography.bodySmall) },
            supportingText = { Text(help) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            minLines = 2,
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
    }

    val state = listOf(timeMaps, contextMaps, barriers, limits, addresses)
    // Planning saves what the fields hold.
    var saved by remember { mutableStateOf(state) }

    GuardedDialog(changed = state != saved, onDismiss = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.widthIn(max = 560.dp).testTag("plan-dialog")) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Plan", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Gives each open task the earliest free time of its time map, tasks with a higher priority first, and writes the result to " +
                        "${CalendarTxt.NAME} next to the todo.txt file. The settings are kept in ${ConfigFile.NAME}.",
                    style = MaterialTheme.typography.bodySmall,
                )
                field(
                    "Time maps", timeMaps, { timeMaps = it },
                    "When tasks are planned, one map per line: General = Mon-Fri 08:00-12:00 13:00-18:00. " +
                        "A map that starts with the name of another one is restricted to its times: Morning-Focus = General 08:00-11:00. " +
                        "The first map is for tasks whose contexts name none. Without a map: Mon-Fri 09:00-17:00.",
                    "plan-time-maps",
                )
                field(
                    "Time maps of contexts", contextMaps, { contextMaps = it },
                    "One context per line: @research = Morning-Focus. It holds for the contexts below it.",
                    "plan-context-maps",
                )
                field(
                    "Barriers", barriers, { barriers = it },
                    "The day behind which a task is not planned: week, month, quarter, year or a date. " +
                        "One per line: default = quarter, +Thesis = 2027-03-31, @research = year. The earliest barrier of a task counts. Without one: quarter.",
                    "plan-barriers",
                )
                // [impl->dsn~plan-limits~1]
                field(
                    "Limits", limits, { limits = it },
                    "How much is planned for a project or a context, rules separated by commas: @chores = max 5h/week, block 30m. " +
                        "max with /day or /week: not more is planned. min with /week: this much is planned first in each week: @paper = min 1h/week. " +
                        "block: a task is not cut into shorter parts; an hour unless stated, also as default = block 2h.",
                    "plan-limits",
                )
                // [impl->dsn~busy-times~1]
                addresses?.let {
                    field(
                        "Calendars with busy times", it, { text -> addresses = text },
                        "Addresses of iCalendar files, one per line, as Outlook and Google Calendar publish them for a calendar. " +
                            "Nothing is planned into the times of their events. Kept on this device only, as such an address is a secret.",
                        "plan-calendars",
                    )
                }
                result?.let { shown ->
                    Text(shown.summary, modifier = Modifier.testTag("plan-summary"))
                    for (problem in shown.problems) {
                        Text(problem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("plan-problem"))
                    }
                    if (shown.unplaced.isNotEmpty()) {
                        Text("Do not fit before their barrier", style = MaterialTheme.typography.titleSmall)
                        for (task in shown.unplaced) {
                            Text(task, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("plan-unplaced"))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("plan-close")) { Text("Close") }
                    Button(
                        onClick = {
                            running = true
                            saved = state
                            scope.launch {
                                try {
                                    result = onPlan(PlanSettings(timeMaps, contextMaps, barriers, limits), addresses)
                                } finally {
                                    running = false
                                }
                            }
                        },
                        enabled = !running,
                        modifier = Modifier.testTag("plan-update"),
                    ) {
                        Text(if (running) "Planning" else "Save and plan")
                    }
                }
            }
        }
    }
}
