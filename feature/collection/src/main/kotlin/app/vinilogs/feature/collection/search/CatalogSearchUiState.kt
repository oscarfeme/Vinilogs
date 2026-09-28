package app.vinilogs.feature.collection.search

import app.vinilogs.core.model.CatalogResult

/**
 * T-16, FR-B1/FR-B2. Not a fixed contract from 02-ARCHITECTURE.md §4 (only `ShelfUiState` is
 * specified verbatim there) -- shaped freely for this task, per CLAUDE.md rule 7, following the
 * same "computed boolean per required state" style as `ShelfUiState`.
 *
 * States required by FR-B1 -- "graceful empty, offline and rate-limited states that always
 * offer manual entry" -- map onto this shape as:
 *  - **empty (no query yet)**: [isIdle]
 *  - **loading**: [isLoading]
 *  - **no-results**: [isNoResults]
 *  - **offline / rate-limited / other error**: [failure] non-null, distinguished by
 *    [CatalogSearchFailure]
 *  - **results**: [results] non-empty
 */
internal data class CatalogSearchUiState(
    val query: String = "",
    val results: List<CatalogResult> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasSearched: Boolean = false,
    val canLoadMore: Boolean = false,
    val failure: CatalogSearchFailure? = null,
) {
    /** No query typed yet, and no search in flight -- the very first state a user sees. */
    val isIdle: Boolean
        get() = !hasSearched && !isLoading && query.isBlank() && failure == null

    /** A search completed successfully but matched nothing -- distinct copy from [isIdle]. */
    val isNoResults: Boolean
        get() = hasSearched && !isLoading && failure == null && results.isEmpty()
}

/**
 * Distinct failure shapes surfaced to the UI, mapped from [app.vinilogs.core.data.remote.discogs.DiscogsFailure]
 * (T-12) so "offline" and "rate-limited" each get their own message -- FR-B1 requires these be
 * distinguishable from a generic error and from [CatalogSearchUiState.isNoResults].
 */
internal enum class CatalogSearchFailure {
    OFFLINE,
    RATE_LIMITED,
    GENERIC,
}
