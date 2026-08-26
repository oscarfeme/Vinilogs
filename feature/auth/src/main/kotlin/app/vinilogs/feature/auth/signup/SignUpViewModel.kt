package app.vinilogs.feature.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vinilogs.core.data.repository.AuthRepository
import app.vinilogs.feature.auth.error.toAuthErrorMessage
import app.vinilogs.feature.auth.validation.validateDisplayName
import app.vinilogs.feature.auth.validation.validateEmail
import app.vinilogs.feature.auth.validation.validatePasswordForSignUp
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

data class SignUpUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val displayNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class SignUpViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val uiStateInternal = MutableStateFlow(SignUpUiState())
        val uiState: StateFlow<SignUpUiState> = uiStateInternal.asStateFlow()

        private val signUpSucceededChannel = Channel<Unit>(Channel.BUFFERED)
        val signUpSucceeded: Flow<Unit> = signUpSucceededChannel.receiveAsFlow()

        fun onDisplayNameChange(value: String) {
            uiStateInternal.update { it.copy(displayName = value, displayNameError = null, generalError = null) }
        }

        fun onEmailChange(value: String) {
            uiStateInternal.update { it.copy(email = value, emailError = null, generalError = null) }
        }

        fun onPasswordChange(value: String) {
            uiStateInternal.update { it.copy(password = value, passwordError = null, generalError = null) }
        }

        fun onSignUpClick() {
            val state = uiStateInternal.value
            val displayNameError = validateDisplayName(state.displayName)
            val emailError = validateEmail(state.email)
            val passwordError = validatePasswordForSignUp(state.password)
            if (displayNameError != null || emailError != null || passwordError != null) {
                uiStateInternal.update {
                    it.copy(
                        displayNameError = displayNameError,
                        emailError = emailError,
                        passwordError = passwordError,
                    )
                }
                return
            }

            uiStateInternal.update { it.copy(isLoading = true, generalError = null) }
            viewModelScope.launch {
                authRepository
                    .signUp(
                        email = state.email.trim(),
                        password = state.password,
                        displayName = state.displayName.trim(),
                    ).onSuccess {
                        uiStateInternal.update { it.copy(isLoading = false) }
                        signUpSucceededChannel.send(Unit)
                    }.onFailure { error ->
                        uiStateInternal.update {
                            it.copy(isLoading = false, generalError = error.toAuthErrorMessage())
                        }
                    }
            }
        }
    }
