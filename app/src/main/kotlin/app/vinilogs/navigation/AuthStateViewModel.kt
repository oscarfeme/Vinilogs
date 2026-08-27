package app.vinilogs.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vinilogs.core.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Drives [VinilogsNavHost]'s choice of start graph from `AuthRepository.currentUser`
 * (02-ARCHITECTURE.md §4 / §5: "auth (start if signed out) ... main (start if
 * signed in)"). T-09.
 */
@HiltViewModel
class AuthStateViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
    ) : ViewModel() {
        val authState: StateFlow<AuthState> = authRepository.currentUser
            .map { user -> if (user != null) AuthState.SignedIn else AuthState.SignedOut }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = AuthState.Loading,
            )

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
