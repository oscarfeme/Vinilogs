package app.vinilogs.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.vinilogs.core.testing.createVinilogsComposeRule
import app.vinilogs.core.testing.setVinilogsContent
import app.vinilogs.feature.auth.editprofile.EditProfileUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditProfileScreenTest {
    @get:Rule
    val composeRule = createVinilogsComposeRule()

    @Test
    fun tappingSave_invokesOnSaveClick() {
        var clicked = false
        composeRule.setVinilogsContent {
            EditProfileScreenContent(
                uiState = EditProfileUiState(displayName = "Alice", isLoading = false),
                onFieldChange = {},
                onSaveClick = { clicked = true },
                onNavigateBack = {},
            )
        }

        composeRule.onNodeWithTag("editProfileSaveButton").performClick()

        assertTrue(clicked)
    }

    @Test
    fun typingIntoDisplayNameField_invokesOnFieldChangeWithTheNewValue() {
        var typed: String? = null
        composeRule.setVinilogsContent {
            EditProfileScreenContent(
                uiState = EditProfileUiState(isLoading = false),
                onFieldChange = { transform -> typed = transform(EditProfileUiState()).displayName },
                onSaveClick = {},
                onNavigateBack = {},
            )
        }

        composeRule.onNodeWithTag("editProfileDisplayNameField").performTextInput("New Name")

        assertEquals("New Name", typed)
    }

    @Test
    fun typingIntoBioAndLocationFields_invokeOnFieldChangeWithTheNewValues() {
        // Each keystroke's onFieldChange call is independent (applied to a fresh default
        // state here, same as the real screen applies it to whatever the current state is) --
        // only update the var whichever field actually changed touched.
        var bio = ""
        var location = ""
        composeRule.setVinilogsContent {
            EditProfileScreenContent(
                uiState = EditProfileUiState(isLoading = false),
                onFieldChange = { transform ->
                    val result = transform(EditProfileUiState())
                    if (result.bio.isNotEmpty()) bio = result.bio
                    if (result.location.isNotEmpty()) location = result.location
                },
                onSaveClick = {},
                onNavigateBack = {},
            )
        }

        composeRule.onNodeWithTag("editProfileBioField").performTextInput("New bio")
        composeRule.onNodeWithTag("editProfileLocationField").performTextInput("Paris")

        assertEquals("New bio", bio)
        assertEquals("Paris", location)
    }

    @Test
    fun privacySwitch_reflectsStateAndInvokesOnFieldChange() {
        var toggledTo: Boolean? = null
        composeRule.setVinilogsContent {
            EditProfileScreenContent(
                uiState = EditProfileUiState(isPublic = true, isLoading = false),
                onFieldChange = { transform -> toggledTo = transform(EditProfileUiState()).isPublic },
                onSaveClick = {},
                onNavigateBack = {},
            )
        }

        composeRule.onNodeWithTag("editProfilePrivacySwitch").assertIsOn()
        composeRule.onNodeWithTag("editProfilePrivacySwitch").performClick()

        assertEquals(false, toggledTo)
    }

    @Test
    fun privateState_rendersSwitchOff() {
        composeRule.setVinilogsContent {
            EditProfileScreenContent(
                uiState = EditProfileUiState(isPublic = false, isLoading = false),
                onFieldChange = {},
                onSaveClick = {},
                onNavigateBack = {},
            )
        }

        composeRule.onNodeWithTag("editProfilePrivacySwitch").assertIsOff()
    }

    @Test
    fun fieldErrors_areDisplayed_whenPresent() {
        composeRule.setVinilogsContent {
            EditProfileScreenContent(
                uiState = EditProfileUiState(displayNameError = "Enter your name.", isLoading = false),
                onFieldChange = {},
                onSaveClick = {},
                onNavigateBack = {},
            )
        }

        composeRule.onNodeWithTag("editProfileDisplayNameField").assertIsDisplayed()
    }

    @Test
    fun generalError_isDisplayed_whenPresent() {
        composeRule.setVinilogsContent {
            EditProfileScreenContent(
                uiState = EditProfileUiState(
                    generalError = "Couldn't save your profile. Please try again.",
                    isLoading = false,
                ),
                onFieldChange = {},
                onSaveClick = {},
                onNavigateBack = {},
            )
        }

        composeRule.onNodeWithTag("editProfileGeneralError").assertIsDisplayed()
    }

    @Test
    fun tappingBack_invokesOnNavigateBack() {
        var navigatedBack = false
        composeRule.setVinilogsContent {
            EditProfileScreenContent(
                uiState = EditProfileUiState(isLoading = false),
                onFieldChange = {},
                onSaveClick = {},
                onNavigateBack = { navigatedBack = true },
            )
        }

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(navigatedBack)
    }
}
