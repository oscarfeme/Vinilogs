package app.vinilogs.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.vinilogs.core.designsystem.theme.tabular
import app.vinilogs.core.designsystem.theme.vinilogsColors

/**
 * Cover art fallback for a record with no Discogs art or a custom image — an
 * `Ink100` square showing the catalogue number, centred. "Never a generic
 * music-note icon; the catalogue number is the useful thing"
 * (05-DESIGN-DIRECTION.md §5). Renders an empty square when [catalogNumber] is
 * null or blank rather than inventing a fallback the spec doesn't define.
 */
@Composable
fun CoverPlaceholder(
    catalogNumber: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!catalogNumber.isNullOrBlank()) {
            Text(
                text = catalogNumber,
                style = MaterialTheme.typography.labelSmall.tabular(),
                color = MaterialTheme.vinilogsColors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
