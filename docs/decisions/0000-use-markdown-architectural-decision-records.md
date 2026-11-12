---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# Use Markdown Architectural Decision Records

`adr~madr~1`

Needs: impl

## Context and Problem Statement

The project is developed in short sessions, partly by an AI assistant, with long gaps in between.
Decisions that exist only in a conversation or a commit message are not found again when they are questioned later.
In which form are architectural decisions recorded?

## Considered Options

* MADR 4.0.0 – Markdown Architectural Decision Records
* Michael Nygard's template
* Commit messages only

## Decision Outcome

Chosen option: "MADR 4.0.0", because it records the considered options and the reason for the choice, is plain Markdown in the repository, and can be checked mechanically with [madrlint](https://github.com/adr/madrlint).

### Consequences

* Good, because a decision and its rejected alternatives are found in one place.
* Bad, because each significant decision costs one additional file.

### Confirmation

The continuous integration runs madrlint on every record in `docs/decisions/`.
