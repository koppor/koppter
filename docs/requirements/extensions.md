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

### Contexts below contexts
`req~context-hierarchy~1`

A context lies below another context, over any number of levels.
A task shows the last level of its context and, on request, the whole path.
From the context of a task, the user gets to the tasks of that context.
Restricting the list to a context includes the contexts below it.

Covers:

* `feat~extensions~1`

Needs: dsn

### Duration of a task
`req~duration~1`

A task states the time it is expected to take.

Covers:

* `feat~extensions~1`

Needs: dsn

### Task of ContextSwitcher
`req~context-switcher-link~1`

A task refers to a task of ContextSwitcher, and the user switches to it from the task.

Covers:

* `feat~extensions~1`

Needs: dsn

### Scheduled tasks
`req~scheduled-tasks~1`

The application adds the tasks that the file `ice_recur.txt` next to the todo.txt file schedules for the current day and for the days since it last did so, in the format of the `ice_recur` add-on.
A task is not added while an open task with the same text exists; as in the add-on, priority, dates and the tags `t:`, `due:` and `note:` are not part of that text.

Covers:

* `feat~extensions~1`

Needs: dsn

### Editor of the scheduled tasks
`req~scheduled-tasks-editor~1`

The user edits the schedule of `ice_recur.txt` in the application.
The application tells for each entry on which day it adds its task next, and which lines it does not understand.

Covers:

* `feat~extensions~1`

Needs: dsn

### Board
`req~board~1`

The application shows the tasks as a board with columns.
The user chooses whether the columns are the priorities, the projects, the contexts, the hashtags or ranges of the due date.
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
For a column defined by a filter, the task then matches the filter.

Covers:

* `feat~kanban~1`

Needs: dsn

### Hashtags
`req~hashtags~1`

A word of a task that starts with `#` and a letter is a hashtag: a label without a fixed meaning.
The application shows, filters and edits hashtags like projects and contexts, and offers them as the columns of the board.

Covers:

* `feat~extensions~1`

Needs: dsn

## Design

### Hashtags of a task
`dsn~hashtags~1`

`Task.hashtags` are the words of the body that start with `#` followed by a letter, without the `#`.
A number sign followed by another character, such as `#123`, and a number sign inside a word stay words of the description.
Hashtags are not part of the description, and `Task.edit` writes them between the contexts and the tags.

Covers:

* `req~hashtags~1`
* `adr~hashtags~1`

Needs: impl, utest

### Hashtags in the user interface
`dsn~hashtags-in-ui~1`

A hashtag is an outlined label on the task, after the contexts.
The filter panel and the edit dialog have a section for hashtags that works like those for projects and contexts; the edit dialog accepts only names that start with a letter.
The board offers the hashtags as columns.

Covers:

* `req~hashtags~1`

Needs: impl, utest

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

`groupEntries` distributes entries over groups by priority, project, context, hashtag or due date.
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
`+project`, `@context` and `#hashtag` match a task with this project, context or hashtag.
`(A)` matches a task with this priority; `(>B)` and `(<=C)` match a higher priority, and this or a lower one; a task without priority matches none of them.
`key:value` matches a task whose tag has this value; with `<`, `<=`, `>`, `>=` in front of the value, the tag is compared to it, as a date if both are dates.
A date is an ISO date, `today`, `tomorrow`, `yesterday`, or an interval from today such as `1w`.
A leading `-` negates a term, and any other word matches a line that contains it, ignoring case.

Covers:

* `req~board-defined-columns~1`

Needs: impl, utest

### Configuration file
`dsn~config-file~1`

`ConfigFile` reads and changes the companion file `koppter.yml`, a YAML file with the configuration that belongs to the todo.txt file; it is named after the application that reads it.
The key `board-columns` maps the title of each column to its filter expression, one column per line, in the order of the board.
Titles and expressions are read as plain, single-quoted or double-quoted scalars and written double-quoted.
Replacing the columns keeps the other content of the file.

Covers:

* `req~board-defined-columns~1`
* `adr~board-columns-by-filter~1`

Needs: impl, utest

### Columns defined by filters
`dsn~board-defined-columns~1`

The configuration file holds the columns.
The settings dialog edits them as text with one column per line as `title = expression`; a line without title is titled by its expression.
A change of the columns is recorded as a change of its own.
The controller reads the configuration file again when it looks for changes of the todo.txt file, so that columns defined on another device appear.
`groupByColumns` distributes the visible tasks over these columns, in the order of their definition.
A task appears in every column whose expression it matches, and a column without tasks stays on the board.
The view options offer these columns once at least one is defined.

Covers:

* `req~board-defined-columns~1`
* `adr~board-columns-by-filter~1`

Needs: impl, utest

### Move between groups
`dsn~board-move~1`

