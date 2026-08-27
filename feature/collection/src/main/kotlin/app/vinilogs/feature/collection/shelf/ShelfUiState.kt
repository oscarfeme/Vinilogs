package app.vinilogs.feature.collection.shelf

import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.core.model.Record
import app.vinilogs.feature.collection.model.ShelfLayout
import app.vinilogs.feature.collection.model.UiText

/**
 * Exact shape specified by 02-ARCHITECTURE.md §4 -- used verbatim, not reinvented.
 */
data class ShelfUiState(
    val records: List<Record> = emptyList(),
    val isLoading: Boolean = true,
    val query: String = "",
    val filter: CollectionFilter = CollectionFilter(),
    val sort: CollectionSort = CollectionSort.DATE_ADDED,
    val layout: ShelfLayout = ShelfLayout.GRID,
    val pendingSyncCount: Int = 0,
    val error: UiText? = null,
) {
    /** True once loaded with no active query/filter and nothing on the shelf (FR-B6 empty state). */
    val isShelfEmpty: Boolean
        get() = !isLoading && records.isEmpty() && query.isBlank() && filter == CollectionFilter()

    /** True once loaded with an active query/filter that matched nothing (distinct empty copy). */
    val isSearchEmpty: Boolean
        get() = !isLoading && records.isEmpty() && (query.isNotBlank() || filter != CollectionFilter())

    val hasActiveFilter: Boolean
        get() = filter != CollectionFilter()
}
