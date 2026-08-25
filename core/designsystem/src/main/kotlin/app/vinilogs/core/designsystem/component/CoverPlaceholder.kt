package app.vinilogs.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.vinilogs.core.designsystem.theme.PlaceholderPalette
import kotlin.math.abs

/**
 * Cover art fallback for a record with no Discogs art or a custom image —
 * "a generated placeholder derived from the artist name — never a grey box
 * with an icon" (02-ARCHITECTURE.md §6). Same artist always maps to the same
 * colour, so a collection with several records by one artist stays visually
 * coherent on the shelf.
 */
@Composable
fun CoverPlaceholder(
    artist: String,
    modifier: Modifier = Modifier,
) {
    val color = remember(artist) { colorForArtist(artist) }
    val initial = remember(artist) { initialOf(artist) }
    val onColor = if (color.luminance() > 0.5f) Color.Black else Color.White

    Box(
        modifier = modifier.background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.displayMedium,
            color = onColor,
        )
    }
}

private fun colorForArtist(artist: String): Color {
    val index = abs(artist.trim().lowercase().hashCode()) % PlaceholderPalette.size
    return PlaceholderPalette[index]
}

private fun initialOf(artist: String): String {
    val firstChar = artist.trim().firstOrNull() ?: return "?"
    return firstChar.uppercaseChar().toString()
}
