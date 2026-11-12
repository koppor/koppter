# Planning

Requirements and design of planning: when the open tasks are worked on.
The decision on where the plan is kept is [decision 0019](../decisions/0019-the-plan-is-derived-and-kept-in-calendar-txt.md).

## Requirements

### Plan of the open tasks
`req~plan~1`

On request, the application gives each open task times in which it is to be worked on.
A task with a higher priority gets earlier times, and every task gets the earliest times that are free.
A task takes its duration and is not planned before its threshold date.
The todo.txt file is not changed by planning.

Covers:

* `feat~planning~1`

Needs: dsn

### Time maps
`req~time-maps~1`

The user defines time maps: named times of the week.
A time map may be restricted to another one, so that general working hours are defined once.
The user gives contexts a time map; the tasks of a context and of the contexts below it are planned in its times.

Covers:

* `feat~planning~1`

Needs: dsn

### Barriers
`req~barriers~1`

A task is not planned behind its barrier.
The barrier is the end of the quarter unless the user chooses the end of the week, month or year or a date, in general or for a project or a context.
Of several barriers of a task, the earliest counts.
A task that does not fit before its barrier is reported instead of being planned later.

Covers:

* `feat~planning~1`

Needs: dsn

### Plan as a file
`req~plan-file~1`

The plan is a text file next to the todo.txt file, so that the history of a git repository tells what a new plan changed.
The plan is also written as an iCalendar file that calendar programs import; its events are tentative.

Covers:

* `feat~planning~1`

Needs: dsn

### Busy times of calendars
`req~busy-times~1`

Nothing is planned into the times of the events of the calendars that the user names.
A calendar is named by the address at which its service publishes it as an iCalendar file.
The application only reads the calendars.

Covers:

* `feat~planning~1`

Needs: dsn

### State of the plan on the task
`req~plan-state~1`

Once there is a plan, each open task tells by the color of a calendar icon how its plan relates to its due date: green if it ends before, yellow if it ends on the due date, red if it ends later or the task is not planned.

Covers:

* `feat~planning~1`

Needs: dsn

## Design

### Time maps
`dsn~time-maps~1`

`TimeMaps` reads the definitions: a definition is a list of rules separated by commas, and a rule is days (`Mon`, `Mon-Fri`) followed by times (`08:00-12:00`).
A rule without days holds on every day, a rule without times for the whole day.
A definition that starts with the name of another map gives the times that both maps have.
The first map is the one for tasks without a map; without any map, the times are Monday to Friday from 09:00 to 17:00.
What is neither days nor times is reported and otherwise ignored.

Covers:

* `req~time-maps~1`

Needs: impl, utest

### Configuration of planning
`dsn~plan-configuration~1`

`koppter.yml` holds the time maps under the key `time-maps`, the time maps of contexts under `context-time-maps` and the barriers under `barriers`, each as a map of names to values.
`PlanConfig.mapOf` gives the time map of the first context of a task that has one, where a context has the map of the nearest context above it unless it has its own.
`PlanConfig.barrierOf` gives the earliest barrier among those of the projects and contexts of a task, else the one of the key `default`, else the end of the quarter.
A barrier is `week`, `month`, `quarter`, `year` or a date; the first four name the end of the current one.

Covers:

* `req~time-maps~1`
* `req~barriers~1`

Needs: impl, utest

### Planner
`dsn~planner~1`

`plan` goes through the open tasks in the order of the task list and gives each the earliest free times of its time map, from now or from its threshold date on.
A task takes its duration, half an hour without one.
A free time that is too short for the task takes a part of it, of at least half an hour or what is left of the task.
Times that are passed in as busy and the blocks of the tasks planned before are not free.
A task whose duration does not fit up to its barrier gets no block and is returned as not planned.

Covers:

* `req~plan~1`
* `req~barriers~1`

Needs: impl, utest

### calendar.txt
`dsn~calendar-txt~1`

`CalendarTxt.write` writes the plan in the format of calendar.txt: one line per day from today to the last planned day, with the date, the ISO week, the weekday and, after two blanks, the blocks of the day as `HH:MM-HH:MM text`, separated by a full stop and a blank.
The text of a block is the description of the task with its projects and contexts.
Lines of days before today and lines that are no day stay as they are.
`CalendarTxt.planned` reads the blocks from today on; `CalendarTxt.state` compares the day of the last block of a task with its due date.

Covers:

* `req~plan-file~1`
* `adr~plan-in-calendar-txt~1`

Needs: impl, utest

### iCalendar export
`dsn~ics-export~1`

`Ics.write` writes the blocks of the plan as events of an iCalendar file, `calendar.ics` next to the todo.txt file.
An event is tentative (`STATUS` and the busy status that Outlook reads), its times are local times without a time zone, and its id is derived from the day, the start and the text of the block, so that the same plan gives the same file.

Covers:

* `req~plan-file~1`

Needs: impl, utest

### Busy times
`dsn~busy-times~1`

`busyTimes` reads the events of an iCalendar file and gives the times they take by day, in the time zone of the device.
Events of whole days, cancelled events, events marked as free and the blocks that the application exported itself are left out.
Repeating events are followed for the frequencies daily, weekly, monthly and yearly with interval, end, count, days of the week, days of the month and excluded dates.
A time zone that the platform does not know by name counts as that of the device.
The addresses of the calendars are a setting of the device, as they are secrets; the plan dialog edits them.
`fetchCalendar` reads an address over HTTP or HTTPS; `TodoController.planWithCalendars` reads all calendars for a year from today, reports those it could not read by their host and plans with the others.

Covers:

* `req~busy-times~1`

Needs: impl, utest

### Plan dialog
`dsn~plan-dialog~1`

`PlanDialog` edits the time maps, the time maps of contexts and the barriers as lines of the form `name = value`.
*Save and plan* makes `TodoController.plan` save them, plan from the current time rounded up to a quarter of an hour, and write `calendar.txt` and `calendar.ics` as one change of the storage.
The dialog then tells how many tasks were planned and until when, what of the configuration was not understood, and the tasks that do not fit before their barrier.
A switch above the tasks opens the dialog if the storage keeps files next to the todo.txt file.

Covers:

* `req~plan~1`
* `req~barriers~1`

Needs: impl, utest

### State of the plan in the labels
`dsn~plan-state~1`

`TodoController.planOf` looks a task up in the blocks of `calendar.txt` by the text of its block.
`TaskLabels` shows a calendar icon in front of the labels of an open task once that file exists: green, yellow or red by the state, with the start of the plan or "Not planned" as a tooltip.
Green is the green of the palette, which `Theme.kt` provides beside the Material color scheme.

Covers:

* `req~plan-state~1`

Needs: impl, utest
