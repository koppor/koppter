---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# Requirements Tracing with OpenFastTrace

`adr~openfasttrace~1`

Needs: impl

## Context and Problem Statement

The planned scope is larger than what one development session implements.
Requirements stated once in a conversation are lost, and decisions are made without later being reflected in the design.
How are requirements and decisions recorded and connected to the code?

## Considered Options

* OpenFastTrace with requirements and decisions in the trace
* OpenFastTrace for requirements only
* GitHub issues

## Decision Outcome

Chosen option: "OpenFastTrace with requirements and decisions in the trace", because the chain `feat → req → dsn → impl/utest` shows mechanically what is not yet designed, implemented or tested, and a decision that has no effect on design or code is reported in the same way.

Each decision record defines an item of the artifact type `adr`.
A decision about the product is covered by the `dsn` items that follow from it; a decision about the project infrastructure is covered by `impl` tags in build scripts and workflows.

### Consequences

* Good, because `./gradlew traceRequirements` fails on a requirement or decision without coverage.
* Good, because requirements and decisions are plain Markdown next to the code.
* Bad, because a feature commit touches the specification, the code and the tags.

### Confirmation

The build workflow runs `traceRequirements`.

## Pros and Cons of the Options

### OpenFastTrace for requirements only

* Good, because decision records stay free of tracing syntax.
* Bad, because nothing checks that an accepted decision was acted upon.

### GitHub issues

* Good, because no tooling is needed in the build.
* Bad, because issues are not versioned with the code and cannot be traced to it.
