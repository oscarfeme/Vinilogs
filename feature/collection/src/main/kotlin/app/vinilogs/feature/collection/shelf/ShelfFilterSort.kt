package app.vinilogs.feature.collection.shelf

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.core.designsystem.theme.vinilogsColors
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.core.model.Condition
import app.vinilogs.core.model.Format
import app.vinilogs.feature.collection.component.CollectionTextField
import kotlinx.coroutines.launch

private val DECADES = (1950..2020 step 10).toList()
private const val MAX_RATING = 5

@Composable
internal fun SortMenu(
    expanded: Boolean,
    selected: CollectionSort,
    onDismiss: () -> Unit,
    onSelect: (CollectionSort) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        CollectionSort.entries.forEach { sort ->
            DropdownMenuItem(
                text = { Text(sort.label()) },
                onClick = { onSelect(sort) },
                trailingIcon = { if (sort == selected) Text("✓") },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSheet(
    filter: CollectionFilter,
    onApply: (CollectionFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(filter) { mutableStateOf(filter) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = MaterialTheme.spacing.screenHorizontal)
                    .padding(bottom = MaterialTheme.spacing.xl),
        ) {
            Text("Filter", style = MaterialTheme.typography.headlineSmall)
            FilterSheetFields(draft = draft, onDraftChange = { draft = it })
            FilterSheetActions(
                onClearAll = { draft = CollectionFilter() },
                onApply = {
                    scope.launch {
                        sheetState.hide()
                        onApply(draft)
                    }
                },
            )
        }
    }
}

@Composable
private fun FilterSheetFields(
    draft: CollectionFilter,
    onDraftChange: (CollectionFilter) -> Unit,
) {
    FilterSection(title = "Format") {
        Format.entries.forEach { format ->
            FilterChip(
                selected = draft.format == format,
                onClick = { onDraftChange(draft.copy(format = if (draft.format == format) null else format)) },
                label = { Text(format.label()) },
            )
        }
    }
    FilterSection(title = "Condition") {
        Condition.entries.forEach { condition ->
            FilterChip(
                selected = draft.condition == condition,
                onClick = {
                    onDraftChange(draft.copy(condition = if (draft.condition == condition) null else condition))
                },
                label = { Text(condition.abbreviation()) },
            )
        }
    }
    FilterSection(title = "Decade") {
        DECADES.forEach { decade ->
            FilterChip(
                selected = draft.decade == decade,
                onClick = { onDraftChange(draft.copy(decade = if (draft.decade == decade) null else decade)) },
                label = { Text("${decade}s") },
            )
        }
    }
    FilterSection(title = "Minimum rating") {
        for (rating in 1..MAX_RATING) {
            FilterChip(
                selected = draft.minRating == rating,
                onClick = { onDraftChange(draft.copy(minRating = if (draft.minRating == rating) null else rating)) },
                label = { Text("$rating+") },
            )
        }
    }
    CollectionTextField(
        value = draft.tag.orEmpty(),
        onValueChange = { onDraftChange(draft.copy(tag = it.ifBlank { null })) },
        label = "Tag",
        placeholder = "e.g. favorite",
        modifier = Modifier.padding(top = MaterialTheme.spacing.md),
    )
}

@Composable
private fun FilterSheetActions(
    onClearAll: () -> Unit,
    onApply: () -> Unit,
) {
    Row(modifier = Modifier.padding(top = MaterialTheme.spacing.xl)) {
        TextButton(onClick = onClearAll) {
            Text("Clear all")
        }
        Button(onClick = onApply, modifier = Modifier.padding(start = MaterialTheme.spacing.md)) {
            Text("Apply")
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(top = MaterialTheme.spacing.lg)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.vinilogsColors.textTertiary,
        )
        Row(
            modifier =
                Modifier
                    .padding(top = MaterialTheme.spacing.sm)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            content()
        }
    }
}

internal fun Format.label(): String =
    when (this) {
        Format.LP -> "LP"
        Format.EP -> "EP"
        Format.SEVEN -> "7\""
        Format.TEN -> "10\""
        Format.TWELVE -> "12\""
        Format.BOX -> "Box"
    }

internal fun Condition.abbreviation(): String =
    when (this) {
        Condition.MINT -> "M"
        Condition.NEAR_MINT -> "NM"
        Condition.VERY_GOOD_PLUS -> "VG+"
        Condition.VERY_GOOD -> "VG"
        Condition.GOOD -> "G"
        Condition.FAIR -> "F"
        Condition.POOR -> "P"
    }

internal fun CollectionSort.label(): String =
    when (this) {
        CollectionSort.ARTIST -> "Artist"
        CollectionSort.TITLE -> "Title"
        CollectionSort.YEAR -> "Year"
        CollectionSort.DATE_ADDED -> "Date added"
        CollectionSort.RATING -> "Rating"
    }
