---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# Signing Key of the Android Application as a Repository Secret

`adr~signing-key-as-secret~1`

Needs: impl

## Context and Problem Statement

Android installs a new build over an existing one only if both are signed with the same key.
The builds come from the continuous integration, whose runners have a different debug key in every run.
The application keeps an SSH private key in its private storage, which any build with the same signature may read.
With which key are the published builds signed, and where is it kept?

## Considered Options

* A release key kept as a secret of the repository
* A debug key committed to the repository
* The debug key of each runner

## Decision Outcome

Chosen option: "A release key kept as a secret of the repository", because builds install over each other and only the workflows of this repository can create a build that Android accepts as an update.

The build workflow decodes the key store from the secret `SIGNING_KEYSTORE_BASE64` and passes its location and the password (`SIGNING_KEYSTORE_PASSWORD`) to Gradle through the environment.
The published build is the release variant, which is not debuggable.
Without the key, as on a developer machine and in pull requests from forks, the release variant stays unsigned.

### Consequences

* Good, because the private storage of an installed application cannot be reached by a build of a third party.
* Bad, because a lost key store means that users have to uninstall the application, losing its private storage, before they can install a build signed with a new key.
  A copy of the key store therefore has to be kept outside of GitHub.
* Bad, because a build made on a developer machine does not install over a published one.

### Confirmation

`apksigner verify --print-certs` on the APK of the build workflow shows the certificate of the key store.

## Pros and Cons of the Options

### A debug key committed to the repository

* Good, because every build, also a local one, installs over every other.
* Bad, because anyone can sign a build that replaces the installed application and reads its private storage, including the SSH key.

### The debug key of each runner

* Good, because there is no key to manage.
* Bad, because every new build requires uninstalling the previous one.
