package io.github.koppor.koppter.ui

import io.github.koppor.koppter.todotxt.LEVEL_SEPARATOR
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.plan.CalendarTxt
import io.github.koppor.koppter.plan.TextBlock
import io.github.koppor.koppter.todotxt.Entry
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private const val NO_PLAN = "Nothing is planned yet. Plan gives the open tasks their times."

/**
 * What is up today and on the six days after it: the blocks of the plan by day, each with its task,
 * and for today the tasks that are due but have no block today.
 */
// [impl->dsn~agenda~1]
@Composable
fun Agenda(
    blocks: List<TextBlock>?,
    dueUnplanned: List<Entry>,
    entryOf: (TextBlock) -> Entry?,
    today: LocalDate,
    onToggle: (Entry) -> Unit,
    onOpen: (Entry) -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp).testTag("agenda"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (blocks == null) Text(NO_PLAN, Modifier.padding(8.dp))
        for (offset in 0..6) {
            val day = today.plus(DatePeriod(days = offset))
            val ofDay = blocks.orEmpty().filter { it.date == day }
            if (ofDay.isEmpty() && offset > 0) continue
            Text(
                listOfNotNull("Today".takeIf { offset == 0 }, "Tomorrow".takeIf { offset == 1 }, CalendarTxt.dayText(day)).joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp).testTag("agenda-day-$day"),
            )
            if (ofDay.isEmpty()) Text("No block", Modifier.padding(start = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            for (block in ofDay) {
                val entry = entryOf(block)
                Row(Modifier.testTag("agenda-block"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(CalendarTxt.timesText(block.span), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(96.dp))
                    Box(Modifier.weight(1f)) {
                        if (entry != null) {
                            TaskCard(entry, today, onToggle = { onToggle(entry) }, onOpen = { onOpen(entry) })
                        } else {
                            // The task was completed, deleted or edited after it was planned.
                            Text(block.text, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (offset == 0 && dueUnplanned.isNotEmpty()) {
                Text(
                    "Due and without a block today",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp).testTag("agenda-due"),
                )
                for (entry in dueUnplanned) TaskCard(entry, today, onToggle = { onToggle(entry) }, onOpen = { onOpen(entry) })
            }
        }
    }
}

private val MIN_HOUR_HEIGHT = 48.dp

/** The blocks of the plan of one week as a calendar: one column per day, the time running down. */
// [impl->dsn~agenda~1]
@Composable
fun Week(blocks: List<TextBlock>?, entryOf: (TextBlock) -> Entry?, today: LocalDate, onOpen: (Entry) -> Unit) {
    val thisWeek = today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1))
    var monday by remember { mutableStateOf(thisWeek) }
    val days = (0..6).map { monday.plus(DatePeriod(days = it)) }
    val ofWeek = blocks.orEmpty().filter { it.date in days }
    // Office hours at least, so that an empty week has a grid as well.
    val firstHour = minOf(8, ofWeek.minOfOrNull { it.span.start / 60 } ?: 8)
    val lastHour = maxOf(18, ofWeek.maxOfOrNull { (it.span.end + 59) / 60 } ?: 18)

    Column(Modifier.padding(8.dp).testTag("week")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { monday = monday.minus(DatePeriod(days = 7)) }, modifier = Modifier.testTag("week-previous")) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
            }
            IconButton(onClick = { monday = monday.plus(DatePeriod(days = 7)) }, modifier = Modifier.testTag("week-next")) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
            }
            Text("Week ${CalendarTxt.week(monday)} · $monday to ${days.last()}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("week-title"))
            if (monday != thisWeek) TextButton(onClick = { monday = thisWeek }, modifier = Modifier.testTag("week-today")) { Text("This week") }
        }
        if (blocks == null) Text(NO_PLAN, Modifier.padding(8.dp))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // On a phone, the days keep a readable width and the week scrolls sideways.
            val dayWidth = maxOf(110.dp, (maxWidth - 44.dp) / 7)
            // The hours share the height that is left; in a low window they keep a readable height and the week scrolls.
            val hourHeight = maxOf(MIN_HOUR_HEIGHT, (maxHeight - 28.dp) / (lastHour - firstHour))
            Row(Modifier.verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                Column(Modifier.width(44.dp).padding(top = 28.dp).testTag("week-hours")) {
                    for (hour in firstHour until lastHour) {
                        Text("${hour.toString().padStart(2, '0')}:00", Modifier.height(hourHeight), style = MaterialTheme.typography.labelSmall)
                    }
                }
                for (day in days) {
                    Column(Modifier.width(dayWidth)) {
                        Text(
                            CalendarTxt.dayText(day).replace("${day.year}-", ""),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.height(28.dp).padding(start = 4.dp),
                        )
                        Box(
                            Modifier.fillMaxWidth().height(hourHeight * (lastHour - firstHour))
                                .background(if (day == today) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
                        ) {
                            for (hour in 0 until lastHour - firstHour) HorizontalDivider(Modifier.offset(y = hourHeight * hour))
                            for (block in ofWeek.filter { it.date == day }) {
                                val entry = entryOf(block)
                                Surface(
                                    color = if (entry != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (entry != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    shape = MaterialTheme.shapes.extraSmall,
                                    modifier = Modifier.padding(horizontal = 2.dp).fillMaxWidth()
                                        .offset(y = hourHeight * (block.span.start - firstHour * 60) / 60)
                                        .height(hourHeight * block.span.length / 60)
                                        .clickable(enabled = entry != null) { entry?.let(onOpen) }
                                        .testTag("week-block"),
                                ) {
                                    Text(
                                        "${CalendarTxt.timesText(block.span).substringBefore('-')} ${entry?.task?.description ?: block.text}",
                                        style = MaterialTheme.typography.labelSmall,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The contexts that the typed text asks for: first those of which a level starts with it, then those that contain it. */
internal fun matchingContexts(contexts: List<String>, typed: String): List<String> {
    val wanted = typed.trimStart('@').lowercase()
    val (starting, containing) = contexts.filter { wanted in it.lowercase() }
        .partition { context -> context.lowercase().split(LEVEL_SEPARATOR).any { it.startsWith(wanted) } }
    return starting + containing
}

/**
 * The tasks without context, to be sorted. One task is marked; typing narrows the contexts, and Enter gives the first of them
 * to the marked task, or the typed text as a new context if none is left. With the pointer, a click on a context gives it to its task.
 */
// [impl->dsn~inbox~1]
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Inbox(entries: List<Entry>, contexts: List<String>, onAssign: (Entry, String) -> Unit, onOpen: (Entry) -> Unit) {
    var typed by remember { mutableStateOf("") }
    var mark by remember { mutableStateOf(0) }
    // A task that got its context leaves the list, so the mark is then on the next one.
    val marked = mark.coerceIn(0, maxOf(0, entries.lastIndex))
    val matching = matchingContexts(contexts, typed)
    val focus = remember { FocusRequester() }
    val keyboard = LocalKeyboard.current
    LaunchedEffect(Unit) { if (keyboard) focus.requestFocus() }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp).testTag("inbox"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (entries.isEmpty()) {
            Text("Every task has a context.", Modifier.padding(4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it.filterNot(Char::isWhitespace) },
                label = { Text("Context for the marked task") },
                supportingText = { Text("Enter gives the first context shown, or the typed one if none is left. The arrow keys mark another task.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("inbox-typed").onPreviewKeyEvent {
                    if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (it.key) {
                        Key.Enter -> (matching.firstOrNull() ?: typed.trimStart('@')).takeIf { typed.isNotEmpty() && it.isNotEmpty() }?.let { context ->
                            onAssign(entries[marked], context)
                            typed = ""
                        }
                        Key.DirectionDown -> mark = marked + 1
                        Key.DirectionUp -> mark = marked - 1
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                },
            )
        }
        for ((index, entry) in entries.withIndex()) {
            Card(
                onClick = { onOpen(entry) },
                border = if (index == marked) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.fillMaxWidth().testTag(if (index == marked) "inbox-marked" else "inbox-task"),
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.task.description, style = MaterialTheme.typography.bodyLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (context in if (index == marked) matching else contexts) {
                            // The area that reacts to a click is larger than the label, which is too small for a finger.
                            ContextLabel(
                                context,
                                Modifier.clip(CircleShape).clickable {
                                    onAssign(entry, context)
                                    // Back to the field, so that typing goes on after a click; the field is gone with the last task.
                                    if (keyboard && entries.size > 1) focus.requestFocus()
                                }.testTag("inbox-context-$context").padding(vertical = 5.dp),
                                path = true,
                            )
                        }
                    }
                }
            }
        }
    }
}
