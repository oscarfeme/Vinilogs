# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

**Phase 0 is code-complete and CI-verified as of 2026-08-26** (T-01 through T-06, plus a
design-direction reconciliation — see `PROGRESS.md` for the full task-by-task history). All
modules exist with real implementations: `app`, `core:{designsystem,model,data,testing}`,
`feature:{auth,collection,discovery}`, `build-logic`. GitHub Actions CI (ktlint, detekt, unit
tests, assemble, Firestore rules tests, Compose UI tests on an emulator) is wired and green.

**What "CI-verified" does *not* mean**: nothing in this repo has ever been built, run, or
tested on a real device, emulator, or local machine — every verification so far happened via
GitHub-hosted CI runners, because the sandbox this was built in had no JDK, Android SDK, or
emulator. **If you're reading this on a machine that has real Android tooling, the single
highest-value thing to do is the manual verification pass in `PROGRESS.md`'s "First things to
do on a machine with real tooling" section** — that's the biggest unclosed gap in the project
right now, bigger than any specific next task.

Real commands, now that they are known:
- `./gradlew build` — full build
- `./gradlew testDebugUnitTest test` — unit tests (the plain `test` task is needed too:
  `core:model` is a pure-JVM module with no debug variant)
- `./gradlew ktlintCheck detekt` — lint (config in `config/detekt/detekt.yml` and
  `.editorconfig` — both are heavily commented explaining why each override exists; read
  them before changing either)
- `./gradlew connectedDebugAndroidTest` — instrumented tests, needs a running
  emulator/device

## Read the docs in order, first

Six numbered documents at the repo root are the source of truth for everything about this
project. Read them before writing any code or plan:

| Document | Purpose |
|---|---|
| `00-README.md` | Product purpose, locked tech stack, working rules for agents, repo layout |
| `01-REQUIREMENTS.md` | Functional (FR-A/B/C) and non-functional (NFR) requirements, with IDs |
| `02-ARCHITECTURE.md` | Layering, data flow, Firestore schema, repository interfaces, ADRs |
| `03-PHASES-AND-TASKS.md` | Phase plan, 32 numbered tasks (`T-XX`), agent tracks, dependencies |
| `04-ALT-BACKEND.md` | Contingency backend spec — do not implement unless a §1 trigger fires |
| `05-DESIGN-DIRECTION.md` | Locked visual design system — colour, type, space/shape, components, motion |

`PROGRESS.md` (not numbered — it is a status log, not a spec) tracks what has actually been
built, what is still open, and what to do next. Read it too, especially before picking a task.

Task IDs (`T-XX`) and requirement IDs (`FR-XX`, `NFR-XX`) are referenced across these docs and
should be referenced the same way in branches, PRs and commits.

## What this app is

Vinilogs (working title — do not invent alternate names) is a phone-first Android app for
cataloguing a personal vinyl record collection, and browsing other collectors' public shelves.
The single most important design fact: the collection is local and owned by the user, not a
front end for a Discogs account. Discogs is a lookup convenience used only at add-record
time; every flow must have a manual, offline-capable path when Discogs is unavailable.

## Locked technical decisions

Defined in `00-README.md`; do not change without an ADR per `02-ARCHITECTURE.md` §7.

Kotlin 2.0 / JDK 17, Jetpack Compose + Material 3, minSdk 26 / targetSdk 35, MVVM +
Repository, single-activity, unidirectional data flow, Hilt DI, Coroutines + Flow,
Navigation Compose (type-safe routes), Room + DataStore (local), Firebase (Auth, Firestore,
Storage), Discogs public API (lookup only), Coil, JUnit5 + Turbine + MockK + Compose UI
tests + Firebase Emulator Suite, GitHub Actions CI.

Firebase Cloud Messaging is explicitly not part of v1.0 — do not add it.

## Architecture

Three strict layers (`02-ARCHITECTURE.md` §1):

```
UI (Compose + ViewModels, per feature)
  ↕ StateFlow<UiState> / events
Domain (core:model — pure Kotlin, no Android/Firebase imports)
  ↕ repository interfaces
Data (core:data — AuthRepository, CollectionRepository, UserRepository)
  → Room (source of truth for the user's own collection)
  → Firestore (remote sync + discovery reads)
  → Discogs API via Retrofit (catalogue lookup only)
```

Rules that shape every change in this layer:

- The UI layer never touches Firestore, Room or Retrofit directly — only repository
  interfaces defined in `02-ARCHITECTURE.md` §4, which are fixed contracts. Not implemented
  yet — `core:model` (T-06) has the domain types the contracts reference, but the
  `AuthRepository`/`CollectionRepository`/`UserRepository` interfaces themselves do not exist
  in the repo yet. That is a gap worth resolving early in T-07/T-08.
