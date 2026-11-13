---
status: accepted
date: 2026-11-13
decision-makers: Oliver Kopp
---

# Priorities Keep Their Letters in the File and Get Names for Display

`adr~priority-names~1`

Needs: dsn

## Context and Problem Statement

The todo.txt format has the priorities `(A)` to `(Z)`.
A user may think in another scale: numbers, a few steps of the Fibonacci sequence, or words.
How does the application offer such a scale?

## Considered Options

* Names for the letters, kept in the configuration file next to the todo.txt file
* The other scale written into the file, such as `(1)`
* A tag for the priority, such as `prio:5`
* Letters only

## Decision Outcome

Chosen option: "Names for the letters, kept in the configuration file next to the todo.txt file", because the file stays one that every todo.txt program reads and orders by priority, and the user sees and chooses the scale that suits them.

The key `priorities` of `koppter.yml` maps a letter to its name.
The order of the priorities stays that of the letters: the name of `A` is the highest priority.

### Consequences

* Good, because the names also limit the choice: the edit dialog offers the priorities that have a name.
* Good, because the names are synchronized with the tasks and are the same on every device.
* Bad, because another program, a filter of a board column and the line of the edit dialog show the letters.
* Bad, because a file chosen through the document picker of Android has no configuration file and therefore no names.

## Pros and Cons of the Options

### The other scale written into the file, such as `(1)`

* Good, because the file shows what the user sees.
* Bad, because the format knows no such priority: other programs read `(1)` as part of the description and do not order by it.

### A tag for the priority, such as `prio:5`

* Good, because other programs keep the tag.
* Bad, because other programs do not order by it, and a task could carry two priorities.

### Letters only

* Good, because nothing is added.
* Bad, because the user has to translate the scale they think in.
