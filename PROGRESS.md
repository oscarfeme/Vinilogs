# Progress log

Status tracker, not a spec — the numbered `0X-*.md` docs are the fixed plan; this file is
where things actually stand. Update it as tasks land. Superseded entries can be trimmed once
they're no longer useful context, but don't delete the "known gotchas" material below without
folding it into `CLAUDE.md` first.

## Where things stand (updated 2026-09-28)

**16 of the 38 tasks in `03-PHASES-AND-TASKS.md` are merged to `master`**: T-01 through T-12,
T-14, T-15, T-17, T-18. Phase 0 is complete. Phase 1 is 9/12 done.

| Task | What it delivered | What's deliberately deferred |
|---|---|---|
| T-01–T-06 | Repo scaffold, design system, navigation host, Firebase SDK wiring, CI, domain models | See the Phase 0 history further down |
| T-07 | `core:testing` fakes for all three repository contracts + the `AuthRepository`/`CollectionRepository`/`UserRepository` interfaces themselves (T-06 deliberately left these out) | — |
| T-08 | `FirebaseAuthRepository` — sign up/in/out, password reset, profile doc creation | — |
| T-09 | Auth screens (sign in, sign up, forgot password), form validation, auth-state routing | — |
| T-10 | Room schema, DAOs, mappers, SQL filter/sort/search | — |
| T-11 | `RoomCollectionRepository` — Room as source of truth, Room→Firestore best-effort sync on write | **No Firestore→Room read-side sync exists** — see "Known issues to fix" below. `SyncWorker` (retry for failed outbound writes) also still not implemented. |
| T-12 | Discogs Retrofit client, typed failure states, 24h cache | No screen calls it yet — that's T-16 |
| T-14 | Firestore security rules (`users`/`records`) + rules unit tests | `publicRecords`/`reports` still deny-by-default, pending T-21 (Phase 2) |
| T-15 | Shelf screen — grid/list, search, filter, sort, empty/error states | — |
| T-17 | Add/edit record manual form, all FR-B4 fields, works offline | — |
| T-18 | Record detail screen — cover, edit, share, delete with undo | — |
| Design-direction reconciliation | Locked monochrome design system applied to `core:designsystem` | — |
| AGP 9 / Gradle 9 migration | Done — see ADR-7 pass 5, `02-ARCHITECTURE.md` §7 | — |

**Not started, dependencies satisfied, all three startable now:**

- **T-13** — cover image pipeline (gallery/camera pick, Storage upload, Coil cache, placeholder). Depends on T-11 ✓.
- **T-16** — add-record catalogue search flow (Discogs query/paginate/prefill/confirm, manual-entry escape hatch). Depends on T-07 ✓, T-12 ✓. **Fully verifiable without Firebase** — it's the only one of the three that doesn't touch Storage.
- **T-19** — profile + edit-profile screens (avatar, bio, location, privacy toggle). Depends on T-08 ✓, T-02 ✓. `feature/auth/ProfileScreens.kt` is still three literal `StubScreen(...)` calls.

Phase 2 (T-20 onward, discovery) is untouched and gated on a real decision, not code: T-20's
Cloud Functions need Firebase's paid Blaze plan (Spark/free tier doesn't run Cloud Functions).
Someone with Firebase console access needs to make that call before Phase 2 can start.

## The app has now actually been run, for the first time ever (2026-09-28)

Until this date, nothing in this repo had ever been visually rendered — every task from T-01
onward was verified by reading code and by GitHub Actions CI, which has a permanent,
unrelated emulator-boot flake on its Compose UI test job. See the "First things to do on a
machine with real tooling" section below for how this finally happened and what it found.

**Two real bugs were found and fixed by actually running the app:**

1. **Cleartext HTTP blocked by default.** Android refuses all cleartext traffic at
   targetSdk 28+ unless a network security config says otherwise. The Firebase emulator SDKs
   talk plain HTTP, so every Auth/Firestore call to the local Emulator Suite failed with
   `Cleartext HTTP traffic to <host> not permitted` until this was added. Fixed via a
   **debug-build-only** network security config — `app/src/debug/AndroidManifest.xml` +
   `app/src/debug/res/xml/network_security_config_debug.xml`, permitting cleartext to
   `10.0.2.2`/`localhost`/`127.0.0.1` only. Never merged into a release build.
