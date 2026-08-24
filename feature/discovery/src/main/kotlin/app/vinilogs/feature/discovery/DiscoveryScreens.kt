package app.vinilogs.feature.discovery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Stub bodies only — real implementations land in T-23 (Discover,
// PublicProfile), T-24 (PublicRecord, SharedRecords). Signatures are the
// contract this task fixes.

@Composable
fun DiscoverScreen(
    onUserClick: (uid: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Discover", modifier)
}

@Composable
fun PublicProfileScreen(
    uid: String,
    onRecordClick: (recordId: String) -> Unit,
    onSharedRecordsClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Public Profile: $uid", modifier)
}

@Composable
fun PublicRecordScreen(
    uid: String,
    recordId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Public Record: $uid/$recordId", modifier)
}

@Composable
fun SharedRecordsScreen(
    uid: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Shared Records: $uid", modifier)
}
