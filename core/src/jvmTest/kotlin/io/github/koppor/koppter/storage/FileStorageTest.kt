package io.github.koppor.koppter.storage

import java.nio.file.Files
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class FileStorageTest {
    private val directory = Files.createTempDirectory("koppter")

    // [utest->dsn~file-storage~1]
    @Test
    fun aMissingFileReadsAsEmptyAndIsCreatedOnWrite() {
        val path = directory.resolve("sub/todo.txt")
        val storage = FileStorage(path)
        assertEquals("", storage.read())
        assertNull(storage.version())

        storage.write("Call Mom\n")

        assertEquals("Call Mom\n", path.readText())
        assertEquals("Call Mom\n", storage.read())
        assertEquals(listOf(path), path.parent.listDirectoryEntries())
    }

    // [utest->dsn~file-storage~1]
    @Test
    fun theVersionChangesWithTheContent() {
        val storage = FileStorage(directory.resolve("todo.txt"))
        storage.write("a\n")
        val before = storage.version()
        storage.write("a\nb\n")
        assertNotEquals(before, storage.version())
    }

    // [utest->dsn~settings-file~1]
    @Test
    fun settingsSurviveSavingAndLoading() {
        val file = SettingsFile(directory.resolve("config/settings.properties"))
        assertEquals(Settings(), file.load())
        val settings = Settings(todoFile = "C:\\Users\\me\\todo.txt", writeDates = true)
        file.save(settings)
        assertEquals(settings, file.load())
    }
}
