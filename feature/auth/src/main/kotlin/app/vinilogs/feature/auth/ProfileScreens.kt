package app.vinilogs.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// ProfileScreen and EditProfileScreen got their real implementations in T-19
// (ProfileScreen.kt, EditProfileScreen.kt). SettingsScreen stays a stub -- it isn't explicitly
// assigned in 03-PHASES-AND-TASKS.md and T-19's brief explicitly excludes it; leave it for
// whichever later polish task claims it (sign-out almost certainly lives here).

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Settings", modifier)
}
