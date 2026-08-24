package app.vinilogs.core.designsystem.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * "Grid density adapts to screen width: 2 columns on compact, 3 on medium and
 * up" (02-ARCHITECTURE.md §6). 600dp is the standard Material compact/medium
 * breakpoint; read directly off configuration width rather than pulling in
 * the separate `material3-window-size-class` artifact for a single threshold.
 */
private const val COMPACT_MAX_WIDTH_DP = 600

@Composable
fun rememberShelfGridColumns(): Int {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return if (widthDp < COMPACT_MAX_WIDTH_DP) 2 else 3
}
