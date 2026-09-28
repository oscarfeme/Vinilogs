package app.vinilogs.feature.collection.addedit

import app.vinilogs.core.model.CatalogResult
import app.vinilogs.core.model.Condition
import app.vinilogs.core.model.Format
import app.vinilogs.core.model.Speed
import app.vinilogs.core.model.SyncState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

/** [RecordDraft.toRecord] / [RecordDraft.toDraft]. Field validation is covered by [RecordDraftTest]. */
class RecordDraftMappingTest {
    @Test
    fun `toRecord trims text fields and blanks become null`() {
        val draft = RecordDraft(artist = "  Miles Davis  ", title = "  Kind of Blue  ", label = "   ", notes = "  ")

        val record = draft.toRecord(id = "abc", discogsId = null, createdAt = Instant.EPOCH)

        assertEquals("Miles Davis", record.artist)
        assertEquals("Kind of Blue", record.title)
        assertNull(record.label)
        assertNull(record.notes)
    }

    @Test
    fun `toRecord parses comma-separated tags, trimming blanks`() {
        val draft = RecordDraft(artist = "A", title = "B", tags = " jazz, favorite ,, sealed")

        val record = draft.toRecord(id = "abc", discogsId = null, createdAt = Instant.EPOCH)

        assertEquals(listOf("jazz", "favorite", "sealed"), record.tags)
    }

    @Test
    fun `toRecord always queues the save for sync -- FR-B5, FR-B11`() {
        val draft = RecordDraft(artist = "A", title = "B")

        val record = draft.toRecord(id = "abc", discogsId = null, createdAt = Instant.EPOCH)

        assertEquals(SyncState.PENDING, record.syncState)
    }

    @Test
    fun `toDraft and toRecord round-trip the physical-format fields`() {
        val draft =
            RecordDraft(
                artist = "A",
                title = "B",
                format = Format.SEVEN,
                speed = Speed.RPM45,
                condition = Condition.VERY_GOOD_PLUS,
            )

        val record = draft.toRecord(id = "abc", discogsId = null, createdAt = Instant.EPOCH)
        val roundTripped = record.toDraft()

        assertEquals(Format.SEVEN, roundTripped.format)
        assertEquals(Speed.RPM45, roundTripped.speed)
        assertEquals(Condition.VERY_GOOD_PLUS, roundTripped.condition)
    }

    @Test
    fun `CatalogResult toDraft prefills identity fields, leaving format-speed-condition at defaults -- FR-B2`() {
        val result =
            CatalogResult(
                discogsId = 12345L,
                artist = "Miles Davis",
                title = "Kind of Blue",
                year = 1959,
                label = "Columbia",
                catalogNumber = "CL 1355",
                coverUrl = "https://example.com/cover.jpg",
            )

        val draft = result.toDraft()

        assertEquals("Miles Davis", draft.artist)
        assertEquals("Kind of Blue", draft.title)
        assertEquals("1959", draft.year)
        assertEquals("Columbia", draft.label)
        assertEquals("CL 1355", draft.catalogNumber)
        assertEquals("https://example.com/cover.jpg", draft.coverUrl)
        // Defaults, per RecordDraft() -- FR-B2 "all prefilled fields remain editable" means the
        // user sets these, not Discogs (see CatalogResult.toDraft's doc comment for why).
        assertEquals(Format.LP, draft.format)
        assertEquals(Condition.NEAR_MINT, draft.condition)
    }

    @Test
    fun `CatalogResult toDraft with a null year and label leaves those fields blank, not the string null`() {
        val result =
            CatalogResult(
                discogsId = 1L,
                artist = "A",
                title = "B",
                year = null,
                label = null,
                catalogNumber = null,
                coverUrl = null,
            )

        val draft = result.toDraft()

        assertEquals("", draft.year)
        assertEquals("", draft.label)
        assertEquals("", draft.catalogNumber)
        assertNull(draft.coverUrl)
    }
}
