package app.vinilogs.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

/**
 * Sleeve art tile — "renders sleeve art at a true 1:1 aspect ratio with a
 * subtle edge treatment. Missing art gets a generated placeholder... never a
 * grey box with an icon" (02-ARCHITECTURE.md §6). The one component every
 * shelf/grid screen builds on; it never shrinks to a text-row thumbnail.
 *
 * @param coverUrl null or blank falls straight through to [CoverPlaceholder],
 *   same as a Coil load failure.
 * @param catalogNumber shown by [CoverPlaceholder] when there is no cover art.
 */
@Composable
fun VinylCard(
    coverUrl: String?,
    artist: String,
    title: String,
    modifier: Modifier = Modifier,
    catalogNumber: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .semantics { contentDescription = "$title, $artist" },
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (coverUrl.isNullOrBlank()) {
            CoverPlaceholder(catalogNumber = catalogNumber, modifier = Modifier.fillMaxSize())
        } else {
            SubcomposeAsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    CoverPlaceholder(catalogNumber = catalogNumber, modifier = Modifier.fillMaxSize())
                },
                error = {
                    CoverPlaceholder(catalogNumber = catalogNumber, modifier = Modifier.fillMaxSize())
                },
                success = { SubcomposeAsyncImageContent() },
            )
        }
    }
}
