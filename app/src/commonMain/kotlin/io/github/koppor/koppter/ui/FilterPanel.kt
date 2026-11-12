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
import androidx.compose.material3.FilterChip
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
 * The filter criteria as a column: text, threshold date, priorities, projects, contexts, hashtags, completion.
 * Priorities, projects, contexts and hashtags offer the values that occur in the file, as the labels that the tasks carry.
 */
// [impl->dsn~filter-panel~1]
@Composable
fun FilterPanel(
    filter: Filter,
    onChange: (Filter) -> Unit,
    priorities: List<Char>,
    projects: List<String>,
    contexts: List<String>,
    hashtags: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(12.dp).testTag("filter-panel")) {
        OutlinedTextField(
            value = filter.text,
            onValueChange = { onChange(filter.copy(text = it)) },
            label = { Text("Text") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("filter-text"),
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
        // [impl->dsn~hashtags-in-ui~1]
        LabelFilter("Hashtags", hashtags, filter.hashtags, { "filter-hashtag-$it" }, { onChange(filter.copy(hashtags = it)) }) { value, suffix, modifier ->
            HashtagLabel(value, modifier, suffix)
        }

        // The criterion that is changed least often stands last.
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val open = filter.completion != Completion.COMPLETED
            val done = filter.completion != Completion.OPEN
            fun show(open: Boolean, done: Boolean) = onChange(
                filter.copy(
                    completion = when {
                        open && done -> Completion.ALL
                        open -> Completion.OPEN
                        done -> Completion.COMPLETED
                        // Switching off the only selected state selects the other one: a list of neither is of no use.
                        else -> if (filter.completion == Completion.OPEN) Completion.COMPLETED else Completion.OPEN
                    },
                ),
            )
            FilterChip(selected = open, onClick = { show(!open, done) }, label = { Text("Open") }, modifier = Modifier.testTag("filter-open"))
            FilterChip(selected = done, onClick = { show(open, !done) }, label = { Text("Done") }, modifier = Modifier.testTag("filter-done"))
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
private fun Option(label: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, onValueChange = onChange, role = Role.Checkbox).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(6.dp))
        Text(label)
    }
}

/** One of several options; the tag of an option is [tag] followed by its label. */
@Composable
fun <T> RadioRow(options: List<Pair<T, String>>, selected: T, tag: String, onSelect: (T) -> Unit) {
    Row {
        for ((option, label) in options) {
            Row(
                Modifier.selectable(selected = selected == option, onClick = { onSelect(option) }, role = Role.RadioButton)
                    .testTag("$tag-$label").padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == option, onClick = null)
                Text(label, Modifier.padding(start = 4.dp))
            }
        }
    }
}
