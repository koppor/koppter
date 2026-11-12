package io.github.koppor.koppter.sync

import org.apache.sshd.common.config.keys.KeyUtils
import org.apache.sshd.common.config.keys.PublicKeyEntry
import org.apache.sshd.common.keyprovider.KeyPairProvider
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.junit.ssh.SshTestGitServer
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The key and the host key handling of [GitSetup] against an SSH server in the same process. */
class SshTransportTest {
    private val root = Files.createTempDirectory("koppter-ssh")
    private val setup = GitSetup(root.resolve("app"))
    private val served = Git.init().setInitialBranch("main").setDirectory(root.resolve("served").toFile()).call().also { git ->
        root.resolve("served/todo.txt").writeText("Call Mom\n")
        git.add().addFilepattern("todo.txt").call()
        git.commit().setMessage("Add task: Call Mom").call()
        // The server updates the checked-out branch on a push.
        git.repository.config.apply { setString("receive", null, "denyCurrentBranch", "ignore"); save() }
    }
    private val server = SshTestGitServer(
        "git",
        PublicKeyEntry.parsePublicKeyEntry(setup.publicKey()).resolvePublicKey(null, null, null),
        served.repository,
        KeyUtils.generateKeyPair(KeyPairProvider.ECDSA_SHA2_NISTP256, 256),
    )

    @AfterTest
    fun stop() {
        server.stop()
        served.close()
    }

    // [utest->dsn~ssh-key~1]
    // [utest->dsn~host-keys~1]
    // [utest->dsn~clone~1]
    @Test
    fun clonesAndPushesOverSshWithTheGeneratedKey() {
        val port = server.start()
        val file = setup.clone("ssh://git@localhost:$port/served")
        assertEquals("Call Mom\n", file.readText())
        assertTrue("localhost" in root.resolve("app/ssh/known_hosts").readText())

        val storage = GitStorage(file, GitStorage.repositoryOf(file)!!, setup.ssh)
        storage.write("x Call Mom\n", "Complete task: Call Mom")
        storage.synchronizeNow()
        assertEquals("Complete task: Call Mom", served.log().call().first().shortMessage)
    }
}
