package app.vinilogs.feature.collection.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.vinilogs.core.designsystem.component.EmptyState
import app.vinilogs.core.designsystem.component.ErrorState
import app.vinilogs.core.designsystem.component.LoadingState
import app.vinilogs.core.designsystem.component.VinilogsTopBar
import app.vinilogs.core.designsystem.component.VinylCard
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.core.designsystem.theme.vinilogsColors
import app.vinilogs.core.model.CatalogResult
import app.vinilogs.feature.collection.component.CollectionTextField

private val RESULT_THUMBNAIL_SIZE = 56.dp

/**
 * FR-B1/FR-B2 (T-16): the catalogue-search step of "Add record". Wires [CatalogSearchViewModel]
 * to the stateless [CatalogSearchContent], matching [app.vinilogs.feature.collection.shelf.ShelfScreen]'s
 * split so the content composable is exercisable directly in Compose UI tests, no Hilt.
 */
@Composable
internal fun CatalogSearchScreen(
    onManualEntry: () -> Unit,
    onResultSelected: (CatalogResult) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    CatalogSearchContent(
        uiState = uiState,
        actions =
            CatalogSearchActions(
                onQueryChange = viewModel::setQuery,
                onResultClick = onResultSelected,
                onManualEntry = onManualEntry,
                onRetry = viewModel::retry,
                onLoadMore = viewModel::loadMore,
                onNavigateBack = onNavigateBack,
            ),
        modifier = modifier,
    )
}

/** Every callback [CatalogSearchContent] needs, bundled per this module's `ShelfActions` pattern. */
internal data class CatalogSearchActions(
    val onQueryChange: (String) -> Unit,
    val onResultClick: (CatalogResult) -> Unit,
    val onManualEntry: () -> Unit,
    val onRetry: () -> Unit,
    val onLoadMore: () -> Unit,
    val onNavigateBack: () -> Unit,
)

@Composable
internal fun CatalogSearchContent(
    uiState: CatalogSearchUiState,
    actions: CatalogSearchActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                VinilogsTopBar(
                    title = "Add record",
                    onNavigateBack = actions.onNavigateBack,
                    navigationIconContentDescription = "Back",
                    actions = {
                        TextButton(onClick = actions.onManualEntry) {
                            Text("Add manually")
                        }
                    },
                )
                CollectionTextField(
                    value = uiState.query,
                    onValueChange = actions.onQueryChange,
                    placeholder = "Search by artist, album or catalogue number",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MaterialTheme.spacing.screenHorizontal),
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            CatalogSearchBody(uiState = uiState, actions = actions)
        }
    }
}

@Composable
private fun CatalogSearchBody(
    uiState: CatalogSearchUiState,
    actions: CatalogSearchActions,
) {
    when {
        uiState.failure != null ->
            ErrorState(
                message = uiState.failure.message(),
                // FR-B1: "always offer manual entry as the way forward" -- primary action here,
                // matching 02-ARCHITECTURE.md §2's data-flow example verbatim ("failure -> error
                // state that offers 'Add manually' as the primary action").
                primaryActionLabel = "Add manually",
                onPrimaryAction = actions.onManualEntry,
                secondaryActionLabel = "Retry",
                onSecondaryAction = actions.onRetry,
            )
        uiState.isLoading -> LoadingState(message = "Searching...")
        uiState.isIdle ->
            EmptyState(
                message = "Search Discogs by artist, album or catalogue number.",
                actionLabel = "Add manually",
                onAction = actions.onManualEntry,
            )
        uiState.isNoResults ->
            EmptyState(
                message = "No matches for \"${uiState.query}\".",
                actionLabel = "Add manually",
                onAction = actions.onManualEntry,
            )
        else -> CatalogResultList(uiState = uiState, actions = actions)
    }
}

private fun CatalogSearchFailure.message(): String =
    when (this) {
        CatalogSearchFailure.OFFLINE -> "You're offline. Check your connection and try again."
        CatalogSearchFailure.RATE_LIMITED -> "Too many searches at once. Wait a moment and try again."
        CatalogSearchFailure.GENERIC -> "Couldn't reach the catalogue right now."
    }

@Composable
private fun CatalogResultList(
    uiState: CatalogSearchUiState,
    actions: CatalogSearchActions,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = MaterialTheme.spacing.sm),
    ) {
        items(uiState.results, key = { it.discogsId }) { result ->
            CatalogResultRow(result = result, onClick = { actions.onResultClick(result) })
        }
        item {
            CatalogResultListFooter(
                canLoadMore = uiState.canLoadMore,
                isLoadingMore = uiState.isLoadingMore,
                onLoadMore = actions.onLoadMore,
            )
        }
    }
}

@Composable
private fun CatalogResultListFooter(
    canLoadMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    if (isLoadingMore) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else if (canLoadMore) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            TextButton(onClick = onLoadMore) {
                Text("Load more")
            }
        }
    }
}

@Composable
private fun CatalogResultRow(
    result: CatalogResult,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.screenHorizontal, vertical = MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VinylCard(
            coverUrl = result.coverUrl,
            artist = result.artist,
            title = result.title,
            catalogNumber = result.catalogNumber,
            onClick = onClick,
            modifier = Modifier.size(RESULT_THUMBNAIL_SIZE).aspectRatio(1f),
        )
        Column(
            modifier = Modifier.padding(start = MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
        ) {
            Text(
                text = result.artist,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = result.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.vinilogsColors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = result.yearAndLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.vinilogsColors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun CatalogResult.yearAndLabel(): String =
    listOfNotNull(year?.toString(), label).joinToString(separator = " · ")
