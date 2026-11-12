---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# The Plan Is Derived from the Tasks and Kept in calendar.txt

`adr~plan-in-calendar-txt~1`

Needs: dsn

## Context and Problem Statement

The application plans the open tasks: each task gets times in which it is to be worked on.
The plan changes whenever tasks, priorities or the time of day change, and the user wants to see in the history what a new plan changed.
Where is the plan kept?

## Considered Options

* A file `calendar.txt` next to the todo.txt file, in the format of calendar.txt, written when the user asks for a plan
* A tag with the planned time on each task of the todo.txt file
* Not kept: computed whenever it is shown

## Decision Outcome

Chosen option: "A file `calendar.txt` next to the todo.txt file", because the todo.txt file stays what the user wrote and each plan is one change of one file in the history.

The todo.txt file leads: the plan is computed from it, and nothing in `calendar.txt` changes a task.
The file has one line per day, as the [format of calendar.txt](https://terokarvinen.com/2021/calendar-txt/) has it.
The lines of past days stay, so that the file also tells what was planned.

### Consequences

* Good, because a text editor and the tools for calendar.txt read the plan.
* Good, because planning does not touch the lines of the tasks, so that it cannot conflict with an edit on another device.
* Bad, because a block refers to its task by the text of the task; after the text is edited, the task counts as not planned until the next plan.
* Bad, because the lines from today on belong to the application: what the user writes there by hand is replaced by the next plan.
* Bad, because two devices that plan at the same time change the same lines, which the merge of the synchronization resolves in favor of one of them.

## Pros and Cons of the Options

### A tag with the planned time on each task

* Good, because the task and its time stand in one line.
* Bad, because every plan changes many lines of the todo.txt file, which hides the changes the user made.
* Bad, because a task that is cut into parts needs several times.

### Not kept

* Good, because there is no second file that can be out of date.
* Bad, because the history does not tell what a plan changed.
* Bad, because the plan moves with every look at it, as the time of day is part of it.
