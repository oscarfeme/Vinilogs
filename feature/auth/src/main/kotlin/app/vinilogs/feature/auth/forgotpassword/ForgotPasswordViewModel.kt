package app.vinilogs.feature.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vinilogs.core.data.repository.AuthRepository
import app.vinilogs.feature.auth.error.toAuthErrorMessage
import app.vinilogs.feature.auth.validation.validateEmail
import com.google.firebase.FirebaseNetworkException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    // FR-A3: "generic confirmation shown regardless of whether the address
    // exists" -- true once the request has been sent, whether or not an
    // account actually exists for that email.
    val isSubmitted: Boolean = false,
)

@HiltViewModel
class ForgotPasswordViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val uiStateInternal = MutableStateFlow(ForgotPasswordUiState())
        val uiState: StateFlow<ForgotPasswordUiState> = uiStateInternal.asStateFlow()

        fun onEmailChange(value: String) {
            uiStateInternal.update {
                it.copy(email = value, emailError = null, generalError = null, isSubmitted = false)
            }
        }

        fun onSendResetClick() {
            val state = uiStateInternal.value
            val emailError = validateEmail(state.email)
            if (emailError != null) {
                uiStateInternal.update { it.copy(emailError = emailError) }
                return
            }

            uiStateInternal.update { it.copy(isLoading = true, generalError = null) }
            viewModelScope.launch {
                val result = authRepository.sendPasswordReset(state.email.trim())
                val error = result.exceptionOrNull()
                // Never let a "no such user" failure distinguish itself from success --
                // that would leak whether the address is registered. A genuine
                // connectivity failure is a real operational error, not an
                // existence leak, so it alone skips the generic confirmation.
                val isNetworkFailure = error is FirebaseNetworkException
                uiStateInternal.update {
                    it.copy(
                        isLoading = false,
                        isSubmitted = !isNetworkFailure,
                        generalError = if (isNetworkFailure) error.toAuthErrorMessage() else null,
                    )
                }
            }
        }
    }
