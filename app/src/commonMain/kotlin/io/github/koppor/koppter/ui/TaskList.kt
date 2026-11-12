package io.github.koppor.koppter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.todotxt.Entry
import kotlinx.datetime.LocalDate

@Composable
fun TaskList(
    entries: List<Entry>,
    today: LocalDate,
    onToggle: (Entry) -> Unit,
    onOpen: (Entry) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("task-list"),
        contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(entries, key = { it.index }) { entry ->
            TaskCard(entry, today, onToggle = { onToggle(entry) }, onOpen = { onOpen(entry) })
        }
    }
}

/**
 * One task: the labels in a row at the top right, the description below them,
 * so that the description always starts at the same position.
 */
// [impl->dsn~task-card~1]
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCard(entry: Entry, today: LocalDate, onToggle: () -> Unit, onOpen: () -> Unit) {
    val task = entry.task
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth().testTag("task")) {
        Row(Modifier.padding(end = 10.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.completed, onCheckedChange = { onToggle() }, modifier = Modifier.testTag("task-done"))
            Column(Modifier.weight(1f)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().testTag("task-labels"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    TaskLabels(task, today)
                }
                Text(
                    task.description,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
