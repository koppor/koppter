---
status: accepted
date: 2026-11-14
decision-makers: Oliver Kopp
---

# Locations on the Desktop From the appdirs Library

`adr~desktop-locations-from-appdirs~1`

Needs: impl

## Context and Problem Statement

The desktop application keeps its settings in a directory of the user, and it now also needs a place for files it creates itself: the clone of a git repository and the todo.txt file of a user who has not chosen one.
Each operating system has its own places for these, and a user can move them.
How does the application find these directories?

## Considered Options

* The library appdirs
* Environment variables read in the application
* The library directories-jvm

## Decision Outcome

Chosen option: "The library appdirs", because it knows the places of Windows, Linux and macOS for settings and for data, and asks Windows for its folders instead of trusting an environment variable.

The settings stay where they are: `%APPDATA%\koppter` on Windows and `$XDG_CONFIG_HOME/koppter` or `~/.config/koppter` on Linux.
The files of the application are in `%APPDATA%\koppter` on Windows and in `$XDG_DATA_HOME/koppter` or `~/.local/share/koppter` on Linux.
On Windows, the roaming folder is used for both, so that they follow the user to another computer of the same domain.

### Consequences

* Good, because macOS gets its own places without code for it.
* Bad, because the application carries JNA, which appdirs needs for the folders of Windows.
* Bad, because the places are not verified on Windows and macOS in the development sessions.

## Pros and Cons of the Options

### Environment variables read in the application

The application read `APPDATA` and `XDG_CONFIG_HOME` itself until this decision.

* Good, because it needs no library.
* Bad, because each further kind of directory and each further operating system is more code of the application.
* Bad, because `APPDATA` is a variable that a program which starts the application can change, while the folder of Windows stays the same.

### The library directories-jvm

* Good, because it needs no native access: it asks Windows through PowerShell.
* Bad, because starting PowerShell takes noticeable time at the start of the application.
* Bad, because its names always contain an organization, so that the settings would move to another directory.
