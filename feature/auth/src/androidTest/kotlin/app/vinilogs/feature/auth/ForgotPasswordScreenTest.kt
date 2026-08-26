package app.vinilogs.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.vinilogs.core.testing.createVinilogsComposeRule
import app.vinilogs.core.testing.setVinilogsContent
import app.vinilogs.feature.auth.forgotpassword.ForgotPasswordUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForgotPasswordScreenTest {
    @get:Rule
    val composeRule = createVinilogsComposeRule()

    @Test
    fun tappingSubmit_invokesOnSendResetClick() {
        var clicked = false
        composeRule.setVinilogsContent {
            ForgotPasswordScreenContent(
                uiState = ForgotPasswordUiState(),
                onEmailChange = {},
                onSendResetClick = { clicked = true },
                onNavigateBack = {},
            )
        }

        composeRule.onNodeWithTag("forgotPasswordSubmitButton").performClick()

        assertTrue(clicked)
    }

    @Test
    fun typingIntoEmailField_invokesOnEmailChange() {
        var typed = ""
        composeRule.setVinilogsContent {
            ForgotPasswordScreenContent(
                uiState = ForgotPasswordUiState(),
                onEmailChange = { typed = it },
                onSendResetClick = {},
                onNavigateBack = {},
            )
        }

        composeRule.onNodeWithTag("forgotPasswordEmailField").performTextInput("user@example.com")

        assertEquals("user@example.com", typed)
    }

    @Test
    fun submittedState_showsGenericConfirmation_notTheForm() {
        composeRule.setVinilogsContent {
            ForgotPasswordScreenContent(
                uiState = ForgotPasswordUiState(email = "user@example.com", isSubmitted = true),
                onEmailChange = {},
                onSendResetClick = {},
                onNavigateBack = {},
            )
        }

        composeRule.onNodeWithTag("forgotPasswordConfirmation").assertIsDisplayed()
    }

    @Test
    fun tappingBackToSignInFromConfirmation_invokesOnNavigateBack() {
        var navigatedBack = false
        composeRule.setVinilogsContent {
            ForgotPasswordScreenContent(
                uiState = ForgotPasswordUiState(isSubmitted = true),
                onEmailChange = {},
                onSendResetClick = {},
                onNavigateBack = { navigatedBack = true },
            )
        }

        composeRule.onNodeWithText("Back to sign in").performClick()

        assertTrue(navigatedBack)
    }
}
