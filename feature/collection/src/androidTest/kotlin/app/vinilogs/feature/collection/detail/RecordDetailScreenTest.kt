package app.vinilogs.feature.collection.detail

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.vinilogs.core.testing.createVinilogsComposeRule
import app.vinilogs.core.testing.fixture.RecordFixtures
import app.vinilogs.core.testing.setVinilogsContent
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented Compose UI test for [RecordDetailContent] (T-18). Needs
 * `connectedDebugAndroidTest` (a device/emulator) -- this module's Compose UI test dependency
 * is wired as `androidTestImplementation` project-wide, so these run as instrumented tests, not
 * under a JVM unit-test runner. Could not be run in this environment (no emulator available).
 */
class RecordDetailScreenTest {
    @get:Rule
    val composeTestRule = createVinilogsComposeRule()

    private fun noopActions(
        onEditClick: () -> Unit = {},
        onNavigateBack: () -> Unit = {},
        onShare: () -> Unit = {},
        onDeleteConfirmed: () -> Unit = {},
        onUndoDelete: () -> Unit = {},
    ) = RecordDetailActions(
        onEditClick = onEditClick,
        onNavigateBack = onNavigateBack,
        onShare = onShare,
        onDeleteConfirmed = onDeleteConfirmed,
        onUndoDelete = onUndoDelete,
    )

    @Test
    fun showsArtistTitleAndFields() {
        val record = RecordFixtures.records(count = 1).first()
        composeTestRule.setVinilogsContent {
            RecordDetailContent(uiState = RecordDetailUiState(record = record, isLoading = false), actions = noopActions())
        }

        composeTestRule.onNodeWithText(record.title).assertExists()
        composeTestRule.onNodeWithText(record.artist).assertExists()
    }

    @Test
    fun notFound_showsErrorStateAndBackAction() {
        var backClicked = false
        composeTestRule.setVinilogsContent {
            RecordDetailContent(
                uiState = RecordDetailUiState(notFound = true, isLoading = false),
                actions = noopActions(onNavigateBack = { backClicked = true }),
            )
        }

        composeTestRule.onNodeWithText("This record no longer exists.").assertExists()
        composeTestRule.onNodeWithText("Back").performClick()
        assert(backClicked)
    }

    @Test
    fun tappingDelete_showsConfirmationDialog_andConfirmingInvokesCallback() {
        val record = RecordFixtures.records(count = 1).first()
        var deleteConfirmed = false
        composeTestRule.setVinilogsContent {
            RecordDetailContent(
                uiState = RecordDetailUiState(record = record, isLoading = false),
                actions = noopActions(onDeleteConfirmed = { deleteConfirmed = true }),
            )
        }

        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        composeTestRule.onNodeWithText("Delete this record?").assertExists()
        composeTestRule.onNodeWithText("Delete").performClick()
        assert(deleteConfirmed)
    }

    @Test
    fun showUndo_rendersUndoBarAndInvokesCallback() {
        var undoClicked = false
        composeTestRule.setVinilogsContent {
            RecordDetailContent(
                uiState = RecordDetailUiState(showUndo = true, isLoading = false),
                actions = noopActions(onUndoDelete = { undoClicked = true }),
            )
        }

        composeTestRule.onNodeWithText("Record deleted.").assertExists()
        composeTestRule.onNodeWithText("Undo").performClick()
        assert(undoClicked)
    }

    @Test
    fun tappingShare_invokesCallback() {
        val record = RecordFixtures.records(count = 1).first()
        var shareClicked = false
        composeTestRule.setVinilogsContent {
            RecordDetailContent(
                uiState = RecordDetailUiState(record = record, isLoading = false),
                actions = noopActions(onShare = { shareClicked = true }),
            )
        }

        composeTestRule.onNodeWithContentDescription("Share").performClick()
        assert(shareClicked)
    }

    @Test
    fun tappingEdit_invokesCallback() {
        val record = RecordFixtures.records(count = 1).first()
        var editClicked = false
        composeTestRule.setVinilogsContent {
            RecordDetailContent(
                uiState = RecordDetailUiState(record = record, isLoading = false),
                actions = noopActions(onEditClick = { editClicked = true }),
            )
        }

        composeTestRule.onNodeWithContentDescription("Edit").performClick()
        assert(editClicked)
    }
}
