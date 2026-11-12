---
status: accepted
date: 2026-11-13
decision-makers: Oliver Kopp
---

# Dependencies Between Tasks Use the Tags of topydo

`adr~dependency-tags~1`

Needs: dsn

## Context and Problem Statement

Some tasks can only be worked on after others are done, and the planner is to give them their times in that order.
The todo.txt format has no notation for a task that waits for another one.
How does a todo.txt line state it?

## Considered Options

* The tags `id:` and `p:` as topydo writes them
* A tag `after:` that names the description of the other task
* Tasks that are indented below the task that waits for them

## Decision Outcome

Chosen option: "The tags `id:` and `p:` as topydo writes them", because a program that many todo.txt files already went through defines them, and its commands for dependencies keep working on a file that koppter plans.

A task with `id:1` waits for the tasks with `p:1`.
A task carries one `id:` and may carry several `p:`, so that it waits for some tasks and others wait for it.

### Consequences

* Good, because topydo lists, adds and removes the dependencies of the same file.
* Good, because the tags are plain tags of the todo.txt format, which other programs leave in place.
* Bad, because the reading direction is not obvious: the task that waits carries the `id:`, and the tasks to do first point to it with `p:`.
* Bad, because the user has to choose values that are not in use; koppter does not assign them.

## Pros and Cons of the Options

### A tag `after:` that names the description of the other task

* Good, because the line tells in words what the task waits for.
* Bad, because a description contains blanks, which a tag value cannot hold.
* Bad, because the dependency breaks when the other task is renamed.

### Tasks that are indented below the task that waits for them

* Good, because the file shows the structure.
* Bad, because the order of the lines then carries meaning, and programs that sort the file destroy it.
* Bad, because a task can then wait for the tasks of one group only.
