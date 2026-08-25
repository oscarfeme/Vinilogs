package app.vinilogs.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.RippleDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The Vinilogs theme.
 *
 *   The interface is black and white. The records are the colour.
 *
 * Wrap the whole app in this once, in the single activity. Screens read tokens
 * from [MaterialTheme], [MaterialTheme.vinilogsColors] and
 * [MaterialTheme.spacing] — never from the raw `internal` values in Color.kt.
 *
 * Dynamic colour (Material You) is **deliberately not supported**: it would tint
 * the shell with the user's wallpaper and destroy the premise of the system. Do
 * not add it without an ADR. See 05-DESIGN-DIRECTION.md.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VinilogsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) VinilogsDarkColorScheme else VinilogsLightColorScheme
    val vinilogsColors = if (darkTheme) DarkVinilogsColors else LightVinilogsColors

    CompositionLocalProvider(
        LocalVinilogsColors provides vinilogsColors,
        LocalSpacing provides DefaultSpacing,
        LocalRippleConfiguration provides MonochromeRipple,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VinilogsTypography,
            shapes = VinilogsShapes,
            content = content,
        )
    }
}

/**
 * A neutral ripple. Material's default derives its ripple from `primary`, which
 * here is near-black in light theme and near-white in dark — correct in both, but
 * pinned explicitly so a future change to `primary` cannot leak a tint into every
 * press state in the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
private val MonochromeRipple = RippleConfiguration(
    color = Color.Unspecified,
    rippleAlpha = RippleDefaults.RippleAlpha,
)

// ---------------------------------------------------------------------------
// Composition locals
// ---------------------------------------------------------------------------

internal val LocalVinilogsColors = staticCompositionLocalOf<VinilogsColors> {
    error("VinilogsColors not provided. Wrap your content in VinilogsTheme { }.")
}

internal val LocalSpacing = staticCompositionLocalOf<Spacing> {
    error("Spacing not provided. Wrap your content in VinilogsTheme { }.")
}

/**
 * Semantic colour roles Material 3's `ColorScheme` has no slot for:
 * `textSecondary`, `textTertiary`, `textDisabled`, `hairline`, `placeholder`.
 */
val MaterialTheme.vinilogsColors: VinilogsColors
    @Composable
    @ReadOnlyComposable
    get() = LocalVinilogsColors.current

/**
 * The 4dp spacing grid. Every gap in the app comes from here — a raw `.dp` in a
 * padding modifier is a review comment.
 */
val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
