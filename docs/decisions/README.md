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
| [0012](0012-board-columns-by-filter-in-the-settings.md) | Board columns defined by filters are kept in the settings |

## Conventions

* File name: `NNNN-title-in-kebab-case.md`, numbered consecutively.
* A record is not rewritten after it is accepted.
  A later decision supersedes it explicitly and sets the old record's status to `superseded by NNNN`.
* Each record defines one OpenFastTrace item `adr~<slug>~1` directly below its title, see [requirements](../requirements/README.md#decisions-in-the-trace).
