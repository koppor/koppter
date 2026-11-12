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

The editor of the scheduled tasks tells for each entry on which day it adds its task next.

![Editor of the scheduled tasks](docs/screenshots/schedule.png)

In a narrow window, as on a phone, the filter panel is a drawer.

![Task list in a narrow window](docs/screenshots/narrow.png)

The colors are those of the palette [Nord](https://www.nordtheme.com/) or [Everforest](https://github.com/sainnhe/everforest), light or dark; the settings choose, and by default the application follows the system ([decision 0015](docs/decisions/0015-colors-from-published-palettes.md)).
The screenshots above show Nord in its light variant.

![Task list in the dark variant of Nord](docs/screenshots/nord-dark.png)

![Task list in the light variant of Everforest](docs/screenshots/everforest-light.png)

![Task list in the dark variant of Everforest](docs/screenshots/everforest-dark.png)

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
* Support for the [extensions](#extensions-of-the-todotxt-format) `note`, `top` and `ice_recur`, for the `rec:` and `t:` tags, and for hashtags.

The planned scope is tracked as requirements in [docs/requirements](docs/requirements/README.md).

## Files

| File | Content |
| --- | --- |
| `todo.txt` | The tasks |
| `notes/<name>.txt` | The note of the task with the tag `note:<name>.txt` |
| `koppter.yml` | Configuration of this application that belongs to the todo.txt file: the [board columns defined by filters](#board) |
| `ice_recur.txt` | Schedules of [recurring tasks](#recurring-tasks) in the format of the ice_recur add-on, such as `weekly ; day Monday - (B) Plan the week` |

If these files lie in a git repository, the application commits every change and synchronizes with the remote repository.

A file in OneDrive, or in another storage service, is used through the platform ([decision 0011](docs/decisions/0011-onedrive-through-the-platform.md)): on Android, choose it with *Choose file* in the settings; on the desktop, choose it in the synchronized folder.

## Extensions of the todo.txt format

The [todo.txt format](https://github.com/todotxt/todo.txt) defines the completion mark `x`, the priority `(A)`, dates, `+project`, `@context` and tags of the form `key:value`.
The application understands the following extensions, which other todo.txt tools use as well.

| Notation | Meaning | How to use it |
| --- | --- | --- |
| `due:2026-12-24` | Due date | *Due date* in the edit dialog. The label is highlighted on the day and marked when the date has passed; the list orders tasks of equal priority by due date. |
| `t:2026-12-01` | Threshold date: the task is not relevant before that day | *Threshold* in the edit dialog. The task is hidden until that day; *Show tasks before their threshold date* in the filter panel shows it. |
| `rec:1w` | Recurrence: completing the task adds its next occurrence | See [recurring tasks](#recurring-tasks). |
| `ice_recur.txt` | Schedules that add tasks on certain days | See [recurring tasks](#recurring-tasks). |
| `note:k3f.txt` | A note for the task, kept in the file `notes/k3f.txt` | *Note* in the edit dialog; the application names the file. As in the [note add-on](https://github.com/mgarrido/todo.txt-cli/tree/note/todo.actions.d). |
| `#urgent` | Hashtag: a third kind of label next to `+project` and `@context`, without a fixed meaning | *Add hashtag* in the edit dialog, or typed into the task. A hashtag starts with a letter: `#123` and `C#` stay words of the description ([decision 0014](docs/decisions/0014-hashtags-as-a-third-kind-of-label.md)). |
| `pri:A` | The priority a completed task had | Written when a task with a priority is completed, as the format has no priority for completed tasks; reopening the task restores the priority. |

The *Top* switch above the list restricts it to the first tasks, whose number is a setting.
There is no notation for a most important task: priority `(A)` together with a due date serves that purpose ([decision 0009](docs/decisions/0009-no-notation-for-most-important-tasks.md)).

Notes, `ice_recur.txt` and the board columns by filter need a todo.txt file in a folder or a git repository.
They are not available for a file chosen through the document picker of Android, as the application then has access to that one file only.

### Recurring tasks

There are two ways to make a task recur.
They differ in what triggers the next task: completing the previous one, or the calendar.

#### After completion: the tag `rec:`

Use it for a task whose next occurrence only makes sense once the current one is done, such as watering the plants.
There is one open task at a time.

1. Add the task with a due date and a recurrence, in the edit dialog or as a line: `Water the plants due:2026-11-12 rec:1w`.
2. Complete the task when it is done.
3. The application marks it as completed and adds the same task with the next due date.

The recurrence is an amount and a unit; without an amount it is one unit.

| Unit | Meaning | Example |
| --- | --- | --- |
| `d` | days | `rec:3d` |
| `b` | business days, Monday to Friday | `rec:b` |
| `w` | weeks | `rec:2w` |
| `m` | months | `rec:m` |
| `y` | years | `rec:1y` |

The next due date counts from the day of completion.
With a `+` in front of the interval, it counts from the previous due date, however late the task was completed:

| Task | Completed on | Next task |
| --- | --- | --- |
| `Water the plants due:2026-11-12 rec:1w` | 2026-11-14 | `Water the plants due:2026-11-21 rec:1w` |
| `Pay the rent due:2026-11-01 rec:+1m` | 2026-11-03 | `Pay the rent due:2026-12-01 rec:+1m` |

A threshold date moves along with the due date and keeps its distance to it.
A task with a threshold date only gets the next threshold date, and a task with neither date gets a due date.
The next task keeps the priority.

#### By the calendar: the file `ice_recur.txt`

Use it for a task that has to appear on certain days whether or not the previous one is done: every Monday, on the first of the month, on the last Friday of the month.
The format is that of the [ice_recur](https://github.com/rlpowell/todo-text-stuff/blob/master/ice_recur) add-on.

1. Open the editor with the calendar button in the top bar.
   It edits the file `ice_recur.txt` next to the todo.txt file, which any text editor can edit as well; the file name is fixed, as in the add-on.
2. Write one entry per line: `[@start] [@@end] timing - task`.
   The part after ` - ` is the task as it is added to the todo.txt file.
3. The application adds the tasks of the day when it is open, once per day.
   The days since it was last open are caught up.
   A task of an entry that is added or changed in the editor and occurs today is added at once.
4. Below the text, the editor tells for each entry the next day on which it adds its task, and which lines it does not understand.

| Entry | Adds the task |
| --- | --- |
| `daily - (A) Check the mail` | every day |
| `weekly ; day Monday - (B) Plan the week` | every Monday |
| `weekly ; day Friday, Sunday - Empty the bins` | every Friday and Sunday |
| `@2026-11-02 weekly 2 ; day Thursday - Put out the paper bin` | every second Thursday, counted from the week of 2026-11-02 |
| `monthly ; day_of_month 1, 15 - Check the account` | on the 1st and the 15th of every month |
| `monthly ; day_of_month -1 - Send the time sheet` | on the last day of every month |
| `monthly ; day_of_week Friday => -1 - Back up the photos` | on the last Friday of every month |
| `monthly ; day_of_week Tuesday => 1, 3 - Team meeting` | on the first and the third Tuesday of every month |
| `@2026-03-01 monthly 3 - Read the meters` | every three months on the day of the start date: 1 March, 1 June, … |
| `@2026-01-04 yearly - Renew the membership` | every year on 4 January |
| `yearly ; month_of_year March, September - Change the tires` | on the 1st of March and of September |
| `@2026-06-01 @@2026-08-31 daily - Water the garden` | every day from 1 June to 31 August 2026 |

* The timing starts with `daily`, `weekly`, `monthly` or `yearly`, optionally followed by an interval: `weekly 2` is every second week.
* Further restrictions follow, each separated by ` ; ` with a blank on both sides: `day`, `day_of_month`, `day_of_week`, `day_of_year`, `month_of_year`.
* Days and months are written as English names or as numbers; for days of the week, 0 is Sunday.
  A negative number counts from the end: `day_of_month -1` is the last day of the month.
* Without a start date, intervals count from 2018-01-01, a Monday, as in the add-on.
  An entry with an interval greater than one should therefore have a start date.
* A line that starts with `#` is a comment.

A task is not added while an open task with the same text exists, so tasks that are not completed do not pile up.
Two tasks have the same text if they differ at most in priority, dates and the tags `t:`, `due:` and `note:`.

The application differs from the add-on in these points:

* The add-on is started once a day by a scheduler; the application runs the schedules itself, and only while it is open.
* The add-on always writes the creation date; the application writes it only if writing dates is switched on in the settings.
* The add-on stops at a line it does not understand; the application skips the line and marks it in its editor.
* The options of the add-on to ignore projects and contexts when comparing tasks do not exist.
* Each device remembers on its own when it last ran the schedules.
  In a git repository, a task that any device added since then counts as existing, also when it is completed already.
  Without git, a second device may add a task again that was added and completed on the first.

## Board

The board shows one column per priority, project, context, hashtag or due date range.
Dragging a task to another column changes that attribute of the task.

Columns can also be defined by filters, one per line as `title = filter`.
They are entered in the settings and kept in `koppter.yml` next to the todo.txt file, so that they are synchronized with the tasks ([decision 0012](docs/decisions/0012-board-columns-by-filter-in-a-file.md)):

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

In `koppter.yml`, a filter that contains a hashtag has to be quoted (`Doing: "#doing"`), as YAML reads an unquoted `#` as the start of a comment.
The application writes all filters quoted.

## Installing

The [latest release](https://github.com/koppor/koppter/releases/latest) carries the Android application as the file `koppter-<date>.apk`.
There is one release per day on which something changed, named after that day.
The desktop application is not packaged yet; it is started from the sources, see [running](#running).

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
The workflows build the release variant, signed with a key that is a secret of the repository ([decision 0010](docs/decisions/0010-signing-key-as-repository-secret.md)): the build workflow publishes it as the artifact `koppter-android`, the daily release workflow attaches it to the release.

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
