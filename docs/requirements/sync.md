# Synchronization

Requirements and design for the synchronization of the todo.txt file.

## Requirements

### Git repository as the synchronization channel
`req~git-sync~1`

A todo.txt file that lies in the working tree of a git repository is synchronized with the remote repository without further configuration: the application commits its changes, fetches and merges the changes of other devices, and pushes.
History is never rewritten: remote changes are merged, not rebased.

Covers:

* `feat~sync-git~1`

Needs: dsn

### One commit per change
`req~commit-per-change~1`

Adding, completing, reopening, editing and deleting a task each create one commit whose message names the change and the task.

Covers:

* `feat~sync-git~1`

Needs: dsn

### No conflicts between devices
`req~no-merge-conflicts~1`

Changes made to different tasks on two devices before either of them synchronized are both kept, without the user having to resolve a conflict.
If the same task was changed differently on both devices, both versions are kept.

Covers:

* `feat~sync-git~1`

Needs: dsn

### Dates from the history
`req~dates-from-history~1`

For a file synchronized through git, the application shows when a task was added and when it was completed, although the file contains neither date.

Covers:

* `feat~sync-git~1`

Needs: dsn

### Synchronization status
`req~sync-status~1`

The application shows whether the file is synchronized, being synchronized, or could not be synchronized and why.
The user can start a synchronization at any time.

Covers:

* `feat~sync-git~1`

Needs: dsn

## Design

### Detection of the repository
`dsn~git-detection~1`

`storageForFile` looks for a git repository whose working tree contains the todo.txt file.
If there is one, the storage is a `GitStorage`, otherwise a `FileStorage`.

Covers:

* `req~git-sync~1`

Needs: impl, utest

### Git storage
`dsn~git-storage~1`

`GitStorage` reads and writes the file in the working tree and uses JGit for all git operations.
A lock guards the working tree and the index against concurrent use by the user interface and by the synchronization.
All synchronizations run one after the other on a single background thread.
Commits are made in the name of the configured git user, or in the name of the application if none is configured.

Covers:

* `req~git-sync~1`
* `adr~jgit~1`

Needs: impl

### Commit per change
`dsn~commit-per-change~1`

`TodoController` passes a description of each change, such as `Complete task: Call Mom`, to the storage.
`GitStorage.write` writes the file and commits it at once, with the description as the message.
A write that does not change the file creates no commit.

Covers:

* `req~commit-per-change~1`
* `adr~dates-from-history~1`

Needs: impl, utest

### Synchronization
`dsn~git-sync~1`

A synchronization commits pending changes of the file made by other programs, fetches, merges the remote branch of the current branch, and pushes.
A branch that is behind is fast-forwarded.
For diverged branches, the todo.txt file of the merge commit is the result of `mergeLines`; a conflict in any other file fails the synchronization and leaves the working tree as it was.
A push that is rejected because the remote branch moved is retried once after another fetch and merge.

Covers:

* `req~git-sync~1`
* `req~no-merge-conflicts~1`

Needs: impl, utest

### Line merge
`dsn~line-merge~1`

`mergeLines` computes the merged file from the common ancestor, our version and their version.
It starts from their version, removes the lines we removed and appends the lines we added.
If we removed as many lines as we added, each added line takes the place of the removed one.
A line that both sides added is kept once.
A line that both sides changed differently is kept in both versions.

Covers:

* `req~no-merge-conflicts~1`

Needs: impl, utest

### Dates from the history
`dsn~dates-from-history~1`

`taskDates` walks through the versions of the file, oldest first.
A task is added at the first version that contains it and completed at the first version in which it carries the completion marker; reopening forgets the completion.
A task is followed by its body without the `pri:` tag.
If a version removes as many tasks as it adds, these are taken as edits and keep their dates.
`GitStorage.dates` supplies the versions from the commits that changed the file, with the author date of each commit.

Covers:

* `req~dates-from-history~1`
* `adr~dates-from-history~1`

Needs: impl, utest

### Dates in the edit dialog
`dsn~dates-in-dialog~1`

The edit dialog shows the dates of the task from the history above its buttons.

Covers:

* `req~dates-from-history~1`

Needs: impl, utest

### Synchronization status
`dsn~sync-status~1`

For a synchronizing storage, the top bar shows the status as text and a button that starts a synchronization.
The controller starts a synchronization when it opens a file; the storage starts one after every write.

Covers:

* `req~sync-status~1`

Needs: impl, utest
