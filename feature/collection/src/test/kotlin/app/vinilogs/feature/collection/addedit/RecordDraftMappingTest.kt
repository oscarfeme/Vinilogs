package app.vinilogs.feature.collection.addedit

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
}
