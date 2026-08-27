package app.vinilogs.core.data.local

import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.core.model.CollectionStats
import app.vinilogs.core.model.NameCount
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Domain-typed façade over [RecordDao] (T-10): does the entity/domain mapping and composes the
 * per-column stats queries into one [CollectionStats], so [RecordEntity] never has to leak past
 * this class. T-11's `CollectionRepository` implementation is meant to depend on this directly.
 */
class RecordLocalDataSource
    @Inject
    constructor(
        private val dao: RecordDao,
    ) {
        /**
         * See [RecordDao.observeRecords] for [query]'s note -- `CollectionFilter` has no
         * free-text field of its own.
         */
        fun observeCollection(filter: CollectionFilter, sort: CollectionSort, query: String = ""): Flow<List<Record>> =
            dao
                .observeRecords(
                    query = query,
                    format = filter.format?.name,
                    condition = filter.condition?.name,
                    decade = filter.decade,
                    minRating = filter.minRating,
                    tag = filter.tag,
                    sort = sort.name,
                ).map { entities -> entities.map { it.toDomain() } }

        fun observeRecord(id: String): Flow<Record?> = dao.observeById(id).map { it?.toDomain() }

        /**
         * Collection-wide sync status (FR-B11): `ERROR` if any record has one, else `PENDING`
         * if any record is still queued, else `SYNCED` (also the empty-collection default).
         */
        fun observeSyncState(): Flow<SyncState> =
            dao.observeDistinctSyncStates().map { states ->
                when {
                    states.contains(SyncState.ERROR.name) -> SyncState.ERROR
                    states.contains(SyncState.PENDING.name) -> SyncState.PENDING
                    else -> SyncState.SYNCED
                }
            }

        /** Aggregate stats for FR-B10, computed in SQL and combined here -- see each `RecordDao.observe*` query. */
        fun observeStats(): Flow<CollectionStats> =
            combine(
                dao.observeTotalRecords(),
                dao.observeTotalSpend(),
                dao.observeRecordsByDecade(),
                dao.observeTopArtists(),
                dao.observeTopLabels(),
            ) { totalRecords, totalSpend, byDecade, topArtists, topLabels ->
                CollectionStats(
                    totalRecords = totalRecords,
                    totalSpend = totalSpend,
                    recordsByDecade = byDecade.associate { it.decade to it.count },
                    topArtists = topArtists.map { NameCount(it.name, it.count) },
                    topLabels = topLabels.map { NameCount(it.name, it.count) },
                )
            }

        /**
         * Insert-or-replace by [Record.id] -- covers both add and update
         * (`CollectionRepository.addRecord`/`updateRecord`, T-11).
         */
        suspend fun save(record: Record) = dao.upsert(record.toEntity())

        suspend fun delete(id: String) = dao.deleteById(id)
    }
