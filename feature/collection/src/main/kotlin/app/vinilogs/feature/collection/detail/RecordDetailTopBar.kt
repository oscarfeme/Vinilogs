package app.vinilogs.feature.collection.detail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import app.vinilogs.core.designsystem.component.VinilogsTopBar

@Composable
internal fun RecordDetailTopBar(
    uiState: RecordDetailUiState,
    onNavigateBack: () -> Unit,
    onShare: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    VinilogsTopBar(
        title = uiState.record?.title.orEmpty(),
        onNavigateBack = onNavigateBack,
        navigationIconContentDescription = "Back",
        actions = {
            if (uiState.record != null) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = "Share")
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        },
    )
}
