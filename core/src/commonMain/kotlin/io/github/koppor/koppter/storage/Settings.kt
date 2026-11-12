package io.github.koppor.koppter.storage

data class Settings(
    /** Location of the todo.txt file; blank means the platform default. */
    val todoFile: String = "",
    /** Write the creation date when a task is added and the completion date when it is completed. */
    val writeDates: Boolean = false,
    /** The number of tasks the *Top* view shows. */
    val topCount: Int = 10,
    /** The day (`YYYY-MM-DD`) on which this device last added the tasks of `ice_recur.txt`. */
    val iceRecurLastRun: String = "",
    /** The columns of the board that the user defined, one per line as `title = filter expression`. */
    val boardColumns: String = "",
) {
    // [impl->dsn~settings-file~1]
    fun toProperties(): Map<String, String> = mapOf(
        TODO_FILE to todoFile,
        WRITE_DATES to writeDates.toString(),
        TOP_COUNT to topCount.toString(),
        ICE_RECUR_LAST_RUN to iceRecurLastRun,
        BOARD_COLUMNS to boardColumns,
    )

    companion object {
        private const val TODO_FILE = "todoFile"
        private const val WRITE_DATES = "writeDates"
        private const val TOP_COUNT = "topCount"
        private const val ICE_RECUR_LAST_RUN = "iceRecurLastRun"
        private const val BOARD_COLUMNS = "boardColumns"

        /** Unknown keys are ignored and missing keys take their default, so that settings files of other versions load. */
        fun fromProperties(properties: Map<String, String>): Settings = Settings(
            todoFile = properties[TODO_FILE].orEmpty(),
            writeDates = properties[WRITE_DATES].toBoolean(),
            topCount = properties[TOP_COUNT]?.toIntOrNull()?.takeIf { it > 0 } ?: Settings().topCount,
            iceRecurLastRun = properties[ICE_RECUR_LAST_RUN].orEmpty(),
            boardColumns = properties[BOARD_COLUMNS].orEmpty(),
        )
    }
}
