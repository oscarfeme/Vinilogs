package app.vinilogs.feature.collection.addedit

import app.vinilogs.core.model.Condition
import app.vinilogs.core.model.Format
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.Speed
import app.vinilogs.core.model.SyncState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * All FR-B4 fields in UI-editable form -- numeric/date fields are kept as raw [String] input
 * (so an in-progress "19" for a year, or an empty purchase-price field, is representable) and
 * parsed/validated only at save time by [validate]. Format/speed/condition default to the most
 * common vinyl values rather than forcing a first choice, since those three are the only
 * required-non-null enum fields on [Record].
 */
internal data class RecordDraft(
    val artist: String = "",
    val title: String = "",
    val year: String = "",
    val label: String = "",
    val catalogNumber: String = "",
    val format: Format = Format.LP,
    val speed: Speed = Speed.RPM33,
    val condition: Condition = Condition.NEAR_MINT,
    val purchasePrice: String = "",
    val purchaseDate: String = "",
    val rating: Int? = null,
    val notes: String = "",
    val coverUrl: String? = null,
    val tags: String = "",
)

internal data class RecordDraftErrors(
    val artist: String? = null,
    val title: String? = null,
    val year: String? = null,
    val purchasePrice: String? = null,
    val purchaseDate: String? = null,
) {
    val isValid: Boolean
        get() = artist == null && title == null && year == null && purchasePrice == null && purchaseDate == null
}

private const val EARLIEST_RELEASE_YEAR = 1860
private const val RATING_MIN = 1
private const val RATING_MAX = 5

/** FR-B3: "Artist and title required; all other fields optional." Everything else is format checks. */
internal fun RecordDraft.validate(): RecordDraftErrors {
    val currentYear = Instant.now().atZone(ZoneOffset.UTC).year
    val parsedYear = year.trim().toIntOrNull()
    val parsedPrice = purchasePrice.trim().toDoubleOrNull()

    return RecordDraftErrors(
        artist = "Artist is required".takeIf { artist.isBlank() },
        title = "Title is required".takeIf { title.isBlank() },
        year =
            "Enter a year between $EARLIEST_RELEASE_YEAR and $currentYear".takeIf {
                year.isNotBlank() && (parsedYear == null || parsedYear !in EARLIEST_RELEASE_YEAR..currentYear)
            },
        purchasePrice =
            "Enter a positive price".takeIf {
                purchasePrice.isNotBlank() && (parsedPrice == null || parsedPrice < 0)
            },
        purchaseDate =
            "Use the format YYYY-MM-DD".takeIf { purchaseDate.isNotBlank() && purchaseDate.toInstantOrNull() == null },
    )
}

/** Converts a validated draft into a full [Record]. Only call once [validate] reports no errors. */
internal fun RecordDraft.toRecord(
    id: String,
    discogsId: Long?,
    createdAt: Instant,
): Record =
    Record(
        id = id,
        artist = artist.trim(),
        title = title.trim(),
        year = year.trim().toIntOrNull(),
        label = label.trim().ifBlank { null },
        catalogNumber = catalogNumber.trim().ifBlank { null },
        format = format,
        speed = speed,
        condition = condition,
        purchasePrice = purchasePrice.trim().toDoubleOrNull(),
        purchaseDate = purchaseDate.toInstantOrNull(),
        rating = rating?.coerceIn(RATING_MIN, RATING_MAX),
        notes = notes.trim().ifBlank { null },
        coverUrl = coverUrl,
        discogsId = discogsId,
        tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        // FR-B5/FR-B11: a save -- new or edited -- always queues for sync; the repository is
        // what flips this back to SYNCED once the write actually lands (02-ARCHITECTURE.md §2).
        syncState = SyncState.PENDING,
        createdAt = createdAt,
        updatedAt = Instant.now(),
    )

internal fun Record.toDraft(): RecordDraft =
    RecordDraft(
        artist = artist,
        title = title,
        year = year?.toString().orEmpty(),
        label = label.orEmpty(),
        catalogNumber = catalogNumber.orEmpty(),
        format = format,
        speed = speed,
        condition = condition,
        purchasePrice = purchasePrice?.let { formatPrice(it) }.orEmpty(),
        purchaseDate = purchaseDate?.let { LocalDate.ofInstant(it, ZoneOffset.UTC).toString() }.orEmpty(),
        rating = rating,
        notes = notes.orEmpty(),
        coverUrl = coverUrl,
        tags = tags.joinToString(", "),
    )

private fun formatPrice(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

private fun String.toInstantOrNull(): Instant? =
    try {
        LocalDate.parse(trim()).atStartOfDay(ZoneOffset.UTC).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }
