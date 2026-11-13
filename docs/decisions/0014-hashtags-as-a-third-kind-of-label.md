---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# Hashtags Are a Third Kind of Label next to Projects and Contexts

`adr~hashtags~1`

Needs: dsn

## Context and Problem Statement

The todo.txt format marks projects with `+` and contexts with `@`.
It predates the hashtag, which other tools and most users know as the notation for a tag: `#urgent`.
todotxt-more reads words that start with `#` as a further kind of tag.
Does the application treat such words as metadata, and if so, as what?

## Considered Options

* Hashtags as a third kind of label, next to projects and contexts
* Hashtags as another notation for projects
* No hashtags: a word that starts with `#` belongs to the description

## Decision Outcome

Chosen option: "Hashtags as a third kind of label, next to projects and contexts", because it gives the user a label without a fixed meaning and leaves the meaning of `+` and `@` in the file as other todo.txt tools read it.

A hashtag is a word that starts with `#` followed by a letter.
A number sign followed by a digit, such as the issue number `#123`, and a number sign inside a word, such as `C#`, belong to the description.
Hashtags are shown, filtered, edited and used for the columns of the board like projects and contexts.

### Consequences

* Good, because a board with the columns `#todo`, `#doing` and `#done` needs no project or context for the state of a task.
* Bad, because a word such as `#hashtag` that the user meant as text is now metadata and moves behind the description when the task is edited ([decision 0004](0004-metadata-after-description-on-edit.md)).
* Bad, because other todo.txt tools show hashtags as part of the description.
* Bad, because a filter of a board column that starts with a hashtag has to be quoted in the configuration file, as YAML reads `#` as the start of a comment; the application writes it quoted.

## Pros and Cons of the Options

### Hashtags as another notation for projects

* Good, because there is no third kind of label to explain.
* Bad, because `#x` and `+x` would be the same project in this application and two different things in every other tool.

### No hashtags

* Good, because the application adds nothing to the format.
* Bad, because the notation that users know from other tools has no effect.
