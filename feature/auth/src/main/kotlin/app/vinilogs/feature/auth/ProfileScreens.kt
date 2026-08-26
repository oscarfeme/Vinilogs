package app.vinilogs.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Stub bodies only — real implementations land in T-19 (Profile, EditProfile)
// and a later polish task (Settings isn't explicitly assigned in
// 03-PHASES-AND-TASKS.md; treat it as part of T-19's track until scoped).

@Composable
fun ProfileScreen(
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Profile", modifier)
}

@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Edit Profile", modifier)
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Settings", modifier)
}
