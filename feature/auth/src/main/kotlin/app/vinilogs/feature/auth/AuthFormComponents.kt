package app.vinilogs.feature.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.vinilogs.core.designsystem.theme.spacing

// Small pieces shared by SignInScreen.kt / SignUpScreen.kt / ForgotPasswordScreen.kt (T-09).
// `internal`, not `private`: used across files in this package, not exposed outside the module.

/** Inline Alert-coloured failure text, shared by all three forms' non-field errors. */
@Composable
internal fun AuthGeneralError(message: String?, testTag: String) {
    if (message != null) {
        Spacer(Modifier.height(MaterialTheme.spacing.sm))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag(testTag),
        )
    }
}

/** Shared submit-button content: a spinner while loading, the label otherwise. */
@Composable
internal fun SubmitButtonLabel(isLoading: Boolean, label: String) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
        )
    } else {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * A text toggle rather than an eye icon -- `material-icons-core` (this
 * project's only icon dependency, 00-README.md) doesn't carry
 * Visibility/VisibilityOff, and a labelled toggle reads fine against
 * 05-DESIGN-DIRECTION.md's minimal, mostly iconless component language.
 */
@Composable
internal fun PasswordVisibilityToggle(passwordVisible: Boolean, onToggle: () -> Unit) {
    TextButton(onClick = onToggle) {
        Text(
            text = if (passwordVisible) "Hide" else "Show",
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
