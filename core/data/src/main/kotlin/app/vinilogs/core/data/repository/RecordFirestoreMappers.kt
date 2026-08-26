package app.vinilogs.core.data.repository

import app.vinilogs.core.model.Record
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
