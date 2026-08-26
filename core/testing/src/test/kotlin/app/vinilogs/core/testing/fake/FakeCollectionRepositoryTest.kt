package app.vinilogs.core.testing.fake

import app.cash.turbine.test
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.core.model.Format
import app.vinilogs.core.testing.fixture.RecordFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FakeCollectionRepositoryTest {
    @Test
    fun `observeCollection with no filter returns every seeded record`() = runTest {
        val records = RecordFixtures.records(count = 50)
        val repository = FakeCollectionRepository(records)

        repository.observeCollection(CollectionFilter(), CollectionSort.ARTIST).test {
            assertEquals(50, awaitItem().size)
        }
    }

    @Test
    fun `observeCollection filters by format`() = runTest {
        val records = RecordFixtures.records(count = 200)
        val repository = FakeCollectionRepository(records)
        val expected = records.count { it.format == Format.LP }

        repository.observeCollection(CollectionFilter(format = Format.LP), CollectionSort.ARTIST).test {
            val result = awaitItem()
            assertEquals(expected, result.size)
            assertTrue(result.all { it.format == Format.LP })
        }
    }

    @Test
    fun `observeCollection sorts by artist ascending`() = runTest {
        val records = RecordFixtures.records(count = 50)
        val repository = FakeCollectionRepository(records)

        repository.observeCollection(CollectionFilter(), CollectionSort.ARTIST).test {
            val result = awaitItem()
            assertEquals(result.map { it.artist.lowercase() }, result.map { it.artist.lowercase() }.sorted())
        }
    }

    @Test
    fun `addRecord makes the record observable`() = runTest {
        val repository = FakeCollectionRepository()
        val record = RecordFixtures.records(count = 1).first()

        val addResult = repository.addRecord(record)

        assertTrue(addResult.isSuccess)
        repository.observeRecord(record.id).test {
            assertEquals(record.artist, awaitItem()?.artist)
        }
    }

    @Test
    fun `deleteRecord removes it from observeRecord`() = runTest {
        val record = RecordFixtures.records(count = 1).first()
        val repository = FakeCollectionRepository(listOf(record))

        repository.deleteRecord(record.id)

        repository.observeRecord(record.id).test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `updateRecord on an unknown id fails`() = runTest {
        val repository = FakeCollectionRepository()
        val unknown = RecordFixtures.records(count = 1).first()

        val result = repository.updateRecord(unknown)

        assertTrue(result.isFailure)
    }

    @Test
    fun `observeStats reports the correct total record count`() = runTest {
        val records = RecordFixtures.records(count = 200)
        val repository = FakeCollectionRepository(records)

        repository.observeStats().test {
            assertEquals(200, awaitItem().totalRecords)
        }
    }
}
