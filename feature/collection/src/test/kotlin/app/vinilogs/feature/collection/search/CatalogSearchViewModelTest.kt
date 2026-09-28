package app.vinilogs.feature.collection.search

import app.cash.turbine.test
import app.vinilogs.core.data.remote.discogs.DiscogsFailure
import app.vinilogs.core.model.CatalogResult
import app.vinilogs.core.testing.MainDispatcherExtension
import app.vinilogs.core.testing.fake.FakeCollectionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

/**
 * Covers [CatalogSearchViewModel]'s debounce, pagination, and DiscogsFailure-to-UI-state
 * mapping (FR-B1/FR-B2). Unlike [app.vinilogs.feature.collection.shelf.ShelfViewModelTest] this
 * ViewModel genuinely delays (the 400ms debounce, 02-ARCHITECTURE.md §2), so the Main dispatcher
 * here is a [StandardTestDispatcher] built on the *same* [testDispatcher] instance `runTest` is
 * given below -- sharing one [kotlinx.coroutines.test.TestCoroutineScheduler] between
 * `viewModelScope` and the test body is what lets [advanceUntilIdle] actually flush the debounce
 * delay, rather than the two-scheduler mismatch `ShelfViewModelTest` sidesteps with
 * `UnconfinedTestDispatcher` (irrelevant there since `ShelfViewModel` has no delay to control).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogSearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension(testDispatcher)

    private fun sampleResult(discogsId: Long = 1L) =
        CatalogResult(
            discogsId = discogsId,
            artist = "Miles Davis",
            title = "Kind of Blue",
            year = 1959,
            label = "Columbia",
            catalogNumber = "CL 1355",
            coverUrl = "https://example.com/cover.jpg",
        )

    @Test
    fun `initial state is idle`() =
        runTest(testDispatcher) {
            val viewModel = CatalogSearchViewModel(FakeCollectionRepository())

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.isIdle)
                assertFalse(state.isLoading)
                assertTrue(state.results.isEmpty())
            }
        }

    @Test
    fun `clearing the query resets to idle immediately, no debounce wait`() =
        runTest(testDispatcher) {
            val repository = FakeCollectionRepository().apply { setCatalogSearchResults(listOf(sampleResult())) }
            val viewModel = CatalogSearchViewModel(repository)

            viewModel.uiState.test {
                awaitItem() // initial idle
                viewModel.setQuery("miles")
                awaitItem() // query set, not yet searched
                viewModel.setQuery("")
                val cleared = awaitItem()
                assertTrue(cleared.isIdle)
                assertEquals("", cleared.query)
            }
        }

    @Test
    fun `typing debounces before searching, then returns results`() =
        runTest(testDispatcher) {
            val results = listOf(sampleResult())
            val repository = FakeCollectionRepository().apply { setCatalogSearchResults(results) }
            val viewModel = CatalogSearchViewModel(repository)

            viewModel.uiState.test {
                awaitItem() // initial idle
                viewModel.setQuery("miles")
                val afterTyping = awaitItem()
                assertFalse(afterTyping.isLoading) // still waiting out the debounce window
                assertTrue(afterTyping.results.isEmpty())

                advanceUntilIdle()
                val loading = awaitItem()
                assertTrue(loading.isLoading)
                val done = awaitItem()
                assertFalse(done.isLoading)
                assertTrue(done.hasSearched)
                assertEquals(results, done.results)
                assertNull(done.failure)
            }
        }

    @Test
    fun `an empty successful result set reports no-results, not a failure`() =
        runTest(testDispatcher) {
            val repository = FakeCollectionRepository()
            val viewModel = CatalogSearchViewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.setQuery("some obscure pressing")
                awaitItem()
                advanceUntilIdle()
                awaitItem() // loading
                val done = awaitItem()
                assertTrue(done.isNoResults)
                assertNull(done.failure)
            }
        }

    @Test
    fun `DiscogsFailure NotFound is folded into no-results, not shown as an error`() =
        runTest(testDispatcher) {
            val repository = FakeCollectionRepository()
            repository.failNextCallWith(DiscogsFailure.NotFound)
            val viewModel = CatalogSearchViewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.setQuery("nothing like this exists")
                awaitItem()
                advanceUntilIdle()
                awaitItem() // loading
                val done = awaitItem()
                assertTrue(done.isNoResults)
                assertNull(done.failure)
            }
        }

    @Test
    fun `DiscogsFailure RateLimited maps to the rate-limited failure state`() =
        runTest(testDispatcher) {
            val repository = FakeCollectionRepository()
            repository.failNextCallWith(DiscogsFailure.RateLimited)
            val viewModel = CatalogSearchViewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.setQuery("miles")
                awaitItem()
                advanceUntilIdle()
                awaitItem() // loading
                val done = awaitItem()
                assertEquals(CatalogSearchFailure.RATE_LIMITED, done.failure)
                assertFalse(done.isNoResults)
            }
        }

    @Test
    fun `DiscogsFailure Network maps to the offline failure state`() =
        runTest(testDispatcher) {
            val repository = FakeCollectionRepository()
            repository.failNextCallWith(DiscogsFailure.Network(IOException("no connectivity")))
            val viewModel = CatalogSearchViewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.setQuery("miles")
                awaitItem()
                advanceUntilIdle()
                awaitItem() // loading
                val done = awaitItem()
                assertEquals(CatalogSearchFailure.OFFLINE, done.failure)
            }
        }

    @Test
    fun `any other failure maps to the generic failure state`() =
        runTest(testDispatcher) {
            val repository = FakeCollectionRepository()
            repository.failNextCallWith(IllegalStateException("boom"))
            val viewModel = CatalogSearchViewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.setQuery("miles")
                awaitItem()
                advanceUntilIdle()
                awaitItem() // loading
                val done = awaitItem()
                assertEquals(CatalogSearchFailure.GENERIC, done.failure)
            }
        }

    @Test
    fun `retry re-runs the current query from page 1`() =
        runTest(testDispatcher) {
            val repository = FakeCollectionRepository()
            repository.failNextCallWith(DiscogsFailure.RateLimited)
            val viewModel = CatalogSearchViewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.setQuery("miles")
                awaitItem()
                advanceUntilIdle()
                awaitItem() // loading
                val failed = awaitItem()
                assertEquals(CatalogSearchFailure.RATE_LIMITED, failed.failure)

                repository.setCatalogSearchResults(listOf(sampleResult()))
                viewModel.retry()
                val retryLoading = awaitItem()
                assertTrue(retryLoading.isLoading)
                val retried = awaitItem()
                assertNull(retried.failure)
                assertEquals(1, retried.results.size)
            }
        }

    @Test
    fun `loadMore appends the next page to existing results`() =
        runTest(testDispatcher) {
            val page1 = listOf(sampleResult(discogsId = 1L))
            val repository = FakeCollectionRepository().apply { setCatalogSearchResults(page1) }
            val viewModel = CatalogSearchViewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.setQuery("miles")
                awaitItem()
                advanceUntilIdle()
                awaitItem() // loading
                val firstPage = awaitItem()
                assertEquals(page1, firstPage.results)
                assertTrue(firstPage.canLoadMore)

                val page2 = listOf(sampleResult(discogsId = 2L))
                repository.setCatalogSearchResults(page2)
                viewModel.loadMore()
                val loadingMore = awaitItem()
                assertTrue(loadingMore.isLoadingMore)
                val appended = awaitItem()
                assertEquals(page1 + page2, appended.results)
            }
        }

    @Test
    fun `loadMore stops offering more once a page comes back empty`() =
        runTest(testDispatcher) {
            val page1 = listOf(sampleResult())
            val repository = FakeCollectionRepository().apply { setCatalogSearchResults(page1) }
            val viewModel = CatalogSearchViewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.setQuery("miles")
                awaitItem()
                advanceUntilIdle()
                awaitItem() // loading
                val firstPage = awaitItem()
                assertTrue(firstPage.canLoadMore)

                repository.setCatalogSearchResults(emptyList())
                viewModel.loadMore()
                awaitItem() // isLoadingMore
                val secondPage = awaitItem()
                assertFalse(secondPage.canLoadMore)
                assertEquals(page1, secondPage.results)
            }
        }
}
