# Vinilogs — Phases and Task Assignment

32 tasks, 6 tracks, 4 phases. Task IDs are stable; do not renumber.

## Agent tracks

| Track | Agent | Owns |
|---|---|---|
| **A** | Platform | Gradle, modules, CI, design system, navigation host, localisation |
| **B** | Backend | Firestore model, security rules, Cloud Functions, emulator setup |
| **C** | Data | `core:data` — repositories, Room, Discogs client, sync |
| **D** | Collection | `feature:collection` |
| **E** | Social | `feature:auth`, `feature:discovery` |
| **F** | QA | Test infrastructure, accessibility, performance, release |

Task format: `T-XX · [Track] · Title · (depends on) · covers FR/NFR`.
A task is only startable when every dependency is merged.

---

## Phase 0 — Foundation
**Goal:** an empty app that builds, runs, and is safe to work on in parallel.
**Exit criteria:** CI green on main; all modules created; app launches to a placeholder screen.

| ID | Track | Task | Depends | Covers |
|---|---|---|---|---|
| T-01 | A | Create the repo, Gradle version catalog, `build-logic` convention plugins, all empty modules per the layout in `00-README.md` | — | — |
| T-02 | A | Design system per `02-ARCHITECTURE.md` §6: Material 3 theme, light/dark, typography scale, colour tokens, motion spec, and shared components (`VinylCard`, `CoverPlaceholder`, `EmptyState`, `ErrorState`, `LoadingState`, `VinilogsTopBar`) | T-01 | NFR-9 |
| T-03 | A | Single-activity host, Navigation Compose graph with all routes stubbed, three-destination bottom bar | T-01 | — |
| T-04 | B | Firebase project (dev + prod), Auth/Firestore/Storage enabled, `google-services.json` wiring, Emulator Suite config and seed script | — | — |
| T-05 | A | GitHub Actions: build, ktlint, detekt, unit tests, emulator UI tests on every PR | T-01 | — |
| T-06 | C | Domain models in `core:model` (`Record`, `PublicRecord`, `User`, `UserProfile`, `CatalogResult`, enums, `CollectionFilter`, `CollectionSort`, `SyncState`) exactly as specified in `02-ARCHITECTURE.md` | T-01 | — |
| T-07 | F | `core:testing`: fake repositories for all three contracts, seeded with a realistic 200-record fixture, coroutine test rule, Compose test helpers | T-06 | — |

> After T-07, tracks C, D and E work fully in parallel: feature agents build against the
> fakes while Track C implements the real repositories.

---

## Phase 1 — Auth and collection core
**Goal:** a user can sign in and manage a real collection, offline.
**Exit criteria:** FR-A1–A5, FR-B1–B9, FR-B11, FR-B13 demoable end to end on a device in
aeroplane mode.

| ID | Track | Task | Depends | Covers |
|---|---|---|---|---|
| T-08 | C | `AuthRepository` on Firebase Auth + profile document creation | T-06, T-04 | FR-A1–A3 |
| T-09 | E | Auth screens: sign in, sign up, forgot password; form validation; auth-state routing between the `auth` and `main` graphs | T-07, T-03 | FR-A1–A3 |
| T-10 | C | Room schema, DAOs and mappers for records; filtering, sorting and search in SQL | T-06 | FR-B7, FR-B8 |
| T-11 | C | `CollectionRepository`: Room as source of truth, Firestore listener sync, offline writes, `SyncWorker` with `syncState` retry, `observeSyncState` | T-10, T-04 | FR-B2–B5, FR-B11 |
| T-12 | C | Discogs Retrofit client with API-key config, 24 h OkHttp cache, debounce, rate-limit backoff, typed failure states | T-06 | FR-B1, Constraints |
| T-13 | C | Cover image pipeline: gallery/camera pick, downscale, Storage upload, Coil disk cache config, generated placeholder | T-11 | FR-B13, NFR-4 |
| T-14 | B | Firestore security rules for `users` and `records` + rules unit tests, including deny-by-default and owner-only reads on `records` | T-04 | NFR-5, NFR-6 |
| T-15 | D | Shelf screen: grid/list toggle, incremental search, filter sheet, sort menu, empty and error states, pending-sync indicator | T-07, T-02 | FR-B6–B8, FR-B11 |
| T-16 | D | Add record — catalogue search flow: query, paginated results, prefill, confirm, and a manual-entry escape hatch on every failure state | T-07, T-12 | FR-B1, FR-B2 |
| T-17 | D | Add/edit record — manual form with all fields from FR-B4, validation, works fully offline | T-07 | FR-B3, FR-B4 |
| T-18 | D | Record detail screen: large cover, shared-element transition from shelf, edit, share, delete with confirmation and 5-second undo | T-07 | FR-B5, FR-B9, FR-C5 |
| T-19 | E | Profile screen and edit profile: avatar upload to Storage, bio, location, privacy toggle | T-08, T-02 | FR-A4, FR-A5 |

---

## Phase 2 — Discovery
**Goal:** collectors can find each other and look at each other's shelves.
**Exit criteria:** FR-C1–C6 working between two real accounts, with private fields verified
absent from the wire.

