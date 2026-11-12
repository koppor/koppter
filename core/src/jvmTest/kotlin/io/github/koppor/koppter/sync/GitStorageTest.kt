package io.github.koppor.koppter.sync

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GitStorageTest {
    private val root = Files.createTempDirectory("koppter-git")
    private val remote = root.resolve("remote.git").also {
        Git.init().setBare(true).setInitialBranch("main").setDirectory(it.toFile()).call().close()
    }

    /** A device: a repository of its own with the shared remote. */
    private fun device(name: String): GitStorage {
        val directory = root.resolve(name)
        val git = Git.init().setInitialBranch("main").setDirectory(directory.toFile()).call()
        git.remoteAdd().setName("origin").setUri(URIish(remote.toUri().toString())).call()
        return storage(directory)
    }

    private fun storage(directory: Path): GitStorage {
        val file = directory.resolve("todo.txt")
        return GitStorage(file, assertNotNull(GitStorage.repositoryOf(file)))
    }

    private fun messages(directory: String): List<String> =
        Git.open(root.resolve(directory).toFile()).use { git -> git.log().call().map { it.shortMessage } }

    // [utest->dsn~commit-per-change~1]
    @Test
    fun everyChangeIsACommitWithTheChangeAsMessage() {
        val phone = device("phone")
        phone.write("Call Mom\n", "Add task: Call Mom")
        phone.write("x Call Mom\n", "Complete task: Call Mom")
        phone.write("x Call Mom\n", "Nothing changed")
        assertEquals(listOf("Complete task: Call Mom", "Add task: Call Mom"), messages("phone"))
    }

    // [utest->dsn~git-sync~1]
    @Test
    fun aSecondDeviceReceivesTheChanges() {
        val phone = device("phone")
        phone.write("Call Mom\n", "Add task: Call Mom")
        phone.synchronizeNow()

        val desktop = device("desktop")
        desktop.synchronizeNow()
        assertEquals("Call Mom\n", desktop.read())

        desktop.write("x Call Mom\n", "Complete task: Call Mom")
        desktop.synchronizeNow()
        phone.synchronizeNow()
        assertEquals("x Call Mom\n", phone.read())
    }

    // [utest->dsn~git-sync~1]
    @Test
    fun concurrentChangesOnTwoDevicesAreMergedWithoutConflict() {
        val phone = device("phone")
        phone.write("Call Mom\nBuy milk\n", "Add tasks")
        phone.synchronizeNow()
        val desktop = device("desktop")
        desktop.synchronizeNow()

        // Both change adjacent lines, which a textual merge reports as a conflict.
        phone.write("x Call Mom\nBuy milk\n", "Complete task: Call Mom")
        desktop.write("Call Mom\nBuy milk +shop\nWater plants\n", "Edit and add")
        phone.synchronizeNow()
        desktop.synchronizeNow()
        phone.synchronizeNow()

        assertEquals("x Call Mom\nBuy milk +shop\nWater plants\n", desktop.read())
        assertEquals(desktop.read(), phone.read())
        assertEquals("Merge remote changes", messages("desktop").first())
    }

    // [utest->dsn~git-sync~1]
    @Test
    fun aChangeByAnotherProgramIsCommittedOnSynchronization() {
        val phone = device("phone")
        phone.write("Call Mom\n", "Add task: Call Mom")
        root.resolve("phone/todo.txt").appendText("Typed in an editor\n")
        phone.synchronizeNow()
        assertEquals("Update todo.txt", messages("phone").first())
        assertEquals("Call Mom\nTyped in an editor\n", root.resolve("phone/todo.txt").readText())
    }

    // [utest->dsn~dates-from-history~1]
    @Test
    fun theDatesOfATaskComeFromTheCommits() {
        val phone = device("phone")
        assertEquals(emptyMap(), phone.dates())
        phone.write("Call Mom\nBuy milk\n", "Add tasks")
        phone.write("x Call Mom\nBuy milk\n", "Complete task: Call Mom")
        val today = java.time.LocalDate.now().toString()
        val dates = phone.dates()
        assertEquals(today, dates.getValue("x Call Mom").added.toString())
        assertEquals(today, dates.getValue("x Call Mom").completed.toString())
        assertNull(dates.getValue("Buy milk").completed)
    }

    // [utest->dsn~git-detection~1]
    @Test
    fun aFileOutsideARepositoryHasNone() {
        // The temporary directory itself could lie in a repository only on an unusual machine.
        assertNull(GitStorage.repositoryOf(Files.createTempDirectory("koppter-plain").resolve("todo.txt")))
        assertNotNull(GitStorage.repositoryOf(root.resolve("phone").also { Git.init().setDirectory(it.toFile()).call() }.resolve("sub/../todo.txt")))
    }
}
