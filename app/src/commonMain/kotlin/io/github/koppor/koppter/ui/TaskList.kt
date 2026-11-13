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
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.Group
import io.github.koppor.koppter.todotxt.Task
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

@Composable
fun TaskList(
    entries: List<Entry>,
    today: LocalDate,
    onToggle: (Entry) -> Unit,
    onOpen: (Entry) -> Unit,
    onStep: (entry: Entry, neighbor: Entry, up: Boolean) -> Unit,
    onDrop: (entry: Entry, target: Entry, before: Boolean) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    // [impl->dsn~list-drag~1]
    val drag = remember { CardDrag<Entry>() }
    fun target(): Pair<Entry, Boolean>? = drag.dragged?.let { entry -> drag.target { true }?.takeIf { (target) -> entry.task.canStepOver(target.task) } }
    val list = rememberLazyListState()
    var area by remember { mutableStateOf<Rect?>(null) }
    DragScroll(drag, list) { area }
    Box(
        modifier.fillMaxSize().onGloballyPositioned {
            drag.origin = it.positionInRoot()
            area = it.boundsInRoot()
        },
    ) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().testTag("task-list"),
            contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(entries, key = { _, entry -> entry.index }) { position, entry ->
                // [impl->dsn~list-step~1]
                fun step(up: Boolean): (() -> Unit)? =
                    entries.getOrNull(if (up) position - 1 else position + 1)?.takeIf { entry.task.canStepOver(it.task) }?.let { { onStep(entry, it, up) } }
                Box(Modifier.cardDrag(drag, entry) { target()?.let { (target, before) -> onDrop(entry, target, before) } }) {
                    TaskCard(entry, today, onToggle = { onToggle(entry) }, onOpen = { onOpen(entry) }) {
                        Column {
                            StepButton(Icons.Default.KeyboardArrowUp, "Move up", "task-up", step(up = true))
                            StepButton(Icons.Default.KeyboardArrowDown, "Move down", "task-down", step(up = false))
                        }
                    }
                }
            }
        }
        DragMarks(drag, drag.dragged?.task?.description, target()?.let { (target, before) -> drag.cards[target]?.let { it to before } })
    }
}

/**
 * A task card that is dragged, the pointer and the cards that are shown.
 * Positions are in the coordinates of the window, in which the cards report their bounds.
 */
private class CardDrag<K : Any> {
    var dragged by mutableStateOf<K?>(null)
    var pointer by mutableStateOf(Offset.Zero)
    var origin by mutableStateOf(Offset.Zero)
    val cards = mutableStateMapOf<K, Rect>()

    /**
     * The card to drop on, and whether the pointer is in its upper half, where the dragged card comes before it.
     * It is the card nearest to the pointer among those above and below it, so that the gaps between the cards and the space below the last one count as well.
     */
    fun target(among: (K) -> Boolean): Pair<K, Boolean>? =
        cards.entries.filter { (key, bounds) -> key != dragged && among(key) && pointer.x in bounds.left..bounds.right }
            .minByOrNull { (_, bounds) -> maxOf(bounds.top - pointer.y, pointer.y - bounds.bottom, 0f) }
            ?.let { it.key to (pointer.y < it.value.center.y) }
}

/**
 * Makes the element a card that is dragged with the mouse, or with the finger after a long press, as a plain drag with the finger scrolls.
 * [onDrop] is called while the state still holds the dragged card.
 */
@Composable
private fun <K : Any> Modifier.cardDrag(state: CardDrag<K>, key: K, onDrop: () -> Unit): Modifier {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val drop by rememberUpdatedState(onDrop)
    DisposableEffect(key) { onDispose { state.cards.remove(key) } }
    // A lazy list drops the items that leave the window, and with the item its gesture: the dragged card is kept while the list scrolls.
    val container = LocalPinnableContainer.current
    val held = state.dragged == key
    DisposableEffect(held, container) {
        val pin = if (held) container?.pin() else null
        onDispose { pin?.release() }
    }
    return onGloballyPositioned {
        coordinates = it
        val bounds = it.boundsInRoot()
        if (state.cards[key] != bounds) state.cards[key] = bounds
    }.pointerInput(key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val start = if (down.type == PointerType.Mouse) {
                awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
            } else {
                awaitLongPressOrCancellation(down.id)
            } ?: return@awaitEachGesture
            state.pointer = coordinates?.localToRoot(start.position) ?: return@awaitEachGesture
            state.dragged = key
            val dropped = drag(start.id) { change ->
                state.pointer += change.positionChange()
                change.consume()
            }
            if (dropped) drop()
            state.dragged = null
        }
    }
}

/**
 * Scrolls the list while a card is held near its upper or lower edge, or beyond it: the nearer to the edge, the faster.
 * With [sideways], these are the left and the right edge.
 *
 * @param area the bounds of the list in the coordinates of the window
 */
// [impl->dsn~drag-scroll~1]
@Composable
private fun DragScroll(state: CardDrag<*>, list: ScrollableState, sideways: Boolean = false, area: () -> Rect?) {
    val zone = with(LocalDensity.current) { 56.dp.toPx() }
    val step = with(LocalDensity.current) { 14.dp.toPx() }
    if (state.dragged == null) return
    LaunchedEffect(list) {
        while (true) {
            delay(16)
            val bounds = area() ?: continue
            val (along, across) = state.pointer.let { if (sideways) it.x to it.y else it.y to it.x }
            val (start, end) = if (sideways) bounds.left to bounds.right else bounds.top to bounds.bottom
            if (across !in (if (sideways) bounds.top..bounds.bottom else bounds.left..bounds.right)) continue
            val pull = ((along - end + zone) / zone).coerceIn(0f, 1f) - ((start + zone - along) / zone).coerceIn(0f, 1f)
            if (pull != 0f) list.scrollBy(pull * step)
        }
    }
}

