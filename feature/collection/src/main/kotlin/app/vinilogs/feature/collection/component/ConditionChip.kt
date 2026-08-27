package app.vinilogs.feature.collection.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.core.designsystem.theme.tabular
import app.vinilogs.core.designsystem.theme.vinilogsColors

/**
 * 05-DESIGN-DIRECTION.md §5: "1dp Ink400 outline, 0dp radius, labelSmall uppercase tabular,
 * Ink700. Renders the grade abbreviation exactly as in the glossary (M, NM, VG+, VG, G, F, P)."
 *
 * Gap note: like `CollectionTextField`/`MetadataRow`, this belongs in `core:designsystem` but
 * that module is outside Track D's boundary (CLAUDE.md rule 2) -- flagged as a hoist candidate.
 */
@Composable
fun ConditionChip(
    abbreviation: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(MaterialTheme.spacing.hairline, MaterialTheme.vinilogsColors.controlOutline),
    ) {
        Text(
            text = abbreviation,
            style = MaterialTheme.typography.labelSmall.tabular(),
            color = MaterialTheme.vinilogsColors.textSecondary,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.xs),
        )
    }
}
