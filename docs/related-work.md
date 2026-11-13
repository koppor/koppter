# Related work

This document compares koppter with other applications that manage tasks in plain text files.
It is filled one application at a time.
The applications that served as models for parts of koppter are listed in the [README](../README.md#related-work).

## Beorg

Beorg is a task manager for iOS that keeps its data in plain text files.
The comparison follows the review [Beorg may well be the best free iOS plain text task manager](https://ellanew.com/2025/07/21/ptpl-166-beorg-best-free-ios-plain-text-task-manager) by Ellane W of July 2025: the column for Beorg states what the review says, and Beorg itself has not been tried for this document.

| Aspect | Beorg, according to the review | koppter |
| --- | --- | --- |
| Platforms | iOS; the same files on a Mac with EasyOrg or Emacs | Android, Windows and Linux; no iOS |
| Price | Free; extensions are paid | Free and open source |
| Data | Plain text files of Org mode, which comes from Emacs | One plain text file in the todo.txt format, next to it files for notes, schedules and the plan |
| Work without a connection | Yes | Yes |
| Synchronization of the files | Not covered by the review | git, a folder of the device, or a storage service such as OneDrive through the platform |
| Agenda | Agenda view for scheduled and repeating tasks | *Agenda* and *Week* show the plan, which is computed from the durations of the tasks, the time maps and the fixed times |
| Repeating tasks | Yes | `rec:` after completion, `ice_recur.txt` by the calendar |
| Overdue tasks | Appear on the following day | Stay in the list and are marked; the overview shows the contexts that have overdue tasks |
| Calendar | Tasks are synchronized to Apple Calendar and Reminders; tasks created in Reminders are shown and cannot be edited | Reads calendars published as iCalendar files and plans around their events; writes the plan to `calendar.ics` for import; writes nothing into a calendar |
| Alerts | Alerts of the system through Reminders | None |
| Widgets | Several widgets, each with its own search | None |
| Subtasks | Any number | None; a task can wait for other tasks (`id:`, `p:`), and contexts have levels |
| Notes | Any number of notes per task | One note file per task (`note:`) |
| Attachments | None; links to files | None |
| Filtering | Less than the Tasks plugin of Obsidian offers: no complex boolean logic, date queries and tag combinations | Filter panel for priorities, projects, contexts and hashtags; board columns by filter expressions with dates and negation |
| Planning of the day | Not covered by the review | *Plan* gives each task times according to its duration, time map, limits and dependencies |
| Collaboration | None | None; the git repository serves the devices of one person |

What the review counts as the strength of Beorg, tasks in the calendar of the system with its alerts, and widgets, is what koppter does not have.
koppter in turn plans the tasks into the free times of the day, which the review does not report for Beorg.

## Applications still to compare

* [Other Todo.txt Projects](https://github.com/todotxt/todo.txt-cli/wiki/Other-Todo.txt-Projects), the list of applications for the todo.txt format in the wiki of the todo.txt command line.
* The other applications that the review names: NotePlan and the Tasks plugin of Obsidian.