/** What a drag shows: the description of the dragged task at the pointer, and a line where the task would be put. */
@Composable
private fun DragMarks(state: CardDrag<*>, description: String?, line: Pair<Rect, Boolean>?) {
    line?.let { (bounds, before) ->
        val gap = with(LocalDensity.current) { 3.dp.toPx() }
        Box(
            Modifier
                .offset { IntOffset((bounds.left - state.origin.x).roundToInt(), ((if (before) bounds.top - gap else bounds.bottom + gap) - state.origin.y - gap / 2).roundToInt()) }
                .width(with(LocalDensity.current) { bounds.width.toDp() }).height(3.dp)
                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                .testTag("drop-line"),
        )
    }
    description?.let {
        Surface(
            shape = MaterialTheme.shapes.small,
            shadowElevation = 6.dp,
            modifier = Modifier
                .offset { IntOffset((state.pointer.x - state.origin.x).roundToInt() + 12, (state.pointer.y - state.origin.y).roundToInt() + 12) }
                .testTag("task-dragged"),
        ) {
            Text(it, maxLines = 1, modifier = Modifier.padding(8.dp))
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
fun TaskCard(entry: Entry, today: LocalDate, onToggle: () -> Unit, onOpen: () -> Unit, trailing: @Composable () -> Unit = {}) {
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
            trailing()
        }
    }
}

/** A button of half the usual height, so that two of them fit a card; without an action, it is disabled. */
@Composable
private fun StepButton(icon: ImageVector, description: String, tag: String, onClick: (() -> Unit)?) {
    IconButton(onClick = onClick ?: {}, enabled = onClick != null, modifier = Modifier.padding(start = 4.dp).size(width = 40.dp, height = 26.dp).testTag(tag)) {
        Icon(icon, contentDescription = description)
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
 * @param moved the task of a card as it is after a move to another column; `null` if the column does not take it
 * @param onMove called when a card is dropped on a column that takes it, or next to a card of its own column
 */
// [impl->dsn~board~1]
// [impl->dsn~board-drag~1]
@Composable
fun Board(
    groups: List<Group>,
    today: LocalDate,
    onToggle: (Entry) -> Unit,
    onOpen: (Entry) -> Unit,
    moved: (Entry, from: Group, to: Group) -> Task?,
    onMove: (Entry, from: Group, to: Group, at: Entry?, before: Boolean) -> Unit,
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
    // A card is known by its column and its task, as a task with two projects has a card in two columns.
    val drag = remember { CardDrag<Pair<Int, Entry>>() }
    val columnBounds = remember { mutableStateMapOf<Int, Rect>() }
    fun columnUnderPointer(): Int? = currentGroups.indices.firstOrNull { columnBounds[it]?.contains(drag.pointer) == true }

    /**
     * Where the dragged card would be dropped: the column, if the card can change to it, and the card it is put next to, if any.
     * Within its column, the card takes the priority of the card it is put next to.
     * In another column, it keeps the priority that the column gives it, so that it has a place only next to cards of that priority.
     */
    // [impl->dsn~board-drag-position~1]
    fun drop(): Triple<Group, Pair<Int, Entry>?, Boolean>? {
        val (from, entry) = drag.dragged ?: return null
        val to = columnUnderPointer() ?: return null
        val task = if (from == to) entry.task else moved(entry, currentGroups[from], currentGroups[to])
        val card = task?.let { drag.target { it.first == to } }?.takeIf { (card) ->
            val other = card.second.task
            task.canStepOver(other) && (from == to || task.priority == other.priority)
        }
        return if (from == to && card == null || task == null) null else Triple(currentGroups[to], card?.first, card?.second ?: false)
    }
    val drop = drop()
    val target = drop?.first?.takeIf { it != drag.dragged?.let { (from) -> groups.getOrNull(from) } }

    val columns = rememberScrollState()
    var area by remember { mutableStateOf<Rect?>(null) }
    DragScroll(drag, columns, sideways = true) { area }
    Box(
        modifier.fillMaxSize().onGloballyPositioned {
            drag.origin = it.positionInRoot()
            area = it.boundsInRoot()
        },
    ) {
        Row(
            Modifier.fillMaxSize().horizontalScroll(columns).padding(horizontal = 8.dp).testTag("board"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            groups.forEachIndexed { index, group ->
                // A column of another title is another column: it starts at its top instead of where its predecessor was scrolled to.
                key(group.title) {
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
                        val list = rememberLazyListState()
                        DragScroll(drag, list) { columnBounds[index] }
                        LazyColumn(
                            state = list,
                            contentPadding = PaddingValues(bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(group.entries, key = { it.index }) { entry ->
                                val source = Modifier.cardDrag(drag, index to entry) {
                                    drop()?.let { (to, card, before) -> onMove(entry, group, to, card?.second, before) }
                                }
                                Box(source) {
                                    TaskCard(entry, today, onToggle = { onToggle(entry) }, onOpen = { onOpen(entry) })
                                }
                            }
                        }
                    }
                }
            }
        }
        DragMarks(drag, drag.dragged?.second?.task?.description, drop?.second?.let { card -> drag.cards[card]?.let { it to drop.third } })
    }
}
