package app.vinilogs.core.data.repository

import android.net.Uri
import app.vinilogs.core.data.di.ApplicationScope
import app.vinilogs.core.data.local.RecordLocalDataSource
import app.vinilogs.core.data.remote.discogs.DiscogsCatalogClient
import app.vinilogs.core.model.CatalogResult
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.core.model.CollectionStats
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.SyncState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

/**
 * [CollectionRepository] with Room as the source of truth (ADR-2) and Firestore as a
 * best-effort sync target (T-11). Every read comes straight from Room -- no screen ever waits
 * on a network read to show the user their own collection (02-ARCHITECTURE.md §1).
 *
 * Writes follow the flow documented in 02-ARCHITECTURE.md §2 ("Add a record"): Room insert with
 * `syncState = PENDING` first (instant UI update), then a best-effort Firestore write that
 * flips the row to `SYNCED` on success. On failure the row simply stays `PENDING` -- the same
 * doc's words are "If the network write fails the row stays PENDING and SyncWorker (WorkManager,
 * network-constrained) retries." **`SyncWorker` itself is not implemented in this change** --
 * see this task's PR description for why and what's needed to add it. Without it, a PENDING row
 * only gets retried on the next `addRecord`/`updateRecord` call for that same record, or once
 * `SyncWorker` lands.
 *
 * The other half of ADR-2 ("Firestore listeners write into Room") is the inbound sync wired up
 * in [init]: whenever [firebaseAuth] reports a signed-in user, this attaches a live listener to
 * that user's `users/{uid}/records` and mirrors every change into Room via [applyRemoteChange].
 * `flatMapLatest` on the auth-state uid means signing out (or switching accounts) detaches the
 * previous listener before anything new attaches, so nothing leaks or cross-contaminates between
 * accounts. See [applyRemoteChange] for how this avoids clobbering an in-flight local write.
 *
 * `internal`: `RepositoryModule` (same module) is the only place that names this type directly --
 * everywhere else depends on the public [CollectionRepository] interface. This became
 * load-bearing once the constructor started taking the also-internal `DiscogsCatalogClient`
 * (T-12): Kotlin disallows a public class exposing an internal type in a public constructor
 * signature.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class RoomCollectionRepository
    @Inject
    constructor(
        private val localDataSource: RecordLocalDataSource,
        private val firestore: FirebaseFirestore,
        private val firebaseAuth: FirebaseAuth,
        private val discogsCatalogClient: DiscogsCatalogClient,
        @param:ApplicationScope private val syncScope: CoroutineScope,
    ) : CollectionRepository {
        init {
            authStateUid()
                .distinctUntilChanged()
                .flatMapLatest { uid -> if (uid != null) remoteRecordChanges(uid) else emptyFlow() }
                .onEach { changes -> changes.forEach { applyRemoteChange(it) } }
                .launchIn(syncScope)
        }

        override fun observeCollection(filter: CollectionFilter, sort: CollectionSort): Flow<List<Record>> =
            localDataSource.observeCollection(filter, sort)

        override fun observeRecord(id: String): Flow<Record?> = localDataSource.observeRecord(id)

        override fun observeStats(): Flow<CollectionStats> = localDataSource.observeStats()

        override fun observeSyncState(): Flow<SyncState> = localDataSource.observeSyncState()

        override suspend fun addRecord(record: Record): Result<String> =
            runCatching {
                val id = record.id.ifBlank { UUID.randomUUID().toString() }
                val pending = record.copy(id = id, syncState = SyncState.PENDING)
                localDataSource.save(pending)
                syncToFirestore(pending)
                id
            }

        override suspend fun updateRecord(record: Record): Result<Unit> =
            runCatching {
                val pending = record.copy(syncState = SyncState.PENDING)
                localDataSource.save(pending)
                syncToFirestore(pending)
            }

        override suspend fun deleteRecord(id: String): Result<Unit> =
            runCatching {
                localDataSource.delete(id)
                // Best-effort only: once the Room row is gone there is nowhere local left to mark
                // PENDING if this fails, so a failed remote delete is not retried by anything in
                // this change (no tombstone/pending-delete queue) -- see the PR description.
                runCatching { recordsCollection()?.document(id)?.delete()?.await() }
            }

        override suspend fun setCoverImage(recordId: String, source: Uri): Result<Unit> =
            Result.failure(NotImplementedError("setCoverImage lands in T-13 (cover image pipeline)"))

        // DiscogsCatalogClient.searchCatalog already matches this method's signature and Result
        // shape exactly (see that class's doc) -- nothing to map, just delegate.
        override suspend fun searchCatalog(query: String, page: Int): Result<List<CatalogResult>> =
            discogsCatalogClient.searchCatalog(query, page)

        override suspend fun exportCsv(): Result<Uri> =
            Result.failure(NotImplementedError("exportCsv lands in T-27 (CSV export)"))

        /**
         * Best-effort Firestore write for [record]: on success, flips the already-saved Room row to
         * `SYNCED`. Any failure (offline, permission denied, signed out, ...) is swallowed here --
         * the row stays `PENDING` in Room, matching 02-ARCHITECTURE.md §2's documented behaviour.
         */
        private suspend fun syncToFirestore(record: Record) {
            val collection = recordsCollection() ?: return
            runCatching {
                collection.document(record.id).set(record.toFirestoreMap()).await()
            }.onSuccess {
                localDataSource.save(record.copy(syncState = SyncState.SYNCED))
            }
        }

        /** `users/{uid}/records`, or null if nobody is signed in (never crashes; sync is just skipped). */
        private fun recordsCollection(): CollectionReference? =
            firebaseAuth.currentUser?.uid?.let { uid ->
                firestore.collection(USERS_COLLECTION).document(uid).collection(RECORDS_COLLECTION)
            }

        /**
         * The signed-in user's uid, or null when signed out -- mirrors
         * [FirebaseAuthRepository.currentUser]'s `AuthStateListener` `callbackFlow` pattern, kept
         * uid-only here since that's all [init]'s sync needs.
         */
        private fun authStateUid(): Flow<String?> =
            callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser?.uid) }
                firebaseAuth.addAuthStateListener(listener)
                awaitClose { firebaseAuth.removeAuthStateListener(listener) }
            }

        /**
         * Live `DocumentChange`s on `uid`'s `users/{uid}/records` -- the inbound half of ADR-2.
         * Firestore errors (permission-denied, offline, ...) are swallowed here, matching
         * [syncToFirestore]'s best-effort convention for the outbound direction: this listener
         * just goes quiet until the next successful snapshot rather than tearing anything down.
         */
        private fun remoteRecordChanges(uid: String): Flow<List<DocumentChange>> =
            callbackFlow {
                val registration =
                    firestore
                        .collection(USERS_COLLECTION)
                        .document(uid)
                        .collection(RECORDS_COLLECTION)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) return@addSnapshotListener
                            snapshot?.documentChanges?.let { trySend(it) }
                        }
                awaitClose { registration.remove() }
            }

        /**
         * Applies one inbound [DocumentChange] to Room. `REMOVED` deletes the local row outright
         * -- a server-side deletion is authoritative, so there's no PENDING-style guard for it.
         * `ADDED`/`MODIFIED` map the document to a [Record] and upsert it, *unless* the local row
         * is currently `PENDING`: that means [addRecord]/[updateRecord] wrote it locally and its
         * own [syncToFirestore] call hasn't completed yet, so this snapshot is necessarily stale
         * with respect to that in-flight write and must not clobber it. Once that write finishes
         * and flips the row to `SYNCED`, the next snapshot (including the echo of that very write)
         * applies normally.
         */
        private suspend fun applyRemoteChange(change: DocumentChange) {
            when (change.type) {
                DocumentChange.Type.REMOVED -> localDataSource.delete(change.document.id)
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                    val remote = change.document.toRecord() ?: return
                    val local = localDataSource.observeRecord(remote.id).first()
                    if (local?.syncState == SyncState.PENDING) return
                    localDataSource.save(remote)
                }
            }
        }

        private companion object {
            const val USERS_COLLECTION = "users"
            const val RECORDS_COLLECTION = "records"
        }
    }
