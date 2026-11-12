package io.github.koppor.koppter.sync

import org.eclipse.jgit.api.Git
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Synchronizes with a real remote repository over SSH, using the SSH configuration of the user.
 * Runs only if `KOPPTER_SSH_TEST_URL` names a repository; without local changes, nothing is pushed.
 */
class SshRemoteTest {
    @Test
    fun clonesAndSynchronizesOverSsh() {
        val url = System.getenv("KOPPTER_SSH_TEST_URL") ?: return
        val directory = Files.createTempDirectory("koppter-ssh")
        Git.cloneRepository().setURI(url).setDirectory(directory.toFile()).call().close()
        val file = directory.resolve("todo.txt")
        GitStorage(file, assertNotNull(GitStorage.repositoryOf(file))).synchronizeNow()
    }
}
