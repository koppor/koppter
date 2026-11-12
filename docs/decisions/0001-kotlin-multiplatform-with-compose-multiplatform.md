---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# Kotlin Multiplatform with Compose Multiplatform

`adr~kotlin-multiplatform~1`

Needs: impl

## Context and Problem Statement

The application has to run on Android and on Windows and Linux desktops; macOS is desirable.
The todo.txt logic and, as far as possible, the user interface should exist once.
Which language and UI technology are used?

## Decision Drivers

* Android is a first-class target, not a port.
* One implementation of parsing, filtering and synchronization.
* A git client library must be available on every target.

## Considered Options

* Kotlin Multiplatform with Compose Multiplatform
* Flutter
* Web technology (Electron on the desktop, Capacitor on Android)
* Java with JavaFX on the desktop and a separate Android app

## Decision Outcome

Chosen option: "Kotlin Multiplatform with Compose Multiplatform", because Android and the desktop both run on a JVM, so that the shared code can use JVM libraries such as JGit directly, and the Compose UI is written once for both.

The platform-independent logic lives in the `commonMain` source set of `:core` and uses no JVM API.
This keeps a later iOS or web target possible without deciding on it now.

### Consequences

* Good, because the desktop target also yields a macOS application without additional code.
* Good, because Compose is the native UI toolkit on Android.
* Bad, because the desktop application ships a Java runtime, which makes the installer larger than a native application.
* Bad, because Compose Multiplatform on the desktop has fewer ready-made components than established desktop toolkits.

## Pros and Cons of the Options

### Flutter

* Good, because it covers Android, desktop and iOS with one UI.
* Bad, because there is no maintained pure-Dart git implementation; git access would need native bindings per platform.

### Web technology

* Good, because Sleek shows that the approach works on the desktop.
* Bad, because two packaging stacks are needed, and file access and background synchronization on Android go through plugins.

### Java with JavaFX and a separate Android app

* Good, because the logic could be shared as a Java library.
* Bad, because the user interface would be written twice.
