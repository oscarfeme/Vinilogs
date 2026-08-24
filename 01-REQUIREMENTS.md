# Vinilogs — Requirements

## 1. Scope

**In scope (v1.0):** account management, personal vinyl catalogue, collection statistics,
CSV export, browsing other users' public collections, sharing a record out through the
Android share sheet.

**Out of scope (v1.0):** in-app chat or messaging of any kind, push notifications, group
features, buying/selling with payments, voice/video, web client, barcode scanning, Discogs
account sync, offline sync conflict resolution beyond last-write-wins. Record these as
backlog, do not build them, and do not leave hooks for them.

## 2. Actors

| Actor | Description |
|---|---|
| Guest | Unauthenticated visitor. Can only see the auth screens. |
| Collector | Authenticated user. Full access to own collection and discovery. |
| Moderator | Reviews reported profiles via the Firebase console. No in-app UI in v1.0. |

## 3. Functional requirements

Each requirement has an ID. Tasks in `03-PHASES-AND-TASKS.md` reference these IDs.

### FR-A — Authentication and profile

| ID | Requirement | Acceptance criteria |
|---|---|---|
| FR-A1 | Sign up with email + password | Password ≥ 8 chars; duplicate email shows inline error; account created in Firebase Auth and a `users/{uid}` profile document written |
| FR-A2 | Sign in / sign out | Session persists across app restarts; sign out clears local cache |
| FR-A3 | Password reset by email | Reset email sent; generic confirmation shown regardless of whether the address exists |
| FR-A4 | Edit profile | Display name, avatar image, short bio, location (city, free text) |
| FR-A5 | Collection privacy toggle | Public (discoverable) or private (hidden from search and profile views); default public; changing to private removes the user from search results immediately |
| FR-A6 | Delete account | Removes profile, collection, avatar and reports authored; irreversible, requires typed confirmation |

### FR-B — Collection management

The core of the product. Every requirement here must work with no network except FR-B1.

| ID | Requirement | Acceptance criteria |
|---|---|---|
| FR-B1 | Search an online catalogue by artist/album/catalogue number | Results show cover, artist, title, year, label; paginated; graceful empty, offline and rate-limited states that always offer manual entry as the way forward |
| FR-B2 | Add a record from search results | Prefills metadata; user confirms and it appears on the shelf immediately; all prefilled fields remain editable before and after saving |
| FR-B3 | Add a record manually | Artist and title required; all other fields optional; works fully offline |
| FR-B4 | Record fields | artist, title, year, label, catalogue number, format (LP/EP/7"/10"/12"/box), speed (33/45/78), condition (M/NM/VG+/VG/G/F/P), purchase price, purchase date, personal rating 1–5, notes, cover image, tags |
| FR-B5 | Edit and delete a record | Edits apply offline and sync later; delete asks for confirmation and supports undo for 5 seconds |
| FR-B6 | Shelf view | Grid of covers and list view toggle; shows total record count; grid is the default and the covers are the primary element |
| FR-B7 | Search within own collection | Matches artist, title, label, catalogue number and tags; results update as the user types; works offline |
| FR-B8 | Filter and sort | Filter by format, condition, decade, rating, tag; sort by artist, title, year, date added, rating; active filters visibly indicated and clearable in one tap |
| FR-B9 | Record detail screen | All fields, large cover, edit, delete and share actions |
| FR-B10 | Collection statistics | Total records, total spend, records per decade, top 5 artists, top 5 labels |
| FR-B11 | Offline read and write | Previously loaded collection is fully readable, searchable and editable with no network; changes queue and sync on reconnect; pending state visible but never blocking |
| FR-B12 | Export collection to CSV | One file with one row per record, shared via the Android share sheet |
| FR-B13 | Custom cover image | User can replace or supply a cover from the device gallery or camera, for records with no Discogs art or a wrong match |

### FR-C — Discovery

| ID | Requirement | Acceptance criteria |
|---|---|---|
| FR-C1 | Search users by display name | Prefix match, paginated, private profiles excluded |
| FR-C2 | View a public profile | Avatar, bio, location, record count, public collection grid |
| FR-C3 | View another user's record detail | Read-only version of FR-B9; no edit, delete or price fields shown |
| FR-C4 | Shared records indicator | Profile shows how many records both users have in common, and lets the user see which ones |
| FR-C5 | Share a record externally | Share action on any record detail (own or another user's) produces a text summary plus cover image via the Android share sheet |
| FR-C6 | Report a profile | Any public profile can be reported with a reason; written to a `reports` collection; reporter sees a confirmation and the profile is hidden from their search results afterwards |

> Purchase price and purchase date are **private**. They are never returned by any discovery
> query and never rendered on another user's record. This is a hard rule, enforced in
> security rules, not only in the UI.

## 4. Non-functional requirements

| ID | Requirement | Target |
|---|---|---|
| NFR-1 | Cold start | < 2 s to first meaningful frame on a mid-range device (Pixel 6a class) |
| NFR-2 | Shelf scrolling | 60 fps with 1,000 records; no dropped-frame jank in Macrobenchmark |
| NFR-3 | Offline | Collection browsing, search, add and edit fully functional with no network |
| NFR-4 | Cover loading | Covers cached on disk; a previously viewed shelf renders its art offline |
| NFR-5 | Security | Firestore rules deny all by default; a user can only write their own documents; private fields never readable by another user |
| NFR-6 | Privacy | Private collections never returned by any query; account deletion completes within 30 days per policy |
| NFR-7 | Accessibility | All interactive elements have content descriptions; supports TalkBack, dynamic font scaling up to 200% |
| NFR-8 | Localisation | Strings externalised; ships with English and Spanish |
| NFR-9 | Theming | Light and dark themes, follows system setting |
| NFR-10 | APK size | Release bundle < 20 MB |
| NFR-11 | Test coverage | ≥ 70% line coverage on `core/data` and all ViewModels |
| NFR-12 | Crash-free sessions | ≥ 99.5%, monitored via Firebase Crashlytics |

## 5. Constraints

- Discogs API rate limit: 60 authenticated requests/minute — responses must be cached
  locally for 24 h and search input debounced.
- Firestore free tier during development; the data model must avoid unbounded fan-out reads.
- No paid third-party SDKs.
- The app must be fully usable by a user who has never heard of Discogs.

## 6. Assumptions

- Users have a Google Play–capable device running Android 8.0 or later.
- One collection per user; no multi-shelf organisation in v1.0.
- Discovery is a browsing feature, not a social network: there is no following, no feed and
  no notification of any kind.
