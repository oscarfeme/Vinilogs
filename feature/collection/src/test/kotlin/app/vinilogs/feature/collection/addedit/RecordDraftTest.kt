package app.vinilogs.feature.collection.addedit

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [RecordDraft.validate] (FR-B3/FR-B4). Mapping to/from [app.vinilogs.core.model.Record] is
 * covered by [RecordDraftMappingTest].
 */
class RecordDraftTest {
    @Test
    fun `blank artist and title are both reported as required`() {
        val errors = RecordDraft(artist = "", title = "").validate()

        assertFalse(errors.isValid)
        assertNotNull(errors.artist)
        assertNotNull(errors.title)
    }

    @Test
    fun `artist and title alone are enough to be valid -- FR-B3`() {
        val errors = RecordDraft(artist = "Miles Davis", title = "Kind of Blue").validate()

        assertTrue(errors.isValid)
    }

    @Test
    fun `a non-numeric year is rejected`() {
        val errors = RecordDraft(artist = "A", title = "B", year = "not a year").validate()

        assertNotNull(errors.year)
        assertFalse(errors.isValid)
    }

    @Test
    fun `a year far in the future is rejected`() {
        val errors = RecordDraft(artist = "A", title = "B", year = "3000").validate()

        assertNotNull(errors.year)
    }

    @Test
    fun `a negative purchase price is rejected`() {
        val errors = RecordDraft(artist = "A", title = "B", purchasePrice = "-5").validate()

        assertNotNull(errors.purchasePrice)
    }

    @Test
    fun `a malformed purchase date is rejected`() {
        val errors = RecordDraft(artist = "A", title = "B", purchaseDate = "not-a-date").validate()

        assertNotNull(errors.purchaseDate)
    }

    @Test
    fun `a well-formed purchase date is accepted`() {
        val errors = RecordDraft(artist = "A", title = "B", purchaseDate = "2020-05-17").validate()

        assertNull(errors.purchaseDate)
    }
}
