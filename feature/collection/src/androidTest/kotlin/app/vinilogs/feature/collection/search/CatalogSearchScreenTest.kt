package app.vinilogs.feature.collection.search

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.vinilogs.core.model.CatalogResult
import app.vinilogs.core.testing.createVinilogsComposeRule
import app.vinilogs.core.testing.setVinilogsContent
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented Compose UI test for [CatalogSearchContent] (T-16, FR-B1/FR-B2). Exercises the
 * stateless content composable directly with a plain [CatalogSearchUiState]/[CatalogSearchActions]
 * -- no Hilt/DI needed, matching [app.vinilogs.feature.collection.shelf.ShelfScreenTest]'s
 * pattern. Needs `connectedDebugAndroidTest` (a device/emulator) -- could not be run in this
 * environment (no emulator available, per this task's environment notes).
 */
class CatalogSearchScreenTest {
    @get:Rule
    val composeTestRule = createVinilogsComposeRule()

    private fun sampleResult() =
        CatalogResult(
            discogsId = 1L,
            artist = "Miles Davis",
            title = "Kind of Blue",
            year = 1959,
            label = "Columbia",
            catalogNumber = "CL 1355",
            coverUrl = null,
        )

    private fun noopActions(
        onQueryChange: (String) -> Unit = {},
        onResultClick: (CatalogResult) -> Unit = {},
        onManualEntry: () -> Unit = {},
        onRetry: () -> Unit = {},
        onLoadMore: () -> Unit = {},
        onNavigateBack: () -> Unit = {},
    ) = CatalogSearchActions(
        onQueryChange = onQueryChange,
        onResultClick = onResultClick,
        onManualEntry = onManualEntry,
        onRetry = onRetry,
        onLoadMore = onLoadMore,
        onNavigateBack = onNavigateBack,
    )

    @Test
    fun idleState_showsPromptAndManualEntryAction() {
        var manualClicked = false
        composeTestRule.setVinilogsContent {
            CatalogSearchContent(
                uiState = CatalogSearchUiState(),
                actions = noopActions(onManualEntry = { manualClicked = true }),
            )
        }

        composeTestRule.onNodeWithText("Search Discogs by artist, album or catalogue number.").assertExists()
        composeTestRule.onNodeWithText("Add manually").assertExists()
        composeTestRule.onNodeWithText("Add manually").performClick()
        assert(manualClicked)
    }

    @Test
    fun topBar_alwaysOffersManualEntry_evenWithResultsShowing() {
        var manualClicked = false
        composeTestRule.setVinilogsContent {
            CatalogSearchContent(
                uiState = CatalogSearchUiState(results = listOf(sampleResult()), hasSearched = true),
                actions = noopActions(onManualEntry = { manualClicked = true }),
            )
        }

        // FR-B1: manual entry is offered on every state, not only failure/empty ones.
        composeTestRule.onNodeWithText("Add manually").performClick()
        assert(manualClicked)
    }

    @Test
    fun loadingState_showsSearchingMessage() {
        composeTestRule.setVinilogsContent {
            CatalogSearchContent(
                uiState = CatalogSearchUiState(query = "miles", isLoading = true),
                actions = noopActions(),
            )
        }

        composeTestRule.onNodeWithText("Searching...").assertExists()
    }

    @Test
    fun noResultsState_showsQueryInMessageAndManualEntryAction() {
        composeTestRule.setVinilogsContent {
            CatalogSearchContent(
                uiState = CatalogSearchUiState(query = "zzzzz", hasSearched = true, results = emptyList()),
                actions = noopActions(),
            )
        }

        composeTestRule.onNodeWithText("No matches for \"zzzzz\".").assertExists()
        composeTestRule.onNodeWithText("Add manually").assertExists()
    }

    @Test
    fun offlineFailure_showsDistinctMessageWithManualEntryPrimaryAndRetrySecondary() {
        var manualClicked = false
        var retried = false
        composeTestRule.setVinilogsContent {
            CatalogSearchContent(
                uiState = CatalogSearchUiState(query = "miles", hasSearched = true, failure = CatalogSearchFailure.OFFLINE),
                actions = noopActions(onManualEntry = { manualClicked = true }, onRetry = { retried = true }),
            )
        }

        composeTestRule.onNodeWithText("You're offline. Check your connection and try again.").assertExists()
        composeTestRule.onNodeWithText("Add manually").performClick()
        assert(manualClicked)
        composeTestRule.onNodeWithText("Retry").performClick()
        assert(retried)
    }

    @Test
    fun rateLimitedFailure_showsDistinctMessage() {
        composeTestRule.setVinilogsContent {
            CatalogSearchContent(
                uiState =
                    CatalogSearchUiState(query = "miles", hasSearched = true, failure = CatalogSearchFailure.RATE_LIMITED),
                actions = noopActions(),
            )
        }

        composeTestRule.onNodeWithText("Too many searches at once. Wait a moment and try again.").assertExists()
    }

    @Test
    fun resultsState_rendersACoverAndInvokesOnResultClick() {
        var selected: CatalogResult? = null
        val result = sampleResult()
        composeTestRule.setVinilogsContent {
            CatalogSearchContent(
                uiState = CatalogSearchUiState(query = "miles", hasSearched = true, results = listOf(result)),
                actions = noopActions(onResultClick = { selected = it }),
            )
        }

        composeTestRule.onNodeWithContentDescription("${result.title}, ${result.artist}").assertExists()
        composeTestRule.onNodeWithContentDescription("${result.title}, ${result.artist}").performClick()
        assert(selected == result)
    }

    @Test
    fun canLoadMore_showsLoadMoreButtonAndInvokesOnLoadMore() {
        var loadMoreClicked = false
        composeTestRule.setVinilogsContent {
            CatalogSearchContent(
                uiState =
                    CatalogSearchUiState(
                        query = "miles",
                        hasSearched = true,
                        results = listOf(sampleResult()),
                        canLoadMore = true,
                    ),
                actions = noopActions(onLoadMore = { loadMoreClicked = true }),
            )
        }

        composeTestRule.onNodeWithText("Load more").performClick()
        assert(loadMoreClicked)
    }

    @Test
    fun typingIntoSearchField_invokesOnQueryChange() {
        var lastQuery: String? = null
        composeTestRule.setVinilogsContent {
            CatalogSearchContent(
                uiState = CatalogSearchUiState(),
                actions = noopActions(onQueryChange = { lastQuery = it }),
            )
        }

        composeTestRule.onNodeWithText("Search by artist, album or catalogue number").performTextInput("Nina Simone")
        assert(lastQuery == "Nina Simone")
    }

    @Test
    fun tappingBack_invokesOnNavigateBack() {
        var backClicked = false
        composeTestRule.setVinilogsContent {
            CatalogSearchContent(
                uiState = CatalogSearchUiState(),
                actions = noopActions(onNavigateBack = { backClicked = true }),
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(backClicked)
    }
}
