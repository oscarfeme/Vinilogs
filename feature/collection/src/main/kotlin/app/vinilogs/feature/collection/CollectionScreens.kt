package app.vinilogs.feature.collection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Stub bodies only — real implementations land in T-15 (Shelf), T-16/T-17
// (AddRecord), T-18 (RecordDetail/EditRecord), T-26 (Stats). Signatures are
// the contract this task fixes.

@Composable
fun ShelfScreen(
    onRecordClick: (recordId: String) -> Unit,
    onAddRecordClick: () -> Unit,
    onStatsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Shelf", modifier)
}

@Composable
fun RecordDetailScreen(
    recordId: String,
    onEditClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Record Detail: $recordId", modifier)
}

@Composable
fun EditRecordScreen(
    recordId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Edit Record: $recordId", modifier)
}

@Composable
fun AddRecordScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Add Record", modifier)
}

@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Stats", modifier)
}
