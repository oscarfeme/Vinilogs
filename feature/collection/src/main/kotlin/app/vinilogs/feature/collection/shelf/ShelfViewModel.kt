package app.vinilogs.feature.collection.shelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vinilogs.core.data.repository.CollectionRepository
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.SyncState
import app.vinilogs.feature.collection.model.ShelfLayout
import app.vinilogs.feature.collection.model.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Backs [ShelfScreen]. Observes [CollectionRepository.observeCollection] and
 * [CollectionRepository.observeSyncState] per this task's spec (T-15) -- Room is the source of
 * truth (ADR-2), so this never waits on network to show the user their own shelf.
 *
 * Search (FR-B7) is applied client-side against whatever [CollectionRepository.observeCollection]
 * emits: the fixed `CollectionRepository` contract (02-ARCHITECTURE.md §4) takes only a filter
 * and a sort, not a query, so incremental search is this ViewModel's job, not the repository's.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShelfViewModel
    @Inject
    constructor(
        private val collectionRepository: CollectionRepository,
    ) : ViewModel() {
        private val query = MutableStateFlow("")
        private val filter = MutableStateFlow(CollectionFilter())
        private val sort = MutableStateFlow(CollectionSort.DATE_ADDED)
        private val layout = MutableStateFlow(ShelfLayout.GRID)

        /**
         * Bumped by [retry]. [StateFlow] conflates equal values, so re-emitting the same
         * filter/sort wouldn't otherwise re-trigger [flatMapLatest]'s resubscription to
         * [CollectionRepository.observeCollection] after that flow terminated on an exception.
         */
        private val retryTrigger = MutableStateFlow(0)

        /**
         * Query/filter/sort/layout captured together, atomically, per subscription cycle -- and
         * paired with the records they produced *inside* [flatMapLatest]'s lambda below, rather
         * than recombined afterwards against the raw source flows. Recombining afterwards is a
         * classic diamond-dependency glitch: [filter] updates its `StateFlow.value` synchronously
         * the instant [setFilter] is called, while the records that actually reflect the new
         * filter only arrive once [flatMapLatest] finishes cancelling the old
         * [CollectionRepository.observeCollection] subscription and collects the new one -- a
         * separately-recombined `filter` would then briefly pair the *new* filter with the *old*
         * records. Bundling both into one params flow and deriving records from it inside the
         * same lambda makes that pairing atomic, so every emitted [ShelfUiState] is internally
         * consistent.
         */
        private val params =
            combine(query, filter, sort, layout, retryTrigger) { q, f, s, l, _ -> ShelfParams(q, f, s, l) }

        private val recordsForParams: Flow<Pair<ShelfParams, List<Record>>> =
            params.flatMapLatest { current ->
                collectionRepository.observeCollection(current.filter, current.sort).map { records ->
                    val visible =
                        if (current.query.isBlank()) records else records.filter { it.matchesQuery(current.query) }
                    current to visible
                }
            }

        val uiState: StateFlow<ShelfUiState> =
            combine(recordsForParams, collectionRepository.observeSyncState()) { (currentParams, records), syncState ->
                ShelfUiState(
                    records = records,
                    isLoading = false,
                    query = currentParams.query,
                    filter = currentParams.filter,
                    sort = currentParams.sort,
                    layout = currentParams.layout,
                    pendingSyncCount = records.pendingSyncCount(syncState),
                    error = null,
                )
            }.catch { throwable ->
                emit(ShelfUiState(isLoading = false, error = UiText.Raw(throwable.messageOrDefault())))
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = ShelfUiState(),
            )

        fun setQuery(value: String) {
            query.value = value
        }

        fun setFilter(value: CollectionFilter) {
            filter.value = value
        }

        fun clearFilters() {
            filter.value = CollectionFilter()
        }

        fun clearSearchAndFilters() {
            query.value = ""
            filter.value = CollectionFilter()
        }

        fun setSort(value: CollectionSort) {
            sort.value = value
        }

        fun setLayout(value: ShelfLayout) {
            layout.value = value
        }

        /** Re-subscribes to [CollectionRepository.observeCollection] after a [ShelfUiState.error]. */
        fun retry() {
            retryTrigger.update { it + 1 }
        }

        /**
         * Per-record [Record.syncState] is the primary signal; the collection-wide aggregate from
         * [CollectionRepository.observeSyncState] is folded in so a pending sync is never
         * under-reported even if the aggregate updates a beat before the per-record rows do.
         */
        private fun List<Record>.pendingSyncCount(aggregate: SyncState): Int {
            val perRecord = count { it.syncState == SyncState.PENDING }
            return if (aggregate == SyncState.PENDING) maxOf(perRecord, 1) else perRecord
        }

        private data class ShelfParams(
            val query: String,
            val filter: CollectionFilter,
            val sort: CollectionSort,
            val layout: ShelfLayout,
        )

        private companion object {
            const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        }
    }

private fun Record.matchesQuery(query: String): Boolean {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return true
    return artist.lowercase().contains(needle) ||
        title.lowercase().contains(needle) ||
        label?.lowercase()?.contains(needle) == true ||
        catalogNumber?.lowercase()?.contains(needle) == true ||
        tags.any { it.lowercase().contains(needle) }
}

private fun Throwable.messageOrDefault(): String = message ?: "Something went wrong loading your shelf."
