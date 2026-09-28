package app.vinilogs.feature.auth.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vinilogs.core.data.repository.AuthRepository
import app.vinilogs.core.data.repository.AvatarUploader
import app.vinilogs.core.data.repository.UserRepository
import app.vinilogs.core.model.ProfileUpdate
import app.vinilogs.feature.auth.validation.validateBio
import app.vinilogs.feature.auth.validation.validateDisplayName
import app.vinilogs.feature.auth.validation.validateLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val displayName: String = "",
    val bio: String = "",
    val location: String = "",
    val isPublic: Boolean = true,
    /** The already-saved, uploaded avatar URL -- what a fresh screen loads from the profile. */
    val avatarUrl: String? = null,
    /** A newly-picked-but-not-yet-saved local image, shown as the live preview in place of
     * [avatarUrl] until [onSaveClick] resolves it to a real URL. */
    val pendingAvatarUri: Uri? = null,
    val displayNameError: String? = null,
    val bioError: String? = null,
    val locationError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
) {
    /** What [app.vinilogs.core.designsystem.component.Avatar] should render right now. */
    val avatarPreview: Any? get() = pendingAvatarUri ?: avatarUrl
}

/**
 * Drives [app.vinilogs.feature.auth.EditProfileScreen] (FR-A4/FR-A5). Prefills from the
 * signed-in user's current [app.vinilogs.core.model.UserProfile] on init; [onSaveClick] uploads
 * a newly-picked avatar (if any) via [avatarUploader] before writing the rest of the fields
 * through [UserRepository.updateProfile] -- see [AvatarUploader]'s KDoc for why avatar upload
 * is a separate step from the fixed [UserRepository] contract.
 */
@HiltViewModel
class EditProfileViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
        private val avatarUploader: AvatarUploader,
    ) : ViewModel() {
        private val uiStateInternal = MutableStateFlow(EditProfileUiState())
        val uiState: StateFlow<EditProfileUiState> = uiStateInternal.asStateFlow()

        private val saveSucceededChannel = Channel<Unit>(Channel.BUFFERED)
        val saveSucceeded: Flow<Unit> = saveSucceededChannel.receiveAsFlow()

        private var uid: String? = null

        init {
            viewModelScope.launch {
                val user = authRepository.currentUser.filterNotNull().first()
                uid = user.uid
                val profile = userRepository.observeProfile(user.uid).first()
                uiStateInternal.update {
                    if (profile == null) {
                        it.copy(isLoading = false)
                    } else {
                        it.copy(
                            displayName = profile.displayName,
                            bio = profile.bio.orEmpty(),
                            location = profile.location.orEmpty(),
                            isPublic = profile.isPublic,
                            avatarUrl = profile.avatarUrl,
                            isLoading = false,
                        )
                    }
                }
            }
        }

        /**
         * A single transform-based setter for every editable field (display name, bio, location,
         * privacy, picked avatar) -- same shape as `AddEditRecordViewModel.updateDraft`, and for
         * the same reason: one callback per field would put
         * [app.vinilogs.feature.auth.EditProfileScreenContent] over detekt's `LongParameterList`
         * threshold. Any edit clears every validation/general error, matching `updateDraft`'s
         * "reset all errors on any change" behaviour rather than per-field error clearing.
         */
        fun onFieldChange(transform: (EditProfileUiState) -> EditProfileUiState) {
            uiStateInternal.update {
                transform(it).copy(
                    displayNameError = null,
                    bioError = null,
                    locationError = null,
                    generalError = null,
                )
            }
        }

        fun onSaveClick() {
            val state = uiStateInternal.value
            val displayNameError = validateDisplayName(state.displayName)
            val bioError = validateBio(state.bio)
            val locationError = validateLocation(state.location)
            if (displayNameError != null || bioError != null || locationError != null) {
                uiStateInternal.update {
                    it.copy(displayNameError = displayNameError, bioError = bioError, locationError = locationError)
                }
                return
            }
            val currentUid = uid ?: return

            uiStateInternal.update { it.copy(isSaving = true, generalError = null) }
            viewModelScope.launch {
                val resolvedAvatarUrl = state.pendingAvatarUri?.let { uri -> uploadAvatar(currentUid, uri) }
                if (state.pendingAvatarUri != null && resolvedAvatarUrl == null) {
                    return@launch // uploadAvatar already recorded the failure in uiState.
                }

                userRepository
                    .updateProfile(
                        ProfileUpdate(
                            displayName = state.displayName.trim(),
                            avatarUrl = resolvedAvatarUrl ?: state.avatarUrl,
                            bio = state.bio.trim().ifBlank { null },
                            location = state.location.trim().ifBlank { null },
                            isPublic = state.isPublic,
                        ),
                    ).onSuccess {
                        uiStateInternal.update { it.copy(isSaving = false, pendingAvatarUri = null) }
                        saveSucceededChannel.send(Unit)
                    }.onFailure {
                        uiStateInternal.update {
                            it.copy(isSaving = false, generalError = "Couldn't save your profile. Please try again.")
                        }
                    }
            }
        }

        /** Null return means the failure was already written into [uiStateInternal]. */
        private suspend fun uploadAvatar(uid: String, uri: Uri): String? =
            avatarUploader.upload(uid, uri).fold(
                onSuccess = { it },
                onFailure = {
                    uiStateInternal.update {
                        it.copy(isSaving = false, generalError = "Couldn't upload your photo. Please try again.")
                    }
                    null
                },
            )
    }
