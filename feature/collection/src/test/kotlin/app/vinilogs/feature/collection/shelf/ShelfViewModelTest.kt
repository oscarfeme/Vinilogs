package app.vinilogs.feature.collection.shelf

import app.cash.turbine.test
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.core.model.Condition
import app.vinilogs.core.model.Format
import app.vinilogs.core.model.Record
import app.vinilogs.core.testing.MainDispatcherExtension
import app.vinilogs.core.testing.fake.FakeCollectionRepository
import app.vinilogs.core.testing.fixture.RecordFixtures
import app.vinilogs.feature.collection.model.ShelfLayout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Covers query/filter/sort/layout state changes. Sync-state and clear-both-query-and-filter
 * cases live in [ShelfViewModelSyncTest] -- split so neither test class trips detekt's
 * per-class TooManyFunctions threshold (config/detekt/detekt.yml).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShelfViewModelTest {
    // UnconfinedTestDispatcher, not the extension's StandardTestDispatcher default: ShelfViewModel's
    // `uiState` is built with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(...), ...)`, whose
    // sharing coroutine launches on Dispatchers.Main (viewModelScope) -- a *different* TestDispatcher
    // instance from the one `runTest {}` below drives internally. StandardTestDispatcher only runs
    // queued work when its own scheduler is advanced, and nothing here advances Main's scheduler
    // specifically, so the sharing coroutine would never actually run. UnconfinedTestDispatcher runs
    // dispatched work eagerly instead, sidestepping the two-scheduler mismatch entirely.
    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension(UnconfinedTestDispatcher())

    private fun viewModel(records: List<Record> = RecordFixtures.records(count = 20)): ShelfViewModel =
        ShelfViewModel(FakeCollectionRepository(records))

    @Test
    fun `initial state exposes the seeded collection, unfiltered, grid layout`() =
        runTest {
            val viewModel = viewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(20, state.records.size)
                assertEquals(ShelfLayout.GRID, state.layout)
                assertEquals(CollectionFilter(), state.filter)
                assertEquals("", state.query)
                assertFalse(state.isLoading)
            }
        }

    @Test
    fun `setQuery filters by artist case-insensitively`() =
        runTest {
            val records = RecordFixtures.records(count = 30)
            val viewModel = viewModel(records)
            val expectedCount = records.count { it.artist.equals("Miles Davis", ignoreCase = true) }

            viewModel.uiState.test {
                awaitItem() // initial
                viewModel.setQuery("miles davis")
                val filtered = awaitItem()
                assertEquals(expectedCount, filtered.records.size)
                assertTrue(filtered.records.all { it.artist.equals("Miles Davis", ignoreCase = true) })
            }
        }

    @Test
    fun `setQuery matching nothing produces isSearchEmpty`() =
        runTest {
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                viewModel.setQuery("no such artist or title exists")
                val filtered = awaitItem()
                assertTrue(filtered.records.isEmpty())
                assertTrue(filtered.isSearchEmpty)
                assertFalse(filtered.isShelfEmpty)
            }
        }

    @Test
    fun `empty collection produces isShelfEmpty, not isSearchEmpty`() =
        runTest {
            val viewModel = viewModel(records = emptyList())

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.records.isEmpty())
                assertTrue(state.isShelfEmpty)
                assertFalse(state.isSearchEmpty)
            }
        }

    @Test
    fun `setFilter narrows by format`() =
        runTest {
            val records = RecordFixtures.records(count = 40)
            val viewModel = viewModel(records)
            val expectedCount = records.count { it.format == Format.LP }

            viewModel.uiState.test {
                awaitItem()
                viewModel.setFilter(CollectionFilter(format = Format.LP))
                val filtered = awaitItem()
                assertEquals(expectedCount, filtered.records.size)
                assertTrue(filtered.records.all { it.format == Format.LP })
                assertTrue(filtered.hasActiveFilter)
            }
        }

    @Test
    fun `clearFilters resets to the unfiltered collection`() =
        runTest {
            val records = RecordFixtures.records(count = 20)
            val viewModel = viewModel(records)

            viewModel.uiState.test {
                awaitItem()
                viewModel.setFilter(CollectionFilter(condition = Condition.MINT))
                awaitItem()
                viewModel.clearFilters()
                val cleared = awaitItem()
                assertEquals(records.size, cleared.records.size)
                assertFalse(cleared.hasActiveFilter)
            }
        }

    @Test
    fun `setSort orders by artist`() =
        runTest {
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                viewModel.setSort(CollectionSort.ARTIST)
                val sorted = awaitItem()
                assertEquals(CollectionSort.ARTIST, sorted.sort)
                val artists = sorted.records.map { it.artist.lowercase() }
                assertEquals(artists.sorted(), artists)
            }
        }

    @Test
    fun `setLayout toggles between grid and list`() =
        runTest {
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                viewModel.setLayout(ShelfLayout.LIST)
                assertEquals(ShelfLayout.LIST, awaitItem().layout)
            }
        }
}
