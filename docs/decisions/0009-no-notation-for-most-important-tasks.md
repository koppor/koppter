---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# No Separate Notation for Most Important Tasks

`adr~no-mit-notation~1`

Needs: dsn

## Context and Problem Statement

The add-ons `mit` and `mitf` of the todo.txt command line mark a task as a most important task of a day by a prefix: `{2026.11.13} Call Mom`.
`mit` lists these tasks by day; `mitf` creates them for a day given as a distance from today.
Does the application support this notation?

## Considered Options

* No notation: priority `A` together with a due date
* Read and write the `{YYYY.MM.DD}` prefix
* Read the prefix and convert it to priority and due date

## Decision Outcome

Chosen option: "No notation: priority `A` together with a due date", because both are part of what every todo.txt tool understands, the list order already shows these tasks first, and the *Top* view restricts the list to them.

A line with the prefix is left unchanged and shown with the prefix as part of its description.

### Consequences

* Good, because there is one way to say that a task is important and one way to say when it is due.
* Bad, because files maintained with `mit` are not shown grouped by day.

## Pros and Cons of the Options

### Read and write the `{YYYY.MM.DD}` prefix

* Good, because files maintained with `mit` keep working as before.
* Bad, because the prefix duplicates the due date in a form that no other tool evaluates.

### Read the prefix and convert it to priority and due date

* Good, because existing files benefit from the list order.
* Bad, because the application would rewrite lines that the user did not edit.
