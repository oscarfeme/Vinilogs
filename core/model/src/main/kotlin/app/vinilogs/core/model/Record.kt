package app.vinilogs.core.model

import java.time.Instant

/**
 * The owner's full record, including the private fields (purchasePrice, purchaseDate, rating,
 * notes) that [PublicRecord] deliberately omits (02-ARCHITECTURE.md §3).
 */
data class Record(
    val id: String,
    val artist: String,
    val title: String,
    val year: Int?,
    val label: String?,
    val catalogNumber: String?,
    val format: Format,
    val speed: Speed,
    val condition: Condition,
    val purchasePrice: Double?,
    val purchaseDate: Instant?,
    val rating: Int?,
    val notes: String?,
    val coverUrl: String?,
    val discogsId: Long?,
    val tags: List<String>,
    val syncState: SyncState,
    val createdAt: Instant,
    val updatedAt: Instant,
)
