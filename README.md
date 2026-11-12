# koppter

A [todo.txt](https://github.com/todotxt/todo.txt) application for Android and for Windows and Linux desktops.

Status: early development on the `experimental` branch.
Nothing is released yet.

## Goals

* The todo.txt file is the only data store, and it stays a file that other todo.txt tools can read and write.
* One code base for phone and desktop: Kotlin Multiplatform with Compose Multiplatform ([decision 0001](docs/decisions/0001-kotlin-multiplatform-with-compose-multiplatform.md)).
* Synchronization through git, with one commit per added and per completed task.
  The dates a task was added and completed are then read from the history instead of being written into the file.
* Synchronization through a local folder and through OneDrive.
* A task list that shows projects, contexts, due date and priority as labels at the top right of each task.
* An edit dialog that separates the description from the metadata and shows the resulting todo.txt line.
* A filter panel for completion state, text, priorities, projects and contexts.
* A Kanban board.
* Support for the todo.txt extensions `note`, `top` and `ice_recur`, and for the `rec:` and `t:` tags.

The planned scope is tracked as requirements in [docs/requirements](docs/requirements/README.md).

## Building

Requirements: JDK 21.
The Gradle wrapper downloads everything else.

```shell
./gradlew build traceRequirements
```

`build` compiles and runs the unit tests.
`traceRequirements` checks that every requirement and decision is covered ([OpenFastTrace](https://github.com/itsallcode/openfasttrace)); the report is written to `build/reports/tracing.txt`.

## Project documents

| Document | Content |
|----------|---------|
| [CHANGELOG.md](CHANGELOG.md) | User-visible changes, [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format, [CalVer](https://calver.org/) releases |
| [docs/requirements](docs/requirements/README.md) | Features, requirements and design, traced to code |
| [docs/decisions](docs/decisions/README.md) | Architectural decisions as [MADR](https://adr.github.io/madr/) |
| [CLAUDE.md](CLAUDE.md) | Conventions for development sessions with Claude Code |

## Related work

* [Sleek](https://github.com/ransome1/sleek) — desktop todo.txt manager; model for the label style and the edit dialog.
* [mindstream](https://github.com/xuhcc/mindstream) — model for the label placement in the list.
* [Web Todo.txt](https://github.com/EpocDotFr/webtodotxt) — model for the filter panel.
* [Simpletask](https://github.com/cdelahousse/simpletask-android) — Android todo.txt app with file-based synchronization.
* [topydo](https://github.com/topydo/topydo) — model for the Kanban columns.

## License

[MIT](LICENSE)
