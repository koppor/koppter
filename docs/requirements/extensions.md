# Extensions and board

Requirements and design for the todo.txt extensions and the Kanban board.

## Requirements

### Recurring tasks
`req~recurring-tasks~1`

When the user completes a task that has a recurrence tag (`rec:`), the application adds the next occurrence of the task.
A plain recurrence counts from the day of completion, a strict one (`rec:+…`) from the due date.

Covers:

* `feat~extensions~1`

Needs: dsn

### Top tasks
`req~top-tasks~1`

One click restricts the list to the tasks to do next, as the `top` add-on of the todo.txt command line does.
The number of tasks is a setting.

Covers:

* `feat~extensions~1`

Needs: dsn

### Most important tasks
`req~most-important-tasks~1`

The user marks the most important tasks and the day they are meant for, and sees them first.

Covers:

* `feat~extensions~1`

Needs: dsn

### Notes
`req~notes~1`

The user attaches a note of several lines to a task.
As with the `note` add-on of the todo.txt command line, the note is a file in the directory `notes` next to the todo.txt file, and the task names it with the tag `note:`.

Covers:

* `feat~extensions~1`

Needs: dsn

### Scheduled tasks
`req~scheduled-tasks~1`

The application adds the tasks that the file `ice_recur.txt` next to the todo.txt file schedules for the current day, in the format of the `ice_recur` add-on.
A task is not added while an open task with the same text exists.

Covers:

* `feat~extensions~1`

Needs: dsn

### Board
`req~board~1`

The application shows the tasks as a board with columns.
The user chooses whether the columns are the priorities, the projects, the contexts or ranges of the due date.
The board shows the tasks that match the filter.

Covers:

* `feat~kanban~1`

Needs: dsn

## Design

### Recurrence on completion
`dsn~recurrence-on-completion~1`

`Task.nextOccurrence` returns the open copy of a recurring task with its dates moved: the due date to the next date of the recurrence, the threshold date by the same distance.
A task with only a threshold date gets a new threshold date; a task without dates gets a due date.
`TodoController.toggle` appends the next occurrence in the same write as the completion, so that both are one commit.

Covers:

* `req~recurring-tasks~1`

Needs: impl, utest

### Top tasks
`dsn~top-tasks~1`

With the *Top* switch on, `TodoController.visible` is cut to the first `topCount` entries of the filtered list in list order.

Covers:

* `req~top-tasks~1`

Needs: impl, utest

### Most important tasks through priority and due date
`dsn~most-important-tasks~1`

There is no separate notation for most important tasks.
Such a task has priority `A` and a due date; the list order shows it first, and the *Top* view restricts the list to these tasks.

Covers:

* `req~most-important-tasks~1`
* `adr~no-mit-notation~1`

Needs: impl

### Groups of the board
`dsn~board-groups~1`

`groupEntries` distributes entries over groups by priority, project, context or due date.
A task with several projects or contexts appears in each of their groups.
The due date groups are overdue, today, later and no due date.
Tasks without a value form the last group; empty groups are left out.

Covers:

* `req~board~1`
* `adr~board-columns-by-attribute~1`

Needs: impl, utest

### Board
`dsn~board~1`

`Board` shows the groups of the visible tasks as columns side by side.
The board scrolls horizontally, each column vertically.
The columns contain the task cards of the list, with the same operations.

Covers:

* `req~board~1`

Needs: impl, utest

### View options
`dsn~view-options~1`

A row of switches above the tasks turns the *Top* view and the board on and off and, for the board, selects the attribute of the columns.

Covers:

* `req~board~1`
* `req~top-tasks~1`

Needs: impl, utest

### Companion files
`dsn~companion-files~1`

A storage that implements `CompanionFiles` reads and writes files relative to the directory of the todo.txt file.
`FileStorage` and `GitStorage` implement it; `GitStorage` commits the companion files written since the last commit together with the next change of the todo.txt file.
The storage of a single document on Android does not implement it, so that notes and scheduled tasks are not available there.

Covers:

* `req~notes~1`
* `req~scheduled-tasks~1`

Needs: impl, utest

### Notes
`dsn~notes~1`

For a storage with companion files, the edit dialog has a field for the note.
A task without a note gets the tag `note:` with a new file name of three characters and the extension `.txt` when the user enters a note; clearing the note removes the tag and leaves the file.
`TodoController` writes the text to `notes/<name>` before it writes the task.
A change of the note alone is passed to the storage as a change of its own, so that a storage with a history records it.

Covers:

* `req~notes~1`

Needs: impl, utest

### Schedule of ice_recur
`dsn~ice-recur-schedule~1`

`IceRecurEntry.parse` reads a line of the form `[@start] [@@end] timing - task`.
The timing is `daily`, `weekly`, `monthly` or `yearly` with an optional interval, followed by the specifiers `day`, `day_of_month`, `day_of_week`, `day_of_year` and `month_of_year`, separated by ` ; `.
`IceRecurEntry.occursOn` tells whether the schedule includes a date.
Without a start, the start is 2018-01-01, as in the add-on; weeks start on Sunday.
A line that is not understood is skipped.

Covers:

* `req~scheduled-tasks~1`

Needs: impl, utest

### Run of ice_recur
`dsn~ice-recur-run~1`

`iceRecurTasks` returns the tasks of the entries that occur today and have no open task with the same text.
`TodoController` adds them once per day and device; the day of the last run is kept in the settings.
With a synchronizing storage, the run waits for a running synchronization, and a task that the history shows as added today counts as present even if it is completed.

Covers:

* `req~scheduled-tasks~1`

Needs: impl, utest
