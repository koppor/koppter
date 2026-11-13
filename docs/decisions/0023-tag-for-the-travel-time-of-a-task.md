---
status: accepted
date: 2026-11-13
decision-makers: Oliver Kopp
---

# A Tag States the Travel Time of a Task

`adr~travel-time-tag~1`

Needs: dsn

## Context and Problem Statement

A task that takes place somewhere else takes more time than its duration: the way there and the way back.
The planner is to keep that time free as well, so that nothing is planned directly before an appointment in town.
How does a todo.txt line state it?

## Considered Options

* A tag `tt:30m` with one time that counts for the way there and for the way back
* Two tags, one for each way
* Places on tasks, with the times between them computed or configured
* The ways as tasks of their own that the task waits for

## Decision Outcome

Chosen option: "A tag `tt:30m` with one time that counts for the way there and for the way back", because one flat value is known when the task is written, is typed quickly, and covers the common case of going somewhere and coming back.

### Consequences

* Good, because the tag has the form of the tag `dur:` and is a plain tag of the todo.txt format, which other programs leave in place.
* Good, because the duration of the task stays the time of the task itself, for limits and for what a calendar shows.
* Bad, because two tasks at the same place after each other each get both ways.
* Bad, because a way that is longer in one direction cannot be stated.
* Bad, because no other todo.txt program is known to read the tag.

## Pros and Cons of the Options

### Two tags, one for each way

* Good, because a one-way trip and ways of different length can be stated.
* Bad, because the common case needs the same value twice.

### Places on tasks, with the times between them computed or configured

* Good, because tasks at the same place share their ways, and the order of tasks could save travel.
* Bad, because it needs places, the times between each pair of them and the place where the day starts, which is more than the question of how long a task keeps the user away.

### The ways as tasks of their own that the task waits for

* Good, because nothing is added to the format.
* Bad, because one appointment becomes three lines, and the ways show in the task list.
* Bad, because a dependency states the order, not that the way lies directly before the task.
