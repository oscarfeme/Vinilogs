package app.vinilogs.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import app.vinilogs.core.designsystem.component.EmptyState
import app.vinilogs.core.designsystem.component.VinilogsTopBar
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.core.designsystem.theme.vinilogsColors
import app.vinilogs.feature.auth.component.AuthTextField
import app.vinilogs.feature.auth.forgotpassword.ForgotPasswordUiState
import app.vinilogs.feature.auth.forgotpassword.ForgotPasswordViewModel

// Real implementation for T-09 (FR-A3). Same thin-shell-plus-stateless-content
// split as SignInScreen.kt.

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    ForgotPasswordScreenContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onSendResetClick = viewModel::onSendResetClick,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@Composable
internal fun ForgotPasswordScreenContent(
    uiState: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSendResetClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        VinilogsTopBar(
            title = "Forgot password",
            onNavigateBack = onNavigateBack,
            navigationIconContentDescription = "Back",
        )

        if (uiState.isSubmitted) {
            EmptyState(
                message = "If an account exists for that email, we've sent instructions to reset the password.",
                actionLabel = "Back to sign in",
                onAction = onNavigateBack,
                modifier = Modifier.testTag("forgotPasswordConfirmation"),
            )
        } else {
            ForgotPasswordForm(uiState, onEmailChange, onSendResetClick)
        }
    }
}

@Composable
private fun ForgotPasswordForm(
    uiState: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSendResetClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = MaterialTheme.spacing.screenHorizontal,
                vertical = MaterialTheme.spacing.xl,
            ),
    ) {
        Text(
            text = "Enter the email for your account and we'll send you a link to reset your password.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.vinilogsColors.textSecondary,
        )
        Spacer(Modifier.height(MaterialTheme.spacing.xl))

        AuthTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = "Email",
            errorMessage = uiState.emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSendResetClick() }),
            modifier = Modifier.testTag("forgotPasswordEmailField"),
        )

        AuthGeneralError(uiState.generalError, testTag = "forgotPasswordGeneralError")

        Spacer(Modifier.height(MaterialTheme.spacing.xl))
        Button(
            onClick = onSendResetClick,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.spacing.minTouchTarget)
                .testTag("forgotPasswordSubmitButton"),
        ) {
            SubmitButtonLabel(isLoading = uiState.isLoading, label = "Send reset link")
        }
    }
}
