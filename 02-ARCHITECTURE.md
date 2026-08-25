# Vinilogs — Architecture

## 1. High-level design

```
┌──────────────────────────────────────────────────────────┐
│  UI layer  (Compose screens + ViewModels, per feature)    │
│  auth · collection · discovery                            │
└───────────────────────┬──────────────────────────────────┘
                        │ StateFlow<UiState> ↑ / events ↓
┌───────────────────────┴──────────────────────────────────┐
│  Domain layer  (core:model + use cases where non-trivial) │
│  pure Kotlin, no Android or Firebase imports              │
└───────────────────────┬──────────────────────────────────┘
                        │ repository interfaces
┌───────────────────────┴──────────────────────────────────┐
│  Data layer  (core:data)                                  │
│  AuthRepository · CollectionRepository · UserRepository    │
│                                                           │
│  ┌────────────┐  ┌────────────┐  ┌──────────────┐         │
│  │ Room (SoT) │  │ Firestore  │  │ Discogs API  │         │
│  │ local DB   │  │ remote     │  │ (Retrofit)   │         │
│  └────────────┘  └────────────┘  └──────────────┘         │
└──────────────────────────────────────────────────────────┘
```

**Rules**
- The UI layer never touches Firestore, Room or Retrofit directly.
- Repositories expose `Flow<T>` for reads and `suspend fun` returning `Result<T>` for writes.
- **Room is the single source of truth for the user's own collection.** Firestore listeners
  write into Room; the UI observes Room only. No screen ever waits on a network read to show
  the user their own records.
- Discovery is the one read path that is network-first: another user's shelf is fetched from
  Firestore and cached in memory for the session, not persisted to Room.
- Domain models are separate from Firestore DTOs and Room entities. Mappers live in
  `core:data`.
- Discogs responses are mapped to domain models at the boundary and never stored as Discogs
  DTOs. Once a record is saved it has no dependency on Discogs.

## 2. Data flow examples

**Add a record**
```
AddRecordScreen → AddRecordViewModel.save()
  → CollectionRepository.addRecord(record)
      → Room.insert(entity, syncState = PENDING)   // instant UI update
      → Firestore.set(users/{uid}/records/{id})
      → Room.update(syncState = SYNCED)
```
If the network write fails the row stays `PENDING` and `SyncWorker` (WorkManager,
network-constrained) retries. The user sees their record on the shelf either way; a pending
record is rendered normally with a small, non-blocking sync indicator.

**Catalogue lookup (Discogs)**
```
AddRecordSearchScreen → debounce 400 ms → CollectionRepository.searchCatalog(q, page)
  → DiscogsApi (OkHttp cache, 24 h)
      → success  → CatalogResult list → prefill form → user edits → save (as above)
      → failure  → error state that offers "Add manually" as the primary action
```
Failure here is never terminal. Losing Discogs costs the user typing, nothing else.

**View another collector's shelf**
```
ProfileScreen → ProfileViewModel
  → UserRepository.observeProfile(uid)            // Firestore, live
  → UserRepository.observePublicCollection(uid)   // Firestore, session-cached
  → UserRepository.sharedRecordCount(uid)         // computed locally against Room
```
Shared-record counting is done on-device by comparing the fetched public collection against
the local Room collection — no server-side join, no extra reads.

## 3. Firestore data model

```
users/{uid}
  displayName: string
  displayNameLower: string        // for prefix search
  avatarUrl: string?
  bio: string?
  location: string?
  isPublic: boolean
  recordCount: number             // denormalised counter
  createdAt: timestamp

users/{uid}/records/{recordId}
  artist, title: string
  artistLower, titleLower: string
  year: number?
  label, catalogNumber: string?
  format: enum   // LP | EP | SEVEN | TEN | TWELVE | BOX
  speed: enum    // RPM33 | RPM45 | RPM78
  condition: enum
  purchasePrice: number?        // PRIVATE — never exposed to another user
  purchaseDate: timestamp?      // PRIVATE — never exposed to another user
  rating: number?               // 1..5, PRIVATE
  notes: string?                // PRIVATE
  coverUrl: string?
  discogsId: number?            // null when manually entered
  tags: string[]
  createdAt, updatedAt: timestamp

reports/{reportId}
  reporterUid, reportedUid: string
  reason: string
  createdAt: timestamp
```

