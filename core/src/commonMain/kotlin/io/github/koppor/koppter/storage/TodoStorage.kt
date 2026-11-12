package io.github.koppor.koppter.storage

import io.github.koppor.koppter.sync.TaskDates

/** The place where the todo.txt file is kept. */
interface TodoStorage {
    /** The content of the file; empty if the file does not exist yet. */
    fun read(): String

    /**
     * @param change what the user did, as one line, such as `Add task: Call Mom`;
     *   a storage with a history records it there
     */
    fun write(text: String, change: String)

    /**
     * A value that changes whenever the file changes, used to notice changes made by other programs.
     * `null` if the file does not exist.
     */
    fun version(): Any?
}

enum class SyncState { IDLE, RUNNING, FAILED }

data class SyncStatus(val state: SyncState, val message: String = "")

/** A storage that exchanges the file with a remote copy. */
interface SyncingStorage : TodoStorage {
    /** Called with every change of the synchronization status, from any thread. */
    var onStatus: (SyncStatus) -> Unit

    /** Starts a synchronization in the background. */
    fun sync()

    /** When the tasks were added and completed according to the history, by line of the file. */
    fun dates(): Map<String, TaskDates>
}
