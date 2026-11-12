# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Calendar Versioning](https://calver.org/) with the scheme `YYYY-0M-0D`.

## [Unreleased]

### Added

- Desktop application that shows the tasks of a todo.txt file with projects, contexts, tags and priority as labels at the top right of each task.
- Adding a task by typing a line in todo.txt syntax.
- Edit dialog with the description separate from the metadata and a preview of the resulting todo.txt line.
- Filter panel for completion state, text, priorities, projects, contexts and threshold date.
- Setting for the location of the todo.txt file and for writing creation and completion dates, which are not written by default.
- Android application with the same screens; the todo.txt file is kept in the storage of the application or chosen with the document picker of the system.
- Button in the settings that opens the file dialog of the platform to choose the todo.txt file.
- Synchronization through git for a todo.txt file that lies in a git repository: one commit per added, completed, reopened, edited and deleted task, merge of remote changes without conflicts, and push.
- The Android application built by the continuous integration is signed with a fixed key, so that a newer build installs over an older one.
- On Android, the settings show the public SSH key of the device and clone a git repository whose todo.txt file is then used.
- Synchronization status and a button to synchronize in the top bar.
- The edit dialog shows when a task was added and completed, derived from the git history.
- Completing a task with a recurrence tag (`rec:`) adds its next occurrence.
- Notes for tasks, kept as files in the directory `notes` next to the todo.txt file and named by the tag `note:`.
- Tasks scheduled in `ice_recur.txt` next to the todo.txt file are added on their days.
- *Top* switch that restricts the list to the first tasks; their number is a setting.
- Board view with one column per priority, project, context or due date range.
- Board columns defined by filter expressions in the syntax of topydo, such as `Today = due:<=today -@waiting`; they are entered in the settings and kept in `todo.txt-config.yml` next to the todo.txt file.
- On the board, dragging a task to another column changes its priority, project, context or due date accordingly; for a column defined by a filter, the task is changed as little as possible so that it matches the filter.
- Changes that other programs make to the todo.txt file are picked up while the application runs.
- Project infrastructure: Gradle build with a Kotlin Multiplatform `core` module, requirements tracing, decision records and continuous integration.

[Unreleased]: https://github.com/koppor/koppter/commits/experimental
