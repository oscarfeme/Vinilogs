# Progress log

Status tracker, not a spec — the numbered `0X-*.md` docs are the fixed plan; this file is
where things actually stand. Update it as tasks land. Superseded entries can be trimmed once
they're no longer useful context, but don't delete the "known gotchas" material below without
folding it into `CLAUDE.md` first.

## Immediate next step: AGP 9 / Gradle 9 migration (do this before anything else)

**CI is currently red on `feat/T-07-testing-fakes`** (and on the shared base branch
`feat/T-01-repo-scaffold`) for a known, understood reason — read this before running
step 2 of the checklist below, since it will fail as-is.

**What happened**: `02-ARCHITECTURE.md`'s ADR-7 (Kotlin 2.0.21 → 2.3.21, forced by a
Firebase Auth metadata incompatibility — read the full ADR before touching any of these
pins again) cascaded through four passes of dependent version fixes — build-logic
classpath, a KSP pin, a Hilt bump — and the fourth pass hit a hard wall: **Hilt
2.60.1's Gradle plugin refuses to apply on anything below AGP 9.0.0**
(`"The Hilt Android Gradle plugin is only compatible with Android Gradle plugin (AGP)
version 9.0.0 or higher (found Android Gradle Plugin version 8.6.1)"`), and this
project is pinned to AGP 8.6.1. Research (google/dagger#5083, #4944) suggests Hilt's
KSP2 support and the AGP 9 requirement shipped together — no older Hilt release is
expected to give KSP2 support on AGP 8.x.

**Decision (made with the user 2026-08-26, once real Android tooling was installed on
this machine)**: do the full cascade — bump AGP to 9.0+ and the Gradle wrapper to
9.x — rather than revert the Kotlin/KSP/Hilt bumps. Do it locally, now that real
tooling exists, instead of continuing to iterate blind through ~2-minute CI-only
round-trips (which is how passes 1–4 above were done, in a sandbox with no JDK/SDK).

**Research already done, so the next session doesn't have to redo it** (all verified
via WebFetch of `developer.android.com/build/releases/agp-9-0-0-release-notes` and
KSP/Dagger GitHub release notes, not guessed):

- Target **AGP 9.0.1** (latest as of this writing, Jan 2026) — needs **Gradle 9.1.0**
  minimum/default (not just "any Gradle 9.x"). SDK Build Tools minimum 36.0.0. JDK
  minimum 17 (already satisfied).
- **`android.builtInKotlin` defaults to `true` in AGP 9.0** and is incompatible with
  KSP (KSP requires the classic `org.jetbrains.kotlin.android` plugin) — this project
  uses KSP for Hilt annotation processing, so **set `android.builtInKotlin=false`
  explicitly in `gradle.properties`** as part of this migration, or every KSP-consuming
  module breaks in a new way. Don't skip this — it's not optional given this project's
  architecture.
- **`targetSdk` is removed from the library variant DSL entirely in AGP 9** (not just
  deprecated — this project already has the deprecation warning surfacing in CI logs:
  `AndroidLibraryConventionPlugin.kt:18:21 'targetSdk: Int?' is deprecated`). Fix this
  as part of the same change — move it to `testOptions.targetSdk`/`lint.targetSdk` per
  the warning text, or drop it if it's not actually needed there.
- AGP 9 also removes legacy `BaseExtension`/variant APIs (e.g. `applicationVariants`)
  in favor of the `androidComponents`/`androidComponents.onVariants` API — grep
  `build-logic/convention/src/main/kotlin/*.kt` for any such legacy usage before
  assuming this migration is DSL-pin-only.
- Once AGP is actually on 9.0+, `ksp` can likely go back to the newest 2.3.x (the
  `2.3.0` pin from ADR-7 pass 2 was specifically to avoid an AGP-9-only API call that
  now legitimately exists) — not required, but worth revisiting as cleanup.
- There's a temporary escape hatch, `android.newDsl=false` in `gradle.properties`, if
  the new-DSL-only requirement proves too disruptive to land in one pass — but it's
  documented as removed in AGP 10 (mid-2026), so treat it as a bridge, not a fix.

