---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# A Tag Holds the Duration of a Task

`adr~duration-tag~1`

Needs: dsn

## Context and Problem Statement

Planning the day needs to know how long a task is expected to take.
The todo.txt format has no notation for it, and the tools that extend the format have not agreed on one.
How does a task state its duration?

## Considered Options

* The tag `dur:` with hours and minutes, such as `dur:1h30m`
* The tag `est:` with hours and minutes
* The tag `pm:` with a number of pomodoros, as Sleek has it
* The tag `min:` with a number of minutes

## Decision Outcome

Chosen option: "The tag `dur:` with hours and minutes", because the value reads as people write a duration and the key says what the value is.

### Consequences

* Good, because other todo.txt tools keep the tag as they keep every `key:value` tag.
* Bad, because no other tool is known to read this tag.

## Pros and Cons of the Options

### The tag `est:`

* Good, because it says that the value is an estimate.
* Bad, because it does not say what is estimated.

### The tag `pm:`

* Good, because an existing tool reads it.
* Bad, because the length of a pomodoro is a setting, so that the line alone does not tell the duration.
* Bad, because a task of ten minutes has no value.

### The tag `min:`

* Good, because the value is a plain number.
* Bad, because `min:240` is harder to read than `dur:4h`.
* Bad, because `min` also reads as minimum.
