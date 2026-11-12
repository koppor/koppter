---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# Board Columns Are the Values of an Attribute

`adr~board-columns-by-attribute~1`

Needs: dsn

## Context and Problem Statement

A Kanban board needs columns, but the todo.txt format has no notion of a workflow state.
topydo, the model for the board, lets the user define each column by a filter expression.
What defines the columns of the board?

## Considered Options

* The values of an attribute the user selects: priority, project, context or due date range
* Columns defined by filter expressions, as in topydo
* A workflow state in a tag, such as `status:doing`

## Decision Outcome

Chosen option: "The values of an attribute the user selects", because it needs no configuration and no convention that other todo.txt tools do not know, and it covers the common boards: by priority, by project, by context and by urgency.

Columns defined by filter expressions remain possible as an addition; they need a filter language and a place to store the definitions.

### Consequences

* Good, because the board works with any existing todo.txt file.
* Bad, because a board with the columns *to do*, *doing* and *done* needs a convention, for example contexts with these names.
* Bad, because the columns cannot be ordered by the user.

## Pros and Cons of the Options

### Columns defined by filter expressions, as in topydo

* Good, because any board can be built.
* Bad, because the user has to learn a filter language before the first board appears.

### A workflow state in a tag

* Good, because it matches the usual notion of a Kanban board.
* Bad, because the tag means nothing to other todo.txt tools.
