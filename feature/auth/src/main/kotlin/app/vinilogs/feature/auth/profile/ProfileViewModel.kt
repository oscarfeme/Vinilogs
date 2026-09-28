package app.vinilogs.feature.auth.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vinilogs.core.data.repository.AuthRepository
import app.vinilogs.core.data.repository.UserRepository
import app.vinilogs.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    /** True until the first (uid, profile) pair has arrived -- distinct from [profile] being
     * null once loaded, which means "signed out" or "no profile document yet". */
    val isLoading: Boolean = true,
)

/**
 * Drives [app.vinilogs.feature.auth.ProfileScreen] -- the signed-in user's own profile
 * (02-ARCHITECTURE.md §2 "View another collector's shelf" is the same shape for a *different*
 * uid, T-23's job; this ViewModel only ever looks at the signed-in user's own uid).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        userRepository: UserRepository,
    ) : ViewModel() {
        val uiState: StateFlow<ProfileUiState> =
            authRepository.currentUser
                .flatMapLatest { user ->
                    if (user == null) {
                        flowOf(ProfileUiState(profile = null, isLoading = false))
                    } else {
                        userRepository.observeProfile(user.uid).map { profile ->
                            ProfileUiState(profile = profile, isLoading = false)
                        }
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    initialValue = ProfileUiState(),
                )

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
