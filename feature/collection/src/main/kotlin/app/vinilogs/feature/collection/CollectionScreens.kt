package app.vinilogs.feature.collection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Stub bodies only — real implementation left is T-26 (Stats). Signatures are the contract
// this task fixes. ShelfScreen's real implementation (T-15) lives in
// app.vinilogs.feature.collection.shelf.ShelfScreen; AddRecordScreen and EditRecordScreen's
// real implementations (T-17) live in app.vinilogs.feature.collection.addedit.
// AddEditRecordScreen; RecordDetailScreen's real implementation (T-18) lives in
// app.vinilogs.feature.collection.detail.RecordDetailScreen.

@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StubScreen("Stats", modifier)
}