2. **No Firestore→Room sync exists.** ADR-2 says "Firestore listeners write into Room"; T-11
   only implements the other direction (Room writes push to Firestore, best-effort). Confirmed
   by grepping `core:data` for `addSnapshotListener` — no hits outside a code comment. Symptom:
   signed in as a seeded user with 3 existing Firestore records, the shelf showed "Your shelf
   is empty" — Room, the shelf's actual data source, never received them. **This blocks any
   multi-device scenario and "existing collection, new install" entirely**, and is distinct
   from the already-documented "`SyncWorker` not implemented" gap (that one's about retrying
   failed outbound writes, not inbound reads never happening at all). Not yet fixed — worth a
   task of its own, probably scoped as a T-11 follow-up.

**Verified working end-to-end, for the first time, against real Firebase emulators:** sign-in
(T-09) with a seeded account, auth-state routing into the main graph, bottom-tab nav switching
(Shelf/Discover/Profile), Discover and Profile correctly showing their T-23/T-19 stub screens,
the T-17 add-record form (all fields, chip selectors), the new record appearing **instantly**
on the shelf (optimistic UI, per FR-B2/B3), the T-18 record detail screen, and the
Room→Firestore write-sync path (confirmed by querying the Firestore emulator directly — the
new record was there). Also verified, for the first time, the project's core "offline is
normal" claim (`00-README.md`, working rule 8): with the connection to the emulator backend
actually severed, the shelf still rendered the existing record correctly.

**Minor cosmetic bug, not yet fixed:** every screen shows a raw system title bar reading
`app.vinilogs.MainActivity` instead of the app's own design — `AndroidManifest.xml`'s
`<activity>` has no `android:theme`, so nothing suppresses the default action bar.

**Local dev setup that made this possible** (all free, no Firebase account needed — this
project's real bar is that it should never have needed one):

- `app/google-services.json` — gitignored, not a real project. Uses a `demo-vinilogs` project
  ID (Firebase's "demo-" prefix convention: the SDK never attempts a real network call for a
  `demo-` project).
- `local.properties` (gitignored) — two new keys: `firebase.useEmulator=true` and
  `firebase.emulatorHost` (`10.0.2.2` for an AVD, `localhost` for a physical device reached via
  `adb reverse tcp:8080/9099/9199`). Read into `BuildConfig.USE_FIREBASE_EMULATOR`/
  `FIREBASE_EMULATOR_HOST` by `core/data/build.gradle.kts`, consumed by
  `core/data/.../di/FirebaseModule.kt`'s `FirebaseAuth`/`FirebaseFirestore` providers — gated
  on `BuildConfig.DEBUG` so this can never reach into a release build.
- `cd firebase && npm install && npx firebase emulators:start --project demo-vinilogs` brings
  up Auth (:9099), Firestore (:8080), Storage (:9199), UI (:4000) — all local, matches
  `firebase.json`'s config.
- `cd firebase/seed && npm install`, then with the emulators running and
  `FIRESTORE_EMULATOR_HOST=localhost:8080 FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
  GCLOUD_PROJECT=demo-vinilogs node seed.js` seeds 3 users / 9 records (password
  `password123` for all — see `firebase/seed/seed.js`).
- **On this machine specifically**: the Android emulator was a dead end — no admin rights to
  enable Windows Hypervisor Platform (x86_64 AVD acceleration), and the installed emulator
  (37.1.11) now hard-refuses to run an ARM system image on an x86_64 host at all. A physical
  Android phone connected via USB (with Developer Options → USB debugging on, and the on-device
  "Allow USB debugging?" prompt accepted) was the path that actually worked.

## First things to do on a machine with real tooling

Mostly done now (2026-09-28), on a physical device rather than an emulator — see the section
above. What's still outstanding from this checklist:

1. ~~`git clone`, open in Android Studio, let it sync.~~ Not done via Android Studio
   specifically, but `./gradlew build` confirms the module graph and Gradle config work outside
   GitHub's runners.
2. ~~`./gradlew build`~~ — done, **BUILD SUCCESSFUL**, every module, both variants.
3. ~~`./gradlew testDebugUnitTest test`~~ — done, part of the full build above.
4. ~~`./gradlew ktlintCheck detekt`~~ — clean on `master`.
5. Run the app on an emulator or device — **done on a physical device**, see above. Still
   outstanding: confirming this on an actual AVD (blocked on this machine specifically, not a
   project problem — see the admin-rights/ARM-image notes above), and:
   - Switching bottom-bar tabs after navigating into a sub-screen restoring each tab's own
     position (`saveState`/`restoreState`) — **not yet explicitly tested**.
6. `./gradlew connectedDebugAndroidTest` — **still not run for real anywhere.** The 6 Compose UI
   tests have never executed outside CI's permanently-flaky emulator job. Now that a physical
   device works, this is finally possible — worth doing before trusting those tests further.

## Known issues to fix (found 2026-09-28)

- ~~**No Firestore→Room sync**~~ — **fixed**, PR #24 (`fix/T-11-firestore-room-sync` →
  `chore/local-firebase-emulator-dev-setup`, stacked since it needs the emulator wiring from
  that PR to be manually verifiable). Auth-state-driven listener on `users/{uid}/records`,
  upserts into Room, skips clobbering a still-`PENDING` local row. Also wires `searchCatalog` to
  the real `DiscogsCatalogClient` (was a `NotImplementedError` stub). 61 `core:data` tests
  green.
