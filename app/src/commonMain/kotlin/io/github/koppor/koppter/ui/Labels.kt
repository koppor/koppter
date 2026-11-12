package io.github.koppor.koppter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.todotxt.Task
import kotlinx.datetime.LocalDate

/**
 * One label of a task: a rounded box with an optional key, such as `due:`, in front of the value.
 *
 * @param outlined draws a border around the box, for a label whose container has the color of its surroundings
 */
@Composable
fun Label(text: String, container: Color, content: Color, modifier: Modifier = Modifier, key: String? = null, outlined: Boolean = false) {
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(percent = 50),
        border = if (outlined) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            if (key != null) {
                Text("$key ", style = MaterialTheme.typography.labelMedium, color = content.copy(alpha = 0.7f))
            }
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * The labels of a task in the order projects, contexts, hashtags, tags, priority.
 * All colors come from the theme, so that the labels follow light and dark mode.
 */
// [impl->dsn~task-labels~1]
@Composable
fun TaskLabels(task: Task, today: LocalDate) {
    val colors = MaterialTheme.colorScheme
    task.projects.forEach { ProjectLabel(it) }
    task.contexts.forEach { ContextLabel(it) }
    task.hashtags.forEach { HashtagLabel(it) }
    task.tags.forEach { (key, value) ->
        val overdue = key == Task.DUE && !task.completed && task.due.let { it != null && it < today }
        val dueToday = key == Task.DUE && !task.completed && task.due == today
        when {
            overdue -> Label(value, colors.errorContainer, colors.onErrorContainer, key = "$key:")
            dueToday -> Label(value, colors.primaryContainer, colors.onPrimaryContainer, key = "$key:")
            else -> Label(value, colors.surfaceVariant, colors.onSurfaceVariant, key = "$key:")
        }
    }
    task.priority?.let { PriorityLabel(it) }
}

/** @param suffix text after the value, such as the mark of a label that can be removed */
@Composable
fun ProjectLabel(project: String, modifier: Modifier = Modifier, suffix: String = "") {
    Label("+$project$suffix", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, modifier)
}

@Composable
fun ContextLabel(context: String, modifier: Modifier = Modifier, suffix: String = "") {
    Label("@$context$suffix", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, modifier)
}

// [impl->dsn~hashtags-in-ui~1]
/** Outlined, as the theme has no third container color that differs enough from those of projects and contexts. */
@Composable
fun HashtagLabel(hashtag: String, modifier: Modifier = Modifier, suffix: String = "") {
    Label("#$hashtag$suffix", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, modifier, outlined = true)
}

@Composable
fun PriorityLabel(priority: Char, modifier: Modifier = Modifier, suffix: String = "") {
    val colors = MaterialTheme.colorScheme
    val text = "$priority$suffix"
    when (priority) {
        'A' -> Label(text, colors.error, colors.onError, modifier)
        'B' -> Label(text, colors.tertiary, colors.onTertiary, modifier)
        'C' -> Label(text, colors.secondary, colors.onSecondary, modifier)
        else -> Label(text, colors.surfaceVariant, colors.onSurfaceVariant, modifier)
    }
}
