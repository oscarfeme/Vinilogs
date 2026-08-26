package app.vinilogs.core.data.remote.discogs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Discogs' `database/search` response envelope. Unknown fields are ignored (see `DiscogsModule`'s `Json` config). */
@Serializable
internal data class DiscogsSearchResponse(
    val results: List<DiscogsSearchResult> = emptyList(),
)

/**
 * One Discogs search hit. Discogs combines artist and title into a single `"Artist - Title"`
 * [title] field and represents [year] as a string (both real API quirks, not a modelling
 * choice) -- see [toCatalogResult] for how those get split/parsed back out.
 */
@Serializable
internal data class DiscogsSearchResult(
    val id: Long,
    val title: String? = null,
    val year: String? = null,
    val label: List<String>? = null,
    @SerialName("catno") val catalogNumber: String? = null,
    @SerialName("cover_image") val coverImage: String? = null,
    @SerialName("thumb") val thumbnail: String? = null,
)
