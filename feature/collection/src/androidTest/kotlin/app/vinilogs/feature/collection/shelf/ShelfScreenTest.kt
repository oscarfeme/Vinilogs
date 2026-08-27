package app.vinilogs.feature.collection.shelf

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.core.testing.createVinilogsComposeRule
import app.vinilogs.core.testing.fixture.RecordFixtures
import app.vinilogs.core.testing.setVinilogsContent
import app.vinilogs.feature.collection.model.ShelfLayout
import app.vinilogs.feature.collection.model.UiText
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented Compose UI test for [ShelfScreenContent] (T-15). Exercises the stateless
 * content composable directly with a plain [ShelfUiState]/[ShelfActions] -- no Hilt/DI needed,
 * since [ShelfScreen] only adds a `hiltViewModel()`-sourced ViewModel on top of this.
 *
 * Needs `connectedDebugAndroidTest` (a device/emulator): this module's Compose UI test
 * dependency is wired as `androidTestImplementation` project-wide (AndroidCompose.kt), so
 * these run as instrumented tests, not under a JVM unit-test runner.
 */
class ShelfScreenTest {
    @get:Rule
    val composeTestRule = createVinilogsComposeRule()

    private fun noopActions(
        onRecordClick: (String) -> Unit = {},
        onAddRecordClick: () -> Unit = {},
        onStatsClick: () -> Unit = {},
        onQueryChange: (String) -> Unit = {},
        onFilterApply: (CollectionFilter) -> Unit = {},
        onClearFilters: () -> Unit = {},
        onClearSearchAndFilters: () -> Unit = {},
        onSortChange: (CollectionSort) -> Unit = {},
        onLayoutChange: (ShelfLayout) -> Unit = {},
        onRetry: () -> Unit = {},
    ) = ShelfActions(
        onRecordClick = onRecordClick,
        onAddRecordClick = onAddRecordClick,
        onStatsClick = onStatsClick,
        onQueryChange = onQueryChange,
        onFilterApply = onFilterApply,
        onClearFilters = onClearFilters,
        onClearSearchAndFilters = onClearSearchAndFilters,
        onSortChange = onSortChange,
        onLayoutChange = onLayoutChange,
        onRetry = onRetry,
    )

    @Test
    fun emptyShelf_showsEmptyStateAndInvokesAddRecord() {
        var addClicked = false
        composeTestRule.setVinilogsContent {
            ShelfScreenContent(
                uiState = ShelfUiState(records = emptyList(), isLoading = false),
                actions = noopActions(onAddRecordClick = { addClicked = true }),
            )
        }

        composeTestRule.onNodeWithText("Your shelf is empty. Add your first record.").assertExists()
        composeTestRule.onNodeWithText("Add record").performClick()
        assert(addClicked)
    }

    @Test
    fun searchWithNoMatches_showsSearchEmptyStateAndClearAction() {
        var clearClicked = false
        composeTestRule.setVinilogsContent {
            ShelfScreenContent(
                uiState = ShelfUiState(records = emptyList(), isLoading = false, query = "nonexistent"),
                actions = noopActions(onClearSearchAndFilters = { clearClicked = true }),
            )
        }

        composeTestRule.onNodeWithText("No records match your search.").assertExists()
        composeTestRule.onNodeWithText("Clear search").performClick()
        assert(clearClicked)
    }

    @Test
    fun gridWithRecords_rendersACover() {
        val records = RecordFixtures.records(count = 5)
        composeTestRule.setVinilogsContent {
            ShelfScreenContent(
                uiState = ShelfUiState(records = records, isLoading = false, layout = ShelfLayout.GRID),
                actions = noopActions(),
            )
        }

        val first = records.first()
        composeTestRule.onNodeWithContentDescription("${first.title}, ${first.artist}").assertExists()
    }

    @Test
    fun tappingLayoutToggle_switchesGridToList() {
        var requestedLayout: ShelfLayout? = null
        composeTestRule.setVinilogsContent {
            ShelfScreenContent(
                uiState = ShelfUiState(records = RecordFixtures.records(count = 3), isLoading = false, layout = ShelfLayout.GRID),
                actions = noopActions(onLayoutChange = { requestedLayout = it }),
            )
        }

        composeTestRule.onNodeWithContentDescription("Switch to list view").performClick()
        assert(requestedLayout == ShelfLayout.LIST)
    }

    @Test
    fun pendingSyncCount_showsNonBlockingIndicator() {
        composeTestRule.setVinilogsContent {
            ShelfScreenContent(
                uiState = ShelfUiState(records = RecordFixtures.records(count = 2), isLoading = false, pendingSyncCount = 2),
                actions = noopActions(),
            )
        }

        composeTestRule.onNodeWithText("2 records pending sync").assertExists()
    }

    @Test
    fun errorState_retryInvokesCallback() {
        var retried = false
        composeTestRule.setVinilogsContent {
            ShelfScreenContent(
                uiState = ShelfUiState(isLoading = false, error = UiText.Raw("Broke")),
                actions = noopActions(onRetry = { retried = true }),
            )
        }

        composeTestRule.onNodeWithText("Retry").performClick()
        assert(retried)
    }
}
