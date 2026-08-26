package app.vinilogs.core.data.repository

import android.net.Uri
import app.vinilogs.core.model.CatalogResult
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.core.model.CollectionStats
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * Fixed contract per 02-ARCHITECTURE.md §4 — implement against this so feature agents can
 * work in parallel with core:testing's fakes. Real implementation lands across T-10/T-11/T-12
 * (Room source of truth, Firestore sync, Discogs lookup).
 */
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
