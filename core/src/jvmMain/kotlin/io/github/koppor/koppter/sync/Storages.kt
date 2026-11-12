package io.github.koppor.koppter.sync

import io.github.koppor.koppter.storage.FileStorage
import io.github.koppor.koppter.storage.TodoStorage
import org.eclipse.jgit.transport.SshSessionFactory
import java.nio.file.Path

/**
 * The storage for a todo.txt file in the file system:
 * synchronized through git if the file lies in the working tree of a repository, a plain file otherwise.
 *
 * @param ssh the SSH configuration for the remote repository; `null` uses the configuration of the user (`~/.ssh`)
 */
// [impl->dsn~git-detection~1]
fun storageForFile(file: Path, ssh: SshSessionFactory? = null): TodoStorage =
    GitStorage.repositoryOf(file)?.let { GitStorage(file, it, ssh) } ?: FileStorage(file)
