package app.vinilogs.core.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.core.model.Condition
import app.vinilogs.core.model.Format
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.Speed
import app.vinilogs.core.model.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/**
 * Exercises [RecordLocalDataSource] (and, through it, [RecordDao]'s hand-written SQL --
 * filtering, sorting, search, stats aggregation) against a real in-memory Room database.
 *
 * Runs under Robolectric (JUnit4, via the JUnit5 Vintage engine -- see the version catalog
 * comment) rather than plain JUnit5 like the rest of this module's tests: a Room DAO test
 * needs a real SQLite engine, and `core:data` is a plain Android Gradle Plugin library
 * (not Kotlin Multiplatform), so Room's context-free `BundledSQLiteDriver` path isn't
 * available here -- only the classic Context-taking builder is, and Robolectric is the only
 * way to get a real `Context` outside an instrumented test.
 */
@RunWith(RobolectricTestRunner::class)
class RecordLocalDataSourceTest {
    private lateinit var database: VinilogsDatabase
    private lateinit var dataSource: RecordLocalDataSource

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    VinilogsDatabase::class.java,
                ).build()
        dataSource = RecordLocalDataSource(database.recordDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ---- round-trip mapping / basic CRUD ----

    @Test
    fun `save then observeRecord round-trips every field`() =
        runTest {
            val record =
                fixture(id = "r1", artist = "Miles Davis", title = "Kind of Blue", tags = listOf("jazz", "fav"))

            dataSource.save(record)

            assertEquals(record, dataSource.observeRecord("r1").first())
        }

    @Test
    fun `save with an existing id replaces the row (upsert)`() =
        runTest {
            dataSource.save(fixture(id = "r1", artist = "Miles Davis", title = "Kind of Blue"))
            dataSource.save(fixture(id = "r1", artist = "Miles Davis", title = "Kind of Blue (Deluxe)"))

            val records = dataSource.observeCollection(CollectionFilter(), CollectionSort.DATE_ADDED).first()

            assertEquals(1, records.size)
            assertEquals("Kind of Blue (Deluxe)", records.single().title)
        }

    @Test
    fun `delete removes the record`() =
        runTest {
            dataSource.save(fixture(id = "r1"))

            dataSource.delete("r1")

            assertNull(dataSource.observeRecord("r1").first())
        }

    @Test
    fun `observeRecord emits null for an unknown id`() =
        runTest {
            assertNull(dataSource.observeRecord("does-not-exist").first())
        }

    // ---- filtering (FR-B8) ----

    @Test
    fun `filters by format`() =
        runTest {
            dataSource.save(fixture(id = "lp", format = Format.LP))
            dataSource.save(fixture(id = "ep", format = Format.EP))

            val filter = CollectionFilter(format = Format.EP)
            val result = dataSource.observeCollection(filter, CollectionSort.ARTIST).first()

            assertEquals(listOf("ep"), result.map { it.id })
        }

    @Test
    fun `filters by condition`() =
        runTest {
            dataSource.save(fixture(id = "mint", condition = Condition.MINT))
            dataSource.save(fixture(id = "good", condition = Condition.GOOD))

            val filter = CollectionFilter(condition = Condition.GOOD)
            val result = dataSource.observeCollection(filter, CollectionSort.ARTIST).first()

            assertEquals(listOf("good"), result.map { it.id })
        }

    @Test
    fun `filters by decade, excluding records with no year`() =
        runTest {
            dataSource.save(fixture(id = "seventies", year = 1975))
            dataSource.save(fixture(id = "eighties", year = 1984))
            dataSource.save(fixture(id = "no-year", year = null))

            val filter = CollectionFilter(decade = 1970)
            val result = dataSource.observeCollection(filter, CollectionSort.ARTIST).first()

            assertEquals(listOf("seventies"), result.map { it.id })
        }

    @Test
    fun `filters by minRating, excluding unrated records`() =
        runTest {
            dataSource.save(fixture(id = "five", rating = 5))
            dataSource.save(fixture(id = "two", rating = 2))
            dataSource.save(fixture(id = "unrated", rating = null))

            val filter = CollectionFilter(minRating = 4)
            val result = dataSource.observeCollection(filter, CollectionSort.ARTIST).first()

            assertEquals(listOf("five"), result.map { it.id })
        }

    @Test
    fun `filters by tag without matching a substring of a different tag`() =
        runTest {
            dataSource.save(fixture(id = "jazz", tags = listOf("jazz")))
            dataSource.save(fixture(id = "jazzy-fusion", tags = listOf("jazz-fusion")))

            val filter = CollectionFilter(tag = "jazz")
            val result = dataSource.observeCollection(filter, CollectionSort.ARTIST).first()

            assertEquals(listOf("jazz"), result.map { it.id })
        }

    // ---- search (FR-B7) ----

    @Test
    fun `query matches artist, title, label, catalogue number and tags, case-insensitively`() =
        runTest {
            dataSource.save(fixture(id = "by-artist", artist = "Radiohead"))
            dataSource.save(fixture(id = "by-title", title = "OK Computer"))
            dataSource.save(fixture(id = "by-label", label = "Parlophone"))
            dataSource.save(fixture(id = "by-catalog", catalogNumber = "PARL-001"))
            dataSource.save(fixture(id = "by-tag", tags = listOf("favorite")))
            dataSource.save(fixture(id = "no-match", artist = "Someone Else", title = "Unrelated"))

            val byArtist =
                dataSource.observeCollection(CollectionFilter(), CollectionSort.ARTIST, query = "radio").first()
            assertEquals(listOf("by-artist"), byArtist.map { it.id })

            val byTag =
                dataSource.observeCollection(CollectionFilter(), CollectionSort.ARTIST, query = "FAVORITE").first()
            assertEquals(listOf("by-tag"), byTag.map { it.id })
        }

    // ---- sorting (FR-B8) ----

    @Test
    fun `sorts by artist case-insensitively`() =
        runTest {
            dataSource.save(fixture(id = "b", artist = "beta"))
            dataSource.save(fixture(id = "a", artist = "Alpha"))

            val result = dataSource.observeCollection(CollectionFilter(), CollectionSort.ARTIST).first()

            assertEquals(listOf("a", "b"), result.map { it.id })
        }

    @Test
    fun `sorts by year ascending with nulls last`() =
        runTest {
            dataSource.save(fixture(id = "no-year", year = null))
            dataSource.save(fixture(id = "old", year = 1960))
            dataSource.save(fixture(id = "new", year = 2020))

            val result = dataSource.observeCollection(CollectionFilter(), CollectionSort.YEAR).first()

            assertEquals(listOf("old", "new", "no-year"), result.map { it.id })
        }

    @Test
    fun `sorts by date added, most recent first`() =
        runTest {
            dataSource.save(fixture(id = "earlier", createdAt = Instant.ofEpochSecond(1_000)))
            dataSource.save(fixture(id = "later", createdAt = Instant.ofEpochSecond(2_000)))

            val result = dataSource.observeCollection(CollectionFilter(), CollectionSort.DATE_ADDED).first()

            assertEquals(listOf("later", "earlier"), result.map { it.id })
        }

    @Test
    fun `sorts by rating descending with unrated records last`() =
        runTest {
            dataSource.save(fixture(id = "unrated", rating = null))
            dataSource.save(fixture(id = "low", rating = 2))
            dataSource.save(fixture(id = "high", rating = 5))

            val result = dataSource.observeCollection(CollectionFilter(), CollectionSort.RATING).first()

            assertEquals(listOf("high", "low", "unrated"), result.map { it.id })
        }

    // ---- stats (FR-B10) ----

    @Test
    fun `observeStats aggregates totals, spend, decades and top artists-labels`() =
        runTest {
            val record1 =
                fixture(id = "1", artist = "Miles Davis", label = "Blue Note", year = 1959, purchasePrice = 20.0)
            val record2 =
                fixture(id = "2", artist = "Miles Davis", label = "Blue Note", year = 1965, purchasePrice = 15.0)
            val record3 =
                fixture(id = "3", artist = "John Coltrane", label = "Impulse!", year = 1965, purchasePrice = null)
            dataSource.save(record1)
            dataSource.save(record2)
            dataSource.save(record3)

            val stats = dataSource.observeStats().first()

            assertEquals(3, stats.totalRecords)
            assertEquals(35.0, stats.totalSpend, 0.0001)
            assertEquals(mapOf(1950 to 1, 1960 to 2), stats.recordsByDecade)
            assertEquals(listOf("Miles Davis" to 2, "John Coltrane" to 1), stats.topArtists.map { it.name to it.count })
            assertEquals(listOf("Blue Note" to 2, "Impulse!" to 1), stats.topLabels.map { it.name to it.count })
        }

    private fun fixture(
        id: String,
        artist: String = "Artist $id",
        title: String = "Title $id",
        year: Int? = 2000,
        label: String? = null,
        catalogNumber: String? = null,
        format: Format = Format.LP,
        condition: Condition = Condition.MINT,
        rating: Int? = null,
        purchasePrice: Double? = null,
        tags: List<String> = emptyList(),
        createdAt: Instant = Instant.ofEpochSecond(1_700_000_000L),
    ): Record =
        Record(
            id = id,
            artist = artist,
            title = title,
            year = year,
            label = label,
            catalogNumber = catalogNumber,
            format = format,
            speed = Speed.RPM33,
            condition = condition,
            purchasePrice = purchasePrice,
            purchaseDate = null,
            rating = rating,
            notes = null,
            coverUrl = null,
            discogsId = null,
            tags = tags,
            syncState = SyncState.SYNCED,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
}
