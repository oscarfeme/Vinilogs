# Firebase setup (T-04)

Two cloud projects — `vinilogs-dev` and `vinilogs-prod` — plus local Emulator Suite config
for day-to-day development. Auth, Firestore and Storage are the only products used (no FCM —
see `00-README.md`).

**What this task did *not* do:** create the actual Firebase projects. That requires a Google
account and either console access or an authenticated `firebase`/`gcloud` CLI, neither of
which is available in the sandbox this was written in. Everything below is config and
scaffolding, ready for whoever has that access to run through once.

## One-time: create the two projects

1. In the [Firebase console](https://console.firebase.google.com), create two projects:
   `vinilogs-dev` and `vinilogs-prod` (or update `.firebaserc` here if you pick different IDs —
   keep the `dev` / `prod` aliases pointing at them).
2. In each project, enable:
   - **Authentication** → Sign-in method → Email/Password.
   - **Firestore Database** → create in production mode (rules are deny-by-default here until
     T-14 anyway), same region for both.
   - **Storage** → create a default bucket.
3. Register an Android app in each project with package name `app.vinilogs` (see
   `AndroidApplicationConventionPlugin.kt` for the applicationId).
4. Download each project's `google-services.json`. For local development, save the **dev**
   project's file as `app/google-services.json` — it's gitignored, never commit it. `app/
   google-services.json.example` shows the expected shape. The **prod** file is only needed by
   the release pipeline (T-37), not for day-to-day work.
5. `npm install -g firebase-tools`, then `firebase login` and confirm `firebase projects:list`
   shows both projects with the aliases in `.firebaserc` (`firebase use dev` / `firebase use
   prod` to switch).

Until step 4 is done, the app still builds: `app/build.gradle.kts` only applies the
`google-services` plugin when `app/google-services.json` exists.

## Day-to-day: run the emulators

No real project needed for this — the emulators run entirely locally.

```
cd firebase
firebase emulators:start
```

This starts Auth (`:9099`), Firestore (`:8080`), Storage (`:9199`) and the Emulator UI
(`:4000`), using `firestore.rules` / `storage.rules` from this directory.

**Pointing the app at the emulators (Auth + Firestore) is already wired** — set two keys in
the repo-root `local.properties` (gitignored, not committed):

```
firebase.useEmulator=true
firebase.emulatorHost=10.0.2.2
```

`10.0.2.2` is the AVD's alias for the host machine; use `localhost` instead for a physical
device reached via `adb reverse tcp:8080 tcp:8080` / `adb reverse tcp:9099 tcp:9099` (and
`tcp:9199` once Storage is wired — see below). `core/data/build.gradle.kts` reads these into
`BuildConfig.USE_FIREBASE_EMULATOR`/`FIREBASE_EMULATOR_HOST`; `core/data/.../di/
FirebaseModule.kt` calls `.useEmulator(...)` on `FirebaseAuth`/`FirebaseFirestore` when that
flag is set **and** the build is debuggable — a release build can never reach a local
emulator even if the flag is left set by mistake. **Storage isn't wired yet** — that's T-13.

You also need `app/google-services.json` to exist at all (gitignored, the build skips
`google-services` entirely without it) — for local emulator work it doesn't need to be real; a
`demo-`prefixed project ID (e.g. `demo-vinilogs`) is Firebase's own convention for "never
attempts a real network call", and matches an emulator suite started with
`firebase emulators:start --project demo-vinilogs`. `app/google-services.json.example` shows
the shape.

One more thing Android needs and easy to miss: it blocks all cleartext (plain HTTP) traffic by
default at targetSdk 28+, and the emulator SDKs talk plain HTTP, not HTTPS. Without a network
security config permitting cleartext to the emulator host, every call fails with `Cleartext
HTTP traffic to <host> not permitted`. `app/src/debug/res/xml/network_security_config_debug.xml`
(+ `app/src/debug/AndroidManifest.xml` wiring it in) already handles this, scoped to
`10.0.2.2`/`localhost`/`127.0.0.1` and to debug builds only.

## Seed data

Either let `emulators:exec` start-run-teardown the emulators for you:

```
cd firebase
firebase emulators:exec --project demo-vinilogs "cd seed && npm install && npm run seed"
```

...or, if the emulators are already running (e.g. you want the app to keep talking to them
afterward, which `emulators:exec`'s teardown would break), run the seed script directly against
them instead:

```
cd firebase/seed && npm install
FIRESTORE_EMULATOR_HOST=localhost:8080 FIREBASE_AUTH_EMULATOR_HOST=localhost:9099 \
  GCLOUD_PROJECT=demo-vinilogs node seed.js
```

Use whichever project ID matches what `app/google-services.json` declares (`demo-vinilogs` if
you're following this README's local-dev convention above; `vinilogs-dev` if you're pointed at
a real dev project instead). Creates three Auth users and their `users/{uid}` profile +
`users/{uid}/records` documents per the data model in `02-ARCHITECTURE.md` §3 (see
`seed/seed.js` for exact records). All seed accounts use password `password123`.

Note: the seeded users with `isPublic: true` won't show up in discovery until T-20's
`onRecordWritten`/`onProfileUpdated` functions exist to build the `publicRecords` projection —
this seed only writes the owner-side `records`, matching what's actually implemented so far.

## What's deliberately not here yet

- **Cloud Functions** (`onRecordWritten`, `onProfileUpdated`, `onAccountDeleted`) — T-20. A
  `functions/` directory and the `functions` block in `firebase.json` land with that task.
- **Storage security rules** — `storage.rules` is still a deny-by-default placeholder; T-13
  (cover uploads) and T-19 (avatar uploads) replace it. `firestore.rules` got its real rules
  in T-14 (see `tests/firestore.rules.test.js` for the unit test suite — run with `npm test`
  from this directory, which drives `firebase emulators:exec` via the devDependencies in
  `package.json`, no global `firebase-tools` install needed). Do not loosen either file
  without a test proving it can't leak a private field. T-21 (Phase 2) still owes rules for
  `users/{uid}/publicRecords` and `reports/{reportId}` — both are deny-by-default in
  `firestore.rules` until then.
- **Composite indexes** in `firestore.indexes.json` cover the compound queries named in
  `02-ARCHITECTURE.md` §3 (`(isPublic, displayNameLower)` and `(format, year)`). The
  single-field ones listed there (`rating`, `createdAt`, `artistLower`) aren't included —
  Firestore indexes every field by default, so those are automatic. Revisit if T-10/T-11/T-22
  need something Firestore's automatic indexing can't satisfy.
- **Production deploy** (`firebase deploy --only firestore:rules,firestore:indexes,storage`
  against `prod`) — T-36.
