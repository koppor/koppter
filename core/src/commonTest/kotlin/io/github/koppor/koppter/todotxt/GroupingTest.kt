package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

// [utest->dsn~board-groups~1]
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

    @Test
    fun groupsByPriorityWithUnprioritizedTasksLast() {
        assertEquals(
            mapOf("A" to listOf("Thank Mom"), "B" to listOf("Schedule pickup"), "No priority" to listOf("Post signs", "Pay bill", "Return book")),
            groups(GroupKey.PRIORITY),
        )
        assertEquals(listOf("A", "B", "No priority"), groupEntries(entries, GroupKey.PRIORITY, today).map { it.title })
    }

    @Test
    fun aTaskWithTwoProjectsAppearsInBothGroups() {
        val groups = groups(GroupKey.PROJECT)
        assertEquals(listOf("Post signs"), groups["+Family"])
        assertEquals(listOf("Schedule pickup", "Post signs"), groups["+GarageSale"])
        assertEquals(listOf("Thank Mom", "Pay bill", "Return book"), groups["No project"])
    }

    @Test
    fun groupsByContext() {
        assertEquals(listOf("Schedule pickup", "Thank Mom"), groups(GroupKey.CONTEXT)["@phone"])
    }

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

    @Test
    fun emptyGroupsAreLeftOut() {
        assertEquals(listOf("No context"), groupEntries(entries.filter { it.task.contexts.isEmpty() }, GroupKey.CONTEXT, today).map { it.title })
    }
}
