---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# A Tag Names the Task of ContextSwitcher

`adr~context-switcher-tag~1`

Needs: dsn

## Context and Problem Statement

[ContextSwitcher](https://github.com/contextswitcher/contextswitcher) restores the working context of a task: terminal, project, browser tabs and notes.
Its tasks have an id, such as `jabref/fix-npe`, and links of the form `contextswitcher://switch/<id>` make it switch to a task.
A task of the todo.txt file often belongs to such a context.
How does a task of the todo.txt file refer to a task of ContextSwitcher?

## Considered Options

* The tag `cs:` with the id
* A word with a new prefix character, such as `§jabref/fix-npe`
* The complete link in the description

## Decision Outcome

Chosen option: "The tag `cs:` with the id", because tags of the form `key:value` are the extension mechanism of the todo.txt format and other tools leave them as they are.

The value is the id as the link has it, so that the link is the tag with another prefix.
The label of the tag switches to the task in ContextSwitcher.
A link copied from ContextSwitcher that the user pastes into a task becomes the tag.

### Consequences

* Good, because the line stays short and a search for `cs:jabref/fix-npe` finds the tasks of a context.
* Bad, because the tag breaks when the task of ContextSwitcher is renamed or moved to another category, as its id then changes; this holds for every option.
* Bad, because an id with a blank appears percent-encoded in the tag.
* Bad, because the label has an effect only where ContextSwitcher has registered its links, which its Android application does not.

## Pros and Cons of the Options

### A word with a new prefix character

* Good, because it is the shortest notation.
* Bad, because it is a fourth kind of label next to `+`, `@` and `#` that no other todo.txt tool knows.
* Bad, because `§` is not an ASCII character and is missing on many keyboards.

### The complete link in the description

* Good, because it is what ContextSwitcher copies to the clipboard.
* Good, because tools that turn links into buttons can open it.
* Bad, because the line gets long and the link is part of the description.
