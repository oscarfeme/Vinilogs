package app.vinilogs.core.data.remote.discogs

import app.vinilogs.core.model.CatalogResult

/**
 * Maps a [DiscogsSearchResult] to the domain [CatalogResult] (ADR-3: Discogs DTOs never leave
 * `core:data`, and once mapped a record has no further dependency on Discogs).
 *
 * Discogs' `title` field is `"Artist - Title"` in one string, not separate fields -- split on
 * the first `" - "`. If a result doesn't follow that shape (no separator found, which does
 * happen for some Discogs entries), the whole string is treated as the title with an empty
 * artist, since guessing wrong would silently corrupt data the user is about to save; an empty
 * artist is visibly wrong and the field is editable before save either way (FR-B2).
 */
internal fun DiscogsSearchResult.toCatalogResult(): CatalogResult {
    val (artist, title) = splitArtistAndTitle(title.orEmpty())
    return CatalogResult(
        discogsId = id,
        artist = artist,
        title = title,
        year = year?.toIntOrNull(),
        label = label?.firstOrNull(),
        catalogNumber = catalogNumber,
        coverUrl = coverImage ?: thumbnail,
    )
}

private const val ARTIST_TITLE_SEPARATOR = " - "

private fun splitArtistAndTitle(raw: String): Pair<String, String> {
    val separatorIndex = raw.indexOf(ARTIST_TITLE_SEPARATOR)
    if (separatorIndex < 0) return "" to raw
    val artist = raw.substring(0, separatorIndex)
    val title = raw.substring(separatorIndex + ARTIST_TITLE_SEPARATOR.length)
    return artist to title
}
