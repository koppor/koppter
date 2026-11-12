---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# The Levels of a Context Are Separated by a Greater-Than Sign

`adr~context-hierarchy~1`

Needs: dsn

## Context and Problem Statement

Contexts group the tasks, and with many tasks a flat list of contexts gets long.
Areas of work have parts: reading is a part of research.
The todo.txt format knows a context as one word after `@` and has no notation for a context below another one.
How does a task name a context that lies below another context?

## Considered Options

* One context whose levels are separated by `>`, such as `@research>reading`
* One context whose levels are separated by `/`, such as `@research/reading`
* Several contexts on the task, such as `@research @reading`, and a file that declares the hierarchy

## Decision Outcome

Chosen option: "One context whose levels are separated by `>`", because the line itself tells the place of the task and the sign reads as the path it is.

The label of a task shows the last level; the path appears when the pointer rests on the label.
A filter for a context includes the contexts below it.

### Consequences

* Good, because other todo.txt tools read the path as one context and keep it.
* Good, because the hierarchy needs no file of its own and no upper context has to be declared before it is used.
* Bad, because other tools show the whole path and do not know that `@research` includes `@research>reading`.
* Bad, because `>` redirects the output in a shell, so that a task added on the command line has to be quoted.
* Bad, because renaming an upper context means changing every task below it.

## Pros and Cons of the Options

### One context whose levels are separated by `/`

* Good, because it is the notation of paths in file systems and needs no quoting in a shell.
* Bad, because ids of ContextSwitcher and links contain `/`, so that the sign has two meanings in one line.
* Bad, because it reads as "or" in prose, as in `@home/office`.

### Several contexts and a file that declares the hierarchy

* Good, because the task line stays short and every tool filters by each level.
* Bad, because the line alone does not tell which context lies below which.
* Bad, because a level with the same name below two contexts cannot be told apart.
