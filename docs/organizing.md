# Organizing many tasks

A todo.txt file with hundreds of tasks stays usable if every task answers a few questions in the same way.
This guide describes one way to do that with what koppter offers.
It is a suggestion, not a rule of the application: the file is yours.

## What each part of a task is for

| Part | Question it answers | Example |
| --- | --- | --- |
| Context `@` | To which area of work or life does the task belong? | `@research>reading` |
| Project `+` | Which outcome with an end does it serve? | `+FirstPaper` |
| Priority | Does it matter more than the rest right now? | `(A)` |
| `due:` | Is there a date that someone else set? | `due:2026-12-01` |
| `t:` | From when on is it worth looking at? | `t:2027-01-10` |
| `dur:` | How long does it take? | `dur:2h` |
| `at:` | At which time does it take place, if it has one? | `at:12:00` |
| Hashtag `#` | Which property does it share with tasks of other areas? | `#money` |

## Contexts are the areas that stay

A context names an area that does not end: `@work`, `@research`, `@home`, `@errands`.
Levels below it, written with `>`, divide a large area: `@work>teaching`, `@home>finances`.

* Five to nine contexts at the outermost level are enough; two levels are usually enough as well.
  A third level is a sign that a project is meant.
* Every task has one context for its area.
  The [inbox](../README.md#inbox) lists the tasks that have none.
* `@waiting` in addition marks a task that waits for someone else.
* The context decides when a task is planned (its time map), how much of the week it may take (its limits), and where it shows in the overview.
  That is why the area is the context and not the project.

## Projects are what ends

A project names an outcome that is reached one day: `+FirstPaper`, `+GarageSale`, `+TaxReturn2026`.
A task carries both: `Outline the related work +FirstPaper @research>writing`.

* A project gets a barrier in the plan dialog if it has a deadline: `+FirstPaper = 2027-03-31`.
* A project whose tasks are all completed stays in the filter panel as long as these tasks are in the file.
  The application does not move completed tasks out of the file; other todo.txt tools archive them to `done.txt`.
* A task that belongs to no outcome needs no project.

## Priorities are rare

Priority `(A)` is for what has to happen this week, `(B)` for what should, and most tasks have none.
If half of the tasks have a priority, it says nothing anymore: the plan and the list order by it.

## Two dates with two meanings

* `due:` is a deadline that the world sets: a submission, an appointment, a bill.
  A day on which you would like to do the task is not a due date; the plan finds that day.
* `t:` is the threshold date: before it, the task is hidden from the list and is not planned.
  It is the answer to "not now": a task for next semester gets `t:` at its start.
* A task for some day gets a threshold date far ahead, such as the start of the next quarter.
  On that day it shows up again and is decided anew: do it, move it again, or delete it.
  A task whose threshold date lies behind its barrier is left out of the plan without a complaint.

## Durations for what is more than half an hour

A task without `dur:` counts as half an hour.
A task of more than a day of work is a project: split it into tasks of at most a few hours, and let them wait for each other with `id:` and `p:` if their order matters.

## Finding a task

* Ctrl+E searches all tasks, the completed ones included.
* The contexts in the filter panel are a tree: a click on `@research` shows it with everything below it.
* A click on the context label of a task shows the tasks of that context.
* The [board](../README.md#board) with columns defined by filters keeps the searches that are needed every day, such as `Now = due:<=today -@waiting`.
* The [overview](../README.md#overview) shows one tile per area and tells which area has overdue tasks.

## A rhythm

* **Whenever something comes up:** type it into the line that adds a task, or share it to koppter on the phone.
  It does not need a context yet.
* **Once a day:** empty the inbox, which takes a few keys per task, and work from the agenda.
* **Once a week:** look at the overview for the areas with overdue tasks, then plan.
  The tasks that the plan lists as not fitting are the decisions of the week: give each a later threshold date, a later barrier, less duration, or delete it.

## Coming from SkedPal

| SkedPal | koppter |
| --- | --- |
| Level of the outline | Context, with `>` for the levels |
| Project with an end | Project `+`, with a barrier for its deadline |
| Time map | Time map, given to contexts |
| Zone with a budget | Limit of a context: `max 5h/week` |
| Minimum block length | Limit `block 2h` |
| Plan with a start date | Threshold date `t:` |
| Deadline | `due:` |
| Priority | Priority `(A)` to `(Z)` |
| Dependency | Tags `id:` and `p:` |
| On hold | `@waiting`, or a threshold date |
| Repeating task | `rec:`, or an entry of `ice_recur.txt` |
| Scheduling window | Barrier: the day behind which nothing is planned |
| Inbox | Inbox: the tasks without context |
| Today, next seven days | Agenda |
| Calendar | Week |
