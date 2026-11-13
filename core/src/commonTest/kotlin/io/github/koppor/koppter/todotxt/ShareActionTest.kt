package io.github.koppor.koppter.todotxt

import io.github.koppor.koppter.storage.ConfigFile
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

// [utest->dsn~share-actions~1]
class ShareActionTest {
    private val today = LocalDate(2026, 11, 12)

    @Test
    fun theTemplateMakesATaskOfTheSharedTextWithDatesFromTheDayOfSharing() {
        val reading = ShareAction("reading", "{} @research>reading due:2w dur:1h")

        assertEquals(
            "https://example.org/article @research>reading due:2026-11-26 dur:1h",
            reading.task(" https://example.org/article\n", today).toLine(),
        )
        // Without the placeholder, the shared text stands in front; a fixed date stays.
        assertEquals(
            "An article https://example.org/a +Reading t:2026-11-13 due:2026-12-24",
            ShareAction("later", "+Reading t:tomorrow due:2026-12-24").task("An article\nhttps://example.org/a", today).toLine(),
        )
        // Shared text that looks like the start of a todo.txt line stays text.
        assertEquals("(A) x 2026-01-01 list @inbox", ShareAction("inbox", "@inbox").task("(A) x 2026-01-01 list", today).toLine())
    }

    @Test
    fun actionsAreReadFromLinesAndKeptInTheConfigurationNextToTheColumns() {
        val actions = ShareAction.parse("reading = {} @research>reading due:2w\n\nno title\ninbox = @inbox")
        assertEquals(listOf(ShareAction("reading", "{} @research>reading due:2w"), ShareAction("inbox", "@inbox")), actions)

        val config = ConfigFile.withShareActions("board-columns:\n  Waiting: \"@waiting\"\n", actions)
        assertEquals(actions, ConfigFile.shareActions(config))
        assertEquals(listOf(BoardColumn("Waiting", "@waiting")), ConfigFile.boardColumns(config))
        assertEquals(emptyList(), ConfigFile.shareActions(ConfigFile.withShareActions(config, emptyList())))
    }
}
