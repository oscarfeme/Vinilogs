package app.vinilogs.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Vinilogs palette.
 *
 * One neutral ramp, two themes, exactly one chromatic token.
 *
 *   The interface is black and white. The records are the colour.
 *
 * Tokens are named by *role*, not by literal colour: [Ink900] is the near-black in
 * light theme and the near-white in dark theme. The ramp inverts, so the same call
 * site is correct in both. Never reference a raw [Color] from a composable — go
 * through [ColorScheme] or [VinilogsColors].
 *
 * See 05-DESIGN-DIRECTION.md §2.
 */

internal val LightInk900 = Color(0xFF0A0A0A) // primary text, primary fill, active icons
internal val LightInk700 = Color(0xFF3D3D3D) // secondary text, subtitles
internal val LightInk500 = Color(0xFF686868) // meta text, placeholders, inactive icons
internal val LightInk400 = Color(0xFF858585) // control outlines — non-text, >=3:1
internal val LightInk300 = Color(0xFFA3A3A3) // disabled only — exempt from AA
internal val LightInk200 = Color(0xFFD4D4D4) // decorative hairlines — non-text
internal val LightInk100 = Color(0xFFEDEDED) // surface variant, skeletons, placeholders
internal val LightSurface = Color(0xFFFFFFFF) // cards, sheets, dialogs
internal val LightBackground = Color(0xFFFAFAFA) // screen background

// ---------------------------------------------------------------------------
// Dark ramp — inverted
// ---------------------------------------------------------------------------

internal val DarkInk900 = Color(0xFFF5F5F5)
internal val DarkInk700 = Color(0xFFC7C7C7)
internal val DarkInk500 = Color(0xFF9A9A9A)
internal val DarkInk400 = Color(0xFF6E6E6E)
internal val DarkInk300 = Color(0xFF5C5C5C)
internal val DarkInk200 = Color(0xFF2E2E2E)
internal val DarkInk100 = Color(0xFF1F1F1F)
internal val DarkSurface = Color(0xFF141414)
internal val DarkBackground = Color(0xFF0A0A0A)

// ---------------------------------------------------------------------------
// The single chromatic token
// ---------------------------------------------------------------------------

/**
 * The only saturated colour in the system, and the only one that is not part of
 * the ramp. Permitted for **destructive confirmation and form validation errors
 * only** — never for badges, unread counts, or emphasis.
 *
 * "The collection is sacred": losing a record must be unmistakable, and a purely
 * monochrome system cannot signal danger by colour. We spend exactly one colour
 * on that, and nothing else.
 */
internal val LightAlert = Color(0xFFB3261E)
internal val DarkAlert = Color(0xFFF2B8B5)

// ---------------------------------------------------------------------------
// Semantic tokens that Material 3's ColorScheme has no slot for
// ---------------------------------------------------------------------------

/**
 * Roles the M3 [ColorScheme] cannot express. Exposed through
 * `MaterialTheme.vinilogsColors`; see Theme.kt.
 */
@androidx.compose.runtime.Immutable
data class VinilogsColors(
    /** Body text one step down from primary. */
    val textSecondary: Color,
    /** Meta text: catalogue numbers, timestamps, counts. Lightest AA-passing token. */
    val textTertiary: Color,
    /** Disabled content. Exempt from the contrast floor — never used for information. */
    val textDisabled: Color,
    /**
     * Boundary of an interactive control at rest: the resting rule under a text
     * field, an unselected chip. Meets WCAG 1.4.11 (>=3:1) because it is what
     * identifies the control. Distinct from [hairline] for that reason.
     */
    val controlOutline: Color,
    /**
     * Decorative 1dp rules: list dividers, the scrolled app-bar rule. Carries no
     * information, so it sits below the non-text floor on purpose — a divider
     * dark enough to pass 3:1 would read as a heavy border and wreck the
     * lightness of the system.
     */
    val hairline: Color,
    /** Sleeve placeholders, skeletons, incoming chat bubbles. */
    val placeholder: Color,
)

internal val LightVinilogsColors = VinilogsColors(
    textSecondary = LightInk700,
    textTertiary = LightInk500,
    textDisabled = LightInk300,
    controlOutline = LightInk400,
    hairline = LightInk200,
    placeholder = LightInk100,
)

internal val DarkVinilogsColors = VinilogsColors(
    textSecondary = DarkInk700,
    textTertiary = DarkInk500,
    textDisabled = DarkInk300,
    controlOutline = DarkInk400,
    hairline = DarkInk200,
    placeholder = DarkInk100,
)

// ---------------------------------------------------------------------------
// Material 3 schemes
// ---------------------------------------------------------------------------

/*
 * Every `surfaceContainer*` slot is pinned to a flat ramp value on purpose.
 * Material 3 would otherwise tint containers by elevation, which reintroduces
 * colour through the back door. Elevation in Vinilogs is always 0.dp and
 * separation is always a hairline — see 05-DESIGN-DIRECTION.md §4.3.
 */

internal val VinilogsLightColorScheme: ColorScheme = lightColorScheme(
    primary = LightInk900,
    onPrimary = LightBackground,
    primaryContainer = LightInk100,
    onPrimaryContainer = LightInk900,
    secondary = LightInk700,
    onSecondary = LightBackground,
    secondaryContainer = LightInk100,
    onSecondaryContainer = LightInk900,
    tertiary = LightInk500,
    onTertiary = LightBackground,
    tertiaryContainer = LightInk100,
    onTertiaryContainer = LightInk900,
    background = LightBackground,
    onBackground = LightInk900,
    surface = LightSurface,
    onSurface = LightInk900,
    surfaceVariant = LightInk100,
    onSurfaceVariant = LightInk700,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightBackground,
    surfaceContainerHigh = LightInk100,
    surfaceContainerHighest = LightInk100,
    surfaceDim = LightBackground,
    surfaceBright = LightSurface,
    // `outline` is what Material draws around OutlinedTextField / OutlinedButton,
    // so it carries the 3:1 control token. `outlineVariant` is HorizontalDivider,
    // which is decorative and stays light.
    outline = LightInk400,
    outlineVariant = LightInk200,
    error = LightAlert,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = LightInk900,
    inverseOnSurface = LightBackground,
    inversePrimary = DarkInk900,
    scrim = Color(0xFF000000),
)

internal val VinilogsDarkColorScheme: ColorScheme = darkColorScheme(
    primary = DarkInk900,
    onPrimary = DarkBackground,
    primaryContainer = DarkInk100,
    onPrimaryContainer = DarkInk900,
    secondary = DarkInk700,
    onSecondary = DarkBackground,
    secondaryContainer = DarkInk100,
    onSecondaryContainer = DarkInk900,
    tertiary = DarkInk500,
    onTertiary = DarkBackground,
    tertiaryContainer = DarkInk100,
    onTertiaryContainer = DarkInk900,
    background = DarkBackground,
    onBackground = DarkInk900,
    surface = DarkSurface,
    onSurface = DarkInk900,
    surfaceVariant = DarkInk100,
    onSurfaceVariant = DarkInk700,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkInk100,
    surfaceContainerHighest = DarkInk100,
    surfaceDim = DarkBackground,
    surfaceBright = DarkInk100,
    outline = DarkInk400,
    outlineVariant = DarkInk200,
    error = DarkAlert,
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    inverseSurface = DarkInk900,
    inverseOnSurface = DarkBackground,
    inversePrimary = LightInk900,
    scrim = Color(0xFF000000),
)
