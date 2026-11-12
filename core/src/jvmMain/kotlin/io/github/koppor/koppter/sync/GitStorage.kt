package io.github.koppor.koppter.sync

import io.github.koppor.koppter.storage.CompanionFiles
import io.github.koppor.koppter.storage.FileStorage
import io.github.koppor.koppter.storage.SyncState
import io.github.koppor.koppter.storage.SyncStatus
import io.github.koppor.koppter.storage.SyncingStorage
import kotlinx.datetime.LocalDate
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.treewalk.TreeWalk
import java.nio.file.Path
import java.time.ZoneId
import java.util.concurrent.Executors

/**
 * A todo.txt file in the working tree of a git repository.
 *
 * Every change of the file is committed at once, with the change as the commit message;
 * the exchange with the remote repository runs afterwards in the background.
 * Remote changes are merged, never rebased, and the todo.txt file is merged line by line
 * ([mergeLines]), so that concurrent changes on two devices do not conflict.
 */
// [impl->dsn~git-storage~1]
class GitStorage(
    private val file: Path,
    private val repository: Repository,
    private val ssh: SshSessionFactory? = null,
) : SyncingStorage, CompanionFiles {
    private val git = Git(repository)
    private val files = FileStorage(file)
    private val path = repository.workTree.toPath().toRealPath().relativize(file.toAbsolutePath().normalize().let { it.parent.toRealPath().resolve(it.fileName) })
        .joinToString("/")

    /** Guards the working tree and the index: commits of the user interface and merges of the background thread. */
    private val lock = Any()
    private val background = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "koppter-git").apply { isDaemon = true } }

    /** Companion files written since the last commit, as paths in the repository. */
    private val companions = mutableSetOf<String>()

    override var onStatus: (SyncStatus) -> Unit = {}

    // [impl->dsn~companion-files~1]
    override fun readCompanion(path: String): String? = synchronized(lock) { files.readCompanion(path) }

    override fun writeCompanion(path: String, text: String) = synchronized(lock) {
        files.writeCompanion(path, text)
        companions += this.path.substringBeforeLast('/', "").let { if (it.isEmpty()) path else "$it/$path" }
        Unit
    }

    override fun read(): String = synchronized(lock) { files.read() }

    override fun version(): Any? = files.version()

    // [impl->dsn~commit-per-change~1]
    override fun write(text: String, change: String) {
        synchronized(lock) {
            files.write(text, change)
            commit(change)
        }
        sync()
    }

    override fun sync() {
        background.execute {
            onStatus(SyncStatus(SyncState.RUNNING))
            try {
                synchronize()
                onStatus(SyncStatus(SyncState.IDLE))
            } catch (e: Exception) {
                onStatus(SyncStatus(SyncState.FAILED, e.message ?: e.toString()))
            }
        }
    }

    /**
     * Synchronizes and waits for the result.
     * Runs on the background thread like [sync], as two synchronizations at the same time would compete for the remote.
     */
    fun synchronizeNow() {
        try {
            background.submit(::synchronize).get()
        } catch (e: java.util.concurrent.ExecutionException) {
            throw e.cause ?: e
        }
    }

    /** Commits pending changes, merges the remote branch and pushes. */
    // [impl->dsn~git-sync~1]
    private fun synchronize() {
        // A change made by another program has no description of its own.
        synchronized(lock) { commit("Update $path") }
        if (repository.remoteNames.isEmpty()) return
        val remote = if (Constants.ORIGIN in repository.remoteNames) Constants.ORIGIN else repository.remoteNames.first()

        // Twice: another device may push between our fetch and our push.
        repeat(2) {
            git.fetch().setRemote(remote).withSsh().call()
            synchronized(lock) { mergeRemote(remote) }
            if (repository.resolve("HEAD") == null) return
            val rejected = git.push().setRemote(remote).withSsh().call()
                .flatMap { it.remoteUpdates }
                .filterNot { it.status == RemoteRefUpdate.Status.OK || it.status == RemoteRefUpdate.Status.UP_TO_DATE }
            if (rejected.isEmpty()) return
            if (rejected.any { it.status != RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD }) {
                error("Push rejected: " + rejected.joinToString { it.message ?: it.status.toString() })
            }
        }
        error("Push rejected twice: the remote repository keeps changing")
    }

    private fun mergeRemote(remote: String) {
        val theirs = repository.resolve("refs/remotes/$remote/${repository.branch}") ?: return
        val ours = repository.resolve("HEAD")
        if (ours == null) {
            // A fresh repository without commits takes over the remote branch.
            git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef(theirs.name).call()
            return
        }
        val base = mergeBase(ours, theirs)
        if (base == theirs) return
        if (base == ours) {
            git.merge().include(theirs).setFastForward(MergeCommand.FastForwardMode.FF_ONLY).call()
            return
        }

        val merged = mergeLines(
            base = base?.let { linesAt(it) } ?: emptyList(),
            ours = linesAt(ours),
            theirs = linesAt(theirs),
        )
        val result = git.merge().include(theirs).setCommit(false).setFastForward(MergeCommand.FastForwardMode.NO_FF).call()
        val conflicts = result.conflicts?.keys.orEmpty() - path
        if (conflicts.isNotEmpty()) {
            git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).call()
            error("Conflicting changes in " + conflicts.joinToString())
        }
        files.write(merged.joinToString("\n", postfix = if (merged.isEmpty()) "" else "\n"), "")
        git.add().addFilepattern(path).call()
        git.commit().setMessage("Merge remote changes").setIdent().call()
    }

    /** Commits the todo.txt file and the companion files written since the last commit, and nothing else. */
    private fun commit(message: String) {
        val paths = listOf(path) + companions
        git.add().apply { paths.forEach(::addFilepattern) }.call()
        val status = git.status().apply { paths.forEach(::addPath) }.call()
        val changed = status.added + status.changed + status.removed
        if (changed.isEmpty()) return
        git.commit().apply { changed.forEach(::setOnly) }.setMessage(message).setIdent().call()
        companions.clear()
    }

    // [impl->dsn~dates-from-history~1]
    override fun dates(): Map<String, TaskDates> = synchronized(lock) {
        if (repository.resolve("HEAD") == null) return emptyMap()
        val zone = ZoneId.systemDefault()
        val history = git.log().addPath(path).call().reversed().map { commit ->
            val date = commit.authorIdent.whenAsInstant.atZone(zone).toLocalDate()
            Snapshot(LocalDate(date.year, date.monthValue, date.dayOfMonth), linesAt(commit))
        }
        taskDates(history)
    }

    private fun mergeBase(a: ObjectId, b: ObjectId): RevCommit? = RevWalk(repository).use { walk ->
        walk.revFilter = RevFilter.MERGE_BASE
        walk.markStart(walk.parseCommit(a))
        walk.markStart(walk.parseCommit(b))
        walk.next()
    }

    private fun linesAt(commit: ObjectId): List<String> = RevWalk(repository).use { walk ->
        val tree = walk.parseCommit(commit).tree
        TreeWalk.forPath(repository, path, tree)?.use { treeWalk ->
            repository.open(treeWalk.getObjectId(0)).bytes.decodeToString().lines().dropLastWhile { it.isEmpty() }
        } ?: emptyList()
    }

    /** Without a configured user, as on a phone, the commits are made in the name of the application. */
    private fun org.eclipse.jgit.api.CommitCommand.setIdent() = apply {
        if (repository.config.getString("user", null, "name") == null) {
            val ident = PersonIdent("koppter", "koppter@localhost")
            setAuthor(ident).setCommitter(ident)
        }
    }

    private fun <C : TransportCommand<C, *>> C.withSsh(): C = setTransportConfigCallback { transport ->
        if (ssh != null && transport is SshTransport) transport.sshSessionFactory = ssh
    }

    private object Constants {
        const val ORIGIN = "origin"
    }

    companion object {
        /** The repository whose working tree contains the file, or `null` if there is none. */
        // [impl->dsn~git-detection~1]
        fun repositoryOf(file: Path): Repository? {
            val directory = file.toAbsolutePath().normalize().parent?.toFile()?.takeIf { it.isDirectory } ?: return null
            val builder = FileRepositoryBuilder().findGitDir(directory)
            return if (builder.gitDir == null) null else builder.build().takeUnless { it.isBare }
        }
    }
}