- **No app theme set** on `MainActivity` — raw system title bar shows instead of the app's own
  chrome. Quick fix, `AndroidManifest.xml` + whatever theme `core:designsystem` already defines.
  Not yet fixed.
- **Bottom-tab `saveState`/`restoreState` behaviour** — still unconfirmed.
- **`./gradlew connectedDebugAndroidTest`** — still never run for real; do this now that a
  physical device is available.

## 2026-09-28, later: three tasks run in parallel via subagents in isolated git worktrees

T-16, a T-11 follow-up, and T-19 were dispatched as three parallel background agents (each
`isolation: "worktree"`, so no shared working directory) once PR #22
(`chore/local-firebase-emulator-dev-setup`) was up. Module boundaries were chosen deliberately
to minimize merge conflicts: T-16 stays in `feature:collection`, the T-11 fix stays in
`core:data`, T-19 spans `feature:auth` + a real `UserRepository` implementation in `core:data`
(mirroring T-08/T-11's own precedent of each task implementing its own repository).

- **PR #23 — T-16 catalogue search** (`feat/T-16-catalog-search` → `master`, MERGEABLE).
  New `feature/collection/.../search/` package: `CatalogSearchViewModel` (400ms debounce
  matching `02-ARCHITECTURE.md` §2, maps `DiscogsFailure` to offline/rate-limited/generic —
  every one of those states plus no-results keeps "Add manually" visible per FR-B1),
  `CatalogSearchScreen`. `AddEditRecordScreen`/`AddEditRecordViewModel` extended with a
  `applyCatalogResult()`/`CatalogResult.toDraft()` path so a search hit prefills into the
  existing T-17 form rather than a second form. Built and tested entirely against
  `FakeCollectionRepository` — did not touch `core:data`, so it doesn't depend on the T-11 PR
  merging first; real Discogs search will "just work" once #24 lands since both go through the
  same `CollectionRepository.searchCatalog` interface. 27 new tests across ViewModel/mapper/
  Compose UI layers.
- **PR #24 — T-11 follow-up** (`fix/T-11-firestore-room-sync` →
  `chore/local-firebase-emulator-dev-setup`, MERGEABLE) — see "Known issues to fix" above.
