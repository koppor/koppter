package io.github.koppor.koppter.storage

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.reader
import kotlin.io.path.writer

/** The settings as a Java properties file. */
// [impl->dsn~settings-file~1]
class SettingsFile(private val path: Path) {
    fun load(): Settings {
        if (!path.exists()) return Settings()
        val properties = Properties()
        path.reader().use(properties::load)
        return Settings.fromProperties(properties.stringPropertyNames().associateWith(properties::getProperty))
    }

    fun save(settings: Settings) {
        Files.createDirectories(path.toAbsolutePath().parent)
        val properties = Properties()
        settings.toProperties().forEach(properties::setProperty)
        path.writer().use { properties.store(it, null) }
    }
}
