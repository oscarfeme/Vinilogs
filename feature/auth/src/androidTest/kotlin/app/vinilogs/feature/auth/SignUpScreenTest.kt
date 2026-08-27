package app.vinilogs.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.vinilogs.core.testing.createVinilogsComposeRule
import app.vinilogs.core.testing.setVinilogsContent
import app.vinilogs.feature.auth.signup.SignUpUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignUpScreenTest {
    @get:Rule
    val composeRule = createVinilogsComposeRule()

    @Test
    fun tappingSubmit_invokesOnSignUpClick() {
        var clicked = false
        composeRule.setVinilogsContent {
            SignUpScreenContent(
                uiState = SignUpUiState(),
                onDisplayNameChange = {},
                onEmailChange = {},
                onPasswordChange = {},
                onSignUpClick = { clicked = true },
                onNavigateBack = {},
            )
        }

        composeRule.onNodeWithTag("signUpSubmitButton").performClick()

        assertTrue(clicked)
    }

    @Test
    fun typingIntoDisplayNameField_invokesOnDisplayNameChange() {
        var typed = ""
        composeRule.setVinilogsContent {
            SignUpScreenContent(
                uiState = SignUpUiState(),
                onDisplayNameChange = { typed = it },
                onEmailChange = {},
                onPasswordChange = {},
                onSignUpClick = {},
                onNavigateBack = {},
            )
        }

        composeRule.onNodeWithTag("signUpDisplayNameField").performTextInput("New User")

        assertEquals("New User", typed)
    }

    @Test
    fun fieldErrors_areDisplayed_whenPresent() {
        composeRule.setVinilogsContent {
            SignUpScreenContent(
                uiState = SignUpUiState(
                    displayNameError = "Enter your name.",
                    emailError = "Enter a valid email address.",
                    passwordError = "Password must be at least 8 characters.",
                ),
                onDisplayNameChange = {},
                onEmailChange = {},
                onPasswordChange = {},
                onSignUpClick = {},
                onNavigateBack = {},
            )
        }

        composeRule.onNodeWithTag("signUpDisplayNameField").assertIsDisplayed()
        composeRule.onNodeWithTag("signUpEmailField").assertIsDisplayed()
        composeRule.onNodeWithTag("signUpPasswordField").assertIsDisplayed()
    }

    @Test
    fun generalError_isDisplayed_whenPresent() {
        composeRule.setVinilogsContent {
            SignUpScreenContent(
                uiState = SignUpUiState(generalError = "An account with this email already exists."),
                onDisplayNameChange = {},
                onEmailChange = {},
                onPasswordChange = {},
                onSignUpClick = {},
                onNavigateBack = {},
            )
        }

        composeRule.onNodeWithTag("signUpGeneralError").assertIsDisplayed()
    }
}