- **PR #25 — T-19 profile screens** (`feat/T-19-profile-screens` →
  `chore/local-firebase-emulator-dev-setup`, MERGEABLE). `ProfileScreen`/`EditProfileScreen`
  replace their stubs (FR-A4/A5); `SettingsScreen` stays a stub, out of scope. First real
  `UserRepository` implementation (`FirebaseUserRepository`) — `observeProfile`/`updateProfile`
  real (Firestore listener + `SetOptions.merge()` so `recordCount`/`createdAt` survive an edit);
  `searchUsers`/`observePublicCollection`/`sharedRecords`/`report` stubbed with
  `NotImplementedError` pointing at T-22, matching `RoomCollectionRepository`'s own precedent
  for `searchCatalog`/`exportCsv`. Avatar upload implemented for real: a new narrow
  `AvatarUploader`/`FirebaseAvatarUploader` abstraction (not one of the three fixed repository
  interfaces — `UserRepository.updateProfile` takes an already-resolved `avatarUrl: String?`,
  so raw `Uri` upload needed its own home) uploading to Storage `avatars/{uid}.jpg`, using the
  modern Photo Picker (no storage permission needed). `FirebaseModule` gained
  `provideFirebaseStorage()`, mirroring the existing Auth/Firestore emulator-wiring pattern.
  `core:designsystem` gained a reusable `Avatar` component.
  **This agent hit a Claude session rate limit mid-task** (terminated right after pushing its
  commit, before opening the PR) — recovered by verifying its already-pushed, already-committed
  work directly (`./gradlew build` + targeted `ktlintCheck`/`detekt` on the touched modules, all
  clean) and creating the PR by hand from its already-written body. No agent work was lost.

All three PRs are independently mergeable; #24 and #25 both stack on #22
(`chore/local-firebase-emulator-dev-setup`) since they both needed its emulator-wiring plumbing
(Storage's `provideFirebaseStorage()` follows the same `BuildConfig.USE_FIREBASE_EMULATOR`
pattern #22 introduced for Auth/Firestore). **Suggested merge order**: #22 first, then #24 and
#25 (rebase/retarget to `master` once #22 lands — both should go cleanly since neither touches
#22's own files), then #23 (independent of the other three throughout).

**Status after this batch**: T-11's gap is fixed, T-16 and T-19 are done. **Only T-13 (cover
image pipeline) and T-39 (shelf-level edit entry point, added 2026-09-28 — see
`03-PHASES-AND-TASKS.md`) remain unstarted in Phase 1**, plus the still-open items in "Known
issues to fix" below (no app theme, tab-state restoration unconfirmed, `connectedDebugAndroidTest`
never run for real).

**Real CI regression found and fixed the same day**: #22 (and by inheritance #24/#25, both
stacked on it) failed the **"Firestore rules tests"** job — a genuinely new failure, not the
known emulator flake. Root cause: `npm install` run locally on Windows/npm 11 during this
session's local-emulator setup left `firebase/package-lock.json` with a lockfile entry for
`tinyglobby`'s optional `picomatch@4.0.7` peer dependency that was never actually installed
locally (platform-gated optional dependency). CI's `npm ci` (Linux, npm 10.8.2 via
`actions/setup-node`) treats that as lockfile drift and hard-fails: `npm error Missing:
picomatch@4.0.7 from lock file`. Fixed with `firebase/.npmrc` (`omit=optional` — nothing in
that directory's own scripts needs `tinyglobby`) and a regenerated lockfile; verified against a
real local Firestore emulator (`npm test`, 25/25 passing) before pushing. Confirmed green on
real CI afterward on all three affected PRs. **#23 was never affected** — it targets `master`
directly and never inherited the broken lockfile.

**Current CI state, all four PRs (2026-09-28)**: ktlint+detekt, Unit tests, Assemble debug,
Firestore rules tests all green on #22/#23/#24/#25. Only "Compose UI tests (emulator API 34)"
is red/pending everywhere — confirmed via its own log (`could not connect to TCP port 5554`,
`Timeout waiting for emulator to boot`, on a `Users/runner/Library/...` macOS runner) to be the
same pre-existing, code-unrelated GitHub-hosted-runner flake documented since T-05.

**Process note for next time**: don't manually edit a subagent's worktree while `ListAgents`
still shows it `running`, even after a `<task-notification status="completed">` arrives — that
notification fires every time the agent's *turn* ends, not when the agent itself is done (a
single task can span several turns while it waits on its own background shell commands). This
session hit exactly that: after two "completed" notifications for the T-11 agent, a `SendMessage`
attempt failed with "worktree could not be verified," which looked like the agent was dead —
manually fixing lint errors directly in its worktree seemed reasonable at that point, but the
agent was still alive and doing the same fixes concurrently, and it detected the drift, discarded
the manual edits via `git checkout --`, and proceeded correctly on its own. No harm done here,
but the reliable signal is `ListAgents`' `running`/`idle`/`stopped` status, not the presence or
absence of a task notification, and a `SendMessage` failure to a `running` agent is worth
retrying rather than treated as proof it's gone.

