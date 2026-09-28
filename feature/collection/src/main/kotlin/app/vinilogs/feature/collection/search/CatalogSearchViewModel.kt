package app.vinilogs.feature.collection.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vinilogs.core.data.remote.discogs.DiscogsFailure
import app.vinilogs.core.data.repository.CollectionRepository
import app.vinilogs.core.model.CatalogResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs [CatalogSearchScreen] (T-16, FR-B1/FR-B2). Debounces keystrokes 400ms before calling
 * [CollectionRepository.searchCatalog] -- the exact figure 02-ARCHITECTURE.md §2's "Catalogue
 * lookup (Discogs)" data-flow example specifies ("AddRecordSearchScreen -> debounce 400 ms ->
 * CollectionRepository.searchCatalog(q, page)"). The Discogs rate-limit debounce/caching
 * itself lives in `core:data`'s client (T-12); this is the call-site debounce that example
 * calls out as this task's job.
 *
 * [DiscogsFailure.NotFound] is deliberately *not* surfaced as [CatalogSearchUiState.failure]:
 * per that type's own doc comment ("a search with zero results is treated as 'not found' by
 * the caller if it chooses to"), this caller chooses to fold it into the plain no-results state
 * rather than showing an error banner for what is, from the user's point of view, just an empty
 * result set.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
internal class CatalogSearchViewModel
    @Inject
    constructor(
        private val collectionRepository: CollectionRepository,
    ) : ViewModel() {
        private val queryInput = MutableStateFlow("")
        private val _uiState = MutableStateFlow(CatalogSearchUiState())
        val uiState: StateFlow<CatalogSearchUiState> = _uiState.asStateFlow()

        private var lastFetchedPage = 0

        init {
            viewModelScope.launch {
                // No distinctUntilChanged: a user who clears the field then retypes the exact
                // same query must still trigger a fresh search (the Discogs client's own 24h
                // OkHttp cache, T-12, already makes a repeated identical query cheap).
                queryInput
                    .debounce(SEARCH_DEBOUNCE_MS)
                    .filter { it.isNotBlank() }
                    .collectLatest { query -> runSearch(query, page = FIRST_PAGE, append = false) }
            }
        }

        /** Called on every keystroke -- updates the visible field immediately; the actual search is debounced. */
        fun setQuery(value: String) {
            queryInput.value = value
            _uiState.update {
                if (value.isBlank()) CatalogSearchUiState(query = value) else it.copy(query = value)
            }
        }

        /** Re-runs the current query from page 1 -- the "Retry" action on any failure state. */
        fun retry() {
            val query = _uiState.value.query
            if (query.isBlank()) return
            viewModelScope.launch { runSearch(query, page = FIRST_PAGE, append = false) }
        }

        /** FR-B1 "paginated" -- fetches the next page and appends it to the current results. */
        fun loadMore() {
            val state = _uiState.value
            if (!state.canAcceptLoadMore()) return
            viewModelScope.launch { runSearch(state.query, page = lastFetchedPage + 1, append = true) }
        }

        private fun CatalogSearchUiState.canAcceptLoadMore(): Boolean {
            val alreadyFetching = isLoading || isLoadingMore
            return canLoadMore && !alreadyFetching && query.isNotBlank()
        }

        private suspend fun runSearch(query: String, page: Int, append: Boolean) {
            _uiState.update {
                if (append) it.copy(isLoadingMore = true) else it.copy(query = query, isLoading = true, failure = null)
            }
            collectionRepository.searchCatalog(query, page).fold(
                onSuccess = { catalogResults -> onSearchSuccess(query, page, append, catalogResults) },
                onFailure = { throwable -> onSearchFailure(query, page, append, throwable) },
            )
        }

        private fun onSearchSuccess(
            query: String,
            page: Int,
            append: Boolean,
            catalogResults: List<CatalogResult>,
        ) {
            lastFetchedPage = page
            _uiState.update {
                it.copy(
                    query = query,
                    results = if (append) it.results + catalogResults else catalogResults,
                    isLoading = false,
                    isLoadingMore = false,
                    hasSearched = true,
                    canLoadMore = catalogResults.isNotEmpty(),
                    failure = null,
                )
            }
        }

        private fun onSearchFailure(
            query: String,
            page: Int,
            append: Boolean,
            throwable: Throwable,
        ) {
            // See class doc: DiscogsFailure.NotFound folds into the ordinary no-results state.
            if (throwable is DiscogsFailure.NotFound) {
                onSearchSuccess(query, page, append, emptyList())
                return
            }
            _uiState.update {
                it.copy(
                    query = query,
                    isLoading = false,
                    isLoadingMore = false,
                    hasSearched = true,
                    failure = throwable.toCatalogSearchFailure(),
                )
            }
        }

        private companion object {
            const val SEARCH_DEBOUNCE_MS = 400L
            const val FIRST_PAGE = 1
        }
    }

private fun Throwable.toCatalogSearchFailure(): CatalogSearchFailure =
    when (this) {
        is DiscogsFailure.Network -> CatalogSearchFailure.OFFLINE
        is DiscogsFailure.RateLimited -> CatalogSearchFailure.RATE_LIMITED
        else -> CatalogSearchFailure.GENERIC
    }
