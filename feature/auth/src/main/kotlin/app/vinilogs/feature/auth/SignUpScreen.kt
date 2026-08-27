package app.vinilogs.feature.auth

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import app.vinilogs.core.designsystem.component.VinilogsTopBar
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.feature.auth.component.AuthTextField
import app.vinilogs.feature.auth.signup.SignUpUiState
import app.vinilogs.feature.auth.signup.SignUpViewModel

// Real implementation for T-09 (FR-A1). Same thin-shell-plus-stateless-content
// split as SignInScreen.kt.

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.signUpSucceeded.collect { onSignUpSuccess() }
    }

    SignUpScreenContent(
        uiState = uiState,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSignUpClick = viewModel::onSignUpClick,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@Composable
internal fun SignUpScreenContent(
    uiState: SignUpUiState,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        VinilogsTopBar(
            title = "Sign up",
            onNavigateBack = onNavigateBack,
            navigationIconContentDescription = "Back",
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = MaterialTheme.spacing.screenHorizontal,
                    vertical = MaterialTheme.spacing.xl,
                ),
        ) {
            SignUpFields(
                uiState = uiState,
                onDisplayNameChange = onDisplayNameChange,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onSignUpClick = onSignUpClick,
            )

            Spacer(Modifier.height(MaterialTheme.spacing.xl))
            Button(
                onClick = onSignUpClick,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaterialTheme.spacing.minTouchTarget)
                    .testTag("signUpSubmitButton"),
            ) {
                SubmitButtonLabel(isLoading = uiState.isLoading, label = "Create account")
            }
        }
    }
}

@Composable
private fun SignUpFields(
    uiState: SignUpUiState,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    AuthTextField(
        value = uiState.displayName,
        onValueChange = onDisplayNameChange,
        label = "Display name",
        errorMessage = uiState.displayNameError,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        modifier = Modifier.testTag("signUpDisplayNameField"),
    )
    Spacer(Modifier.height(MaterialTheme.spacing.lg))

    AuthTextField(
        value = uiState.email,
        onValueChange = onEmailChange,
        label = "Email",
        errorMessage = uiState.emailError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        modifier = Modifier.testTag("signUpEmailField"),
    )
    Spacer(Modifier.height(MaterialTheme.spacing.lg))

    SignUpPasswordField(uiState, onPasswordChange, onSignUpClick, focusManager, passwordVisible) {
        passwordVisible = !passwordVisible
    }

    AuthGeneralError(uiState.generalError, testTag = "signUpGeneralError")
}

@Composable
private fun SignUpPasswordField(
    uiState: SignUpUiState,
    onPasswordChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
    focusManager: FocusManager,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
) {
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
                onSignUpClick()
            },
        ),
        trailingIcon = { PasswordVisibilityToggle(passwordVisible, onTogglePasswordVisible) },
        modifier = Modifier.testTag("signUpPasswordField"),
    )
}
