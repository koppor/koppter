# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Calendar Versioning](https://calver.org/) with the scheme `YYYY-0M-0D`.

## [Unreleased]

### Added

- Desktop application that shows the tasks of a todo.txt file with projects, contexts, tags and priority as labels at the top right of each task.
- Adding a task by typing a line in todo.txt syntax.
- Edit dialog with the description separate from the metadata and a preview of the resulting todo.txt line.
- Filter panel for completion state, text, priorities, projects, contexts and threshold date; priorities, projects and contexts are selected as the labels that the tasks carry, and the completion state with the switches *Open* and *Done*.
- Setting for the location of the todo.txt file and for writing creation and completion dates, which are not written by default.
- Android application with the same screens; the todo.txt file is kept in the storage of the application or chosen with the document picker of the system.
- Button in the settings that opens the file dialog of the platform to choose the todo.txt file.
- Synchronization through git for a todo.txt file that lies in a git repository: one commit per added, completed, reopened, edited and deleted task, merge of remote changes without conflicts, and push.
- The Android application built by the continuous integration is signed with a fixed key, so that a newer build installs over an older one.
- On Android, the settings show the public SSH key of the device and clone a git repository whose todo.txt file is then used.
- Synchronization status and a button to synchronize in the top bar.
- The edit dialog shows when a task was added and completed, derived from the git history.
- Completing a task with a recurrence tag (`rec:`, such as `rec:2w`, `rec:m` or `rec:+1y`) adds its next occurrence.
- Notes for tasks, kept as files in the directory `notes` next to the todo.txt file and named by the tag `note:`.
- Tasks scheduled in `ice_recur.txt` next to the todo.txt file are added on their days; days on which the application was not open are caught up.
- Editor for the schedule of `ice_recur.txt`, which shows for each entry the next day on which it adds its task and marks lines that are not understood.
- Colors of the palettes Nord and Everforest, each light and dark; the settings choose the palette and whether the application follows the system.
- The tag `cs:` names a task of ContextSwitcher; its label switches to that task, a pasted link of ContextSwitcher becomes the tag, and the edit dialog has a field for it.
- Application icon, a K as the cabin of a helicopter in the colors of Nord, for the window on the desktop and the launcher on Android.
- *Top* switch that restricts the list to the first tasks; their number is a setting.
- Hashtags (`#tag`) as a third kind of label next to projects and contexts: shown on the task, offered in the filter panel, the edit dialog and the filters of board columns.
- Contexts below contexts, written as `@research>reading`: the label shows the last level, a click on it shows the tasks of the context, and a filter for a context includes the contexts below it.
- The tag `dur:` holds the time a task is expected to take, such as `dur:1h30m`; the edit dialog has a field for it.
- On Android, text shared from another application becomes a task without a dialog; share actions in the settings state the context, dates and duration it gets, and each action is a target of its own in the share sheet.
- Overview with one tile per context that tells whether tasks of the context are overdue or due today; a click on a tile lists the tasks of the context by urgency, each with a bar that fills up to its due date.
- Planning: *Plan* gives each open task the earliest free times of its time map and writes the plan to `calendar.txt` and, with tentative events, to `calendar.ics`; time maps may be restricted to other time maps and are given to contexts, a barrier keeps tasks from being planned behind the end of the quarter or another date, and a calendar icon on each task tells whether its plan meets its due date.
- Limits for planning per project or context: a budget per day and per week (`max 5h/week`), a minimum per week that is planned first (`min 1h/week`), and the shortest part a task is cut into (`block 2h`, an hour by default).
- Planning gives a task that waits for others its times after them; the tags `id:` and `p:` of topydo state the dependencies.
- Planning leaves out a task whose threshold date lies behind its barrier, instead of listing it as not fitting.
- Guide on organizing many tasks in `docs/organizing.md`.
- A planned block starts on a quarter of an hour after an event of a calendar.
- Planning leaves out the times of the events of calendars that are named by the address of their iCalendar file, as Outlook and Google Calendar publish it.
- *Agenda* shows the planned blocks of today and the six days after it with the tasks that are due but not planned for today, and *Week* shows the plan of a week as a calendar.
- *Inbox* lists the tasks without context; a click on a context gives it to the task.
- Keyboard shortcuts: Ctrl+N for a new task, Ctrl+Enter to save the edit dialog, Ctrl+1 to Ctrl+6 for the views, Ctrl+Shift+A for the line that adds a task, Ctrl+E for a search through all tasks, Ctrl+Shift+F for the text of the filter, and Ctrl+Shift+L for the plan dialog.
- A dialog with changes asks before Esc, a click beside it or the back button closes it, and offers to return to it.
- In the inbox, typing narrows the contexts and Enter gives the first one to the marked task, or the typed text as a new context.
- Board view with one column per priority, project, context, hashtag or due date range.
- Board columns defined by filter expressions in the syntax of topydo, such as `Today = due:<=today -@waiting`; they are entered in the settings and kept in `koppter.yml` next to the todo.txt file.
- On the board, dragging a task to another column changes its priority, project, context or due date accordingly; for a column defined by a filter, the task is changed as little as possible so that it matches the filter.
- Changes that other programs make to the todo.txt file are picked up while the application runs.
- The release of a day on GitHub carries the signed Android application as an APK file.
- Project infrastructure: Gradle build with a Kotlin Multiplatform `core` module, requirements tracing, decision records and continuous integration.

[Unreleased]: https://github.com/koppor/koppter/commits/experimental
