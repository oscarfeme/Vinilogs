package app.vinilogs.core.model

/**
 * One Discogs search hit (FR-B1). Mapped into a [Record] at save time — once a record is
 * saved it has no further dependency on Discogs (ADR-3).
 */
data class CatalogResult(
    val discogsId: Long,
    val artist: String,
    val title: String,
    val year: Int?,
    val label: String?,
    val catalogNumber: String?,
    val coverUrl: String?,
)
