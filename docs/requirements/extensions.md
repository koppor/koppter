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

### Board columns defined by filters
`req~board-defined-columns~1`

The user defines columns of the board, each by a title and a filter expression, and switches the board to these columns.

Covers:

* `feat~kanban~1`

Needs: dsn

### Moving a task on the board
`req~board-move~1`

The user moves a task to another column of the board by dragging it there.
The task then has the value of that column: its priority, its project, its context or a due date in its range.

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

### Filter expressions
`dsn~query~1`

`Query` evaluates a filter expression in the syntax of topydo: terms separated by blanks, all of which a task has to match.
`+project` and `@context` match a task with this project or context.
`(A)` matches a task with this priority; `(>B)` and `(<=C)` match a higher priority, and this or a lower one; a task without priority matches none of them.
`key:value` matches a task whose tag has this value; with `<`, `<=`, `>`, `>=` in front of the value, the tag is compared to it, as a date if both are dates.
A date is an ISO date, `today`, `tomorrow`, `yesterday`, or an interval from today such as `1w`.
A leading `-` negates a term, and any other word matches a line that contains it, ignoring case.

Covers:

* `req~board-defined-columns~1`

Needs: impl, utest

### Columns defined by filters
`dsn~board-defined-columns~1`

The setting `boardColumns` holds one column per line as `title = expression`; a line without title is titled by its expression.
`groupByColumns` distributes the visible tasks over these columns, in the order of their definition.
A task appears in every column whose expression it matches, and a column without tasks stays on the board.
The view options offer these columns once at least one is defined.
Tasks are not dragged between them.

Covers:

* `req~board-defined-columns~1`
* `adr~board-columns-by-filter~1`

Needs: impl, utest

### Move between groups
`dsn~board-move~1`

`moveToGroup` returns the task as it is after a move from one group to another.
For priorities, the task gets the priority of the target group, or none.
For projects and contexts, the value of the source group is replaced by that of the target group; the other projects and contexts of the task stay.
For due dates, the group *today* sets today, the group *later* tomorrow, and the group without due date removes the date.
A move within a group or into the group of overdue tasks changes nothing.

Covers:

* `req~board-move~1`
* `adr~board-columns-by-attribute~1`

Needs: impl, utest

### Dragging on the board
`dsn~board-drag~1`

A task card is dragged with the mouse, or with the finger after a long press, as a plain drag with the finger scrolls.
While a card is dragged, its description follows the pointer and the column under the pointer is marked.
Dropping the card on another column applies the move as one change, with a message that names the column.
Only the columns that are shown can be a target, so a value that no visible task has is set in the edit dialog.

Covers:

* `req~board-move~1`

Needs: impl, utest

### View options
`dsn~view-options~1`

A row of switches above the tasks turns the *Top* view and the board on and off and, for the board, selects the attribute of the columns or the columns defined by filters.

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
