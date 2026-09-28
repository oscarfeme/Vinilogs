package app.vinilogs.feature.collection.addedit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import app.vinilogs.core.designsystem.component.ErrorState
import app.vinilogs.core.designsystem.component.LoadingState
import app.vinilogs.core.designsystem.component.VinilogsTopBar
import app.vinilogs.core.designsystem.haptics.recordAdded
import app.vinilogs.feature.collection.search.CatalogSearchScreen

/**
 * FR-B1/FR-B2/FR-B3/FR-B4 (T-16/T-17): "Add record" is catalogue search first (FR-B1) with a
 * confirm/edit step (FR-B2) that reuses T-17's manual form -- 02-ARCHITECTURE.md §5's nav
 * diagram draws `addRecord (search | manual)` as one node with two variants, not a branch, so
 * this stays a single `AddRecordRoute` destination with two internal steps rather than a second
 * nav route. [showManualForm] (not [AddEditRecordViewModel.uiState]) drives which step renders,
 * and the *same* [AddEditRecordViewModel] instance backs both the "Add manually" escape hatch
 * and a search-result confirm step -- created once here (scoped to this composable's
 * `NavBackStackEntry`, same as any other `hiltViewModel()` call) so a result picked in the
 * search step is still there if the form step's back arrow returns to search and the user picks
 * a different result, or edits fields, without losing state.
 *
 * The two steps get different back-navigation targets, which is why [AddEditRecordRoot] takes
 * `onNavigateBack` and `onSaved` separately: this screen's search step pops the whole route
 * ([onNavigateBack], back to the shelf), but the confirm/edit step's back arrow steps back to
 * search instead ([showManualForm] = false) -- only a *successful save* ([onSaved]) leaves the
 * route entirely. `EditRecordScreen` below has no search step, so its `onSaved` stays defaulted
 * to `onNavigateBack`, unchanged from before this task.
 */
@Composable
fun AddRecordScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showManualForm by rememberSaveable { mutableStateOf(false) }
    val addEditViewModel: AddEditRecordViewModel = hiltViewModel()

    if (showManualForm) {
        AddEditRecordRoot(
            onNavigateBack = { showManualForm = false },
            onSaved = onNavigateBack,
            viewModel = addEditViewModel,
            modifier = modifier,
        )
    } else {
        CatalogSearchScreen(
            onManualEntry = { showManualForm = true },
            onResultSelected = { result ->
                addEditViewModel.applyCatalogResult(result)
                showManualForm = true
            },
            onNavigateBack = onNavigateBack,
            modifier = modifier,
        )
    }
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
    onSaved: () -> Unit = onNavigateBack,
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
            onSaved()
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
