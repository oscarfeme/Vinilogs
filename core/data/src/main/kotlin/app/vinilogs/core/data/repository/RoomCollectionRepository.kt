package app.vinilogs.core.data.repository

import android.net.Uri
import app.vinilogs.core.data.local.RecordLocalDataSource
import app.vinilogs.core.model.CatalogResult
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.core.model.CollectionStats
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.SyncState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
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
 */
class RoomCollectionRepository
    @Inject
    constructor(
        private val localDataSource: RecordLocalDataSource,
        private val firestore: FirebaseFirestore,
        private val firebaseAuth: FirebaseAuth,
    ) : CollectionRepository {
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

        override suspend fun searchCatalog(query: String, page: Int): Result<List<CatalogResult>> =
            Result.failure(
                NotImplementedError(
                    "searchCatalog needs T-12's DiscogsCatalogClient, which is a separate, " +
                        "not-yet-merged PR -- wire it in once that lands",
                ),
            )

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

        private companion object {
            const val USERS_COLLECTION = "users"
            const val RECORDS_COLLECTION = "records"
        }
    }
