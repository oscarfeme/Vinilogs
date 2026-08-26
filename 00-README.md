# Vinilogs — Project Instructions

> **Name is not final.** `Vinilogs` is a working title used consistently across these
> documents, the package ID (`app.vinilogs`) and the deep-link scheme. Renaming is a
> single find-and-replace; do not invent variations in the meantime.

Android app for cataloguing a personal vinyl record collection, and for browsing other
collectors' public shelves.

These six documents are the source of truth for all agents working on this project. Read
them in order before writing code.

| Document | Purpose |
|---|---|
| `00-README.md` | Purpose, stack decisions, working rules (this file) |
| `01-REQUIREMENTS.md` | What the app must do (functional + non-functional) |
| `02-ARCHITECTURE.md` | How it is built: layers, modules, data model, APIs |
| `03-PHASES-AND-TASKS.md` | Phase plan and numbered tasks assigned to agent tracks |
| `04-ALT-BACKEND.md` | Contingency only — self-hosted backend spec. **Not** to be implemented for v1.0 |
| `05-DESIGN-DIRECTION.md` | Visual design system: colour, type, space/shape, components, motion. Locked for v1.0 — see its own §-level ADR requirement |

## Purpose

### The problem

Vinyl collectors track their records badly. Most use a spreadsheet, a photo roll, or
nothing at all — so they buy duplicates, forget what a pressing cost them, and can't answer
"do I already own this?" while standing in a shop.

The tools that do exist share one flaw: they are **front ends for a Discogs account**. They
require a Discogs login, they treat your collection as a remote resource to be fetched, and
they are close to useless in a basement record shop with no signal — which is precisely
where a collector needs them. Most are also iOS-first.

Collectors are also curious about each other. Knowing who owns what is half the hobby, and
no phone-first tool makes another collector's shelf pleasant to look at.

### What Vinilogs is

A phone-first shelf you actually enjoy opening, that works with no signal, and that lets you
look at other collectors' shelves.

A user signs up, builds a catalogue of the records they own — looked up in an online music
database or entered by hand — and browses, filters and sorts their shelf. They can find
other users, look at their public collections, and see how many records they have in common.
Any record can be shared out to whatever app they already use to talk to people.

**The collection is local.** Discogs is a lookup service used to save typing, not the store
of record. A user never needs a Discogs account, and once a record is added it belongs to
them: editable, offline, and permanent even if Discogs changes or disappears. This is the
single most important difference between this app and everything else on the store.

### Who it is for

The **active collector**: owns 50–2,000 records, buys a few a month, shops in physical
stores and at fairs, and cares about pressing details (label, catalogue number, condition).
They are on their phone, often offline, and impatient. Design for one hand, poor signal, and
someone who knows more about vinyl than we do.

Not built for: casual listeners with no physical media, dealers managing inventory at scale,
or archivists needing fine-grained pressing variants.

### What success looks like

The v1.0 bar. Every task should move at least one of these:

1. A new user gets their first record onto the shelf in under 60 seconds — with no Discogs
   account and no tutorial.
2. A collector with 500 records can find any one of them in under 5 seconds.
3. The shelf is fully browsable, searchable and editable with no signal.
4. A collector can go from finding another user to seeing what they have in common in two
   taps.
5. The app is nice enough to open that people open it when they don't need to.

Criterion 5 is not decoration. In a category full of functional-but-joyless Discogs clients,
how the shelf feels is the product.

### Product principles

Use these to resolve ambiguity in a task without waiting for clarification. Note the
assumption you made in the PR.

- **The collection is sacred.** A user's data is never lost, silently changed, or made
  public without an explicit choice. When in doubt, confirm before destroying and offer undo.
- **Offline is normal, not an edge case.** Record shops have no signal. Everything except
  catalogue lookup and discovery must work anyway, including adding and editing.
- **Speed over completeness.** A fast screen with four fields beats a slow one with twelve.
  Optimistic UI everywhere; never make a user wait on the network to see their own data.
- **The covers are the interface.** Sleeve art is the thing collectors recognise. Give it
  room, load it fast, and never let a layout reduce it to a thumbnail in a list of text.
- **Delight, but never at the cost of speed.** Undo snackbars, a considered empty state, a
  satisfying add-record confirmation, real haptics on destructive actions. If an animation
  delays a frame, cut the animation.
- **Respect the collector's knowledge.** Don't oversimplify metadata or hide fields.
  Condition grades and catalogue numbers matter to this user.
- **Discogs is a convenience, never a dependency.** Every flow must have a manual path that
  works when the API is down, rate-limited, or wrong.

