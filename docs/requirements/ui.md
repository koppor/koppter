# User interface

Requirements and design for the screens shared by all platforms.

## Requirements

### Labels at the top right of a task
`req~labels-top-right~1`

Each task in the list shows its description and, in a row at the top right, its projects, contexts, hashtags, tags such as the due date, and its priority as labels.
The description contains no metadata.

Covers:

* `feat~task-list~1`

Needs: dsn

### Add a task by typing a line
`req~quick-add~1`

The user adds a task by typing a line in todo.txt syntax into a field above the list.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Complete, reopen and delete a task
`req~complete-and-delete~1`

The user completes and reopens a task in the list and deletes a task in its edit dialog.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Edit dialog
`req~edit-dialog~1`

The edit dialog shows the description without metadata in its first field.
Below it, the dialog has fields for priority, due date, threshold date, recurrence, projects, contexts and hashtags.
Below these fields, the dialog shows the line as it will be written to the todo.txt file, in a monospaced font.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Filter panel
`req~filter-panel~1`

A panel offers the filter criteria.
Priorities, projects, contexts and hashtags are offered as the values that occur in the file.
The panel stands beside the list where the window is wide enough and slides over the list otherwise.

Covers:

* `feat~filtering~1`

Needs: dsn

### Changes by other programs
`req~external-changes~1`

A change that another program makes to the todo.txt file appears in the application without a restart.

Covers:

* `feat~sync-local-folder~1`

Needs: dsn

### Choose the file
`req~choose-file~1`

The user sets the location of the todo.txt file.

Covers:

* `feat~sync-local-folder~1`

Needs: dsn

### Settings survive a restart
`req~persistent-settings~1`

The settings are kept between runs of the application.

Covers:

* `feat~sync-local-folder~1`
* `feat~task-editing~1`

Needs: dsn

## Design

### Controller
`dsn~task-operations~1`

`TodoController` holds the file, the filter and the settings as Compose state.
Adding, completing, reopening, replacing and deleting a task write the whole file to the storage at once; there is no unsaved state.
Before an operation, the controller looks the task up in the current file, because the file may have been reloaded since the task was shown.
Whether dates are written is taken from the settings.

Covers:

* `req~complete-and-delete~1`
* `req~no-dates-by-default~1`

Needs: impl, utest

### Visible tasks
`dsn~visible-tasks~1`

`TodoController.visible` is the list of entries that match the filter, in list order.

Covers:

* `req~filter-criteria~1`
* `req~list-order~1`

Needs: impl, utest

### Task card
`dsn~task-card~1`

`TaskCard` shows a checkbox for the completion state and, to its right, a column with the labels in a right-aligned flow row and the description below it.
A click on the card opens the edit dialog.

Covers:

* `req~labels-top-right~1`
* `req~complete-and-delete~1`

Needs: impl, utest

### Task labels
`dsn~task-labels~1`

`TaskLabels` emits one label per project, context, hashtag and tag and one for the priority, in this order.
A tag label shows its key in front of the value.
The colors are taken from the Material color scheme; a due date in the past uses the error colors.

Covers:

* `req~labels-top-right~1`

Needs: impl

### Quick add
`dsn~quick-add~1`

A text field above the list passes its content to `TodoController.add` on Enter and clears itself.

Covers:

* `req~quick-add~1`

Needs: impl, utest

### Edit dialog
`dsn~edit-dialog~1`

`EditDialog` keeps the edited values as dialog state and computes the resulting task with `Task.edit` and `Task.withTag` on every change.
The resulting line is shown below the fields in a monospaced font.
*Save* is disabled while a date or the recurrence is invalid or the task is empty.
The dialog also serves for a new task.

Covers:

* `req~edit-dialog~1`
* `req~complete-and-delete~1`
* `adr~metadata-after-description~1`

Needs: impl, utest

### Filter panel
`dsn~filter-panel~1`

`FilterPanel` shows the criteria of `Filter`: the completion state as radio buttons, the text as a text field, and the threshold date as a checkbox.
Priorities, projects, contexts and hashtags are shown as the labels that the task list uses for them.
The selected values stand beside the title of the criterion and the other values below it, both sorted; a click on a label moves it to the other group.
From a width of 840 dp on, the main screen places the panel beside the list; below that, the panel is the content of a navigation drawer.

Covers:

* `req~filter-panel~1`

Needs: impl, utest

### Main screen
`dsn~main-screen~1`

The main screen consists of the top bar, the filter panel, the quick add field, the task list and a button for a new task.
Errors of the storage are shown in a snackbar.

Covers:

* `req~filter-panel~1`
* `req~quick-add~1`

Needs: impl

### Reload on external change
`dsn~reload-on-external-change~1`

The main screen asks the storage for its version every two seconds and reloads the file when the version differs from the one last read or written.

Covers:

* `req~external-changes~1`

Needs: impl, utest

### File storage
`dsn~file-storage~1`

`FileStorage` reads and writes a file in the file system.
It writes to a temporary file in the same directory and moves it over the target.
The version is the modification time together with the size.

Covers:

* `req~choose-file~1`
* `req~external-changes~1`

Needs: impl, utest

### Settings file
`dsn~settings-file~1`

The settings are stored as a Java properties file.
Unknown keys are ignored and missing keys take their default.

Covers:

* `req~persistent-settings~1`

Needs: impl, utest

### Settings dialog
`dsn~settings-dialog~1`

`SettingsDialog` edits the location of the todo.txt file, the switch for writing dates, the number of top tasks and the board columns defined by filters.
A changed location makes the controller load the other file.

Covers:

* `req~choose-file~1`
* `req~no-dates-by-default~1`

Needs: impl, utest

### Locations on the desktop
`dsn~desktop-locations~1`

On the desktop, the settings file is `settings.properties` in `%APPDATA%\koppter` on Windows and in `$XDG_CONFIG_HOME/koppter` or `~/.config/koppter` elsewhere.
Without a configured location, the todo.txt file is `todo.txt` in the home directory.

Covers:

* `req~persistent-settings~1`
* `req~choose-file~1`

Needs: impl