**Private fields.** `purchasePrice`, `purchaseDate`, `rating` and `notes` are owner-only.
Firestore security rules cannot filter fields on a document read, so discovery does **not**
read `users/{uid}/records` directly for another user. Instead:

- `onRecordWritten` maintains a projection at `users/{uid}/publicRecords/{recordId}`
  containing only the shareable fields (artist, title, year, label, catalogNumber, format,
  speed, condition, coverUrl, tags).
- The projection is written only while `isPublic == true`, and deleted for the whole
  collection when a user switches to private.
- Discovery reads `publicRecords`. Rules deny all reads on `records` except by the owner.

This costs one extra write per record change and removes an entire class of privacy bug. It
is not optional.

**Indexes required:** `users` on `(isPublic, displayNameLower)`; `records` on
`(format, year)`, `(rating)`, `(createdAt)`; `publicRecords` on `(artistLower)`.

## 4. Repository contracts

Fixed interfaces — implement against these so feature agents can work in parallel with fakes.

```kotlin
interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun signUp(email: String, password: String, displayName: String): Result<User>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun signOut()
    suspend fun deleteAccount(): Result<Unit>
}

interface CollectionRepository {
    fun observeCollection(filter: CollectionFilter, sort: CollectionSort): Flow<List<Record>>
    fun observeRecord(id: String): Flow<Record?>
    fun observeStats(): Flow<CollectionStats>
    fun observeSyncState(): Flow<SyncState>
    suspend fun addRecord(record: Record): Result<String>
    suspend fun updateRecord(record: Record): Result<Unit>
    suspend fun deleteRecord(id: String): Result<Unit>
    suspend fun setCoverImage(recordId: String, source: Uri): Result<Unit>
    suspend fun searchCatalog(query: String, page: Int): Result<List<CatalogResult>>
    suspend fun exportCsv(): Result<Uri>
}

interface UserRepository {
    fun observeProfile(uid: String): Flow<UserProfile?>
    fun observePublicCollection(uid: String): Flow<List<PublicRecord>>
    suspend fun searchUsers(query: String, page: Int): Result<List<UserProfile>>
    suspend fun updateProfile(update: ProfileUpdate): Result<Unit>
    suspend fun sharedRecords(otherUid: String): Result<List<Record>>
    suspend fun report(reportedUid: String, reason: String): Result<Unit>
}
```

`Record` is the owner's full model. `PublicRecord` is the projection — a separate type, so
it is structurally impossible for a discovery screen to render a private field.

Every screen exposes a single immutable state class:

```kotlin
data class ShelfUiState(
    val records: List<Record> = emptyList(),
    val isLoading: Boolean = true,
    val query: String = "",
    val filter: CollectionFilter = CollectionFilter(),
    val sort: CollectionSort = CollectionSort.DATE_ADDED,
    val layout: ShelfLayout = ShelfLayout.GRID,
    val pendingSyncCount: Int = 0,
    val error: UiText? = null,
)
```

## 5. Navigation graph

```
auth (start if signed out)
 ├── signIn → signUp → forgotPassword
main (start if signed in, bottom bar: Shelf · Discover · Profile)
 ├── shelf ──→ recordDetail/{id} ──→ editRecord/{id}
 │        ├──→ addRecord (search | manual)
 │        └──→ stats
 ├── discover ──→ profile/{uid} ──→ publicRecord/{uid}/{id}
 │                            └──→ sharedRecords/{uid}
 └── profile ──→ editProfile → settings
```

Three bottom-bar destinations, not four. No deep links in v1.0 — nothing external opens the
app.

## 6. Design system notes

