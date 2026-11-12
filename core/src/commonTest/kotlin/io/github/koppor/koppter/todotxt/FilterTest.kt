package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FilterTest {
    private val today = LocalDate(2026, 11, 12)
    private val file = TodoFile.parse(
        """
        (B) Schedule pickup +GarageSale @phone
        Post signs +GarageSale
        (A) Thank Mom @phone due:2026-11-20
        x Call Mom +Family @phone
        Plan holiday t:2026-12-01
        (A) Pay bill due:2026-11-13
        """.trimIndent(),
    )

    private fun descriptions(filter: Filter) =
        file.entries.filter { filter.matches(it.task, today) }.map { it.task.description }

    // [utest->dsn~filter~1]
    @Test
    fun theDefaultShowsOpenTasksThatAreNotInTheFuture() {
        assertEquals(listOf("Schedule pickup", "Post signs", "Thank Mom", "Pay bill"), descriptions(Filter()))
    }

    // [utest->dsn~filter~1]
    @Test
    fun filtersByCompletion() {
        assertEquals(listOf("Call Mom"), descriptions(Filter(completion = Completion.COMPLETED)))
        assertEquals(6, descriptions(Filter(completion = Completion.ALL, hideFuture = false)).size)
    }

    // [utest->dsn~filter~1]
    @Test
    fun criteriaAreCombinedWithAndValuesWithOr() {
        assertEquals(
            listOf("Schedule pickup", "Thank Mom"),
            descriptions(Filter(contexts = setOf("phone"), priorities = setOf('A', 'B'))),
        )
        assertEquals(listOf("Schedule pickup"), descriptions(Filter(contexts = setOf("phone"), projects = setOf("GarageSale"))))
    }

    // [utest->dsn~filter~1]
    @Test
    fun textMatchesTheWholeLineIgnoringCase() {
        assertEquals(listOf("Schedule pickup", "Post signs"), descriptions(Filter(text = "garagesale")))
    }

    // [utest->dsn~filter~1]
    @Test
    fun futureTasksAreShownOnRequest() {
        assertEquals(listOf("Plan holiday"), descriptions(Filter(text = "holiday", hideFuture = false)))
    }

    // [utest->dsn~context-hierarchy~1]
    @Test
    fun aContextIncludesTheContextsBelowIt() {
        val tasks = listOf("Read the survey @research>reading", "Review a paper @research", "Read the manual @researching", "Call @phone").map(Task::parse)
        fun matching(vararg contexts: String) = tasks.filter { Filter(contexts = contexts.toSet()).matches(it, today) }.map { it.description }

        assertEquals(listOf("Read the survey", "Review a paper"), matching("research"))
        assertEquals(listOf("Read the survey"), matching("research>reading"))
        assertEquals(listOf("a", "a>b", "a>b>c"), "a>b>c".withAncestors())
        assertEquals("c", "a>b>c".lastLevel)
        // A context stands directly before the contexts below it, not behind a name that only starts alike.
        assertEquals(listOf("a", "a>b", "a1"), listOf("a1", "a>b", "a").sortedWith(hierarchyOrder))
    }

    // [utest->dsn~sort-order~1]
    @Test
    fun ordersByCompletionPriorityDueDateAndPosition() {
        assertEquals(
            listOf("Pay bill", "Thank Mom", "Schedule pickup", "Post signs", "Plan holiday", "Call Mom"),
            file.entries.sortedWith(listOrder).map { it.task.description },
        )
    }

    // [utest->dsn~filter~1]
    @Test
    fun hashtagsRestrictLikeProjectsAndContexts() {
        val tasks = listOf("Call Mom #family", "Pay bill #money #urgent", "Post signs").map(Task::parse)
        fun matching(filter: Filter) = tasks.filter { filter.matches(it, today) }.map { it.description }

        assertEquals(listOf("Call Mom", "Pay bill"), matching(Filter(hashtags = setOf("family", "urgent"))))
        assertEquals(listOf("Call Mom", "Pay bill", "Post signs"), matching(Filter()))
    }
}
