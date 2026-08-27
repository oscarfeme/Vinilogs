package app.vinilogs.feature.collection.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.core.designsystem.theme.vinilogsColors

/**
 * The record-detail key/value table row from 05-DESIGN-DIRECTION.md §5: "a distinct, denser
 * component: sm vertical padding rather than a generic list row's md... meta-k in labelMedium
 * uppercase Ink500, meta-v in bodyLarge Ink900, baseline-aligned." [showDivider] is false for
 * the last row in a table ("same full-bleed Ink200 divider rule, none after the last row").
 *
 * Gap note: like `CollectionTextField` (T-15/T-17), this belongs in `core:designsystem` but
 * that module is outside Track D's boundary (CLAUDE.md rule 2) -- flagged as a hoist candidate.
 */
@Composable
fun MetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.sm),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.vinilogsColors.textTertiary,
            modifier = Modifier.padding(end = MaterialTheme.spacing.md),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    if (showDivider) {
        HorizontalDivider(color = MaterialTheme.vinilogsColors.hairline, thickness = MaterialTheme.spacing.hairline)
    }
}