`core:designsystem` is not a dumping ground for defaults. It carries the app's character and
is built once, in T-02, before any feature screen. See `05-DESIGN-DIRECTION.md` for the full,
locked visual spec (colour ramp, type scale, spacing/shape tokens, component specs,
accessibility floor) — this section states the principles the doc implements.

- **Covers first.** `VinylCard` renders sleeve art at a true 1:1 aspect ratio with a subtle
  edge treatment. Missing art gets a generated placeholder derived from the artist name — never
  a grey box with an icon.
- **Grid density** adapts to screen width: 2 columns on compact, 3 on medium and up.
- **Empty states are written, not decorated.** Each one names the next action ("Your shelf is
  empty. Add your first record.") with the action as a button.
- **Motion is short.** Shared-element transition from shelf to record detail; nothing over
  250 ms; all animation respects the system reduce-motion setting.
- **Haptics** on destructive confirmation and on successful record add. Nowhere else.
- Colour, typography and spacing tokens live here and nowhere else. No feature module
  declares a raw `Color` or `dp` value for anything reusable.

## 7. Key decisions (ADR summaries)

**ADR-1 — Firebase over a custom backend.** Chosen for auth, hosted storage and offline
persistence with no server to operate, on a free tier that covers v1.0 comfortably.
Trade-off: vendor lock-in and query limitations (no full-text search, no joins). All Firebase
access is behind repository interfaces so a swap is contained in `core:data`. See
`04-ALT-BACKEND.md` for the costed contingency.

**ADR-2 — Room as source of truth for the collection.** Guarantees instant, offline-capable
shelf rendering, offline writes, and stable filtering/sorting in SQL rather than in memory.
Trade-off: sync code to maintain (`SyncWorker`, `syncState` column). Accepted — offline is
the product's main differentiator, not a nicety.

**ADR-3 — Discogs as a lookup service, not a sync source.** Records are copied into the
user's own collection at add time and never re-fetched. Trade-off: metadata can go stale
relative to Discogs, and no "import my Discogs collection" feature. Accepted deliberately:
it removes the account requirement, the rate-limit dependency on every screen, and the
offline failure mode that makes competing apps unusable in a shop.

**ADR-4 — Public projection collection for discovery.** Discovery reads a server-maintained
`publicRecords` projection rather than the owner's `records`. Trade-off: one extra write per
record change and a Cloud Function to maintain. Accepted: Firestore rules cannot hide fields
within a document, so any alternative leaks purchase price to anyone who can call the SDK.

**ADR-5 — Multi-module by feature.** Enables parallel agent work with minimal merge conflicts
and faster incremental builds. Trade-off: more Gradle configuration upfront, absorbed by
convention plugins in `build-logic`.

**ADR-6 — No in-app messaging in v1.0.** Sharing goes out through the Android share sheet.
Trade-off: no owned communication channel and no engagement loop. Accepted: chat represented
roughly half the original build and all of the moderation burden, in exchange for an empty
inbox at launch against WhatsApp. Nothing in the data model, navigation graph or repository
contracts anticipates its return.

## 8. Cloud Functions (Node 20)

| Function | Trigger | Purpose |
|---|---|---|
| `onRecordWritten` | Firestore write on `records` | Maintain `users/{uid}.recordCount` and the `publicRecords` projection |
| `onProfileUpdated` | Firestore update on `users` | On `isPublic` change, build or tear down the whole `publicRecords` projection |
| `onAccountDeleted` | Auth delete | Cascade-delete records, projection, reports and storage objects |

Three functions, all data-integrity. No messaging, no FCM.

## 9. Quality gates

Every PR runs: `ktlint` + `detekt`, `./gradlew testDebugUnitTest`, Firestore rules unit
tests, and Compose UI tests on an emulator (API 34). Release builds additionally run
Macrobenchmark for NFR-2 and a bundle-size check for NFR-10.

One manual gate, every release: **install the build, enable aeroplane mode, and add, edit,
search and delete a record.** If any of that fails, the release does not ship.