| ID | Track | Task | Depends | Covers |
|---|---|---|---|---|
| T-20 | B | Cloud Functions: `onRecordWritten` (counter + `publicRecords` projection), `onProfileUpdated` (projection build/teardown on privacy change) | T-04 | FR-A5, FR-C2 |
| T-21 | B | Security rules for `publicRecords` and `reports` + rules unit tests proving a non-owner cannot read `records` or any private field | T-14, T-20 | NFR-5, NFR-6 |
| T-22 | C | `UserRepository`: prefix search on `displayNameLower`, public collection reads from the projection, local shared-record computation, report writes | T-08, T-20 | FR-C1–C4, FR-C6 |
| T-23 | E | Discovery: user search screen with empty and no-results states, and public profile with collection grid and shared-record badge | T-07, T-22 | FR-C1, FR-C2, FR-C4 |
| T-24 | E | Read-only public record detail + shared-records list screen | T-23 | FR-C3, FR-C4 |
| T-25 | E | Report a profile: reason sheet, confirmation, local hiding of the reported user from search results | T-23, T-21 | FR-C6 |

---

## Phase 3 — Polish and extras
**Goal:** the app is complete, pleasant and accessible.
**Exit criteria:** all remaining FRs done; NFR-3, NFR-7 and NFR-8 verified.

| ID | Track | Task | Depends | Covers |
|---|---|---|---|---|
| T-26 | D | Collection statistics screen: totals, spend, per-decade chart, top 5 artists and labels | T-11 | FR-B10 |
| T-27 | D | CSV export via the share sheet | T-11 | FR-B12 |
| T-28 | E | Account deletion with typed confirmation + `onAccountDeleted` cascade function | T-19, T-20 | FR-A6 |
| T-29 | A | Offline and error UX polish: connectivity banner, consistent retry affordances, aeroplane-mode pass over every screen | T-11 | FR-B11, NFR-3 |
| T-30 | A | Spanish localisation; audit every hardcoded string | T-15–T-25 | NFR-8 |
| T-31 | F | Accessibility pass: content descriptions, touch targets, TalkBack run-through, 200% font scaling, reduce-motion honoured | T-15–T-25 | NFR-7 |

---

## Phase 4 — Hardening and release
**Goal:** shippable build on the Play Store internal track.
**Exit criteria:** all NFR targets met and evidenced; signed AAB uploaded.

| ID | Track | Task | Depends | Covers |
|---|---|---|---|---|
| T-32 | F | Test coverage to target: ViewModel unit tests, repository tests against the emulator, Compose UI tests per screen | Phase 3 | NFR-11 |
| T-33 | F | Macrobenchmark: cold start and shelf scroll with a 1,000-record seeded collection; fix regressions | T-32 | NFR-1, NFR-2 |
| T-34 | F | R8/ProGuard rules, baseline profile, resource shrinking, bundle-size check | T-33 | NFR-10 |
| T-35 | F | Crashlytics + Analytics wiring, key funnel events (sign-up, first record added, tenth record added) | Phase 3 | NFR-12 |
| T-36 | B | Production Firebase environment, rules deployed, indexes created, backup policy | T-21 | NFR-5 |
| T-37 | A | Release pipeline: signing config, versioning, Play Console internal track upload from CI | T-34 | — |
| T-38 | F | Release checklist: privacy policy, Play data-safety form, store listing, screenshots, manual regression pass including a full offline run | T-37 | NFR-6 |

---

## Parallelisation summary

| Phase | Runs in parallel |
|---|---|
| 0 | T-01 → then T-02/T-03/T-05 (A) ∥ T-04 (B) ∥ T-06 (C); T-07 last |
| 1 | Track C (T-10–T-13) ∥ Track D (T-15–T-18) ∥ Track E (T-09, T-19) ∥ Track B (T-14) |
| 2 | Track B (T-20, T-21) → Track C (T-22) → Track E (T-23–T-25) |
| 3 | All tracks in parallel; T-30 and T-31 gate on feature completion |
| 4 | F leads; A and B support |

Phase 2 is the least parallel phase: the projection has to exist before the repository, and
the repository before the screens. Feature agents on Track E should build against fakes from
T-07 rather than waiting.

## Risk register

| Risk | Mitigation |
|---|---|
| Discogs rate limits or downtime break the add-record flow | Aggressive local caching, debounced search, manual entry always presented as the primary action on any failure |
| Discogs metadata is wrong or the cover is missing | Every prefilled field editable; custom cover upload (T-13) |
| Firestore lacks full-text search | Own-collection search runs in Room SQL, so it is unaffected. User search is prefix-only in v1.0 |
| A private field leaks through discovery | Separate `publicRecords` projection and a distinct `PublicRecord` type; rules tests in T-21 assert it |
| Sync conflicts on multiple devices | Last-write-wins by `updatedAt`, documented as a known limitation |
| Discovery is empty at launch | Accepted. The shelf must justify the app on its own; discovery is upside |
| Feature agents blocked on `core:data` | Fakes from T-07 make every feature task independent of Track C |
