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
- Changes that other programs make to the todo.txt file are picked up while the application runs.
- Project infrastructure: Gradle build with a Kotlin Multiplatform `core` module, requirements tracing, decision records and continuous integration.

[Unreleased]: https://github.com/koppor/koppter/commits/experimental
