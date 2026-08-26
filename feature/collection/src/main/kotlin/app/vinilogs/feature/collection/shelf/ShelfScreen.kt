package app.vinilogs.feature.collection.shelf

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import app.vinilogs.core.designsystem.component.EmptyState
import app.vinilogs.core.designsystem.component.ErrorState
import app.vinilogs.core.designsystem.component.LoadingState
import app.vinilogs.core.designsystem.component.VinilogsTopBar
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.core.designsystem.theme.vinilogsColors
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.feature.collection.component.CollectionTextField
import app.vinilogs.feature.collection.model.ShelfLayout
import app.vinilogs.feature.collection.model.asString

/**
 * FR-B6-B8, FR-B11 (T-15). Wires [ShelfViewModel] to the stateless [ShelfScreenContent] so the
 * latter can be exercised directly in Compose UI tests with a plain [ShelfUiState], no Hilt.
 */
@Composable
fun ShelfScreen(
    onRecordClick: (recordId: String) -> Unit,
    onAddRecordClick: () -> Unit,
    onStatsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShelfViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    ShelfScreenContent(
        uiState = uiState,
        actions =
            ShelfActions(
                onRecordClick = onRecordClick,
                onAddRecordClick = onAddRecordClick,
                onStatsClick = onStatsClick,
                onQueryChange = viewModel::setQuery,
                onFilterApply = viewModel::setFilter,
                onClearFilters = viewModel::clearFilters,
                onClearSearchAndFilters = viewModel::clearSearchAndFilters,
                onSortChange = viewModel::setSort,
                onLayoutChange = viewModel::setLayout,
                onRetry = viewModel::retry,
            ),
        modifier = modifier,
    )
}

/** Every callback [ShelfScreenContent] needs, bundled so the composable itself stays under a sane parameter count. */
internal data class ShelfActions(
    val onRecordClick: (recordId: String) -> Unit,
    val onAddRecordClick: () -> Unit,
    val onStatsClick: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onFilterApply: (CollectionFilter) -> Unit,
    val onClearFilters: () -> Unit,
    val onClearSearchAndFilters: () -> Unit,
    val onSortChange: (CollectionSort) -> Unit,
    val onLayoutChange: (ShelfLayout) -> Unit,
    val onRetry: () -> Unit,
)

@Composable
internal fun ShelfScreenContent(
    uiState: ShelfUiState,
    actions: ShelfActions,
    modifier: Modifier = Modifier,
) {
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { ShelfTopBar(uiState = uiState, actions = actions, onShowFilterSheet = { showFilterSheet = true }) },
        floatingActionButton = { ShelfFab(uiState = uiState, onAddRecordClick = actions.onAddRecordClick) },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            ShelfBody(uiState = uiState, actions = actions)
        }
    }

    if (showFilterSheet) {
        FilterSheet(
            filter = uiState.filter,
            onApply = {
                actions.onFilterApply(it)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false },
        )
    }
}

@Composable
private fun ShelfFab(
    uiState: ShelfUiState,
    onAddRecordClick: () -> Unit,
) {
    if (!uiState.isShelfEmpty && uiState.error == null) {
        FloatingActionButton(onClick = onAddRecordClick, shape = MaterialTheme.shapes.small) {
            Icon(Icons.Filled.Add, contentDescription = "Add record")
        }
    }
}

@Composable
private fun ShelfTopBar(
    uiState: ShelfUiState,
    actions: ShelfActions,
    onShowFilterSheet: () -> Unit,
) {
    Column {
        VinilogsTopBar(
            title = "Shelf",
            actions = {
                ShelfTopBarActions(uiState = uiState, actions = actions, onShowFilterSheet = onShowFilterSheet)
            },
        )
        ShelfSearchAndStatusBar(uiState = uiState, actions = actions)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShelfTopBarActions(
    uiState: ShelfUiState,
    actions: ShelfActions,
    onShowFilterSheet: () -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }
    val isGrid = uiState.layout == ShelfLayout.GRID
    val layoutIcon = if (isGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView
    val layoutDescription = if (isGrid) "Switch to list view" else "Switch to grid view"
    val filterTint =
        if (uiState.hasActiveFilter) MaterialTheme.colorScheme.onSurface else MaterialTheme.vinilogsColors.textTertiary

    IconButton(onClick = { actions.onLayoutChange(uiState.layout.toggled()) }) {
        Icon(imageVector = layoutIcon, contentDescription = layoutDescription)
    }
    Box {
        IconButton(onClick = { showSortMenu = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
        }
        SortMenu(
            expanded = showSortMenu,
            selected = uiState.sort,
            onDismiss = { showSortMenu = false },
            onSelect = {
                showSortMenu = false
                actions.onSortChange(it)
            },
        )
    }
    IconButton(onClick = onShowFilterSheet) {
        Icon(
            imageVector = Icons.Filled.FilterList,
            contentDescription = if (uiState.hasActiveFilter) "Filter, active" else "Filter",
            tint = filterTint,
        )
    }
    IconButton(onClick = actions.onStatsClick) {
        Icon(Icons.Filled.BarChart, contentDescription = "Collection stats")
    }
}

@Composable
private fun ShelfSearchAndStatusBar(
    uiState: ShelfUiState,
    actions: ShelfActions,
) {
    CollectionTextField(
        value = uiState.query,
        onValueChange = actions.onQueryChange,
        placeholder = "Search your shelf",
        modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.screenHorizontal),
    )
    if (uiState.hasActiveFilter) {
        ActiveFiltersRow(
            filter = uiState.filter,
            onClear = actions.onClearFilters,
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.screenHorizontal,
                    vertical = MaterialTheme.spacing.sm,
                ),
        )
    }
    if (uiState.pendingSyncCount > 0) {
        PendingSyncIndicator(
            count = uiState.pendingSyncCount,
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.screenHorizontal,
                    vertical = MaterialTheme.spacing.xs,
                ),
        )
    }
}

@Composable
private fun ShelfBody(
    uiState: ShelfUiState,
    actions: ShelfActions,
) {
    when {
        uiState.error != null ->
            ErrorState(
                message = uiState.error.asString(),
                primaryActionLabel = "Retry",
                onPrimaryAction = actions.onRetry,
            )
        uiState.isLoading -> LoadingState()
        uiState.isShelfEmpty ->
            EmptyState(
                message = "Your shelf is empty. Add your first record.",
                actionLabel = "Add record",
                onAction = actions.onAddRecordClick,
            )
        uiState.isSearchEmpty ->
            EmptyState(
                message = "No records match your search.",
                actionLabel = "Clear search",
                onAction = actions.onClearSearchAndFilters,
            )
        uiState.layout == ShelfLayout.GRID ->
            ShelfGridContent(records = uiState.records, onRecordClick = actions.onRecordClick)
        else ->
            ShelfListContent(records = uiState.records, onRecordClick = actions.onRecordClick)
    }
}

private fun ShelfLayout.toggled(): ShelfLayout = if (this == ShelfLayout.GRID) ShelfLayout.LIST else ShelfLayout.GRID
