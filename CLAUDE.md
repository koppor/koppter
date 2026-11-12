# koppter — instructions for Claude

## Session protocol

* Session history lives in git commit messages: subject line plus a short paragraph on what and why.
* `PLAN.md` in the worktree root is the working plan (untracked).
  Read it first, continue at the first unchecked item, tick items as they are completed.

## Conventions

* Gate: `./gradlew build traceRequirements` must be green before every commit.
* Branches: commit and push to `experimental` after every change; no pull requests.
  The daily release workflow (`.github/workflows/release.yml`) merges `experimental` into `main` and tags the release — sessions do not merge into `main` or tag by hand.
* Git: when a push is rejected, `git pull --no-rebase`. Never rebase.
* Changelog: `CHANGELOG.md` in Keep a Changelog format with CalVer releases ([decision 0002](docs/decisions/0002-calver-changelog-verified-with-heylogs.md)).
  Every user-visible change adds a bullet under `## [Unreleased]`; released sections are never edited.
  Check: `jbang heylogs@nbbrd check CHANGELOG.md`.
* Requirements: OpenFastTrace in `docs/requirements/`, chain `feat → req → dsn → impl/utest` ([decision 0003](docs/decisions/0003-requirements-tracing-with-openfasttrace.md)).
  New behavior means a new or updated specification item and the covering tags in the same commit.
  An item gains its `Needs:` line in the commit that adds the covering item.
* Decisions: MADR in `docs/decisions/` with honest considered options.
  Each record defines an `adr~<slug>~1` item directly below its title, covered by `dsn` items or by `impl` tags.
  Check: `jbang madrlint@adr/madrlint --no-warn 11,12 <file>` (needs Java 25).
* Markdown: one sentence per line.
  Check: `npx markdownlint-cli2`.
* Spelling: `typos` (configuration in `.typos.toml`).
* Prose in documents, comments, commit messages and UI texts: precise and sober, no marketing language.
* Code comments explain a non-obvious *why*, not the *what*.
* Kotlin, official code style, package `io.github.koppor.koppter`.
  `:core` `commonMain` stays free of JVM APIs; dates are `kotlinx.datetime.LocalDate`, and "today" is always passed in.
* Tests: unit tests for logic in `:core` (`kotlin.test`); Compose UI tests for UI behavior.
* License is MIT: no strong-copyleft dependencies.

## Architecture

* `:core` — Kotlin Multiplatform, JVM target.
  `commonMain`: todo.txt model, parser, filter, sort.
* The file is the data store.
  A line that the user did not edit is written back unchanged.
