package app.vinilogs.feature.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.vinilogs.core.designsystem.component.Avatar
import app.vinilogs.core.designsystem.component.VinilogsTopBar
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.core.designsystem.theme.vinilogsColors
import app.vinilogs.feature.auth.component.AuthTextField
import app.vinilogs.feature.auth.editprofile.EditProfileUiState
import app.vinilogs.feature.auth.editprofile.EditProfileViewModel

// Real implementation for T-19 (FR-A4, FR-A5). Same thin-shell-plus-stateless-content split as
// SignUpScreen.kt.

@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.saveSucceeded.collect { onNavigateBack() }
    }

    EditProfileScreenContent(
        uiState = uiState,
        onFieldChange = viewModel::onFieldChange,
        onSaveClick = viewModel::onSaveClick,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

/**
 * [onFieldChange] bundles every editable field into one draft-transform callback -- same shape
 * as `AddEditRecordScreenContent`'s `onDraftChange`, and for the same reason: a callback per
 * field (display name, bio, location, privacy, picked avatar) would put this composable over
 * detekt's `LongParameterList` threshold.
 */
@Composable
internal fun EditProfileScreenContent(
    uiState: EditProfileUiState,
    onFieldChange: ((EditProfileUiState) -> EditProfileUiState) -> Unit,
    onSaveClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        VinilogsTopBar(
            title = "Edit profile",
            onNavigateBack = onNavigateBack,
            navigationIconContentDescription = "Back",
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.screenHorizontal, vertical = MaterialTheme.spacing.xl),
        ) {
            AvatarPicker(
                uiState = uiState,
                onAvatarPicked = { uri -> onFieldChange { it.copy(pendingAvatarUri = uri) } },
            )
            Spacer(Modifier.height(MaterialTheme.spacing.xl))

            EditProfileFields(uiState = uiState, onFieldChange = onFieldChange)
            AuthGeneralError(uiState.generalError, testTag = "editProfileGeneralError")

            Spacer(Modifier.height(MaterialTheme.spacing.xl))
            Button(
                onClick = onSaveClick,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaterialTheme.spacing.minTouchTarget)
                    .testTag("editProfileSaveButton"),
            ) {
                SubmitButtonLabel(isLoading = uiState.isSaving, label = "Save")
            }
        }
    }
}

@Composable
private fun EditProfileFields(
    uiState: EditProfileUiState,
    onFieldChange: ((EditProfileUiState) -> EditProfileUiState) -> Unit,
) {
    AuthTextField(
        value = uiState.displayName,
        onValueChange = { value -> onFieldChange { it.copy(displayName = value) } },
        label = "Display name",
        errorMessage = uiState.displayNameError,
        modifier = Modifier.testTag("editProfileDisplayNameField"),
    )
    Spacer(Modifier.height(MaterialTheme.spacing.lg))

    AuthTextField(
        value = uiState.bio,
        onValueChange = { value -> onFieldChange { it.copy(bio = value) } },
        label = "Bio",
        errorMessage = uiState.bioError,
        singleLine = false,
        modifier = Modifier.testTag("editProfileBioField"),
    )
    Spacer(Modifier.height(MaterialTheme.spacing.lg))

    AuthTextField(
        value = uiState.location,
        onValueChange = { value -> onFieldChange { it.copy(location = value) } },
        label = "Location",
        errorMessage = uiState.locationError,
        modifier = Modifier.testTag("editProfileLocationField"),
    )
    Spacer(Modifier.height(MaterialTheme.spacing.xl))

    PrivacyToggleRow(
        isPublic = uiState.isPublic,
        onPrivacyToggle = { isPublic -> onFieldChange { it.copy(isPublic = isPublic) } },
    )
}

@Composable
private fun AvatarPicker(uiState: EditProfileUiState, onAvatarPicked: (Uri) -> Unit) {
    val pickAvatar = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) onAvatarPicked(uri) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Avatar(
            model = uiState.avatarPreview,
            displayName = uiState.displayName,
            modifier = Modifier
                .size(96.dp)
                .testTag("editProfileAvatar"),
        )
        TextButton(
            onClick = {
                pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.testTag("editProfileChangePhotoButton"),
        ) {
            Text("Change photo", style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * FR-A5: "Public (discoverable) or private (hidden from search and profile views); default
 * public." The label always states the *current* state plainly rather than describing the
 * toggle action, so a screen reader announcing "Public profile, on" is unambiguous.
 */
@Composable
private fun PrivacyToggleRow(isPublic: Boolean, onPrivacyToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = "Public profile", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (isPublic) {
                    "Discoverable in search and visible to other collectors."
                } else {
                    "Hidden from search and other collectors' views."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.vinilogsColors.textSecondary,
            )
        }
        Switch(
            checked = isPublic,
            onCheckedChange = onPrivacyToggle,
            modifier = Modifier.testTag("editProfilePrivacySwitch"),
        )
    }
}
