package app.vinilogs.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing, shape and motion constants.
 *
 * Every gap in the app is one of these tokens. A raw `.dp` in a padding modifier
 * is a review comment.
 *
 * See docs/DESIGN-DIRECTION.md §4 and §6.
 */

@Immutable
data class Spacing(
    /** Optical nudge between an icon and its label. */
    val xxs: Dp = 2.dp,
    /** Inside chips. */
    val xs: Dp = 4.dp,
    /** Between related lines of text. */
    val sm: Dp = 8.dp,
    /** List item vertical padding. */
    val md: Dp = 12.dp,
    /** Grid gutter, standard block gap. */
    val lg: Dp = 16.dp,
    /** Between sections. */
    val xl: Dp = 24.dp,
    /** Above a section header. */
    val xxl: Dp = 32.dp,
    /** Around empty states. */
    val xxxl: Dp = 48.dp,

    /**
     * Standard screen horizontal gutter. Deliberately wider than Android's
     * default 16dp — whitespace is the primary tool of this system.
     */
    val screenHorizontal: Dp = 20.dp,

    /**
     * The shelf grid is the one exception to [screenHorizontal]: it sits at 16dp
     * so a third column of sleeves stays legible on a small phone.
     */
    val shelfHorizontal: Dp = 16.dp,

    /** Minimum touch target, regardless of the drawn size of the control. */
    val minTouchTarget: Dp = 48.dp,

    /** Every rule in the app. Never 0.5dp — it disappears on ldpi. */
    val hairline: Dp = 1.dp,

    /** Focused text field underline, and destructive outlines. */
    val hairlineEmphasis: Dp = 2.dp,
)

internal val DefaultSpacing = Spacing()

/**
 * Sharp by default.
 *
 * Album sleeves are **not** covered here: they are always `RectangleShape` and
 * always 1:1. A sleeve is a square object — rounding it or cropping it to another
 * ratio is wrong regardless of what the layout wants.
 */
val VinilogsShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp), // chips, inputs
    small = RoundedCornerShape(0.dp), // buttons
    medium = RoundedCornerShape(2.dp), // cards, chat bubbles
    large = RoundedCornerShape(4.dp), // dialogs
    extraLarge = RoundedCornerShape( // bottom sheets — top corners only
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp,
    ),
)

/**
 * Motion durations, in milliseconds.
 *
 * No bounce, no overshoot, no spring. Optimistic UI draws the result before the
 * network confirms it, so motion must never imply a wait that is not happening.
 */
object VinilogsMotion {
    /** State changes: selection, toggles, ripples. */
    const val STATE_MS = 150

    /** Screen and container transitions. */
    const val TRANSITION_MS = 200

    /** Sleeve artwork fading in from its placeholder. */
    const val IMAGE_CROSSFADE_MS = 150
}