## Known gotchas from this session (2026-08-24 to 2026-08-26)

Most of this is now folded into `CLAUDE.md`'s "Known gotchas" section and the comments inside
`config/detekt/detekt.yml`/`.editorconfig` — this is the fuller story for context.

**Nothing in this repo had ever been compiled before 2026-08-25.** Every task from T-01
onward was written against a sandbox with no JDK/Gradle/Android SDK, verified only by careful
reading. The first time any of it was actually run (via a from-scratch GitHub Actions CI
setup) turned up a cascade of real, previously-undiscovered bugs — a `build-logic` compile
error affecting every module, a systemic Gradle version-catalog accessor bug, a missing
Gradle wrapper, an unavailable Compose API, and ktlint/detekt configs that had never once run
clean. All are fixed now, but the pattern is worth remembering: **written and verified are not
the same claim for anything in this repo's early history** — if something behaves
unexpectedly, check whether it was ever actually compiled before assuming the logic is wrong.
(The 2026-09-28 findings above are a second instance of the same pattern, one layer up: CI-green
and "a human looked at it" are not the same claim either.)

**The systemic accessor bug** (now documented in `CLAUDE.md`): type-safe multi-segment
`libs.foo.bar` *library* accessors don't resolve anywhere in the root build, while
`libs.plugins.foo.bar` (via `alias()`) and the same pattern inside `build-logic`'s own
included build work fine. Root cause was never fully identified — plugin-accessor and
library-accessor generation are separate Gradle mechanisms, and whatever's broken is scoped to
the root build's library-accessor class specifically. The workaround
(`libs.findLibrary("alias").get()`) is applied consistently throughout the repo. **If you hit
"Unresolved reference" on a `libs.` accessor that looks syntactically fine, this is almost
certainly why** — check whether the same alias resolves via `findLibrary`/`findPlugin` before
assuming the catalog entry itself is wrong.

**GitHub Actions stopped dispatching new runs for a stretch of this session** — pushes,
closes/reopens, and even a brand-new PR all failed to create a new workflow run or even a
check-suite, for reasons never conclusively identified (billing was the leading theory,
disproven when the repo went public and the issue persisted; it self-resolved after a manual
"Re-run all jobs" click from the Actions tab in the browser, after which both the UI and
`gh workflow run` started working normally again). If this happens again: try
`gh workflow run <name> --ref <branch>` (needs a `workflow_dispatch:` trigger in the workflow
file — already present in `ci.yml` for this reason) first; if that also fails, a manual re-run
from the Actions tab in-browser is what unstuck it last time.

**Branch topology**: `master` and `feat/T-01-repo-scaffold` diverged after PR #1 (T-01's own
merge) and were reconciled by PR #10, "Phase 0 complete" (2026-08-26). Every task since T-07
has merged straight to `master`. A local `master` branch can go stale for weeks without anyone
noticing if nothing pulls it — `git fetch && git rev-list --left-right --count
master...origin/master` before trusting a local checkout's history.

**Repo-level branch protection blocks force-push and branch deletion** on every branch
(likely enabled when the repo went public). A handful of disposable `tmp/*-verify*` branches
used for one-off CI verification during the Phase 0 session couldn't be cleaned up as a
result — they're harmless and unreferenced by any open PR; delete them manually via GitHub if
it bothers you. The same is true of five `.claude/worktrees/agent-*` / `worktree-*` local
worktrees left over from parallel-agent sessions — all confirmed fully merged into `master`,
safe to `git worktree remove` whenever convenient.
