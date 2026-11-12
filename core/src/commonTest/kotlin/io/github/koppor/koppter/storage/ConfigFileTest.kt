package io.github.koppor.koppter.storage

import io.github.koppor.koppter.todotxt.BoardColumn
import kotlin.test.Test
import kotlin.test.assertEquals

// [utest->dsn~config-file~1]
class ConfigFileTest {
    private val columns = listOf(BoardColumn("Now", "due:<=today -@waiting"), BoardColumn("Waiting", "@waiting"))

    @Test
    fun aNewConfigurationHoldsTheColumnsAsQuotedScalars() {
        val config = ConfigFile.withBoardColumns("", columns)

        assertEquals(
            """
            # Configuration of koppter for the todo.txt file in this directory
            board-columns:
              "Now": "due:<=today -@waiting"
              "Waiting": "@waiting"
            """.trimIndent() + "\n",
            config,
        )
        assertEquals(columns, ConfigFile.boardColumns(config))
    }

    @Test
    fun plainAndQuotedScalarsWrittenByHandAreRead() {
        val config = """
            other: value
            board-columns:
              # the day
              Now: due:<=today -@waiting   # comment
              'It''s late': "(>C) \"quoted\" back\\slash"
              a:b: +Project

              "Key: with colon": '@phone'
            more:
              Not a column: x
        """.trimIndent()

        assertEquals(
            listOf(
                BoardColumn("Now", "due:<=today -@waiting"),
                BoardColumn("It's late", "(>C) \"quoted\" back\\slash"),
                BoardColumn("a:b", "+Project"),
                BoardColumn("Key: with colon", "@phone"),
            ),
            ConfigFile.boardColumns(config),
        )
    }

    @Test
    fun replacingTheColumnsKeepsTheRestOfTheConfiguration() {
        val config = "other: value\nboard-columns:\n  Old: +Old\n\n# next\nmore: 1\n"

        assertEquals(
            "other: value\nboard-columns:\n  \"Now\": \"due:<=today -@waiting\"\n  \"Waiting\": \"@waiting\"\n\n# next\nmore: 1\n",
            ConfigFile.withBoardColumns(config, columns),
        )
        assertEquals("other: value\n\n# next\nmore: 1\n", ConfigFile.withBoardColumns(config, emptyList()))
        assertEquals(
            "other: value\n\nboard-columns:\n  \"A \\\"b\\\"\": \"c\\\\d\"\n",
            ConfigFile.withBoardColumns("other: value\n", listOf(BoardColumn("A \"b\"", "c\\d"))),
        )
    }

    @Test
    fun aConfigurationWithoutColumnsHasNone() {
        assertEquals(emptyList(), ConfigFile.boardColumns(""))
        assertEquals(emptyList(), ConfigFile.boardColumns("other: value\n"))
        assertEquals("", ConfigFile.withBoardColumns("", emptyList()))
    }
}