### Domain glossary

Agents writing UI copy or naming things should use these terms correctly.

| Term | Meaning |
|---|---|
| Record / release | One physical item in the collection. The core entity. |
| Pressing | A specific manufacturing run of a release — same album, different year, label or country. Distinguished by catalogue number. |
| Catalogue number | Label-assigned identifier printed on the sleeve and label, e.g. `PCS 7027`. The most reliable way to identify a pressing. |
| Format | Physical type: LP, EP, 7", 10", 12", box set. |
| Speed | Playback RPM: 33⅓, 45 or 78. |
| Condition grading | Standard collector scale, best to worst: M (Mint), NM (Near Mint), VG+ (Very Good Plus), VG, G (Good), F (Fair), P (Poor). Use these abbreviations, never invent a star rating for condition. |
| Shelf | The user's own collection view. Use this word in UI copy, not "library" or "inventory". |
| Discovery | Finding and viewing other users' public shelves. |
| Discogs | The public music database used to look up release metadata. |

### Explicit non-goals for v1.0

Do not build these, and **do not design "hooks" for them**. No placeholder interfaces, no
unused columns, no "we'll need this later" abstractions. Log them as backlog if a user asks.

In-app chat or messaging · payments or marketplace · monetization · voice or video ·
barcode scanning · wantlist or wishlist · multiple shelves per user · play-count or turntable
tracking · price-guide valuations · a web or iOS client · social feed, follows or likes ·
Discogs account sync.

**On chat specifically:** 1-to-1 messaging was scoped for v1.0 and deliberately cut. It
would have been roughly half the build and all of the moderation burden, in exchange for an
empty inbox at launch, competing with WhatsApp. Discovery is retained; sharing a record goes
out through the Android share sheet to whatever the user already uses. If in-app messaging is
ever revived it will be a new phase with its own requirements — nothing in v1.0 should
anticipate it.

## Locked technical decisions

Agents must not change these without an ADR (see `02-ARCHITECTURE.md` §7).

| Area | Decision |
|---|---|
| Language | Kotlin 2.3, JDK 17 (bumped from 2.0 — ADR-7, `02-ARCHITECTURE.md` §7) |
| UI | Jetpack Compose + Material 3 |
| Min / target SDK | minSdk 26, targetSdk 35 |
| Architecture | MVVM + Repository, single-activity, unidirectional data flow |
| DI | Hilt |
| Async | Coroutines + Flow |
| Navigation | Navigation Compose (type-safe routes) |
| Local storage | Room + DataStore (preferences) |
| Backend | Firebase — Auth, Firestore, Storage |
| Album metadata | Discogs public API (lookup only, no account linking) |
| Image loading | Coil |
| Testing | JUnit5 + Turbine + MockK; Compose UI tests; Firebase Emulator Suite |
| CI | GitHub Actions |

Firebase Cloud Messaging is **not** part of v1.0 — there is nothing to notify a user about.
Do not add the dependency.

## Working rules for agents

1. **One task = one branch = one PR.** Branch name `feat/T-XX-short-slug`. PR title starts
   with the task ID.
2. **Stay inside your module.** If you need a change in another agent's module, open an
   issue instead of editing it.
3. **Contracts before implementation.** Interfaces, data models and API shapes defined in
   `02-ARCHITECTURE.md` are fixed; implement against them so parallel work integrates.
4. **No task is done without tests.** Minimum: unit tests for ViewModels and repositories,
   instrumented test for each new screen.
5. **No hardcoded secrets.** API keys go in `local.properties`, exposed via `BuildConfig`.
6. **Definition of Done:** code compiles, `./gradlew check` passes, tests written, acceptance
   criteria in the task met, PR description lists what was verified manually.
7. **Report blockers early.** If a task's acceptance criteria are ambiguous, state the
   assumption you made in the PR rather than stalling.
8. **Test with the network off.** Any task touching the collection must be verified in
   aeroplane mode before it is marked done.

## Repository layout

```
vinilogs/
├── app/                    # Application module, DI setup, navigation host
├── core/
│   ├── designsystem/       # Theme, colors, typography, shared components
│   ├── model/              # Domain models (pure Kotlin, no Android deps)
│   ├── data/               # Repositories, Firestore/Room/Discogs sources
│   └── testing/            # Test fakes and rules
├── feature/
│   ├── auth/               # Sign up, sign in, profile
│   ├── collection/         # Shelf, record detail, add/edit record, stats
│   └── discovery/          # User search, public collections
└── build-logic/            # Convention plugins for Gradle
```
