package io.github.koppor.koppter.sync

import io.github.koppor.koppter.storage.TodoStorage
import org.apache.sshd.common.config.keys.KeyUtils
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter
import org.apache.sshd.common.keyprovider.KeyPairProvider
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Git access for a device without an SSH configuration of its own, such as a phone:
 * a key pair and the known hosts in a directory of the application, and a clone of the repository next to it.
 *
 * @param ownKey `false` on a device with an SSH configuration of the user: the clone then uses that one, as [storageForFile] does.
 */
class GitSetup(private val directory: Path, private val ownKey: Boolean = true) {
    private val sshDirectory = directory.resolve("ssh")
    private val privateKey = sshDirectory.resolve("id_ecdsa")
    private val publicKeyFile = sshDirectory.resolve("id_ecdsa.pub")
    val repositoryDirectory: Path = directory.resolve("repository")

    /** The storage for the file, as [storageForFile], but with the SSH key and the known hosts of the application. */
    fun storageFor(file: Path): TodoStorage = storageForFile(file, ssh)

    /** SSH with the key and the known hosts of the application instead of those in `~/.ssh`. */
    internal val ssh: SshSessionFactory by lazy {
        prepare()
        SshdSessionFactoryBuilder()
            .setPreferredAuthentications("publickey")
            .setHomeDirectory(directory.toFile())
            .setSshDirectory(sshDirectory.toFile())
            .build(null)
    }

    /**
     * The public key in the form `ecdsa-sha2-nistp256 AAAA… koppter`, to be registered with the git server.
     * The key pair is created on first use.
     */
    // [impl->dsn~ssh-key~1]
    fun publicKey(): String {
        prepare()
        return publicKeyFile.readText().trim()
    }

    private fun prepare() {
        if (privateKey.exists()) return
        Files.createDirectories(sshDirectory)
        // ECDSA rather than Ed25519: every supported Android version can create it without an additional library.
        val pair = KeyUtils.generateKeyPair(KeyPairProvider.ECDSA_SHA2_NISTP256, 256)
        privateKey.outputStream().use { OpenSSHKeyPairResourceWriter.INSTANCE.writePrivateKey(pair, COMMENT, null, it) }
        publicKeyFile.outputStream().use { OpenSSHKeyPairResourceWriter.INSTANCE.writePublicKey(pair, COMMENT, it) }
        // [impl->dsn~host-keys~1]
        sshDirectory.resolve("config").writeText("Host *\n    StrictHostKeyChecking accept-new\n")
        // JGit records a new host key only in an existing file; it would ask before creating one.
        sshDirectory.resolve("known_hosts").writeText("")
    }

    /**
     * Clones the repository and returns the location of its todo.txt file.
     * A previous clone is kept if it is of the same repository and replaced otherwise.
     */
    // [impl->dsn~clone~1]
    fun clone(url: String, fileName: String = "todo.txt"): Path {
        val existing = GitStorage.repositoryOf(repositoryDirectory.resolve(fileName))
        val sameRemote = existing?.use { it.config.getString("remote", "origin", "url") == url } ?: false
        if (!sameRemote) {
            if (repositoryDirectory.exists() && repositoryDirectory.listDirectoryEntries().isNotEmpty()) {
                repositoryDirectory.toFile().deleteRecursively()
            }
            Git.cloneRepository()
                .setURI(url)
                .setDirectory(repositoryDirectory.toFile())
                .setTransportConfigCallback { if (ownKey && it is SshTransport) it.sshSessionFactory = ssh }
                .call()
                .close()
        }
        return repositoryDirectory.resolve(fileName)
    }

    private companion object {
        const val COMMENT = "koppter"
    }
}
