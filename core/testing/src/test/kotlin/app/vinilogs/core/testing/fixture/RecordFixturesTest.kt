package app.vinilogs.core.testing.fixture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecordFixturesTest {
    @Test
    fun `default call returns 200 records`() {
        assertEquals(200, RecordFixtures.records().size)
    }

    @Test
    fun `same seed produces identical records`() {
        val first = RecordFixtures.records(count = 50, seed = 42L)
        val second = RecordFixtures.records(count = 50, seed = 42L)

        assertEquals(first, second)
    }

    @Test
    fun `different seeds produce different records`() {
        val first = RecordFixtures.records(count = 50, seed = 1L)
        val second = RecordFixtures.records(count = 50, seed = 2L)

        assertTrue(first != second)
    }

    @Test
    fun `every record has a unique id`() {
        val records = RecordFixtures.records(count = 200)

        assertEquals(200, records.map { it.id }.toSet().size)
    }
}
