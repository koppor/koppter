package io.github.koppor.koppter.storage

/** The palette the colors of the application come from. */
enum class ColorTheme(val title: String) { NORD("Nord"), EVERFOREST("Everforest") }

/** Whether the colors are light or dark. */
enum class Appearance(val title: String) { SYSTEM("As the system"), LIGHT("Light"), DARK("Dark") }

data class Settings(
    /** Location of the todo.txt file; blank means the platform default. */
    val todoFile: String = "",
    /** Write the creation date when a task is added and the completion date when it is completed. */
    val writeDates: Boolean = false,
    /** The number of tasks the *Top* view shows. */
    val topCount: Int = 10,
    /** The day (`YYYY-MM-DD`) on which this device last added the tasks of `ice_recur.txt`. */
    val iceRecurLastRun: String = "",
    val theme: ColorTheme = ColorTheme.NORD,
    val appearance: Appearance = Appearance.SYSTEM,
    /**
     * The addresses of the iCalendar files whose events are busy times for planning, one per line.
     * Such an address is a secret, which is why it is a setting of the device and not kept next to the todo.txt file.
     */
    val calendars: String = "",
) {
    // [impl->dsn~settings-file~1]
    fun toProperties(): Map<String, String> = mapOf(
        TODO_FILE to todoFile,
        WRITE_DATES to writeDates.toString(),
        TOP_COUNT to topCount.toString(),
        ICE_RECUR_LAST_RUN to iceRecurLastRun,
        THEME to theme.name,
        APPEARANCE to appearance.name,
        CALENDARS to calendars,
    )

    companion object {
        private const val TODO_FILE = "todoFile"
        private const val WRITE_DATES = "writeDates"
        private const val TOP_COUNT = "topCount"
        private const val ICE_RECUR_LAST_RUN = "iceRecurLastRun"
        private const val THEME = "theme"
        private const val APPEARANCE = "appearance"
        private const val CALENDARS = "calendars"

        /** Unknown keys are ignored and missing keys take their default, so that settings files of other versions load. */
        fun fromProperties(properties: Map<String, String>): Settings = Settings(
            todoFile = properties[TODO_FILE].orEmpty(),
            writeDates = properties[WRITE_DATES].toBoolean(),
            topCount = properties[TOP_COUNT]?.toIntOrNull()?.takeIf { it > 0 } ?: Settings().topCount,
            iceRecurLastRun = properties[ICE_RECUR_LAST_RUN].orEmpty(),
            theme = ColorTheme.entries.firstOrNull { it.name == properties[THEME] } ?: ColorTheme.NORD,
            appearance = Appearance.entries.firstOrNull { it.name == properties[APPEARANCE] } ?: Appearance.SYSTEM,
            calendars = properties[CALENDARS].orEmpty(),
        )
    }
}
