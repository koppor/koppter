package io.github.koppor.koppter.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.todotxt.Completion
import io.github.koppor.koppter.todotxt.Filter

/**
 * The filter criteria as a column: completion, text, priorities, projects, contexts.
 * Priorities, projects and contexts list the values that occur in the file.
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

        if (priorities.isNotEmpty()) {
            Heading("Priorities")
            for (priority in priorities) {
                Option(priority.toString(), priority in filter.priorities, "filter-priority-$priority") {
                    onChange(filter.copy(priorities = filter.priorities.toggled(priority, it)))
                }
            }
        }
        if (projects.isNotEmpty()) {
            Heading("Projects")
            for (project in projects) {
                Option(project, project in filter.projects, "filter-project-$project") {
                    onChange(filter.copy(projects = filter.projects.toggled(project, it)))
                }
            }
        }
        if (contexts.isNotEmpty()) {
            Heading("Contexts")
            for (context in contexts) {
                Option(context, context in filter.contexts, "filter-context-$context") {
                    onChange(filter.copy(contexts = filter.contexts.toggled(context, it)))
                }
            }
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

private fun <T> Set<T>.toggled(value: T, selected: Boolean): Set<T> = if (selected) this + value else this - value

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
