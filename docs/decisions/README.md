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

## Conventions

* File name: `NNNN-title-in-kebab-case.md`, numbered consecutively.
* A record is not rewritten after it is accepted.
  A later decision supersedes it explicitly and sets the old record's status to `superseded by NNNN`.
* Each record defines one OpenFastTrace item `adr~<slug>~1` directly below its title, see [requirements](../requirements/README.md#decisions-in-the-trace).
