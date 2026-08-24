package app.vinilogs.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Starting brand palette — a warm amber/copper primary evoking a vinyl label,
// kept deliberately muted everywhere else so cover art stays the focal point
// ("The covers are the interface", 00-README.md). Not derived from any brand
// spec (none exists yet); swap freely, this file is its only home.

internal val LightPrimary = Color(0xFF8B5000)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFFFDCBE)
internal val LightOnPrimaryContainer = Color(0xFF2C1600)
internal val LightSecondary = Color(0xFF6F5B40)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFFBDEBC)
internal val LightOnSecondaryContainer = Color(0xFF261904)
internal val LightTertiary = Color(0xFF4C6547)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFCEEBC2)
internal val LightOnTertiaryContainer = Color(0xFF092008)
internal val LightError = Color(0xFFBA1A1A)
internal val LightOnError = Color(0xFFFFFFFF)
internal val LightErrorContainer = Color(0xFFFFDAD6)
internal val LightOnErrorContainer = Color(0xFF410002)
internal val LightBackground = Color(0xFFFFFBFF)
internal val LightOnBackground = Color(0xFF1F1B16)
internal val LightSurface = Color(0xFFFFFBFF)
internal val LightOnSurface = Color(0xFF1F1B16)
internal val LightSurfaceVariant = Color(0xFFF0E0D0)
internal val LightOnSurfaceVariant = Color(0xFF4F4539)
internal val LightOutline = Color(0xFF817567)
internal val LightOutlineVariant = Color(0xFFD3C4B4)
internal val LightScrim = Color(0xFF000000)
internal val LightInverseSurface = Color(0xFF362F27)
internal val LightInverseOnSurface = Color(0xFFF9EFE7)
internal val LightInversePrimary = Color(0xFFFFB876)

internal val DarkPrimary = Color(0xFFFFB876)
internal val DarkOnPrimary = Color(0xFF4A2800)
internal val DarkPrimaryContainer = Color(0xFF693C00)
internal val DarkOnPrimaryContainer = Color(0xFFFFDCBE)
internal val DarkSecondary = Color(0xFFDEC2A1)
internal val DarkOnSecondary = Color(0xFF3E2D16)
internal val DarkSecondaryContainer = Color(0xFF56432A)
internal val DarkOnSecondaryContainer = Color(0xFFFBDEBC)
internal val DarkTertiary = Color(0xFFB3CFA8)
internal val DarkOnTertiary = Color(0xFF1F361C)
internal val DarkTertiaryContainer = Color(0xFF354D30)
internal val DarkOnTertiaryContainer = Color(0xFFCEEBC2)
internal val DarkError = Color(0xFFFFB4AB)
internal val DarkOnError = Color(0xFF690005)
internal val DarkErrorContainer = Color(0xFF93000A)
internal val DarkOnErrorContainer = Color(0xFFFFDAD6)
internal val DarkBackground = Color(0xFF17130E)
internal val DarkOnBackground = Color(0xFFEAE1D9)
internal val DarkSurface = Color(0xFF17130E)
internal val DarkOnSurface = Color(0xFFEAE1D9)
internal val DarkSurfaceVariant = Color(0xFF4F4539)
internal val DarkOnSurfaceVariant = Color(0xFFD3C4B4)
internal val DarkOutline = Color(0xFF9C8F80)
internal val DarkOutlineVariant = Color(0xFF4F4539)
internal val DarkScrim = Color(0xFF000000)
internal val DarkInverseSurface = Color(0xFFEAE1D9)
internal val DarkInverseOnSurface = Color(0xFF34302A)
internal val DarkInversePrimary = Color(0xFF8B5000)

/**
 * Fixed palette [CoverPlaceholder][app.vinilogs.core.designsystem.component.CoverPlaceholder]
 * picks from deterministically by artist name. Kept separate from the theme
 * roles above: a placeholder needs to stay legibly distinct in both themes
 * without shifting with dark mode.
 */
internal val PlaceholderPalette = listOf(
    Color(0xFF8B5000),
    Color(0xFF4C6547),
    Color(0xFF3E5C76),
    Color(0xFF7A4069),
    Color(0xFF6F5B40),
    Color(0xFF9C4A3C),
    Color(0xFF3E6659),
    Color(0xFF5A5A8C),
)
