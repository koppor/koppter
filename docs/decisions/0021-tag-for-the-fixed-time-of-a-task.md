---
status: accepted
date: 2026-11-13
decision-makers: Oliver Kopp
---

# A Tag States the Fixed Time of a Task

`adr~fixed-time-tag~1`

Needs: dsn

## Context and Problem Statement

Some things take place at a time of the day: lunch from 12:00 to 12:45, a training on Tuesday evening.
The planner is to keep that time free of other tasks, and the todo.txt file is to be the one place where such a time is written, instead of a calendar program next to it.
The todo.txt format has dates, but no time of the day.
How does a todo.txt line state it?

## Considered Options

* A tag `at:12:00`, with the length in the tag `dur:`
* The time as part of the due date, `due:2026-11-17T12:00`
* The time in the description
* Fixed times in the configuration of planning, not in tasks

## Decision Outcome

Chosen option: "A tag `at:12:00`, with the length in the tag `dur:`", because it leaves the date tags as other programs read them, works for a task without date, and is carried by the entries of `ice_recur.txt` without a change of that format.

The day of the task is its due date, else its threshold date, else today.
The planner does not move such a task: it takes its time whatever the time maps and the calendars say.
An entry of `ice_recur.txt` whose task carries the tag also takes that time on its later days, before the task of the day is added.

### Consequences

* Good, because lunch on working days is one line of `ice_recur.txt`, and an appointment is one task.
* Good, because the tag is a plain tag of the todo.txt format, which other programs leave in place.
* Bad, because no other todo.txt program is known to read the tag: elsewhere the task is a task without time.
* Bad, because a task with a time and without a date counts for today as long as it is open, also when it was meant for a day that is over.

## Pros and Cons of the Options

### The time as part of the due date, `due:2026-11-17T12:00`

* Good, because date and time are one value, as in ISO 8601.
* Bad, because programs that read `due:` as a date do not read that value: the task loses its due date there.
* Bad, because a task that recurs by a schedule has no due date to carry the time.

### The time in the description

* Good, because nothing is added to the format.
* Bad, because a time in a description is not always the time of the task: "Ask whether 12:00 suits them".

### Fixed times in the configuration of planning, not in tasks

* Good, because the time maps already keep times free.
* Bad, because such a time shows in no list and no agenda, and cannot be completed.
* Bad, because an appointment on one day is then written in another place than the task that prepares it.
