---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# OneDrive through the File Access of the Platform

`adr~onedrive-through-platform~1`

Needs: dsn

## Context and Problem Statement

The todo.txt file should be usable from OneDrive, on Android and on the desktop.
OneDrive can be reached in two ways: through the mechanisms each platform has for files of other applications, or directly through the Microsoft Graph interface.
How does the application access a file in OneDrive?

## Considered Options

* The file access of the platform: the document picker on Android, the synchronized folder on the desktop
* A client for Microsoft Graph in the application

## Decision Outcome

Chosen option: "The file access of the platform", because it needs no code that is specific to OneDrive, no account handling in the application, and no application registration at Microsoft.

On Android, the OneDrive application offers its files in the document picker of the system; the user chooses the todo.txt file there.
On the desktop, the OneDrive client keeps a folder synchronized; the user chooses the todo.txt file in that folder.
The same holds for any other storage service that integrates with the platform in this way.

A client for Microsoft Graph is future work.
It needs an application registration in Microsoft Entra by the owner of the project.

### Consequences

* Good, because the application handles a file in OneDrive like any other file.
* Bad, because the OneDrive application has to be installed on Android and a OneDrive client on the desktop; on Linux this is a third-party program.
* Bad, because conflicts are resolved by OneDrive, which keeps a second copy of the file instead of merging the lines.
* Bad, because notes and scheduled tasks are not available for a file chosen in the document picker on Android, as the application has access to this one document only.
* Bad, because the access through the document picker of Android is not verified on a device yet.

## Pros and Cons of the Options

### A client for Microsoft Graph in the application

* Good, because it works without the OneDrive application and could merge concurrent changes line by line.
* Bad, because it needs sign-in, token storage and an application registration.
