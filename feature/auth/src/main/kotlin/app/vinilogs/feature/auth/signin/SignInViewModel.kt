package app.vinilogs.feature.auth.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vinilogs.core.data.repository.AuthRepository
import app.vinilogs.feature.auth.error.toAuthErrorMessage
import app.vinilogs.feature.auth.validation.validateEmail
import app.vinilogs.feature.auth.validation.validatePasswordForSignIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class SignInViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val uiStateInternal = MutableStateFlow(SignInUiState())
        val uiState: StateFlow<SignInUiState> = uiStateInternal.asStateFlow()

        // One-shot navigation signal -- collected once by the screen (LaunchedEffect)
        // to call onSignInSuccess, rather than living in uiState where it could
        // re-fire on recomposition/process restore.
        private val signInSucceededChannel = Channel<Unit>(Channel.BUFFERED)
        val signInSucceeded: Flow<Unit> = signInSucceededChannel.receiveAsFlow()

        fun onEmailChange(value: String) {
            uiStateInternal.update { it.copy(email = value, emailError = null, generalError = null) }
        }

        fun onPasswordChange(value: String) {
            uiStateInternal.update { it.copy(password = value, passwordError = null, generalError = null) }
        }

        fun onSignInClick() {
            val state = uiStateInternal.value
            val emailError = validateEmail(state.email)
            val passwordError = validatePasswordForSignIn(state.password)
            if (emailError != null || passwordError != null) {
                uiStateInternal.update { it.copy(emailError = emailError, passwordError = passwordError) }
                return
            }

            uiStateInternal.update { it.copy(isLoading = true, generalError = null) }
            viewModelScope.launch {
                authRepository
                    .signIn(state.email.trim(), state.password)
                    .onSuccess {
                        uiStateInternal.update { it.copy(isLoading = false) }
                        signInSucceededChannel.send(Unit)
                    }.onFailure { error ->
                        uiStateInternal.update {
                            it.copy(isLoading = false, generalError = error.toAuthErrorMessage())
                        }
                    }
            }
        }
    }