`moveToGroup` returns the task as it is after a move from one group to another.
For priorities, the task gets the priority of the target group, or none.
For projects, contexts and hashtags, the value of the source group is replaced by that of the target group; the other projects, contexts and hashtags of the task stay.
For due dates, the group *today* sets today, the group *later* tomorrow, and the group without due date removes the date.
A move within a group or into the group of overdue tasks changes nothing.

Covers:

* `req~board-move~1`
* `adr~board-columns-by-attribute~1`

Needs: impl, utest

### Move to a column defined by a filter
`dsn~board-move-to-filter~1`

`Query.applyTo` returns the task with the smallest change that makes it match the expression.
A term that the task matches changes nothing.
Otherwise, `+project`, `@context` and `#hashtag` add the project, context or hashtag, and `key:value` sets the tag; negated, they remove it.
A priority term sets the nearest priority that matches, and its negation removes the priority.
A comparison of a tag with a date sets the nearest date that matches.
A word, a negated comparison, and a comparison with a value that is not a date cannot be established; the column then does not take the task.
Projects, contexts and tags that the expression does not name stay, so the task may still match the filter of its former column.

Covers:

* `req~board-move~1`
* `adr~smallest-change-on-drop~1`

Needs: impl, utest

### Dragging on the board
`dsn~board-drag~1`

A task card is dragged with the mouse, or with the finger after a long press, as a plain drag with the finger scrolls.
While a card is dragged, its description follows the pointer and the column under the pointer is marked.
Only a column that can take the card is marked.
Dropping the card on such a column applies the move as one change, with a message that names the column.
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
The storage of a single document on Android does not implement it, so that notes, scheduled tasks and board columns defined by filters are not available there.

Covers:

* `req~notes~1`
* `req~scheduled-tasks~1`
* `req~board-defined-columns~1`

Needs: impl, utest

### Hierarchy of contexts
`dsn~context-hierarchy~1`

`>` separates the levels of a context: `research>reading` lies below `research`.
`Filter` and the context term of `Query` match a context and the contexts below it (`isWithin`); removing a context term from a task removes the contexts below it as well.
`TaskLabels` shows the last level of a context and the path in a tooltip; a click on the label sets the filter to this context.
`FilterPanel` offers the contexts of the file and the contexts above them as a tree, one context per line and indented by its level, as soon as one context has levels.

Covers:

* `req~context-hierarchy~1`
* `adr~context-hierarchy~1`

Needs: impl, utest

### Tag for the duration
`dsn~duration~1`

The tag `dur:` holds the duration in hours and minutes, such as `45m`, `2h` or `1h30m`.
`Task.duration` reads it with the parser of the Kotlin standard library and gives nothing for a value that is no positive duration.
The edit dialog has a field for the tag and disables *Save* while its value is no duration.

Covers:

* `req~duration~1`
* `adr~duration-tag~1`

Needs: impl, utest

### Tag for the task of ContextSwitcher
`dsn~context-switcher-tag~1`

The tag `cs:` holds the id of a task of ContextSwitcher as its links have it.
`TaskLabels` shows the tag as a label that opens the link `contextswitcher://switch/<id>` through the platform; `App` reports a link that the platform cannot open.
`Task.withContextSwitcherTags` replaces links of the forms `contextswitcher://task/<id>` and `contextswitcher://switch/<id>` by the tag; the quick add line and the edit dialog apply it.
The edit dialog has a field for the tag, which takes the id or such a link (`Task.contextSwitcherId`); emptying the field removes the tag.

Covers:

* `req~context-switcher-link~1`
* `adr~context-switcher-tag~1`

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

`iceRecurTasks` returns, for a range of days, the tasks of the entries that occur on a day and have no open task with the same text, each with its day; a task is returned once.
`TodoController` adds them once per day and device, for the days after the last run up to today; the day of the last run is kept in the settings.
The day of an added task is its creation date if dates are written.
With a synchronizing storage, the run waits for a running synchronization, and a task that the history shows as added since the last run counts as present even if it is completed.

Covers:

* `req~scheduled-tasks~1`

Needs: impl, utest

### Editor of ice_recur
`dsn~ice-recur-editor~1`

`ScheduleDialog`, opened from the top bar if the storage keeps files next to the todo.txt file, edits the text of `ice_recur.txt`.
`iceRecurLines` returns the lines that are neither blank nor comments; the dialog shows for each the day of `IceRecurEntry.next`, or that the line is not understood.
Such a line does not prevent saving, as the run skips it.
`TodoController.updateSchedule` writes the file and adds the tasks that the lines which were not in the file before have for today, as the run of the day does not come back to them.

Covers:

* `req~scheduled-tasks-editor~1`

Needs: impl, utest
