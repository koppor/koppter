package io.github.koppor.koppter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.todotxt.ContextState
import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.UrgencyState
import io.github.koppor.koppter.todotxt.urgency
import kotlinx.datetime.LocalDate

@Composable
private fun UrgencyState.color(): Color = when (this) {
    UrgencyState.FINE -> MaterialTheme.colorScheme.primary
    UrgencyState.DUE -> MaterialTheme.colorScheme.secondary
    UrgencyState.OVERDUE -> MaterialTheme.colorScheme.error
}

/**
 * One tile per context of the outermost level, which tells by a colored dot how the context is doing.
 * A click on a tile shows the tasks of the context, the most urgent first, each with a bar that fills up to its due date.
 */
// [impl->dsn~overview~1]
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Overview(states: List<ContextState>, today: LocalDate, onOpen: (Entry) -> Unit, emptyText: String) {
    var selected by remember { mutableStateOf<String?>(null) }
    var showing by remember { mutableStateOf(false) }
    val shown = states.firstOrNull { it.context == selected }.takeIf { showing }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp).testTag("overview")) {
        if (states.isEmpty()) Text(emptyText, Modifier.padding(16.dp))
        if (shown == null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (state in states) {
                    Card(
                        Modifier.width(170.dp).clickable {
                            selected = state.context
                            showing = true
                        }.testTag("overview-${state.context ?: "none"}"),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(state.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Box(Modifier.size(14.dp).background(state.state.color(), CircleShape))
                            }
                            Text("${state.entries.size} open", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                listOfNotNull(
                                    state.overdue.takeIf { it > 0 }?.let { "$it overdue" },
                                    state.dueToday.takeIf { it > 0 }?.let { "$it due today" },
                                ).joinToString(" · ").ifEmpty { "Nothing due" },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showing = false }, modifier = Modifier.testTag("overview-back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "All contexts")
                }
                Text(shown.title, style = MaterialTheme.typography.titleMedium)
            }
            for (entry in shown.entries) {
                val urgency = entry.task.urgency(today)
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(entry) }.padding(horizontal = 12.dp, vertical = 8.dp).testTag("overview-task"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(entry.task.description, Modifier.weight(1f))
                    if (urgency != null) {
                        Column(Modifier.width(150.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(
                                progress = { urgency.elapsed },
                                color = urgency.state.color(),
                                drawStopIndicator = {},
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                            )
                            Text(urgency.text, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

private val ContextState.title: String get() = context?.let { "@$it" } ?: "No context"
