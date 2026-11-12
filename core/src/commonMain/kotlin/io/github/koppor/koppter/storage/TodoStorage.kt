package io.github.koppor.koppter.storage

/** The place where the todo.txt file is kept. */
interface TodoStorage {
    /** The content of the file; empty if the file does not exist yet. */
    fun read(): String

    fun write(text: String)

    /**
     * A value that changes whenever the file changes, used to notice changes made by other programs.
     * `null` if the file does not exist.
     */
    fun version(): Any?
}
