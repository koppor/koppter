---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# CalVer Changelog, Verified with heylogs

`adr~calver-changelog~1`

Needs: impl

## Context and Problem Statement

Changes are pushed continuously to `experimental`; there is no release planning.
Users need a readable history of what changed, and a version number that tells how old a build is.
How are versions numbered, and how is the change history kept?

## Considered Options

* Keep a Changelog with CalVer day releases, checked by heylogs
* Keep a Changelog with SemVer releases
* Changelog generated from commit messages

## Decision Outcome

Chosen option: "Keep a Changelog with CalVer day releases, checked by heylogs", because the application has no API whose compatibility a semantic version could express, a date is a version number that needs no decision, and [heylogs](https://github.com/nbbrd/heylogs) turns format errors into build failures.

A scheduled workflow merges `experimental` into `main` every morning.
If `CHANGELOG.md` has an `Unreleased` section, the workflow renames it to the date of the previous day, the day on which the changes were made, and tags `main` with `v<date>`.

### Consequences

* Good, because releasing needs no manual step.
* Good, because the same check runs locally (`jbang heylogs@nbbrd check CHANGELOG.md`) and in the continuous integration.
* Bad, because at most one release per day is possible.
* Bad, because jbang is required locally to run the check.

### Confirmation

The `changelog` job of the build workflow fails on a changelog that violates the format or the versioning scheme.

## Pros and Cons of the Options

### Keep a Changelog with SemVer releases

* Good, because it is the most common combination.
* Bad, because every release needs a decision on the version number that carries no information here.

### Changelog generated from commit messages

* Good, because no separate file is maintained.
* Bad, because commit messages describe the work, not the effect for the user.
