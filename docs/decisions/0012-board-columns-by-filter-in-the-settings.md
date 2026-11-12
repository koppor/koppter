---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# Board Columns Defined by Filters Are Kept in the Settings

`adr~board-columns-by-filter~1`

Needs: dsn

## Context and Problem Statement

The columns of the board are the values of an attribute ([decision 0008](0008-board-columns-by-attribute.md)).
A board such as *now*, *waiting*, *someday* cannot be built that way; topydo offers it through columns that are each defined by a filter expression.
Such columns need a syntax for the expressions and a place where the definitions are kept.
Which syntax is used, and where are the definitions kept?

## Considered Options

* Expressions in the syntax of topydo, kept in the settings of the device
* Expressions in the syntax of topydo, kept in a file next to the todo.txt file
* The column configuration file of topydo

## Decision Outcome

Chosen option: "Expressions in the syntax of topydo, kept in the settings of the device", because the syntax is known to users of topydo and the settings exist for every kind of storage, including a document chosen in the document picker of Android, next to which the application cannot keep a file.

The columns by filter are an addition to the columns by attribute; the user switches between them on the board.
The supported terms are projects, contexts, priorities with comparison, tags with comparison, words and negation.
The sort and group expressions of topydo are not supported; a column has the order of the task list.

### Consequences

* Good, because any board can be built, and a board by attribute still needs no configuration.
* Bad, because the columns have to be defined on each device.
* Bad, because a task cannot be dragged between such columns: a filter does not say which change makes a task match it.

## Pros and Cons of the Options

### Expressions in the syntax of topydo, kept in a file next to the todo.txt file

* Good, because the definitions are synchronized together with the tasks.
* Bad, because such a file is not available for a document chosen in the document picker of Android.
* Bad, because it adds a file that no other todo.txt tool knows.

### The column configuration file of topydo

* Good, because a user of topydo has the same board in both programs.
* Bad, because the file lies in the configuration directory of topydo, which does not exist on Android.
* Bad, because the application would have to support the sort and group expressions as well, or ignore a part of the file.
