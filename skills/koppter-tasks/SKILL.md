---
name: koppter-tasks
description: Add or change tasks in a todo.txt file that is used with koppter - todo.txt with contexts in levels, durations, dependencies, notes, recurrence and ice_recur.txt. Use this skill whenever the user wants something remembered, noted or scheduled as a task ("add a todo", "remind me to", "put this on my list", "neue Aufgabe", "Aufgabe eintragen"), and whenever a task is to recur ("every Monday", "jeden Monat", "again two weeks after I did it"), even if koppter or todo.txt is not named but the user keeps their tasks in a todo.txt file.
---

# Tasks for koppter

koppter shows, filters and plans the tasks of a todo.txt file.
The file is the only data store, and the user also reads it in a text editor and in the history of a git repository.
A task that is written well lands in the right area, shows up on the right day and gets a sensible time in the plan; a task that is written carelessly ends up as one of hundreds that nobody finds again.

## Find the file

1. A file the user names, or a `todo.txt` in the working directory, is the one.
2. Otherwise read `todoFile` from the settings of koppter: `~/.config/koppter/settings.properties` (or `$XDG_CONFIG_HOME/koppter`, on Windows `%APPDATA%\koppter`).
3. If that key is empty, the file is `~/todo.txt`.

The files next to it belong to it: `ice_recur.txt` (schedules), `notes/` (notes), `koppter.yml` (configuration), `calendar.txt` (the plan, written by koppter: do not edit it).

## Read before writing

Look at what the file already uses before adding a line:

```bash
grep -o '@[^ ]*' todo.txt | sort | uniq -c | sort -rn   # contexts
grep -o '+[^ ]*' todo.txt | sort | uniq -c | sort -rn   # projects
```

Reuse an existing context or project instead of inventing a spelling.
The context decides when the task is planned and where it shows in the overview, so `@Errands` next to an existing `@errands` creates a second area without a time map.

## The line

```text
(A) Description +Project @context>level #hashtag key:value
```

One task is one line, in this order: priority, description, projects, contexts, hashtags, tags.
koppter writes tasks in that order, so a line written the same way does not change when the user edits the task.

| Part | Use it for | Notes |
| --- | --- | --- |
| Description | What is to be done, starting with a verb | Keep URLs as they are. |
| `@context` | The area of work or life, which stays | One per task. Levels with `>`: `@research>reading`. `@waiting` in addition marks a task that waits for someone. |
| `+Project` | An outcome that ends | Only if the task serves one. Not for areas. |
| `(A)` | What has to happen this week | `(B)` for what should. Most tasks have none; ask rather than guess. The file holds the letter also when the user names priorities otherwise: the key `priorities` of `koppter.yml` next to the file maps letters to their names, such as `"D": "5"`. |
| `due:2026-12-01` | A deadline that someone else set | Not the day the user would like to do it: the plan finds that day. |
| `by:14:00` | The time of the due date by which the task has to be done | Only next to `due:`: "the slides have to be ready for the call on Monday at 14:00". |
| `t:2027-01-10` | The day from which the task is worth looking at | The task is hidden and not planned before. Use it for "not now" and "some day". |
| `dur:1h30m` | The time the task takes | Without it, the plan assumes half an hour. Give it when the user states it or the task clearly takes longer. |
| `tt:30m` | The time that the way to the task takes; the way back counts the same | Only when the user states it for a task that takes place somewhere else. The plan keeps it free before and after the task. |
| `at:14:00` | The time of the day at which the task takes place | Only for what has its time: an appointment, lunch, a training. The plan does not move the task. The day is `due:`, else `t:`, else today. |
| `#hashtag` | A property shared across areas | `#money`, `#quick`. Starts with a letter. |
| `id:1`, `p:1` | Order between tasks | The task with `id:1` waits for the tasks with `p:1`. Pick a value that the file does not use yet: `grep -o ' id:[^ ]*' todo.txt`. |
| `note:k3f.md` | Details of several lines | Write them to `notes/k3f.md` (three characters, a name not in use) and name the file in the tag. |

Dates are absolute, `YYYY-MM-DD`: work them out from today's date, and tell the user which date "next Friday" became.

If no existing context fits and the user did not name one, leave the context out and say so.
koppter lists tasks without context in its inbox, where the user gives them one with a few keys; a guessed context hides the task in the wrong area.

Do not put a creation date in front of the description unless the other lines of the file have one: koppter reads that date from the git history.

Give `at:` only when the time is part of the task ("dentist on Friday at 14:00"), together with `due:` for the day and `dur:` for the length.
A time by which something has to be done is no fixed time but a deadline, `due:` with `by:`: koppter plans such a task into free times itself, whereas a task with `at:` is not moved when the day gets full.

## Recurring tasks

There are two mechanisms, and choosing the wrong one gives a task that either piles up or never comes back:

* The next one is due only after the current one is done (water the plants, descale the machine): the tag `rec:` on the task.
* It has to appear on certain days whatever happened before (every Monday, the last day of the month): an entry in `ice_recur.txt`.

Read [references/recurrence.md](references/recurrence.md) before writing either; the syntax of `ice_recur.txt` is strict and has pitfalls that are listed there.

## Write

* Append the new line at the end of the file, and end it with a newline.
* Leave every other line as it is: do not sort, reformat or deduplicate.
  koppter writes back unchanged lines byte for byte, other devices merge the file line by line, and the history shows one change per task.
* To change a task, edit its line only.
* Completing a task is best left to koppter, which also adds the next occurrence of a recurring task.
  If the user asks for it anyway: put `x ` in front, and move a priority to the tag `pri:A` at the end, as the format has no priority for completed tasks.
* In a git repository, do not commit unless the user asks: koppter commits changes made by other programs at its next synchronization.

## Report

Show the lines written, and name what was decided for the user: the context that was chosen or left out, the dates that relative words became, and whether a recurring task went to `rec:` or to `ice_recur.txt`.

## Examples

"Remind me to send the reviewer the revised figures, takes two hours, the journal wants them by 4 December" with an existing `@research>writing` and `+FirstPaper`:

```text
Send the reviewer the revised figures +FirstPaper @research>writing due:2026-12-04 dur:2h
```

"Some day I should look into a new bike", no fitting context, said on 2026-11-13:

```text
Look into a new bike #someday t:2027-01-01
```

"Dentist next Friday at two, takes an hour", said on 2026-11-13:

```text
Go to the dentist @errands>city due:2026-11-20 at:14:00 dur:1h
```

"The slides have to be ready for the call on Monday at 14:00, two hours of work", said on 2026-11-13:

```text
Prepare the slides for the call @work due:2026-11-16 by:14:00 dur:2h
```

"Book the flight once the talk is accepted", where the file has no ids yet:

```text
Book the flight to the conference @work id:1
Wait for the decision on the talk @work @waiting p:1
```
