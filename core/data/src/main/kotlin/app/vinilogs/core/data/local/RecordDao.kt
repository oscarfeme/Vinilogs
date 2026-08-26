package app.vinilogs.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** One row of the by-decade `GROUP BY` stats query. */
data class DecadeCountRow(val decade: Int, val count: Int)

/** One row of the top-artist/top-label `GROUP BY ... ORDER BY count DESC LIMIT 5` stats queries. */
data class NameCountRow(val name: String, val count: Int)

/**
 * Room DAO for [RecordEntity] (T-10). Filtering, sorting and search all run in SQL
 * ([observeRecords]) rather than in memory, per FR-B7/FR-B8 and this task's explicit scope --
 * that's the difference from `core:testing`'s in-memory `FakeCollectionRepository`.
 *
 * Deliberately kept to raw entity-level Room methods only (no domain-typed convenience
 * wrappers) -- see [RecordLocalDataSource] for those, which is what T-11's
 * `CollectionRepository` implementation is meant to depend on.
 */
@Dao
interface RecordDao {
    /** Insert-or-replace by primary key -- covers both add and update. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecordEntity)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM records WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<RecordEntity?>

    /**
     * [query] matches artist/title/label/catalogue number/tags (FR-B7); all other params are
     * [app.vinilogs.core.model.CollectionFilter]'s fields by name, and [sort] is a
     * [app.vinilogs.core.model.CollectionSort] enum-name string. `CollectionFilter` has no
     * free-text field of its own (that lives on `ShelfUiState`, one layer up) -- [query] is
     * this DAO's own addition for FR-B7, wired up by [RecordLocalDataSource.observeCollection].
     *
     * The `ORDER BY` uses one `CASE WHEN :sort = '...'` per sort case: only the matching
     * case's column contributes non-null values, so the others are inert tie-breakers
     * (Room/SQLite can't bind a column *name* as a query parameter, only values). `YEAR`'s null
     * handling is explicit (nulls pushed last) rather than relying on SQLite's version-dependent
     * default `NULLS LAST` support, so it matches `FakeCollectionRepository`'s
     * `year ?: Int.MAX_VALUE` semantics on every device back to minSdk 26.
     */
    @Suppress("LongParameterList")
    @Query(
        """
        SELECT * FROM records
        WHERE (:query IS NULL OR :query = '' OR
               artist LIKE '%' || :query || '%' COLLATE NOCASE OR
               title LIKE '%' || :query || '%' COLLATE NOCASE OR
               (label IS NOT NULL AND label LIKE '%' || :query || '%' COLLATE NOCASE) OR
               (catalogNumber IS NOT NULL AND catalogNumber LIKE '%' || :query || '%' COLLATE NOCASE) OR
               tags LIKE '%' || :query || '%' COLLATE NOCASE)
          AND (:format IS NULL OR format = :format)
          AND (:condition IS NULL OR condition = :condition)
          AND (:decade IS NULL OR (year / 10) * 10 = :decade)
          AND (:minRating IS NULL OR (rating IS NOT NULL AND rating >= :minRating))
          AND (:tag IS NULL OR tags LIKE '%,' || :tag || ',%')
        ORDER BY
          CASE WHEN :sort = 'YEAR' THEN (CASE WHEN year IS NULL THEN 1 ELSE 0 END) END ASC,
          CASE WHEN :sort = 'ARTIST' THEN artist END COLLATE NOCASE ASC,
          CASE WHEN :sort = 'TITLE' THEN title END COLLATE NOCASE ASC,
          CASE WHEN :sort = 'YEAR' THEN year END ASC,
          CASE WHEN :sort = 'DATE_ADDED' THEN createdAt END DESC,
          CASE WHEN :sort = 'RATING' THEN rating END DESC
        """,
    )
    fun observeRecords(
        query: String?,
        format: String?,
        condition: String?,
        decade: Int?,
        minRating: Int?,
        tag: String?,
        sort: String,
    ): Flow<List<RecordEntity>>

    @Query("SELECT COUNT(*) FROM records")
    fun observeTotalRecords(): Flow<Int>

    @Query("SELECT COALESCE(SUM(purchasePrice), 0.0) FROM records")
    fun observeTotalSpend(): Flow<Double>

    @Query(
        "SELECT (year / 10) * 10 AS decade, COUNT(*) AS count FROM records " +
            "WHERE year IS NOT NULL GROUP BY decade",
    )
    fun observeRecordsByDecade(): Flow<List<DecadeCountRow>>

    @Query(
        "SELECT artist AS name, COUNT(*) AS count FROM records " +
            "GROUP BY artist ORDER BY count DESC, name ASC LIMIT 5",
    )
    fun observeTopArtists(): Flow<List<NameCountRow>>

    @Query(
        "SELECT label AS name, COUNT(*) AS count FROM records WHERE label IS NOT NULL " +
            "GROUP BY label ORDER BY count DESC, name ASC LIMIT 5",
    )
    fun observeTopLabels(): Flow<List<NameCountRow>>
}
