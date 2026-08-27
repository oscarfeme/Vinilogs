package app.vinilogs.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.vinilogs.core.testing.createVinilogsComposeRule
import app.vinilogs.core.testing.setVinilogsContent
import app.vinilogs.feature.auth.signin.SignInUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Drives [SignInScreenContent] directly with plain state and callback spies,
 * bypassing Hilt entirely -- no test runner/rule setup needed. T-09.
 */
class SignInScreenTest {
    @get:Rule
    val composeRule = createVinilogsComposeRule()

    @Test
    fun tappingSubmit_invokesOnSignInClick() {
        var clicked = false
        composeRule.setVinilogsContent {
            SignInScreenContent(
                uiState = SignInUiState(),
                onEmailChange = {},
                onPasswordChange = {},
                onSignInClick = { clicked = true },
                onNavigateToSignUp = {},
                onNavigateToForgotPassword = {},
            )
        }

        composeRule.onNodeWithTag("signInSubmitButton").performClick()

        assertTrue(clicked)
    }

    @Test
    fun typingIntoEmailField_invokesOnEmailChange() {
        var typed = ""
        composeRule.setVinilogsContent {
            SignInScreenContent(
                uiState = SignInUiState(),
                onEmailChange = { typed = it },
                onPasswordChange = {},
                onSignInClick = {},
                onNavigateToSignUp = {},
                onNavigateToForgotPassword = {},
            )
        }

        composeRule.onNodeWithTag("signInEmailField").performTextInput("user@example.com")

        assertEquals("user@example.com", typed)
    }

    @Test
    fun generalError_isDisplayed_whenPresent() {
        composeRule.setVinilogsContent {
            SignInScreenContent(
                uiState = SignInUiState(generalError = "Incorrect email or password."),
                onEmailChange = {},
                onPasswordChange = {},
                onSignInClick = {},
                onNavigateToSignUp = {},
                onNavigateToForgotPassword = {},
            )
        }

        composeRule.onNodeWithTag("signInGeneralError").assertIsDisplayed()
    }

    @Test
    fun tappingSignUpLink_invokesOnNavigateToSignUp() {
        var navigated = false
        composeRule.setVinilogsContent {
            SignInScreenContent(
                uiState = SignInUiState(),
                onEmailChange = {},
                onPasswordChange = {},
                onSignInClick = {},
                onNavigateToSignUp = { navigated = true },
                onNavigateToForgotPassword = {},
            )
        }

        composeRule.onNodeWithTag("signInGoToSignUpLink").performClick()

        assertTrue(navigated)
    }

    @Test
    fun tappingForgotPasswordLink_invokesOnNavigateToForgotPassword() {
        var navigated = false
        composeRule.setVinilogsContent {
            SignInScreenContent(
                uiState = SignInUiState(),
                onEmailChange = {},
                onPasswordChange = {},
                onSignInClick = {},
                onNavigateToSignUp = {},
                onNavigateToForgotPassword = { navigated = true },
            )
        }

        composeRule.onNodeWithTag("signInForgotPasswordLink").performClick()

        assertTrue(navigated)
    }

    @Test
    fun signInTitle_isDisplayed() {
        composeRule.setVinilogsContent {
            SignInScreenContent(
                uiState = SignInUiState(),
                onEmailChange = {},
                onPasswordChange = {},
                onSignInClick = {},
                onNavigateToSignUp = {},
                onNavigateToForgotPassword = {},
            )
        }

        composeRule.onNodeWithTag("signInTitle").assertIsDisplayed()
    }
}
