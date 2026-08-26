package app.vinilogs.feature.collection.shelf

import app.cash.turbine.test
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.SyncState
import app.vinilogs.core.testing.MainDispatcherExtension
import app.vinilogs.core.testing.fake.FakeCollectionRepository
import app.vinilogs.core.testing.fixture.RecordFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant

/** Pending-sync-count and combined clear behaviour. See [ShelfViewModelTest] for the rest. */
@OptIn(ExperimentalCoroutinesApi::class)
class ShelfViewModelSyncTest {
    // See ShelfViewModelTest for why this needs to be UnconfinedTestDispatcher specifically.
    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension(UnconfinedTestDispatcher())

    private fun viewModel(records: List<Record>): Pair<ShelfViewModel, FakeCollectionRepository> {
        val repository = FakeCollectionRepository(records)
        return ShelfViewModel(repository) to repository
    }

    @Test
    fun `pendingSyncCount reflects per-record pending sync state`() =
        runTest {
            val now = Instant.now()
            val pendingRecord =
                RecordFixtures.records(count = 1).first().copy(
                    id = "pending-1",
                    syncState = SyncState.PENDING,
                    createdAt = now,
                    updatedAt = now,
                )
            val syncedRecord =
                RecordFixtures.records(count = 1).first().copy(
                    id = "synced-1",
                    syncState = SyncState.SYNCED,
                    createdAt = now,
                    updatedAt = now,
                )
            val (viewModel, _) = viewModel(records = listOf(pendingRecord, syncedRecord))

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(1, state.pendingSyncCount)
            }
        }

    @Test
    fun `aggregate PENDING sync state is reflected even with no per-record pending rows`() =
        runTest {
            val (viewModel, repository) = viewModel(records = RecordFixtures.records(count = 5))
            repository.setSyncState(SyncState.PENDING)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.pendingSyncCount >= 1)
            }
        }

    @Test
    fun `clearSearchAndFilters resets both query and filter`() =
        runTest {
            val (viewModel, _) = viewModel(records = RecordFixtures.records(count = 20))

            viewModel.uiState.test {
                awaitItem()
                viewModel.setQuery("xyz")
                awaitItem()
                viewModel.setFilter(CollectionFilter(minRating = 4))
                awaitItem()
                // clearSearchAndFilters sets two separate MutableStateFlows (query, then filter),
                // each a distinct emission -- await both and assert on the final, settled state.
                viewModel.clearSearchAndFilters()
                awaitItem()
                val cleared = awaitItem()
                assertEquals("", cleared.query)
                assertEquals(CollectionFilter(), cleared.filter)
            }
        }
}
