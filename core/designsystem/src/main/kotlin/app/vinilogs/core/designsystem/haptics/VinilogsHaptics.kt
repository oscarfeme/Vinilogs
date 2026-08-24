package app.vinilogs.core.designsystem.haptics

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * The only two haptic effects this app uses: "Haptics on destructive
 * confirmation and on successful record add. Nowhere else." (02-ARCHITECTURE.md
 * §6). Deliberately not a generic `trigger(type)` wrapper — that would let a
 * feature reach for haptics anywhere, defeating the point of the rule.
 */
fun HapticFeedback.destructiveConfirm() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}

fun HapticFeedback.recordAdded() {
    performHapticFeedback(HapticFeedbackType.Confirm)
}
