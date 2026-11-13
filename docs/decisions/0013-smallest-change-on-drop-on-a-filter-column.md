---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# A Task Dropped on a Column Defined by a Filter Gets the Smallest Change That Makes It Match

`adr~smallest-change-on-drop~1`

Needs: dsn

## Context and Problem Statement

A column that is the value of an attribute says what a task dropped on it becomes: it gets that value.
A column defined by a filter ([decision 0012](0012-board-columns-by-filter-in-a-file.md)) only says which tasks it shows.
What happens to a task that the user drags from one such column to another?

## Considered Options

* The smallest change that makes the task match the filter of the target column
* That change, and the removal of the projects, contexts and tags that only the filter of the source column requires
* No dragging between columns defined by filters

## Decision Outcome

Chosen option: "The smallest change that makes the task match the filter of the target column", because every change to the task then follows from the column the user chose, and nothing is removed that the user did not ask to remove.

Each term of the filter that the task does not match changes the task: a project, a context or a tag is added or, for a negated term, removed; a priority or a date is set to the nearest value that matches.
A filter with a term that no such change establishes, such as a word of the description, makes its column refuse the task.

Removing what the source column requires is to be reconsidered once the board has been used.

### Consequences

* Good, because a board whose filters exclude each other, such as `@waiting` and `-@waiting`, moves tasks as a Kanban board does.
* Bad, because a task stays in the source column as well if the filters do not exclude each other: after a move from `@todo` to `@doing` it has both contexts.
* Bad, because a column whose filter contains a word or a negated comparison takes no tasks.

## Pros and Cons of the Options

### That change, and the removal of what only the filter of the source column requires

* Good, because the task always leaves the source column where a project, a context or a tag put it there.
* Bad, because the move removes metadata that the target column says nothing about, which is wrong for boards whose columns are views that overlap on purpose.

### No dragging between columns defined by filters

* Good, because nothing has to be inferred from a filter.
* Bad, because the board by filter is then for reading only, while the board by attribute is not.
