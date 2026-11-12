package io.github.koppor.koppter.storage

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.readText
import kotlin.io.path.writeText

/** A todo.txt file in the file system. */
// [impl->dsn~file-storage~1]
class FileStorage(private val path: Path) : TodoStorage {
    override fun read(): String = if (path.exists()) path.readText() else ""

    /** Writes to a temporary file first, so that a crash never leaves a half-written todo.txt behind. */
    override fun write(text: String, change: String) {
        val directory = path.toAbsolutePath().parent
        Files.createDirectories(directory)
        val temporary = Files.createTempFile(directory, path.fileName.toString(), ".tmp")
        try {
            temporary.writeText(text)
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    override fun version(): Any? = if (path.exists()) path.getLastModifiedTime() to Files.size(path) else null
}
