package app.vinilogs.feature.collection.addedit

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.vinilogs.core.testing.createVinilogsComposeRule
import app.vinilogs.core.testing.setVinilogsContent
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented Compose UI test for [AddEditRecordContent] (T-17). Needs
 * `connectedDebugAndroidTest` (a device/emulator) -- this module's Compose UI test dependency
 * is wired as `androidTestImplementation` project-wide, so these run as instrumented tests, not
 * under a JVM unit-test runner. Could not be run in this environment (no emulator available).
 */
class AddEditRecordScreenTest {
    @get:Rule
    val composeTestRule = createVinilogsComposeRule()

    @Test
    fun addMode_showsAddTitleAndEmptyFields() {
        composeTestRule.setVinilogsContent {
            AddEditRecordContent(
                uiState = AddEditRecordUiState(mode = AddEditMode.ADD),
                onDraftChange = {},
                onSave = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Add record").assertExists()
        composeTestRule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun editMode_showsEditTitle() {
        composeTestRule.setVinilogsContent {
            AddEditRecordContent(
                uiState = AddEditRecordUiState(
                    mode = AddEditMode.EDIT,
                    draft = RecordDraft(artist = "Miles Davis", title = "Kind of Blue"),
                ),
                onDraftChange = {},
                onSave = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Edit record").assertExists()
    }

    @Test
    fun notFound_showsErrorStateAndBackAction() {
        var backClicked = false
        composeTestRule.setVinilogsContent {
            AddEditRecordContent(
                uiState = AddEditRecordUiState(notFound = true),
                onDraftChange = {},
                onSave = {},
                onNavigateBack = { backClicked = true },
            )
        }

        composeTestRule.onNodeWithText("This record no longer exists.").assertExists()
        composeTestRule.onNodeWithText("Back").performClick()
        assert(backClicked)
    }

    @Test
    fun validationError_showsSupportingTextUnderArtistField() {
        composeTestRule.setVinilogsContent {
            AddEditRecordContent(
                uiState = AddEditRecordUiState(errors = RecordDraftErrors(artist = "Artist is required")),
                onDraftChange = {},
                onSave = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Artist is required").assertExists()
    }

    @Test
    fun typingIntoArtistField_invokesOnDraftChange() {
        var lastArtist: String? = null
        composeTestRule.setVinilogsContent {
            AddEditRecordContent(
                uiState = AddEditRecordUiState(),
                onDraftChange = { transform -> lastArtist = transform(RecordDraft()).artist },
                onSave = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("e.g. Miles Davis").performTextInput("Nina Simone")
        assert(lastArtist == "Nina Simone")
    }

    @Test
    fun tappingSave_invokesOnSave() {
        var saveClicked = false
        composeTestRule.setVinilogsContent {
            AddEditRecordContent(
                uiState = AddEditRecordUiState(draft = RecordDraft(artist = "A", title = "B")),
                onDraftChange = {},
                onSave = { saveClicked = true },
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Save").performClick()
        assert(saveClicked)
    }
}
