package app.vinilogs.core.designsystem.layout

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * "Square sleeves, 3 columns portrait / 5 landscape" (05-DESIGN-DIRECTION.md §5)
 * — orientation-based, not a width breakpoint, so a phone rotated to landscape
 * always gains the extra columns regardless of how wide it is.
 */
@Composable
fun rememberShelfGridColumns(): Int {
    val orientation = LocalConfiguration.current.orientation
    return if (orientation == Configuration.ORIENTATION_LANDSCAPE) 5 else 3
}
