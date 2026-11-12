package io.github.koppor.koppter.sync

import org.eclipse.jgit.api.Git
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitSetupTest {
    private val root = Files.createTempDirectory("koppter-setup")
    private val setup = GitSetup(root.resolve("app"))

    // [utest->dsn~ssh-key~1]
    @Test
    fun theKeyPairIsCreatedOnceAndThePublicKeyIsInOpenSshForm() {
        val key = setup.publicKey()
        assertTrue(key.startsWith("ecdsa-sha2-nistp256 AAAA"), key)
        assertTrue(key.endsWith(" koppter"), key)
        assertEquals(key, GitSetup(root.resolve("app")).publicKey())
        assertTrue(root.resolve("app/ssh/id_ecdsa").readText().startsWith("-----BEGIN OPENSSH PRIVATE KEY-----"))
    }

    // [utest->dsn~host-keys~1]
    @Test
    fun unknownHostsAreAcceptedOnFirstContact() {
        setup.publicKey()
        assertTrue("StrictHostKeyChecking accept-new" in root.resolve("app/ssh/config").readText())
    }

    // [utest->dsn~clone~1]
    @Test
    fun clonesTheRepositoryAndKeepsAnExistingCloneOfTheSameRepository() {
        val origin = root.resolve("origin")
        Git.init().setInitialBranch("main").setDirectory(origin.toFile()).call().use { git ->
            origin.resolve("todo.txt").writeText("Call Mom\n")
            git.add().addFilepattern("todo.txt").call()
            git.commit().setMessage("Add task: Call Mom").call()
        }
        val url = origin.toUri().toString()

        val file = setup.clone(url)
        assertEquals("Call Mom\n", file.readText())

        file.writeText("Call Mom\nNot yet committed\n")
        assertEquals(file, setup.clone(url))
        assertEquals("Call Mom\nNot yet committed\n", file.readText())
    }
}
