package app.vinilogs.feature.collection.addedit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import app.vinilogs.core.designsystem.component.ErrorState
import app.vinilogs.core.designsystem.component.LoadingState
import app.vinilogs.core.designsystem.component.VinilogsTopBar
import app.vinilogs.core.designsystem.haptics.recordAdded

/**
 * FR-B3/FR-B4 (T-17): manual add/edit form. `AddRecordRoute` and `EditRecordRoute(recordId)`
 * both render this -- same [AddEditRecordViewModel], same form -- see this task's PR for why
 * (CLAUDE.md rule 7: the task doc doesn't spell out whether add/edit share a ViewModel).
 */
@Composable
fun AddRecordScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AddEditRecordRoot(onNavigateBack = onNavigateBack, modifier = modifier)
}

/**
 * [recordId] isn't read directly -- [AddEditRecordViewModel] pulls it from the `SavedStateHandle`
 * Navigation Compose populates automatically for the type-safe `EditRecordRoute(recordId)` arg.
 * Kept as a parameter because `CollectionScreens.kt`'s signatures are a fixed contract (T-03).
 */
@Composable
fun EditRecordScreen(
    recordId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AddEditRecordRoot(onNavigateBack = onNavigateBack, modifier = modifier)
}

@Composable
private fun AddEditRecordRoot(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditRecordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            // "Haptics on... successful record add" (02-ARCHITECTURE.md §6) -- ADD only, not
            // EDIT: the spec calls out *add* specifically, and a haptic on every routine edit
            // save would dilute the signal the destructive-confirm haptic (T-18) relies on.
            if (uiState.mode == AddEditMode.ADD) haptics.recordAdded()
            onNavigateBack()
        }
    }

    AddEditRecordContent(
        uiState = uiState,
        onDraftChange = viewModel::updateDraft,
        onSave = viewModel::save,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@Composable
internal fun AddEditRecordContent(
    uiState: AddEditRecordUiState,
    onDraftChange: ((RecordDraft) -> RecordDraft) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            VinilogsTopBar(
                title = if (uiState.mode == AddEditMode.ADD) "Add record" else "Edit record",
                onNavigateBack = onNavigateBack,
                navigationIconContentDescription = "Back",
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.notFound ->
                    ErrorState(
                        message = "This record no longer exists.",
                        primaryActionLabel = "Back",
                        onPrimaryAction = onNavigateBack,
                    )
                uiState.isLoading -> LoadingState()
                else -> AddEditRecordForm(uiState = uiState, onDraftChange = onDraftChange, onSave = onSave)
            }
        }
    }
}
