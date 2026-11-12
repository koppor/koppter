---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# JGit for Git Access

`adr~jgit~1`

Needs: dsn

## Context and Problem Statement

The application commits, fetches, merges and pushes the repository that contains the todo.txt file.
It runs on desktops, where a git program is usually installed, and on Android, where none is.
How does the application access git?

## Decision Drivers

* The same behavior on all platforms, covered by the same tests.
* SSH with a key, as used for GitHub.

## Considered Options

* JGit with its Apache MINA sshd transport on all platforms
* The git program on the desktop, JGit on Android
* JGit with sshj as SSH transport

## Decision Outcome

Chosen option: "JGit with its Apache MINA sshd transport on all platforms", because one implementation serves the desktop and Android, both parts are maintained together by the JGit project, and both have licenses compatible with MIT (EDL/BSD-3-Clause and Apache-2.0).

On the desktop, the transport reads `~/.ssh` as OpenSSH does: keys, `known_hosts` and `config`.

### Consequences

* Good, because the synchronization logic is tested once, on the JVM, without a git installation.
* Bad, because settings of the git program that JGit does not know, such as credential helpers and hooks, have no effect.
* Bad, because the application grows by JGit, Apache MINA sshd and their dependencies.
* Bad, because the SSH transport on Android is not verified on a device yet.

### Confirmation

`GitStorageTest` runs the synchronization of two repositories against a shared remote repository.

## Pros and Cons of the Options

### The git program on the desktop, JGit on Android

* Good, because the desktop behaves exactly as the user's git does, including credential helpers.
* Bad, because there are two implementations, and the one for Android cannot be tested on the development machine.

### JGit with sshj as SSH transport

* Good, because sshj is known to work on Android.
* Bad, because the transport between JGit and sshj would be code of this project.
