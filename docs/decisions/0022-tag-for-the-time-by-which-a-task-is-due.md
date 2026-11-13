---
status: accepted
date: 2026-11-13
decision-makers: Oliver Kopp
---

# A Tag States the Time by Which a Task Is Due

`adr~due-time-tag~1`

Needs: dsn

## Context and Problem Statement

Some tasks are due at a time of the day: the slides have to be done before the call on Monday at 14:00.
The due date of the todo.txt format is a day.
How does a todo.txt line state the time?

## Considered Options

* A tag `by:14:00` next to the due date
* The time as part of the due date, `due:2026-11-16T14:00`
* The tag `at:`, which states the fixed time of a task
* A tag that names the event of a calendar before which the task is due

## Decision Outcome

Chosen option: "A tag `by:14:00` next to the due date", because the due date stays a date that other programs read, and the tag is short enough to be typed.

The tag counts together with a due date only.

### Consequences

* Good, because a program that does not know the tag still shows the task as due on that day.
* Good, because the tag reads as what it means next to the fixed time: `at:` is when the task takes place, `by:` is when it has to be done.
* Bad, because no other todo.txt program is known to read the tag.
* Bad, because the deadline is spread over two tags, and one can be changed without the other.

## Pros and Cons of the Options

### The time as part of the due date, `due:2026-11-16T14:00`

* Good, because date and time are one value, as in ISO 8601.
* Bad, because programs that read `due:` as a date do not read that value: the task loses its due date there.

### The tag `at:`, which states the fixed time of a task

* Good, because there is one tag for times.
* Bad, because the planner would then give the task the time at which it has to be done already.

### A tag that names the event of a calendar before which the task is due

* Good, because the deadline follows the event when it is moved.
* Bad, because the application only reads the times of calendars, not their events, and writes to none.
* Bad, because the id of an event cannot be typed.
