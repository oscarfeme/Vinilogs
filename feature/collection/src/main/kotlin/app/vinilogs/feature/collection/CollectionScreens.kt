package app.vinilogs.feature.collection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Stub bodies only — real implementations land in T-15 (Shelf), T-18 (RecordDetail),
// T-26 (Stats). Signatures are the contract this task fixes. AddRecordScreen and
// EditRecordScreen's real implementations (T-17) live in
// app.vinilogs.feature.collection.addedit.AddEditRecordScreen.

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
fun StatsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Stats", modifier)
}
