package io.github.koppor.koppter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.todotxt.Completion
import io.github.koppor.koppter.todotxt.Filter

/**
 * The filter criteria as a column: completion, text, priorities, projects, contexts.
 * Priorities, projects and contexts offer the values that occur in the file, as the labels that the tasks carry.
 */
// [impl->dsn~filter-panel~1]
@Composable
fun FilterPanel(
    filter: Filter,
    onChange: (Filter) -> Unit,
    priorities: List<Char>,
    projects: List<String>,
    contexts: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(12.dp).testTag("filter-panel")) {
        Heading("Completed")
        Row {
            for ((completion, label) in listOf(Completion.ALL to "All", Completion.COMPLETED to "Yes", Completion.OPEN to "No")) {
                Row(
                    Modifier.selectable(
                        selected = filter.completion == completion,
                        onClick = { onChange(filter.copy(completion = completion)) },
                        role = Role.RadioButton,
                    ).testTag("filter-completed-$label").padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = filter.completion == completion, onClick = null)
                    Text(label, Modifier.padding(start = 4.dp))
                }
            }
        }

        OutlinedTextField(
            value = filter.text,
            onValueChange = { onChange(filter.copy(text = it)) },
            label = { Text("Text") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("filter-text"),
        )

        Option("Show tasks before their threshold date", !filter.hideFuture, "filter-future") {
            onChange(filter.copy(hideFuture = !it))
        }

        LabelFilter("Priorities", priorities, filter.priorities, { "filter-priority-$it" }, { onChange(filter.copy(priorities = it)) }) { value, suffix, modifier ->
            PriorityLabel(value, modifier, suffix)
        }
        LabelFilter("Projects", projects, filter.projects, { "filter-project-$it" }, { onChange(filter.copy(projects = it)) }) { value, suffix, modifier ->
            ProjectLabel(value, modifier, suffix)
        }
        LabelFilter("Contexts", contexts, filter.contexts, { "filter-context-$it" }, { onChange(filter.copy(contexts = it)) }) { value, suffix, modifier ->
            ContextLabel(value, modifier, suffix)
        }

        OutlinedButton(
            onClick = { onChange(Filter()) },
            enabled = !filter.isDefault,
            modifier = Modifier.padding(top = 12.dp).testTag("filter-clear"),
        ) {
            Text("Clear all filters")
        }
    }
}

/**
 * One criterion with several values, shown as the labels the task list uses for them.
 * The selected values stand beside the title, the others below it; a click moves a label to the other group.
 * Both groups are sorted.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T : Comparable<T>> LabelFilter(
    title: String,
    values: List<T>,
    selected: Set<T>,
    tag: (T) -> String,
    onChange: (Set<T>) -> Unit,
    label: @Composable (value: T, suffix: String, modifier: Modifier) -> Unit,
) {
    if (values.isEmpty() && selected.isEmpty()) return
    // The area that reacts to a click is larger than the label, which is too small for a finger.
    fun modifier(value: T, isSelected: Boolean) = Modifier
        .clip(CircleShape)
        .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onChange(if (it) selected + value else selected - value) })
        .testTag(tag(value))
        .padding(vertical = 5.dp)
    FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.CenterVertically).padding(vertical = 5.dp))
        if (selected.isEmpty()) {
            Text(
                "all",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        for (value in selected.sorted()) label(value, " \u00d7", modifier(value, true))
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (value in values.sorted().filter { it !in selected }) label(value, "", modifier(value, false))
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
}

@Composable
private fun Option(label: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, onValueChange = onChange, role = Role.Checkbox).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(6.dp))
        Text(label)
    }
}
