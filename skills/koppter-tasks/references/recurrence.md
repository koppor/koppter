# Recurring tasks

## Which mechanism

| The user says | Mechanism | Why |
| --- | --- | --- |
| "again two weeks after I did it", "every three months or so" | `rec:` on the task | The interval counts from the completion, and there is one open task at a time. |
| "every Monday", "on the 1st and the 15th", "the last Friday of the month" | `ice_recur.txt` | The calendar decides, whether or not the last one is done. |
| "every month on the 1st, but only once the last one is paid" | `rec:+1m` on the task | The `+` counts from the previous due date, not from the completion. |

If it is not clear which one is meant, ask: "Does the next one start counting when you finish this one, or does it come on fixed days?"

## The tag `rec:`

A task with `rec:` needs a due date, from which the next one is computed:

```text
Descale the coffee machine @home due:2026-11-20 rec:3m
```

| Unit | Meaning | Example |
| --- | --- | --- |
| `d` | days | `rec:3d` |
| `b` | business days, Monday to Friday | `rec:b` |
| `w` | weeks | `rec:2w` |
| `m` | months | `rec:m` |
| `y` | years | `rec:1y` |

Without an amount it is one unit.
`rec:1w` counts from the day of completion; `rec:+1w` counts from the previous due date, however late the task was completed.

koppter adds the next task when the user completes the current one in koppter.
When completing such a task by hand, add the next one as well: the same line with the next due date; a threshold date moves along and keeps its distance to the due date.

## The file `ice_recur.txt`

It lies next to the todo.txt file, with that fixed name.
One entry per line:

```text
[@start] [@@end] timing - task
```

The part after ` - ` is the task exactly as koppter adds it to the todo.txt file, so it follows the rules for a task line.

| Entry | Adds the task |
| --- | --- |
| `daily - (A) Check the mail` | every day |
| `weekly ; day Monday - (B) Plan the week` | every Monday |
| `weekly ; day Friday, Sunday - Empty the bins` | every Friday and Sunday |
| `weekly ; day Monday, Tuesday, Wednesday, Thursday, Friday - Stand-up @work` | every working day |
| `@2026-11-02 weekly 2 ; day Thursday - Put out the paper bin` | every second Thursday, counted from the week of 2026-11-02 |
| `monthly ; day_of_month 1, 15 - Check the account` | on the 1st and the 15th of every month |
| `monthly ; day_of_month -1 - Send the time sheet` | on the last day of every month |
| `monthly ; day_of_week Friday => -1 - Back up the photos` | on the last Friday of every month |
| `monthly ; day_of_week Tuesday => 1, 3 - Team meeting` | on the first and the third Tuesday of every month |
| `@2026-03-01 monthly 3 - Read the meters` | every three months on the day of the start date |
| `@2026-01-04 yearly - Renew the membership` | every year on 4 January |
| `yearly ; month_of_year March, September - Change the tires` | on the 1st of March and of September |
| `@2026-06-01 @@2026-08-31 daily - Water the garden` | every day from 1 June to 31 August 2026 |

Rules:

* The timing starts with `daily`, `weekly`, `monthly` or `yearly`, optionally followed by an interval: `weekly 2`.
* Restrictions follow, each separated by ` ; ` with a blank on both sides: `day`, `day_of_month`, `day_of_week`, `day_of_year`, `month_of_year`.
* Days and months are English names or numbers; for days of the week, 0 is Sunday.
  A negative number counts from the end.
* The timing and the task are separated by ` - ` with a blank on both sides.
* A line that starts with `#` is a comment; use comments to group the entries as the file already does.

## Pitfalls

* **An interval above one needs a start date.**
  Without `@start`, intervals count from 2018-01-01, so `weekly 2` falls on weeks the user does not expect.
  Use the first day on which the task is to appear.
* **No date in the task of an entry.**
  The task is added as it is written, so `due:2026-12-01` in an entry gives every occurrence that same date.
  Leave dates out; the day it appears is the day it is meant for.
* **No `rec:` in the task of an entry.**
  The two mechanisms would both add tasks.
* **A task is not added while an open one with the same text exists.**
  That keeps tasks from piling up, and it means two entries must not have the same task text.
* **koppter runs the schedules once a day, while it is open.**
  An entry written today whose day is today may not give its task today.
  If the user expects the task now, also append it to the todo.txt file; koppter does not add it a second time.
* **There is no time of day.**
  "Lunch from 12:00 to 12:45 on working days" cannot be written as an entry that blocks that time.
  Time that is to stay free belongs in the time maps of the plan dialog of koppter, or in the calendar.

## Check

After editing `ice_recur.txt`, read each new line again against the rules above: the separators with their blanks, English names, a start date for intervals.
koppter marks a line it does not understand in its editor of scheduled tasks (the calendar button in the top bar) and skips it, so a wrong line fails silently until the user looks there.
Tell the user to open that editor once: it shows for each entry the next day on which it adds its task.
