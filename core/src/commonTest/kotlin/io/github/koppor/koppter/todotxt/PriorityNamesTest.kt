package io.github.koppor.koppter.todotxt

import kotlin.test.Test
import kotlin.test.assertEquals

class PriorityNamesTest {
    private val fibonacci = PriorityNames.of(listOf("A" to "1", "B" to "2", "C" to "3", "D" to "5", "E" to "8", "low" to "9", "F" to ""))

    // [utest->dsn~priority-names~1]
    @Test
    fun aPriorityShowsItsNameAndOnlyNamedPrioritiesAreOffered() {
        assertEquals("5", fibonacci.name('D'))
        assertEquals("F", fibonacci.name('F'))
        assertEquals("ABCDE".toList(), fibonacci.choices)
        assertEquals("ABCDEF".toList(), PriorityNames().choices)
    }

    // [utest->dsn~priority-names~1]
    @Test
    fun aNameTypedAtTheStartOfALineBecomesItsLetter() {
        assertEquals("(D) Call the dentist (5) times", fibonacci.toLetters("(5) Call the dentist (5) times"))
        assertEquals("(B) Call", fibonacci.toLetters("(B) Call"))
        assertEquals("(13) Call", fibonacci.toLetters("(13) Call"))
        assertEquals("(5)Call", fibonacci.toLetters("(5)Call"))
        assertEquals("Call (5) times", fibonacci.toLetters("Call (5) times"))
    }

    // [utest->dsn~priority-names~1]
    @Test
    fun aFilterNamesAPriorityByItsNameOrByItsLetter() {
        assertEquals("(>C) -(E) @phone (B) due:<1w", fibonacci.toLettersInFilter("(>3) -(8) @phone (B) due:<1w"))
        assertEquals("(<=D)", fibonacci.toLettersInFilter("(<=5)"))
        assertEquals("(13) (5", fibonacci.toLettersInFilter("(13) (5"))
        val highFirst = Query.parse(fibonacci.toLettersInFilter("(>3)"))
        assertEquals(listOf(true, true, false), listOf("(A) a", "(B) b", "(C) c").map { highFirst.matches(Task.parse(it), kotlinx.datetime.LocalDate(2026, 11, 13)) })
    }

    // [utest->dsn~priority-names~1]
    @Test
    fun aListOfNamesBecomesTheLinesOfTheSettings() {
        assertEquals("A = 1\nB = 2\nC = 3", PriorityNames.lines(listOf("1", "2", "3")))
    }
}