**How to execute, now that real tooling exists on this machine**:
1. Confirm tooling is actually on `PATH` after the restart: `java -version`,
   `./gradlew --version` (this session's shell couldn't see the newly-installed JDK/SDK
   yet — that's expected, it needs the restart the user is about to do).
2. Work on `feat/T-01-repo-scaffold` (the shared base branch all these version pins
   live on) — bump `agp` in `gradle/libs.versions.toml`, the Gradle wrapper in
   `gradle/wrapper/gradle-wrapper.properties`, add `android.builtInKotlin=false` to
   `gradle.properties`, fix the `targetSdk` library-DSL usage.
3. Iterate locally with `./gradlew build` — fast feedback now, unlike the CI-only loop
   used for passes 1–4. Fix whatever AGP 9's new-DSL-only requirement surfaces.
4. Only once a local build is green: push, merge into `feat/T-07-testing-fakes`
   (currently at `62e09cc`; base branch is at `3710963`, both already on `origin`), and
   re-dispatch CI (`gh workflow run CI --ref feat/T-07-testing-fakes`) to confirm all 4
   jobs pass before considering T-07 (PR #11) done.
5. Record the outcome as a continuation of ADR-7 in `02-ARCHITECTURE.md` (pass 5),
   same pattern as passes 1–4 — what broke, what fixed it, why.

One session-sandbox oddity, probably irrelevant on a real machine but worth knowing if
it recurs: mid-session, `git checkout <branch>` in this repo's primary working
directory started failing with `fatal: unable to update HEAD` / `Permission denied` on
`.git/config`, even for a no-op re-set to the branch already checked out — while
ordinary working-tree writes, commits, and pushes all worked fine. Worked around by
doing the branch-specific edits in a separate `git worktree` instead of switching HEAD
in place. No data was lost (each incident was caught and recovered with
`git reset --hard HEAD` before anything was pushed), but if a fresh session hits the
same "unable to update HEAD" error, don't fight it — reach for `git worktree add
<path> <branch>` instead of `git checkout <branch>`.

## First things to do on a machine with real tooling

Everything in this repo up to 2026-08-26 was built and verified entirely through GitHub
Actions CI, in a sandbox with **no JDK, Android SDK, or emulator**. Nothing has ever been run
by a human, on a device, or in Android Studio. If you're picking this up somewhere that has
real tooling, do this before starting any new task:

1. `git clone`, open in Android Studio, let it sync. First real signal on whether the module
   graph and Gradle config actually work outside GitHub's runners.
2. `./gradlew build` — full build, every module.
3. `./gradlew testDebugUnitTest test` — unit tests.
4. `./gradlew ktlintCheck detekt` — lint. Should already be clean; if it isn't, something
   about the local environment differs from CI (Gradle/JDK version, most likely) and is worth
   tracking down before writing more code on top of it.
5. Run the app on an emulator or device. Confirm:
   - It launches to the sign-in stub screen (auth-state routing is hardcoded to always start
     signed-out — see T-03 notes below, this is expected until T-09).
   - Every stubbed destination is reachable and doesn't crash.
   - Switching bottom-bar tabs after navigating into a sub-screen restores each tab's own
     position (the nav graph uses `saveState`/`restoreState` — never actually confirmed to
     work).
   - The bottom bar is hidden on the auth graph, visible on all three main-graph tabs.
6. `./gradlew connectedDebugAndroidTest` — the Compose UI instrumented tests, for real, on a
   real emulator. CI's version of this job has a persistent GitHub-hosted-macOS-runner flake
   (`HVF error: HV_UNSUPPORTED`) that has nothing to do with the code — running it locally is
   the first real signal on whether these tests actually pass.

None of the above is expected to fail. But "CI is green" and "actually works" have not yet
been the same claim for this project, and it's worth spending the twenty minutes to make them
the same claim before trusting the green checkmarks further.

## Where things stand

**Phase 0 (T-01–T-06) is code-complete.** All six tasks, plus a design-direction
reconciliation (see below), are merged into `feat/T-01-repo-scaffold`, which in turn has a PR
open (or merged, if you're reading this after — check `git log master` for a merge commit
titled "Phase 0 complete") into `master`. If that PR is still open, `master` is stale — pull
`feat/T-01-repo-scaffold` instead, or check whether the PR merged since this was written.

| Task | What it delivered | What's deliberately deferred |
|---|---|---|
| T-01 | Repo scaffold, Gradle version catalog, build-logic convention plugins, empty module skeletons | — |
| T-02 | Original design system (theme, typography, components) | Superseded by the design-direction reconciliation below — its placeholder amber palette was explicitly commented "swap freely" |
| T-03 | Single-activity host, type-safe Navigation Compose graph, three-tab bottom bar, 15 stubbed routes | Auth-state routing (hardcoded to signed-out; T-09's job). Bottom bar icons are generic placeholders. No custom launcher icon/theme. Settings screen ownership is ambiguous in the task doc — flagged, not resolved. **Never run on a device** — see checklist above. |
| T-04 | Firebase SDK wiring (Auth/Firestore/Storage), Emulator Suite config, seed script, `firebase/README.md` runbook | **The actual `vinilogs-dev`/`vinilogs-prod` Firebase projects do not exist.** Needs a human with Firebase console access — see `firebase/README.md` for the exact steps. Real security rules are T-14's job (current ones are deny-by-default placeholders). |
| T-05 | GitHub Actions CI: ktlint, detekt, unit tests, assemble, Firestore rules tests, Compose UI tests on an emulator. Project-specific lint config (`config/detekt/detekt.yml`, `.editorconfig`) tuned against real Compose code | The emulator test job has a recurring GitHub-hosted-runner flake (`HV_UNSUPPORTED`), retried once automatically; not always enough |
| T-06 | Domain models in `core:model` (`Record`, `PublicRecord`, `User`, `UserProfile`, `CatalogResult`, enums, `CollectionFilter`, `CollectionSort`, `SyncState`) | **`AuthRepository`/`CollectionRepository`/`UserRepository` interfaces are not implemented anywhere yet** — deliberately scoped out of T-06 (models only), not yet claimed by T-07 either. Whoever starts T-07 should resolve this first. |
| Design-direction reconciliation | Replaced T-02's placeholder palette with the locked monochrome system (`05-DESIGN-DIRECTION.md`); rewrote `CoverPlaceholder` to show the catalogue number instead of a per-artist colour; fixed `ShelfGrid`'s column logic to match the locked spec | No screen renders `VinilogsTheme` yet — nothing to visually check until Phase 1 builds real screens |

**Next task**: T-07 (`core:testing`: fake repositories for all three contracts, seeded with a
~200-record fixture, coroutine test rule, Compose test helpers). Depends on T-06 (done). Per
`03-PHASES-AND-TASKS.md`, T-07 is Phase 0's last task — after it lands, Tracks C/D/E (the
actual feature screens) unlock to work in parallel.

**Note on T-07's real starting blocker**: T-07 needs to fake `AuthRepository`,
`CollectionRepository`, `UserRepository` — none of which exist as actual interfaces yet (see
the T-06 row above). Whoever starts T-07 needs to either write those three interfaces first
(they're fully specified in `02-ARCHITECTURE.md` §4, just not yet typed into the repo) or
treat writing them as part of T-07's own scope.

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
merge) and were never reconciled until the "Phase 0 complete" PR mentioned above. If you're
starting fresh work and `git log master` doesn't show T-02 or later, that PR hasn't merged —
branch from `feat/T-01-repo-scaffold` instead, or better, get that PR merged first.

**Repo-level branch protection blocks force-push and branch deletion** on every branch
(likely enabled when the repo went public). A handful of disposable `tmp/*-verify*` branches
used for one-off CI verification during this session couldn't be cleaned up as a result —
they're harmless and unreferenced by any open PR; delete them manually via GitHub if it
bothers you.
