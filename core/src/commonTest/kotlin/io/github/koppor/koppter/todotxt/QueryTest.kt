package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class QueryTest {
    private val today = LocalDate(2026, 11, 12)

    private val lines = listOf(
        "(A) Call Mom +Family @phone due:2026-11-12",
        "(B) Schedule pickup +GarageSale @phone due:2026-11-20",
        "(C) Post signs +GarageSale due:2026-11-10 status:doing",
        "Buy milk @shop",
    )

    /** The first words of the lines that match the expression. */
    private fun matching(expression: String): String {
        val query = Query.parse(expression)
        return lines.map(Task::parse).filter { query.matches(it, today) }.joinToString(",") { it.description.substringBefore(' ') }
    }

    // [utest->dsn~query~1]
    @Test
    fun projectsContextsAndWords() {
        assertEquals("Schedule,Post", matching("+GarageSale"))
        assertEquals("Schedule", matching("+GarageSale @phone"))
        assertEquals("Buy", matching("MILK"))
        assertEquals("Call,Schedule,Post,Buy", matching(""))
        // A project matches as a whole, not as a part of a longer one.
        assertEquals("", matching("+Garage"))
    }

    // [utest->dsn~query~1]
    @Test
    fun negation() {
        assertEquals("Call,Buy", matching("-+GarageSale"))
        assertEquals("Post,Buy", matching("-@phone"))
        assertEquals("Call,Schedule,Buy", matching("-status:doing"))
    }

    // [utest->dsn~query~1]
    @Test
    fun priorities() {
        assertEquals("Schedule", matching("(B)"))
        assertEquals("Call", matching("(>B)"))
        assertEquals("Schedule,Post", matching("(<=B)"))
        assertEquals("Buy", matching("-(>=Z)"))
    }

    // [utest->dsn~query~1]
    @Test
    fun tagsCompareAsDatesOrAsText() {
        assertEquals("Call", matching("due:today"))
        assertEquals("Call,Post", matching("due:<=today"))
        assertEquals("Post", matching("due:<2026-11-11"))
        assertEquals("Call,Post", matching("due:<1w"))
        assertEquals("Schedule", matching("due:>tomorrow"))
        assertEquals("Post", matching("status:doing"))
        assertEquals("", matching("status:done"))
    }

    // [utest->dsn~board-defined-columns~1]
    @Test
    fun columnsAreReadFromLinesWithTitleAndExpression() {
        assertEquals(
            listOf(BoardColumn("Now", "due:<=today"), BoardColumn("@phone", "@phone"), BoardColumn("Doing", "status:doing -@shop")),
            parseBoardColumns("Now = due:<=today\n\n  @phone\nDoing=status:doing -@shop\n"),
        )
        // Without a title, the equals sign of a comparison is part of the expression.
        assertEquals(listOf(BoardColumn("due:<=today", "due:<=today")), parseBoardColumns("due:<=today"))
    }

    // [utest->dsn~board-defined-columns~1]
    @Test
    fun definedColumnsKeepTheirOrderAndStayWhenEmpty() {
        val entries = lines.mapIndexed { index, line -> Entry(index, Task.parse(line)) }
        val groups = groupByColumns(entries, parseBoardColumns("Now = due:<=today\nNothing = +Nowhere\nPhone = @phone"), today)

        assertEquals(listOf("Now", "Nothing", "Phone"), groups.map { it.title })
        assertEquals(listOf(listOf(0, 2), emptyList(), listOf(0, 1)), groups.map { group -> group.entries.map { it.index } })
    }
}
