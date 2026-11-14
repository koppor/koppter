package io.github.koppor.koppter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.plan.PlanState
import io.github.koppor.koppter.todotxt.LEVEL_SEPARATOR
import io.github.koppor.koppter.todotxt.PriorityNames
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.lastLevel
import kotlinx.datetime.LocalDate

/** Tells the state of the plan of a task and a text about it; `null` for a task that shows none. */
val LocalPlanOf = staticCompositionLocalOf<(Task) -> Pair<PlanState, String>?> { { null } }

/** What a click on the context label of a task does with the context. */
/** The names under which the priorities are shown. */
val LocalPriorityNames = staticCompositionLocalOf { PriorityNames() }

val LocalShowContext = staticCompositionLocalOf<(String) -> Unit> { {} }

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskLabels(task: Task, today: LocalDate) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    // [impl->dsn~plan-state~1]
    LocalPlanOf.current(task)?.let { (state, text) ->
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(text) } },
            state = rememberTooltipState(),
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = text,
                tint = when (state) {
                    PlanState.FITS -> LocalFineColor.current
                    PlanState.TIGHT -> colors.secondary
                    PlanState.LATE -> colors.error
                },
                modifier = Modifier.size(18.dp).testTag("plan-$state"),
            )
        }
    }
    task.projects.forEach { ProjectLabel(it) }
    // [impl->dsn~context-hierarchy~1]
    val showContext = LocalShowContext.current
    task.contexts.forEach { context ->
        val label = @Composable {
            ContextLabel(
                context,
                Modifier.clip(RoundedCornerShape(percent = 50))
                    .clickable(onClickLabel = "Show the tasks of this context", role = Role.Button) { showContext(context) }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .testTag("label-context-$context"),
            )
        }
        if (LEVEL_SEPARATOR in context) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text("@$context") } },
                state = rememberTooltipState(),
            ) { label() }
        } else {
            label()
        }
    }
    task.hashtags.forEach { HashtagLabel(it) }
    task.tags.forEach { (key, value) ->
        val overdue = key == Task.DUE && !task.completed && task.due.let { it != null && it < today }
        val dueToday = key == Task.DUE && !task.completed && task.due == today
        when {
            // [impl->dsn~context-switcher-tag~1]
            key == Task.CONTEXT_SWITCHER -> Label(
                value,
                colors.surfaceVariant,
                colors.onSurfaceVariant,
                Modifier.clip(RoundedCornerShape(percent = 50))
                    .clickable(onClickLabel = "Switch to the task in ContextSwitcher", role = Role.Button) { uriHandler.openUri(Task.contextSwitcherLink(value)) }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .testTag("label-cs"),
                key = "$key:",
            )
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

/** @param path shows all levels of a hierarchical context instead of the last one */
@Composable
fun ContextLabel(context: String, modifier: Modifier = Modifier, suffix: String = "", path: Boolean = false) {
    Label("@${if (path) context else context.lastLevel}$suffix", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, modifier)
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
    val text = LocalPriorityNames.current.name(priority) + suffix
    when (priority) {
        'A' -> Label(text, colors.error, colors.onError, modifier)
        'B' -> Label(text, colors.tertiary, colors.onTertiary, modifier)
        'C' -> Label(text, colors.secondary, colors.onSecondary, modifier)
        else -> Label(text, colors.surfaceVariant, colors.onSurfaceVariant, modifier)
    }
}
