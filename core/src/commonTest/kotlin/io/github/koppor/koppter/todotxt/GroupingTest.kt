package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupingTest {
    private val today = LocalDate(2026, 11, 12)
    private val entries = TodoFile.parse(
        """
        (B) Schedule pickup +GarageSale @phone
        Post signs +GarageSale +Family
        (A) Thank Mom @phone due:2026-11-20
        Pay bill due:2026-11-12
        Return book due:2026-11-01
        """.trimIndent(),
    ).entries

    private fun groups(key: GroupKey) =
        groupEntries(entries, key, today).associate { group -> group.title to group.entries.map { it.task.description } }

    // [utest->dsn~board-groups~1]
    @Test
    fun groupsByPriorityWithUnprioritizedTasksLast() {
        assertEquals(
            mapOf("A" to listOf("Thank Mom"), "B" to listOf("Schedule pickup"), "No priority" to listOf("Post signs", "Pay bill", "Return book")),
            groups(GroupKey.PRIORITY),
        )
        assertEquals(listOf("A", "B", "No priority"), groupEntries(entries, GroupKey.PRIORITY, today).map { it.title })
    }

    // [utest->dsn~board-groups~1]
    @Test
    fun aTaskWithTwoProjectsAppearsInBothGroups() {
        val groups = groups(GroupKey.PROJECT)
        assertEquals(listOf("Post signs"), groups["+Family"])
        assertEquals(listOf("Schedule pickup", "Post signs"), groups["+GarageSale"])
        assertEquals(listOf("Thank Mom", "Pay bill", "Return book"), groups["No project"])
    }

    // [utest->dsn~board-groups~1]
    @Test
    fun groupsByContext() {
        assertEquals(listOf("Schedule pickup", "Thank Mom"), groups(GroupKey.CONTEXT)["@phone"])
    }

    // [utest->dsn~board-groups~1]
    @Test
    fun groupsByDueDateRelativeToToday() {
        assertEquals(
            mapOf(
                "Overdue" to listOf("Return book"),
                "Today" to listOf("Pay bill"),
                "Later" to listOf("Thank Mom"),
                "No due date" to listOf("Schedule pickup", "Post signs"),
            ),
            groups(GroupKey.DUE),
        )
    }

    // [utest->dsn~board-groups~1]
    @Test
    fun emptyGroupsAreLeftOut() {
        assertEquals(listOf("No context"), groupEntries(entries.filter { it.task.contexts.isEmpty() }, GroupKey.CONTEXT, today).map { it.title })
    }

    private fun move(line: String, key: GroupKey, from: String, to: String): String? {
        val task = Task.parse(line)
        val groups = groupEntries(entries + Entry(99, task), key, today)
        return moveToGroup(task, key, groups.first { it.title == from }, groups.first { it.title == to }, today)?.toLine()
    }

    // [utest->dsn~board-move~1]
    @Test
    fun movingBetweenPriorityColumnsSetsThePriority() {
        assertEquals("(A) Call Mom", move("(B) Call Mom", GroupKey.PRIORITY, "B", "A"))
        assertEquals("Call Mom", move("(B) Call Mom", GroupKey.PRIORITY, "B", "No priority"))
    }

    // [utest->dsn~board-move~1]
    @Test
    fun movingBetweenProjectColumnsReplacesTheProjectOfTheSourceColumn() {
        assertEquals("Call Mom +Other +Family", move("Call Mom +GarageSale +Other", GroupKey.PROJECT, "+GarageSale", "+Family"))
        assertEquals("Call Mom", move("Call Mom +GarageSale", GroupKey.PROJECT, "+GarageSale", "No project"))
        assertEquals("Call Mom @phone", move("Call Mom", GroupKey.CONTEXT, "No context", "@phone"))
    }

    // [utest->dsn~board-move~1]
    @Test
    fun movingBetweenDueDateColumnsSetsOrRemovesTheDueDate() {
        assertEquals("Call Mom due:2026-11-12", move("Call Mom", GroupKey.DUE, "No due date", "Today"))
        assertEquals("Call Mom due:2026-11-13", move("Call Mom due:2026-11-12", GroupKey.DUE, "Today", "Later"))
        assertEquals("Call Mom", move("Call Mom due:2026-11-12", GroupKey.DUE, "Today", "No due date"))
    }

    // [utest->dsn~board-move~1]
    @Test
    fun movesWithoutMeaningAreRefused() {
        assertEquals(null, move("(B) Call Mom", GroupKey.PRIORITY, "B", "B"))
        assertEquals(null, move("Call Mom", GroupKey.DUE, "No due date", "Overdue"))
    }

    // [utest->dsn~board-groups~1]
    // [utest->dsn~board-move~1]
    @Test
    fun hashtagsFormColumnsAndAMoveReplacesTheHashtagOfTheSourceColumn() {
        val tagged = TodoFile.parse("Call Mom #todo #family\nPay bill #doing\nPost signs").entries
        val groups = groupEntries(tagged, GroupKey.HASHTAG, today)

        assertEquals(listOf("#doing", "#family", "#todo", "No hashtag"), groups.map { it.title })
        val moved = moveToGroup(tagged[0].task, GroupKey.HASHTAG, groups.first { it.title == "#todo" }, groups.first { it.title == "#doing" }, today)
        assertEquals("Call Mom #family #doing", moved?.toLine())
    }
}
