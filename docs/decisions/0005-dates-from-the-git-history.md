---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# Creation and Completion Dates Come from the Git History

`adr~dates-from-history~1`

Needs: dsn

## Context and Problem Statement

The todo.txt format has optional fields for the dates on which a task was created and completed.
They lengthen every line and are rarely looked at.
The file is synchronized through git, where every change has a date already.
Where are the two dates kept?

## Considered Options

* Do not write the dates; derive them from the git history
* Always write both dates
* Do not track the dates

## Decision Outcome

Chosen option: "Do not write the dates; derive them from the git history", because adding and completing a task each create a commit, whose date is the date of the event, and the lines stay short.

Writing the dates is a setting, off by default, for users who synchronize without git or who use tools that rely on the dates.

### Consequences

* Good, because the file contains only what the user wrote.
* Bad, because the dates are not available when the file is synchronized without git and the setting is off.
* Bad, because the derived date is the date of the commit; for a task that was added by another tool and committed later, it is the later date.
* Bad, because a task with a creation date must still get a completion date when it is completed, as the format cannot express the one without the other.
