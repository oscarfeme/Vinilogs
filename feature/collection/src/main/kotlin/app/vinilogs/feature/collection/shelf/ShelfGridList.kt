package app.vinilogs.feature.collection.shelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vinilogs.core.designsystem.component.VinylCard
import app.vinilogs.core.designsystem.layout.rememberShelfGridColumns
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.core.designsystem.theme.vinilogsColors
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.Record

private val LIST_THUMBNAIL_SIZE = 56.dp

@Composable
internal fun ShelfGridContent(
    records: List<Record>,
    onRecordClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(rememberShelfGridColumns()),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MaterialTheme.spacing.shelfHorizontal),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
    ) {
        items(records, key = { it.id }) { record ->
            ShelfGridItem(record = record, onClick = { onRecordClick(record.id) })
        }
    }
}

@Composable
private fun ShelfGridItem(
    record: Record,
    onClick: () -> Unit,
) {
    Column {
        VinylCard(
            coverUrl = record.coverUrl,
            artist = record.artist,
            title = record.title,
            catalogNumber = record.catalogNumber,
            onClick = onClick,
        )
        Text(
            text = record.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
        )
        Text(
            text = record.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.vinilogsColors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ShelfListContent(
    records: List<Record>,
    onRecordClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(records, key = { it.id }) { record ->
            ShelfListRow(record = record, onClick = { onRecordClick(record.id) })
            if (record.id != records.last().id) {
                HorizontalDivider(
                    color = MaterialTheme.vinilogsColors.hairline,
                    thickness = MaterialTheme.spacing.hairline,
                )
            }
        }
    }
}

@Composable
private fun ShelfListRow(
    record: Record,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.screenHorizontal, vertical = MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VinylCard(
            coverUrl = record.coverUrl,
            artist = record.artist,
            title = record.title,
            catalogNumber = record.catalogNumber,
            onClick = onClick,
            modifier = Modifier.size(LIST_THUMBNAIL_SIZE).aspectRatio(1f),
        )
        Column(modifier = Modifier.padding(start = MaterialTheme.spacing.lg)) {
            Text(
                text = record.artist,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = record.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.vinilogsColors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun PendingSyncIndicator(
    count: Int,
    modifier: Modifier = Modifier,
) {
    // FR-B11: "pending state visible but never blocking" -- text only, no spinner, no colour.
    Text(
        text = if (count == 1) "1 record pending sync" else "$count records pending sync",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.vinilogsColors.textTertiary,
        modifier = modifier,
    )
}

@Composable
internal fun ActiveFiltersRow(
    filter: CollectionFilter,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = activeFilterSummary(filter),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.vinilogsColors.textTertiary,
        )
        TextButton(onClick = onClear) {
            Text("Clear", style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun activeFilterSummary(filter: CollectionFilter): String {
    val parts =
        buildList {
            filter.format?.let { add(it.label()) }
            filter.condition?.let { add(it.abbreviation()) }
            filter.decade?.let { add("${it}s") }
            filter.minRating?.let { add("$it+ stars") }
            filter.tag?.let { add("#$it") }
        }
    return "Filtered: " + parts.joinToString(", ")
}
