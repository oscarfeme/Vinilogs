package app.vinilogs.core.data.local

import app.vinilogs.core.model.Condition
import app.vinilogs.core.model.Format
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.Speed
import app.vinilogs.core.model.SyncState
import java.time.Instant

/** Maps a Room [RecordEntity] to the domain [Record] (02-ARCHITECTURE.md §1: mappers live in `core:data`). */
fun RecordEntity.toDomain(): Record =
    Record(
        id = id,
        artist = artist,
        title = title,
        year = year,
        label = label,
        catalogNumber = catalogNumber,
        format = Format.valueOf(format),
        speed = Speed.valueOf(speed),
        condition = Condition.valueOf(condition),
        purchasePrice = purchasePrice,
        purchaseDate = purchaseDate?.let(Instant::ofEpochMilli),
        rating = rating,
        notes = notes,
        coverUrl = coverUrl,
        discogsId = discogsId,
        tags = tagsFromColumn(tags),
        syncState = SyncState.valueOf(syncState),
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

/** Maps the domain [Record] to its Room [RecordEntity] representation. */
fun Record.toEntity(): RecordEntity =
    RecordEntity(
        id = id,
        artist = artist,
        title = title,
        year = year,
        label = label,
        catalogNumber = catalogNumber,
        format = format.name,
        speed = speed.name,
        condition = condition.name,
        purchasePrice = purchasePrice,
        purchaseDate = purchaseDate?.toEpochMilli(),
        rating = rating,
        notes = notes,
        coverUrl = coverUrl,
        discogsId = discogsId,
        tags = tagsToColumn(tags),
        syncState = syncState.name,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )

/**
 * `["jazz", "favorite"]` -> `",jazz,favorite,"`. The leading/trailing commas let
 * `RecordDao`'s tag filter use `LIKE '%,' || :tag || ',%'` to match a whole tag without
 * matching a tag that merely contains [tag] as a substring.
 */
internal fun tagsToColumn(tags: List<String>): String =
    if (tags.isEmpty()) "" else tags.joinToString(separator = ",", prefix = ",", postfix = ",")

internal fun tagsFromColumn(column: String): List<String> =
    column.trim(',').let { if (it.isEmpty()) emptyList() else it.split(",") }
