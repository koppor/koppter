package io.github.koppor.koppter.storage

data class Settings(
    /** Location of the todo.txt file; blank means the platform default. */
    val todoFile: String = "",
    /** Write the creation date when a task is added and the completion date when it is completed. */
    val writeDates: Boolean = false,
) {
    // [impl->dsn~settings-file~1]
    fun toProperties(): Map<String, String> = mapOf(
        TODO_FILE to todoFile,
        WRITE_DATES to writeDates.toString(),
    )

    companion object {
        private const val TODO_FILE = "todoFile"
        private const val WRITE_DATES = "writeDates"

        /** Unknown keys are ignored and missing keys take their default, so that settings files of other versions load. */
        fun fromProperties(properties: Map<String, String>): Settings = Settings(
            todoFile = properties[TODO_FILE].orEmpty(),
            writeDates = properties[WRITE_DATES].toBoolean(),
        )
    }
}
