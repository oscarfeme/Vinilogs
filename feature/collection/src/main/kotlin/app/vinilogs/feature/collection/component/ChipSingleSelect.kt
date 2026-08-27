package app.vinilogs.feature.collection.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.vinilogs.core.designsystem.theme.spacing

/**
 * A required single-select chip row -- exactly one of [options] is always selected, unlike a
 * filter chip row (see `feature:collection/shelf/ShelfFilterSort.kt`'s `FilterSection`, which
 * allows none selected). Used for the non-nullable enum fields on the manual-entry form
 * (format, speed, condition), where [Record.format]/[Record.speed]/[Record.condition] always
 * need a value.
 */
@Composable
fun <T> RequiredChipRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

/**
 * An optional single-select chip row -- tapping the already-selected chip clears the selection
 * back to `null`. Used for the personal-rating field (FR-B4), which is nullable.
 */
@Composable
fun <T> OptionalChipRow(
    options: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(if (option == selected) null else option) },
                label = { Text(label(option)) },
            )
        }
    }
}
