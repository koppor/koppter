# Features

The features describe the planned scope.
A feature without a `Needs:` line is not yet refined into requirements.

## todo.txt file as the data store
`feat~todo-txt-file~1`

The application reads and writes a file in the [todo.txt format](https://github.com/todotxt/todo.txt).
The file remains usable with other todo.txt tools at any time.

Needs: req

## Task list
`feat~task-list~1`

The application shows the tasks as a list.
Projects, contexts, due date and priority of a task are shown as labels at the top right of the task.

Needs: req

## Task editing
`feat~task-editing~1`

The user adds, edits, completes and deletes tasks.
The edit dialog separates the description from the metadata and shows the resulting todo.txt line.

Needs: req

## Filtering
`feat~filtering~1`

The user restricts the list by completion state, text, priorities, projects, contexts and hashtags.

Needs: req

## Synchronization through git
`feat~sync-git~1`

The todo.txt file is synchronized through a git repository.
Adding and completing a task each create a commit, so that the dates of both events are available without being written into the file.

Needs: req

## Synchronization through a local folder
`feat~sync-local-folder~1`

The todo.txt file lives in a folder chosen by the user, which may be synchronized by another program.

Needs: req

## Synchronization through OneDrive
`feat~sync-onedrive~1`

The todo.txt file lives in the user's OneDrive.

Needs: req

## todo.txt extensions
`feat~extensions~1`

The application supports recurring tasks (`rec:`, `ice_recur`), threshold dates (`t:`), notes attached to tasks (`note:`), hashtags (`#tag`) and a view of the top tasks.

Needs: req

## Kanban board
`feat~kanban~1`

The application shows the tasks as a board with columns.

Needs: req

## Planning
`feat~planning~1`

The application plans when the open tasks are worked on.

Needs: req

## Platforms
`feat~platforms~1`

The application runs on Android and on Windows and Linux desktops.

Needs: req
