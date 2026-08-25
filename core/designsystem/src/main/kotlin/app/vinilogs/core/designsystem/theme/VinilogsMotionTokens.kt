package app.vinilogs.core.designsystem.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Motion tokens. "Motion is short... nothing over 250 ms" (02-ARCHITECTURE.md
 * §6) — [DurationLong] is that hard cap, not a suggestion.
 */
object VinilogsMotionTokens {
    const val DURATION_SHORT = 100
    const val DURATION_MEDIUM = 180
    const val DURATION_LONG = 250

    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/**
 * Reads the system "Remove animations" developer/accessibility setting
 * (`Settings.Global.ANIMATOR_DURATION_SCALE`). Compose animations don't honour
 * this automatically the way `ValueAnimator`-based ones do, so components must
 * check it explicitly — pass the result through [motionDurationMillis] before
 * handing a duration to an `AnimationSpec`.
 */
@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        scale == 0f
    }
}

/** Returns 0 when the system asks for reduced motion, [base] otherwise. */
@Composable
fun motionDurationMillis(base: Int): Int {
    return if (rememberReducedMotionEnabled()) 0 else base
}
