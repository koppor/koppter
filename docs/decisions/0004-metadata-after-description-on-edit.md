---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# Metadata Is Written after the Description When a Task Is Edited

`adr~metadata-after-description~1`

Needs: dsn

## Context and Problem Statement

In todo.txt, projects, contexts and tags may stand anywhere in a line, also in the middle of a sentence: `Call +Family Mom @phone`.
The edit dialog shows the description without metadata in its first field and the metadata in separate fields.
Where does the metadata go when the edited task is written back?

## Considered Options

* Write the metadata after the description in a fixed order
* Keep each project, context and tag at its original position
* Edit the raw line only

## Decision Outcome

Chosen option: "Write the metadata after the description in a fixed order", because the dialog has no notion of a position inside the description, the result is predictable, and the line the dialog shows below the fields is exactly what is written.

The order is description, projects, contexts, tags.
A task is rewritten only when the user changes a value; saving the dialog without a change leaves the line as it is.

### Consequences

* Good, because edited tasks have a uniform shape.
* Bad, because a project or context used as a word of a sentence (`Call +Mom`) moves to the end of the line on the first edit.
  The description then reads `Call`.

## Pros and Cons of the Options

### Keep each project, context and tag at its original position

* Good, because the wording of the user is preserved.
* Bad, because the description field would have to show the metadata after all, which is what the dialog is meant to avoid.

### Edit the raw line only

* Good, because it is the simplest implementation.
* Bad, because it offers no help with dates, priorities and existing project names.
