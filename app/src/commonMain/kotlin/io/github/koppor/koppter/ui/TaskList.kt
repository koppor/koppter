package io.github.koppor.koppter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import io.github.koppor.koppter.todotxt.Group
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

/**
 * The tasks as a board: one column per group, side by side, each a list of task cards.
 * The board scrolls horizontally, each column vertically.
 *
 * A card is dragged to another column with the mouse, or with the finger after a long press,
 * as a plain drag with the finger scrolls. While a card is dragged, its description follows the pointer
 * and the column under the pointer is marked.
 *
 * @param canMove whether a card can be dropped on a column
 * @param onMove called when a card is dropped on a column that takes it
 */
// [impl->dsn~board~1]
// [impl->dsn~board-drag~1]
@Composable
fun Board(
    groups: List<Group>,
    today: LocalDate,
    onToggle: (Entry) -> Unit,
    onOpen: (Entry) -> Unit,
    canMove: (Entry, from: Group, to: Group) -> Boolean,
    onMove: (Entry, from: Group, to: Group) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    if (groups.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val currentGroups by rememberUpdatedState(groups)
    var dragged by remember { mutableStateOf<Pair<Entry, Group>?>(null) }
    // Positions in the coordinates of the window, in which the columns report their bounds.
    var pointer by remember { mutableStateOf(Offset.Zero) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val columnBounds = remember { mutableStateMapOf<Int, Rect>() }
    fun columnUnderPointer(): Group? =
        currentGroups.indices.firstOrNull { columnBounds[it]?.contains(pointer) == true }?.let(currentGroups::get)
    val target = dragged?.let { (entry, from) -> columnUnderPointer()?.takeIf { canMove(entry, from, it) } }

    Box(modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        Row(
            Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp).testTag("board"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            groups.forEachIndexed { index, group ->
                Column(
                    Modifier.width(320.dp).fillMaxHeight()
                        .onGloballyPositioned { columnBounds[index] = it.boundsInRoot() }
                        .background(
                            if (group == target) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                            MaterialTheme.shapes.medium,
                        )
                        .testTag("board-column"),
                ) {
                    Text(
                        "${group.title} (${group.entries.size})",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp).testTag("board-column-title"),
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(group.entries, key = { it.index }) { entry ->
                            var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            val drag = Modifier.onGloballyPositioned { coordinates = it }.pointerInput(entry, group) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val start = if (down.type == PointerType.Mouse) {
                                        awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                                    } else {
                                        awaitLongPressOrCancellation(down.id)
                                    } ?: return@awaitEachGesture
                                    pointer = coordinates?.localToRoot(start.position) ?: return@awaitEachGesture
                                    dragged = entry to group
                                    val dropped = drag(start.id) { change ->
                                        pointer += change.positionChange()
                                        change.consume()
                                    }
                                    val to = columnUnderPointer()
                                    dragged = null
                                    if (dropped && to != null && canMove(entry, group, to)) onMove(entry, group, to)
                                }
                            }
                            Box(drag) {
                                TaskCard(entry, today, onToggle = { onToggle(entry) }, onOpen = { onOpen(entry) })
                            }
                        }
                    }
                }
            }
        }
        dragged?.let { (entry, _) ->
            Surface(
                shape = MaterialTheme.shapes.small,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .offset { IntOffset((pointer.x - origin.x).roundToInt() + 12, (pointer.y - origin.y).roundToInt() + 12) }
                    .testTag("board-dragged"),
            ) {
                Text(entry.task.description, maxLines = 1, modifier = Modifier.padding(8.dp))
            }
        }
    }
}
