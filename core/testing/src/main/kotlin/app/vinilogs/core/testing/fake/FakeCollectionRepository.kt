package app.vinilogs.core.testing.fake

import android.net.Uri
import app.vinilogs.core.data.repository.CollectionRepository
import app.vinilogs.core.model.CatalogResult
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.core.model.CollectionStats
import app.vinilogs.core.model.NameCount
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * In-memory [CollectionRepository] fake, seeded from [initialRecords] (typically
 * [app.vinilogs.core.testing.fixture.RecordFixtures.records]). Filtering/sorting happens
 * in-memory here, matching the real Room-query semantics closely enough for ViewModel-level
 * tests -- the real SQL implementation lands in T-10.
 */
class FakeCollectionRepository(
    initialRecords: List<Record> = emptyList(),
) : CollectionRepository {
    private val recordsFlow = MutableStateFlow(initialRecords.associateBy { it.id })
    private val syncStateFlow = MutableStateFlow(SyncState.SYNCED)
    private var nextFailure: Throwable? = null
    private var catalogSearchResults: List<CatalogResult> = emptyList()

    fun failNextCallWith(error: Throwable) {
        nextFailure = error
    }

    /** Sets what [searchCatalog] returns next -- there's no real Discogs API behind this fake. */
    fun setCatalogSearchResults(results: List<CatalogResult>) {
        catalogSearchResults = results
    }

    /** Directly sets the collection-wide sync state, for testing pending/error UI. */
    fun setSyncState(state: SyncState) {
        syncStateFlow.value = state
    }

    private fun <T> consumeFailureOr(onSuccess: () -> T): Result<T> {
        val failure = nextFailure
        if (failure != null) {
            nextFailure = null
            return Result.failure(failure)
        }
        return runCatching(onSuccess)
    }

    override fun observeCollection(filter: CollectionFilter, sort: CollectionSort): Flow<List<Record>> =
        recordsFlow.map { records ->
            records.values.filter { it.matches(filter) }.sortedWith(sort.toComparator())
        }

    override fun observeRecord(id: String): Flow<Record?> = recordsFlow.map { it[id] }

    override fun observeStats(): Flow<CollectionStats> = recordsFlow.map { it.values.toStats() }

    override fun observeSyncState(): Flow<SyncState> = syncStateFlow

    override suspend fun addRecord(record: Record): Result<String> =
        consumeFailureOr {
            val id = record.id.ifBlank { UUID.randomUUID().toString() }
            recordsFlow.update { it + (id to record.copy(id = id)) }
            id
        }

    override suspend fun updateRecord(record: Record): Result<Unit> =
        consumeFailureOr {
            check(record.id in recordsFlow.value) { "No record with id ${record.id}" }
            recordsFlow.update { it + (record.id to record) }
        }

    override suspend fun deleteRecord(id: String): Result<Unit> =
        consumeFailureOr { recordsFlow.update { it - id } }

    override suspend fun setCoverImage(recordId: String, source: Uri): Result<Unit> =
        consumeFailureOr {
            val record = requireNotNull(recordsFlow.value[recordId]) { "No record with id $recordId" }
            recordsFlow.update { it + (recordId to record.copy(coverUrl = source.toString())) }
        }

    override suspend fun searchCatalog(query: String, page: Int): Result<List<CatalogResult>> =
        consumeFailureOr { catalogSearchResults }

    override suspend fun exportCsv(): Result<Uri> =
        consumeFailureOr { Uri.parse("file:///fake/export.csv") }
}

private fun Record.matches(filter: CollectionFilter): Boolean {
    if (filter.format != null && format != filter.format) return false
    if (filter.condition != null && condition != filter.condition) return false
    if (filter.decade != null && (year == null || (year / 10) * 10 != filter.decade)) return false
    if (filter.minRating != null && (rating == null || rating < filter.minRating)) return false
    if (filter.tag != null && filter.tag !in tags) return false
    return true
}

private fun CollectionSort.toComparator(): Comparator<Record> = when (this) {
    CollectionSort.ARTIST -> compareBy { it.artist.lowercase() }
    CollectionSort.TITLE -> compareBy { it.title.lowercase() }
    CollectionSort.YEAR -> compareBy { it.year ?: Int.MAX_VALUE }
    CollectionSort.DATE_ADDED -> compareByDescending { it.createdAt }
    CollectionSort.RATING -> compareByDescending { it.rating ?: -1 }
}

private fun Collection<Record>.toStats(): CollectionStats {
    val byDecade = filter { it.year != null }
        .groupingBy { (it.year!! / 10) * 10 }
        .eachCount()
    val topArtists = groupingBy { it.artist }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(5)
        .map { NameCount(it.key, it.value) }
    val topLabels = filter { it.label != null }
        .groupingBy { it.label!! }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(5)
        .map { NameCount(it.key, it.value) }
    return CollectionStats(
        totalRecords = size,
        totalSpend = sumOf { it.purchasePrice ?: 0.0 },
        recordsByDecade = byDecade,
        topArtists = topArtists,
        topLabels = topLabels,
    )
}
