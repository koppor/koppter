# koppter

A [todo.txt](https://github.com/todotxt/todo.txt) application for Android and for Windows and Linux desktops.

Status: early development on the `experimental` branch.
The Android application is built but has not been run on a device yet.

## Screenshots

The tasks in the screenshots are invented.

The task list with the filter panel.
Projects, contexts, tags and the priority are labels at the top right of each task.

![Task list with the filter panel on the left](docs/screenshots/list.png)

The filter panel offers priorities, projects, contexts and hashtags as the labels that the tasks carry.
A click moves a label to the selected ones beside the title; here, the list shows the tasks of the projects *GarageSale* or *Work* that have the context *phone*.

![Task list filtered by two projects and a context](docs/screenshots/filter.png)

The board with one column per priority, while a task is dragged from column B to column A.

![Board by priority with a task being dragged to another column](docs/screenshots/board-priority.png)

The board with one column per project.

![Board with one column per project](docs/screenshots/board-project.png)

The board with [columns defined by filters](#board): *Now* is `due:<=today -@waiting`, *Waiting* is `@waiting`, *This week* is `due:<1w`, *Important* is `(>C)`.

![Board with the columns Now, Waiting, This week and Important](docs/screenshots/board-filter.png)

The edit dialog: the description without metadata at the top, the metadata below, then the note, the resulting line of the todo.txt file, and the date the task was added according to the git history.

![Edit dialog of a task](docs/screenshots/edit.png)

The settings, with the board columns by filter and the public SSH key for the git repository.

![Settings dialog](docs/screenshots/settings.png)

In a narrow window, as on a phone, the filter panel is a drawer.

![Task list in a narrow window](docs/screenshots/narrow.png)

## Goals

* The todo.txt file is the only data store, and it stays a file that other todo.txt tools can read and write.
* One code base for phone and desktop: Kotlin Multiplatform with Compose Multiplatform ([decision 0001](docs/decisions/0001-kotlin-multiplatform-with-compose-multiplatform.md)).
* Synchronization through git, with one commit per added and per completed task.
  The dates a task was added and completed are then read from the history instead of being written into the file.
* Synchronization through a local folder and through OneDrive.
* A task list that shows projects, contexts, hashtags, due date and priority as labels at the top right of each task.
* An edit dialog that separates the description from the metadata and shows the resulting todo.txt line.
* A filter panel for completion state, text, priorities, projects, contexts and hashtags.
* A Kanban board.
* Support for the todo.txt extensions `note`, `top` and `ice_recur`, and for the `rec:` and `t:` tags.

The planned scope is tracked as requirements in [docs/requirements](docs/requirements/README.md).

## Files

| File | Content |
| --- | --- |
| `todo.txt` | The tasks |
| `notes/<name>.txt` | The note of the task with the tag `note:<name>.txt` |
| `todo.txt-config.yml` | Configuration that belongs to the todo.txt file: the [board columns defined by filters](#board) |
| `ice_recur.txt` | Schedules of recurring tasks in the format of the [ice_recur](https://github.com/rlpowell/todo-text-stuff/blob/master/ice_recur) add-on, such as `weekly ; day Monday - (B) Plan the week` |

If these files lie in a git repository, the application commits every change and synchronizes with the remote repository.

A file in OneDrive, or in another storage service, is used through the platform ([decision 0011](docs/decisions/0011-onedrive-through-the-platform.md)): on Android, choose it with *Choose file* in the settings; on the desktop, choose it in the synchronized folder.

## Hashtags

A word that starts with `#` and a letter, such as `#urgent`, is a hashtag: a third kind of label next to `+project` and `@context`, without a fixed meaning ([decision 0014](docs/decisions/0014-hashtags-as-a-third-kind-of-label.md)).
`#123` and `C#` stay words of the description.

## Board

The board shows one column per priority, project, context, hashtag or due date range.
Dragging a task to another column changes that attribute of the task.

Columns can also be defined by filters, one per line as `title = filter`.
They are entered in the settings and kept in `todo.txt-config.yml` next to the todo.txt file, so that they are synchronized with the tasks ([decision 0012](docs/decisions/0012-board-columns-by-filter-in-a-file.md)):

```text
Now = due:<=today -@waiting
Waiting = @waiting
This week = due:<1w
Important = (>C)
```

| Term | Matches a task |
| --- | --- |
| `+project`, `@context`, `#hashtag` | with this project, context or hashtag |
| `(A)`, `(>B)`, `(<=C)` | with this priority, a higher one, this or a lower one |
| `due:today`, `due:<1w`, `t:>=2026-12-01`, `status:doing` | whose tag has this value or compares so to it; dates are ISO dates, `today`, `tomorrow`, `yesterday` or an interval from today |
| `-term` | that does not match the term |
| any other word | whose line contains it |

Dragging a task to such a column changes the task as little as possible so that it matches the filter ([decision 0013](docs/decisions/0013-smallest-change-on-drop-on-a-filter-column.md)): projects, contexts and tags are added or, for negated terms, removed, and a priority or date is set to the nearest value that matches.
Nothing else is removed, so the task stays in its former column as well unless the filter of the new column excludes that.
A column whose filter contains a word or a negated comparison does not take tasks.

In `todo.txt-config.yml`, a filter that contains a hashtag has to be quoted (`Doing: "#doing"`), as YAML reads an unquoted `#` as the start of a comment.
The application writes all filters quoted.

## Building

Requirements: a JDK that starts Gradle (17 or newer) and the Android SDK (`ANDROID_HOME`, or `sdk.dir` in `local.properties`).
The build itself runs on JDK 25 and compiles for it.
The Gradle wrapper downloads everything else, including that JDK and the Android platform the build needs.

```shell
./gradlew build traceRequirements
```

`build` compiles and runs the unit tests and the user interface tests.
`traceRequirements` checks that every requirement and decision is covered ([OpenFastTrace](https://github.com/itsallcode/openfasttrace)); the report is written to `build/reports/tracing.txt`.

## Running

```shell
./gradlew :app:run
```

starts the desktop application.
It reads `todo.txt` in the home directory unless another file is set in the settings.

```shell
./gradlew :android:assembleDebug
```

builds the Android application as `android/build/outputs/apk/debug/android-debug.apk`.
The build workflow publishes the release variant, signed with a key that is a secret of the repository ([decision 0010](docs/decisions/0010-signing-key-as-repository-secret.md)), as the artifact `koppter-android`.

## Project documents

| Document | Content |
| --- | --- |
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
