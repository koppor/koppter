---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# Board Columns Defined by Filters Are Kept in a Configuration File next to the todo.txt File

`adr~board-columns-by-filter~1`

Needs: dsn

## Context and Problem Statement

The columns of the board are the values of an attribute ([decision 0008](0008-board-columns-by-attribute.md)).
A board such as *now*, *waiting*, *someday* cannot be built that way; topydo offers it through columns that are each defined by a filter expression.
Such columns need a syntax for the expressions and a place where the definitions are kept.
Which syntax is used, and where are the definitions kept?

## Considered Options

* Expressions in the syntax of topydo, kept in a configuration file next to the todo.txt file
* Expressions in the syntax of topydo, kept in the settings of the device
* The column configuration file of topydo

## Decision Outcome

Chosen option: "Expressions in the syntax of topydo, kept in a configuration file next to the todo.txt file", because the syntax is known to users of topydo and the board is then the same on every device that synchronizes the tasks.

The file is `koppter.yml`.
The directory of the todo.txt file collects the files that belong to the tasks, as `ice_recur.txt` and `notes` show, so the name needs no reference to todo.txt.
It names the application instead, as the content is in a format of this application and the directory may hold files of other tools.
It is a YAML file, so that it can take further configuration that belongs to the todo.txt file rather than to a device.
The key `board-columns` maps the title of each column to its expression, one column per line.
The columns are edited in the settings dialog, and the file is committed like the notes.
The columns by filter are an addition to the columns by attribute; the user switches between them on the board.
The supported terms are projects, contexts, priorities with comparison, tags with comparison, words and negation.
The sort and group expressions of topydo are not supported; a column has the order of the task list.

### Consequences

* Good, because any board can be built, and a board by attribute still needs no configuration.
* Good, because the definitions are synchronized together with the tasks.
* Bad, because columns by filter are not available for a document chosen in the document picker of Android, next to which the application cannot keep a file.
* Bad, because the file is merged as a whole: definitions changed on two devices before a synchronization conflict.
* Bad, because it adds a file that no other todo.txt tool knows.
* Bad, because the application reads only the subset of YAML that it writes: one line per column, with plain or quoted titles and expressions.

## Pros and Cons of the Options

### Expressions in the syntax of topydo, kept in the settings of the device

* Good, because the settings exist for every kind of storage.
* Bad, because the columns have to be defined on each device.

### The column configuration file of topydo

* Good, because a user of topydo has the same board in both programs.
* Bad, because the file lies in the configuration directory of topydo, which does not exist on Android and is not synchronized.
* Bad, because the application would have to support the sort and group expressions as well, or ignore a part of the file.
