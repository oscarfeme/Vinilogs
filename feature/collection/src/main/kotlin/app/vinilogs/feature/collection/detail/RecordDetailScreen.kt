package app.vinilogs.feature.collection.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import app.vinilogs.core.designsystem.component.ErrorState
import app.vinilogs.core.designsystem.component.LoadingState
import app.vinilogs.core.designsystem.haptics.destructiveConfirm

/**
 * FR-B5/FR-B9/FR-C5 (T-18): large cover, edit, share, delete-with-undo. [recordId] isn't read
 * directly here -- [RecordDetailViewModel] pulls it from the `SavedStateHandle` Navigation
 * Compose populates for the type-safe `RecordDetailRoute(recordId)` arg, same pattern as T-17's
 * `EditRecordScreen`.
 */
@Composable
fun RecordDetailScreen(
    recordId: String,
    onEditClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RecordDetailRoot(onEditClick = onEditClick, onNavigateBack = onNavigateBack, modifier = modifier)
}

@Composable
private fun RecordDetailRoot(
    onEditClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(uiState.permanentlyDeleted) {
        if (uiState.permanentlyDeleted) onNavigateBack()
    }

    RecordDetailContent(
        uiState = uiState,
        actions =
            RecordDetailActions(
                onEditClick = onEditClick,
                onNavigateBack = onNavigateBack,
                onShare = { uiState.record?.let { shareRecord(context, it) } },
                onDeleteConfirmed = {
                    // "Haptics on destructive confirmation" (02-ARCHITECTURE.md §6).
                    haptics.destructiveConfirm()
                    viewModel.delete()
                },
                onUndoDelete = viewModel::undoDelete,
            ),
        modifier = modifier,
    )
}

internal data class RecordDetailActions(
    val onEditClick: () -> Unit,
    val onNavigateBack: () -> Unit,
    val onShare: () -> Unit,
    val onDeleteConfirmed: () -> Unit,
    val onUndoDelete: () -> Unit,
)

@Composable
internal fun RecordDetailContent(
    uiState: RecordDetailUiState,
    actions: RecordDetailActions,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            RecordDetailTopBar(
                uiState = uiState,
                onNavigateBack = actions.onNavigateBack,
                onShare = actions.onShare,
                onEditClick = actions.onEditClick,
                onDeleteClick = { showDeleteDialog = true },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.notFound ->
                    ErrorState(
                        message = "This record no longer exists.",
                        primaryActionLabel = "Back",
                        onPrimaryAction = actions.onNavigateBack,
                    )
                uiState.showUndo -> UndoDeleteBar(onUndo = actions.onUndoDelete)
                uiState.isLoading -> LoadingState()
                uiState.record != null -> RecordDetailBody(record = uiState.record)
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            onConfirm = {
                showDeleteDialog = false
                actions.onDeleteConfirmed()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}
