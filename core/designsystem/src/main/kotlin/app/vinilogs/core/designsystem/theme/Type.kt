package app.vinilogs.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.vinilogs.core.designsystem.R

/**
 * Vinilogs type scale.
 *
 * Inter, subset to Latin + Latin-Ext. Negative tracking on headlines and positive
 * tracking on labels are the most recognisable traits of the reference — do not
 * normalise them to zero.
 *
 * Only [Typography.labelMedium] and [Typography.labelSmall] are ever uppercased,
 * and only for eyebrows, nav labels and chips. Album titles and artist names are
 * proper nouns and are never uppercased.
 *
 * See 05-DESIGN-DIRECTION.md §3.
 */

internal val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/**
 * Tabular figures. Catalogue numbers, condition grades and any column of numbers
 * must align vertically down a list — `PCS 7027` above `SHVL 804` should not
 * wobble. Apply via [tabular] rather than reaching for the feature string.
 */
private const val FEATURE_TABULAR_NUMS = "tnum"

/** Returns this style with tabular (monospaced) figures enabled. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = FEATURE_TABULAR_NUMS)

val VinilogsTypography = Typography(
    // --- Headlines: negative tracking, SemiBold -----------------------------
    headlineLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
    ),
    // --- Titles ------------------------------------------------------------
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    // --- Body --------------------------------------------------------------
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    ),
    // --- Labels: positive tracking; the two smallest are uppercase ----------
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.4.sp,
    ),
    /** Uppercase at the call site: section eyebrows, nav labels, field labels. */
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
    ),
    /** Uppercase at the call site: chips, condition grades, timestamps. */
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.0.sp,
    ),
)
