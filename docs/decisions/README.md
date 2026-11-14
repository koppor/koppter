# Decisions

Architectural decisions are recorded as [Markdown Architectural Decision Records](https://adr.github.io/madr/) ([decision 0000](0000-use-markdown-architectural-decision-records.md)).

| Record | Decision |
| --- | --- |
| [0000](0000-use-markdown-architectural-decision-records.md) | Use Markdown Architectural Decision Records |
| [0001](0001-kotlin-multiplatform-with-compose-multiplatform.md) | Kotlin Multiplatform with Compose Multiplatform |
| [0002](0002-calver-changelog-verified-with-heylogs.md) | CalVer changelog, verified with heylogs |
| [0003](0003-requirements-tracing-with-openfasttrace.md) | Requirements tracing with OpenFastTrace |
| [0004](0004-metadata-after-description-on-edit.md) | Metadata is written after the description when a task is edited |
| [0005](0005-dates-from-the-git-history.md) | Creation and completion dates come from the git history |
| [0006](0006-jgit-for-git-access.md) | JGit for git access |
| [0007](0007-ssh-key-created-in-the-application.md) | SSH key created in the application on devices without SSH configuration |
| [0008](0008-board-columns-by-attribute.md) | Board columns are the values of an attribute |
| [0009](0009-no-notation-for-most-important-tasks.md) | No separate notation for most important tasks |
| [0010](0010-signing-key-as-repository-secret.md) | Signing key of the Android application as a repository secret |
| [0011](0011-onedrive-through-the-platform.md) | OneDrive through the file access of the platform |
| [0012](0012-board-columns-by-filter-in-a-file.md) | Board columns defined by filters are kept in a configuration file next to the todo.txt file |
| [0013](0013-smallest-change-on-drop-on-a-filter-column.md) | A task dropped on a column defined by a filter gets the smallest change that makes it match |
| [0014](0014-hashtags-as-a-third-kind-of-label.md) | Hashtags are a third kind of label next to projects and contexts |
| [0015](0015-colors-from-published-palettes.md) | Colors come from the palettes Nord and Everforest |
| [0016](0016-tag-for-the-task-of-contextswitcher.md) | A tag names the task of ContextSwitcher |
| [0017](0017-levels-of-a-context-separated-by-a-greater-than-sign.md) | The levels of a context are separated by a greater-than sign |
| [0018](0018-tag-for-the-duration-of-a-task.md) | A tag holds the duration of a task |
| [0019](0019-the-plan-is-derived-and-kept-in-calendar-txt.md) | The plan is derived from the tasks and kept in calendar.txt |
| [0020](0020-dependencies-with-the-tags-of-topydo.md) | Dependencies between tasks use the tags of topydo |
| [0021](0021-tag-for-the-fixed-time-of-a-task.md) | A tag states the fixed time of a task |
| [0022](0022-tag-for-the-time-by-which-a-task-is-due.md) | A tag states the time by which a task is due |
| [0023](0023-tag-for-the-travel-time-of-a-task.md) | A tag states the travel time of a task |
| [0024](0024-names-for-priorities-kept-in-the-configuration-file.md) | Priorities keep their letters in the file and get names for display |
| [0025](0025-locations-on-the-desktop-from-appdirs.md) | Locations on the desktop from the appdirs library |

## Conventions

* File name: `NNNN-title-in-kebab-case.md`, numbered consecutively.
* A record is not rewritten after it is accepted.
  A later decision supersedes it explicitly and sets the old record's status to `superseded by NNNN`.
* Each record defines one OpenFastTrace item `adr~<slug>~1` directly below its title, see [requirements](../requirements/README.md#decisions-in-the-trace).
