# Requirements

Requirements are traced with [OpenFastTrace](https://github.com/itsallcode/openfasttrace) ([decision 0003](../decisions/0003-requirements-tracing-with-openfasttrace.md)).

## Structure

| File | Content |
| --- | --- |
| [features.md](features.md) | Features (`feat`) |
| [todotxt.md](todotxt.md) | Reading, writing, filtering and ordering tasks (`req`, `dsn`) |
| [ui.md](ui.md) | Screens, storage and settings (`req`, `dsn`) |
| [android.md](android.md) | Android application (`req`, `dsn`) |

Chain: `feat` → `req` (what the user needs) → `dsn` (how it is built) → `impl` and `utest` tags in the source code.

A tag in the source code is a comment of the form `[impl->dsn~<name>~<revision>]` or `[utest->dsn~<name>~<revision>]`.

## Conventions

* An item gains its `Needs:` line in the commit that adds the covering item.
  The trace is therefore green at every commit, and an item without `Needs:` is planned scope that is not yet worked on.
* Item names are kebab-case and stable.
  A change of meaning increases the revision; all references are updated in the same commit.

## Decisions in the trace

Each decision record in [docs/decisions](../decisions/README.md) defines one item of the artifact type `adr` directly below its title.

* A decision about the product declares `Needs: dsn`.
  The design items that follow from it list the decision under `Covers:`.
* A decision about the project infrastructure declares `Needs: impl`.
  It is covered by a tag `[impl->adr~<name>~<revision>]` in the build script or workflow that implements it.

## Running the trace

```shell
./gradlew traceRequirements
```

The report is written to `build/reports/tracing.txt`.
The build fails on any tracing defect.
