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
(`:4000`), using `firestore.rules` / `storage.rules` from this directory. Point the app at
the emulators the same way the Firebase Android SDKs always do:
`FirebaseFirestore.useEmulator("10.0.2.2", 8080)` etc. from an emulator/debug build (wired up
by whichever task first needs it — T-08 for Auth, T-11 for Firestore, T-13 for Storage).

## Seed data

```
cd firebase
firebase emulators:exec --project vinilogs-dev "cd seed && npm install && npm run seed"
```

Creates three Auth users and their `users/{uid}` profile + `users/{uid}/records` documents per
the data model in `02-ARCHITECTURE.md` §3 (see `seed/seed.js` for exact records). All seed
accounts use password `password123`. `emulators:exec` is important here — it starts the
emulators, runs the script against `FIRESTORE_EMULATOR_HOST`/`FIREBASE_AUTH_EMULATOR_HOST`,
then tears them down; the script refuses to run against anything else.

Note: the seeded users with `isPublic: true` won't show up in discovery until T-20's
`onRecordWritten`/`onProfileUpdated` functions exist to build the `publicRecords` projection —
this seed only writes the owner-side `records`, matching what's actually implemented so far.

## What's deliberately not here yet

- **Cloud Functions** (`onRecordWritten`, `onProfileUpdated`, `onAccountDeleted`) — T-20. A
  `functions/` directory and the `functions` block in `firebase.json` land with that task.
- **Real security rules** — `firestore.rules` and `storage.rules` here are deny-by-default
  placeholders. T-14 (Firestore) and T-13/T-19 (Storage, implicitly) replace them, with rules
  unit tests. Do not loosen either file without a test proving it can't leak a private field.
- **Composite indexes** in `firestore.indexes.json` cover the compound queries named in
  `02-ARCHITECTURE.md` §3 (`(isPublic, displayNameLower)` and `(format, year)`). The
  single-field ones listed there (`rating`, `createdAt`, `artistLower`) aren't included —
  Firestore indexes every field by default, so those are automatic. Revisit if T-10/T-11/T-22
  need something Firestore's automatic indexing can't satisfy.
- **Production deploy** (`firebase deploy --only firestore:rules,firestore:indexes,storage`
  against `prod`) — T-36.