- Room is the single source of truth for the user's own collection. Firestore listeners
  write into Room; screens observe Room only, never wait on network to show the user their own
  data. Discovery (other users' shelves) is the one network-first read path and is session-
  cached in memory, not persisted to Room.
- Domain models, Firestore DTOs, Room entities, and Discogs API responses are four distinct
  types, mapped at the boundaries in `core:data`. Once a record is saved it has zero
  dependency on Discogs.
- `Record` (owner's full model) and `PublicRecord` (discovery projection) are separate
  types — this is deliberate so a discovery screen cannot structurally render a private field
  (`purchasePrice`, `purchaseDate`, `rating`, `notes`). Discovery reads a server-maintained
  `publicRecords` Firestore projection, never the owner's `records` collection directly
  (ADR-4, `02-ARCHITECTURE.md` §3).
- Module layout (`00-README.md`): `app/`, `core/{designsystem,model,data,testing}`,
  `feature/{auth,collection,discovery}`, `build-logic/`. Multi-module by feature is deliberate
  (ADR-5) to keep agent tracks working in parallel with minimal merge conflicts — stay inside
  your module's boundary; changes needed elsewhere go through the owning track, not a direct
  edit.

## Known gotchas — read before touching Gradle config

- Type-safe version-catalog accessors do not resolve for multi-segment library aliases
  anywhere in the root build. `libs.foo.bar` (2+ hyphen segments, e.g.
  `libs.androidx.activity.compose`) fails with "Unresolved reference" when used as a library
  dependency, even though the catalog entry is valid and `alias(libs.plugins.foo.bar)` for
  plugins works fine. Root cause never fully identified (plugin-accessor and library-accessor
  generation are separate Gradle mechanisms; whatever is wrong is scoped to the root build's
  library-accessor class specifically). Workaround used everywhere in this repo:
  `libs.findLibrary("foo-bar").get()` instead of the typed accessor (string-keyed, same
  catalog, unaffected) — and `libs.findPlugin("foo-bar").get().get().pluginId` for a plugin ID
  needed as a runtime value (e.g. conditional `apply(plugin = ...)`). Use this pattern for any
  new catalog entry from the start — do not reach for the typed dotted accessor and expect it
  to work.
- ktlint/detekt were only run for real for the first time this session (T-05's CI never
  actually executed against real code until late in Phase 0). The overrides in
  `config/detekt/detekt.yml` and `.editorconfig` exist because the raw defaults are mostly
  noise for a Compose codebase (MagicNumber on every `Color(0xFF..)` literal, FunctionNaming
  on every `@Composable`, several ktlint formatting rules that force one exact canonical shape
  and fight this project's existing style either direction). Read those two files' comments
  before re-enabling anything they disable — each one records what was tried first and why it
  did not work.
- Firebase projects do not exist. `vinilogs-dev`/`vinilogs-prod` were never created — that
  needs a human with Firebase console access, documented step-by-step in `firebase/README.md`.
  Nothing Firebase-related (Auth, Firestore sync, Storage) can be tested end-to-end until this
  happens.

## Working rules for agents

From `00-README.md` — these apply to any code contribution in this repo:

1. One task = one branch = one PR. Branch `feat/T-XX-short-slug`; PR title starts with the
   task ID.
2. Stay inside your module/track (`03-PHASES-AND-TASKS.md` "Agent tracks"). Need a change
   elsewhere — open an issue, do not edit it directly.
3. Contracts before implementation — the repository interfaces and data models in
   `02-ARCHITECTURE.md` are fixed; implement against them so parallel tracks integrate cleanly.
4. No task is done without tests: unit tests for ViewModels/repositories, instrumented test
   for each new screen.
5. No hardcoded secrets — API keys via `local.properties` → `BuildConfig`.
6. Definition of Done: compiles, `./gradlew check` passes, tests written, task's acceptance
   criteria met, PR lists what was manually verified.
7. If a task's acceptance criteria are ambiguous, state the assumption made in the PR rather
   than stalling — same for the product-principles ambiguity rule in `00-README.md`.
8. Test with the network off. Any task touching the collection must be verified in
   aeroplane mode before being marked done — this is the project's core differentiator, not
   an edge case.

## Explicit non-goals (v1.0)

Do not build, and do not add hooks, unused columns, or "we will need this later" abstractions
for: in-app chat/messaging, payments/marketplace, monetization, voice/video, barcode scanning,
wantlist/wishlist, multiple shelves per user, play-count/turntable tracking, price-guide
valuations, a web/iOS client, social feed/follows/likes, Discogs account sync. Full rationale
(especially why chat was cut) is in `00-README.md` "Explicit non-goals for v1.0".

## Alternative backend

`04-ALT-BACKEND.md` specifies a costed Kotlin/Ktor + Postgres backend as a contingency. It is
not selected and must not be implemented unless one of the triggers in its §1 fires (cost
threshold, search complaints, query shapes Firestore cannot serve, etc.). The three
repository interfaces in `02-ARCHITECTURE.md` §4 exist specifically so that migration, if it
ever happens, is contained to `core:data`.
