package app.vinilogs.core.data.repository

import app.vinilogs.core.model.Condition
import app.vinilogs.core.model.Format
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.Speed
import app.vinilogs.core.model.SyncState
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Maps a [Record] to the `users/{uid}/records/{recordId}` document shape (02-ARCHITECTURE.md
 * §3): `artistLower`/`titleLower` for prefix search, `syncState` deliberately excluded (that
 * column is local-only, per ADR-2 -- Firestore has no opinion on it). `purchasePrice`,
 * `purchaseDate`, `rating` and `notes` are written as-is; keeping them out of the separate
 * `publicRecords` projection (never written from here) is what keeps them private (ADR-4).
 */
internal fun Record.toFirestoreMap(): Map<String, Any?> =
    mapOf(
        "artist" to artist,
        "title" to title,
        "artistLower" to artist.lowercase(),
        "titleLower" to title.lowercase(),
        "year" to year,
        "label" to label,
        "catalogNumber" to catalogNumber,
        "format" to format.name,
        "speed" to speed.name,
        "condition" to condition.name,
        "purchasePrice" to purchasePrice,
        "purchaseDate" to purchaseDate?.let(Date::from),
        "rating" to rating,
        "notes" to notes,
        "coverUrl" to coverUrl,
        "discogsId" to discogsId,
        "tags" to tags,
        "createdAt" to Date.from(createdAt),
        "updatedAt" to Date.from(updatedAt),
    )

/**
 * The inbound half of [toFirestoreMap]: maps a `users/{uid}/records/{recordId}` document to the
 * domain [Record] for the Firestore -> Room sync (ADR-2). `syncState` is always [SyncState.SYNCED]
 * -- by definition, a document that just arrived from Firestore is in sync with the server; it's
 * the caller's job (see `RoomCollectionRepository.applyRemoteChange`) not to let this clobber a
 * row that is `PENDING` from a not-yet-completed local write.
 *
 * Returns null for a document missing any field this mapping can't sensibly default -- artist,
 * title, the three enums, and the two timestamps -- rather than throwing and killing the whole
 * snapshot listener over one malformed remote document.
 */
internal fun DocumentSnapshot.toRecord(): Record? {
    val artist = getString("artist")
    val title = getString("title")
    val format = getString("format")?.let { runCatching { Format.valueOf(it) }.getOrNull() }
    val speed = getString("speed")?.let { runCatching { Speed.valueOf(it) }.getOrNull() }
    val condition = getString("condition")?.let { runCatching { Condition.valueOf(it) }.getOrNull() }
    val createdAt = getDate("createdAt")?.toInstant()
    val updatedAt = getDate("updatedAt")?.toInstant()

    if (artist == null || title == null || format == null || speed == null ||
        condition == null || createdAt == null || updatedAt == null
    ) {
        return null
    }

    return Record(
        id = id,
        artist = artist,
        title = title,
        year = getLong("year")?.toInt(),
        label = getString("label"),
        catalogNumber = getString("catalogNumber"),
        format = format,
        speed = speed,
        condition = condition,
        purchasePrice = getDouble("purchasePrice"),
        purchaseDate = getDate("purchaseDate")?.toInstant(),
        rating = getLong("rating")?.toInt(),
        notes = getString("notes"),
        coverUrl = getString("coverUrl"),
        discogsId = getLong("discogsId"),
        tags = (get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        syncState = SyncState.SYNCED,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
