package app.vinilogs.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.core.designsystem.theme.vinilogsColors
import app.vinilogs.feature.auth.component.AuthTextField
import app.vinilogs.feature.auth.component.AuthTextLink
import app.vinilogs.feature.auth.signin.SignInUiState
import app.vinilogs.feature.auth.signin.SignInViewModel

// Real implementation for T-09 (FR-A1-A3). A thin, Hilt-wired shell around a
// stateless `*Content` composable -- `SignInScreenContent`/`SignInFields` are
// `internal`/private so this module's Compose UI tests can drive the content
// directly with plain state + fakes, without a Hilt test runner.

@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.signInSucceeded.collect { onSignInSuccess() }
    }

    SignInScreenContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSignInClick = viewModel::onSignInClick,
        onNavigateToSignUp = onNavigateToSignUp,
        onNavigateToForgotPassword = onNavigateToForgotPassword,
        modifier = modifier,
    )
}

@Composable
internal fun SignInScreenContent(
    uiState: SignInUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInClick: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = MaterialTheme.spacing.screenHorizontal,
                vertical = MaterialTheme.spacing.xxl,
            ),
    ) {
        Text(
            text = "Sign in",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("signInTitle"),
        )
        Spacer(Modifier.height(MaterialTheme.spacing.xl))

        SignInFields(
            uiState = uiState,
            onEmailChange = onEmailChange,
            onPasswordChange = onPasswordChange,
            onSignInClick = onSignInClick,
            onNavigateToForgotPassword = onNavigateToForgotPassword,
        )

        Spacer(Modifier.height(MaterialTheme.spacing.xl))
        Button(
            onClick = onSignInClick,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.spacing.minTouchTarget)
                .testTag("signInSubmitButton"),
        ) {
            SubmitButtonLabel(isLoading = uiState.isLoading, label = "Sign in")
        }

        Spacer(Modifier.height(MaterialTheme.spacing.xl))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                text = "Don't have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.vinilogsColors.textSecondary,
            )
            AuthTextLink(
                text = "Sign up",
                onClick = onNavigateToSignUp,
                modifier = Modifier.testTag("signInGoToSignUpLink"),
            )
        }
    }
}

@Composable
private fun SignInFields(
    uiState: SignInUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInClick: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    AuthTextField(
        value = uiState.email,
        onValueChange = onEmailChange,
        label = "Email",
        errorMessage = uiState.emailError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        modifier = Modifier.testTag("signInEmailField"),
    )
    Spacer(Modifier.height(MaterialTheme.spacing.lg))

    AuthTextField(
        value = uiState.password,
        onValueChange = onPasswordChange,
        label = "Password",
        errorMessage = uiState.passwordError,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                onSignInClick()
            },
        ),
        trailingIcon = { PasswordVisibilityToggle(passwordVisible) { passwordVisible = !passwordVisible } },
        modifier = Modifier.testTag("signInPasswordField"),
    )

    Spacer(Modifier.height(MaterialTheme.spacing.sm))
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        AuthTextLink(
            text = "Forgot your password?",
            onClick = onNavigateToForgotPassword,
            modifier = Modifier.testTag("signInForgotPasswordLink"),
        )
    }

    AuthGeneralError(uiState.generalError, testTag = "signInGeneralError")
}
