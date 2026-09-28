package app.vinilogs.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.vinilogs.core.designsystem.component.Avatar
import app.vinilogs.core.designsystem.component.LoadingState
import app.vinilogs.core.designsystem.component.VinilogsTopBar
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.core.designsystem.theme.vinilogsColors
import app.vinilogs.core.model.UserProfile
import app.vinilogs.feature.auth.profile.ProfileUiState
import app.vinilogs.feature.auth.profile.ProfileViewModel

// Real implementation for T-19 (FR-A4, FR-A5's read side). Same thin-shell-plus-stateless-content
// split as SignInScreen.kt/SignUpScreen.kt.

@Composable
fun ProfileScreen(
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    ProfileScreenContent(
        uiState = uiState,
        onNavigateToEditProfile = onNavigateToEditProfile,
        onNavigateToSettings = onNavigateToSettings,
        modifier = modifier,
    )
}

@Composable
internal fun ProfileScreenContent(
    uiState: ProfileUiState,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        VinilogsTopBar(
            title = "Profile",
            actions = {
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.testTag("profileSettingsButton"),
                ) {
                    Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                }
            },
        )
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())
            uiState.profile == null -> ProfileUnavailable()
            else -> ProfileBody(
                profile = uiState.profile,
                onNavigateToEditProfile = onNavigateToEditProfile,
            )
        }
    }
}

@Composable
private fun ProfileBody(profile: UserProfile, onNavigateToEditProfile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.screenHorizontal, vertical = MaterialTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfileHeader(profile)
        Spacer(Modifier.height(MaterialTheme.spacing.lg))
        ProfileMeta(profile)
        Spacer(Modifier.height(MaterialTheme.spacing.xl))
        HorizontalDivider(color = MaterialTheme.vinilogsColors.hairline)
        Spacer(Modifier.height(MaterialTheme.spacing.lg))
        Button(
            onClick = onNavigateToEditProfile,
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.spacing.minTouchTarget)
                .testTag("profileEditButton"),
        ) {
            Text("Edit profile")
        }
    }
}

@Composable
private fun ProfileHeader(profile: UserProfile) {
    Avatar(
        model = profile.avatarUrl,
        displayName = profile.displayName,
        modifier = Modifier
            .size(96.dp)
            .testTag("profileAvatar"),
    )
    Spacer(Modifier.height(MaterialTheme.spacing.md))
    Text(
        text = profile.displayName,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.testTag("profileDisplayName"),
    )
    val location = profile.location
    if (!location.isNullOrBlank()) {
        Text(
            text = location,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.vinilogsColors.textSecondary,
        )
    }
    val bio = profile.bio
    if (!bio.isNullOrBlank()) {
        Spacer(Modifier.height(MaterialTheme.spacing.sm))
        Text(
            text = bio,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("profileBio"),
        )
    }
}

@Composable
private fun ProfileMeta(profile: UserProfile) {
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xl)) {
        Text(
            text = "${profile.recordCount} records",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.vinilogsColors.textSecondary,
        )
        Text(
            text = if (profile.isPublic) "Public" else "Private",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.vinilogsColors.textSecondary,
            modifier = Modifier.testTag("profilePrivacyLabel"),
        )
    }
}

/**
 * Shown when there is no signed-in user or no profile document yet -- should be unreachable in
 * practice (this screen is only navigated to from behind the signed-in graph), but a real state
 * is safer than assuming a non-null profile and crashing.
 */
@Composable
private fun ProfileUnavailable() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Your profile isn't available right now.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("profileUnavailable"),
        )
    }
}
