package app.vinilogs.feature.auth.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import app.vinilogs.core.designsystem.theme.spacing

/**
 * Tertiary "text-link CTA" per 05-DESIGN-DIRECTION.md §5: label only, 1dp
 * underline, "for anything non-committal" -- here, moving between the sign-in,
 * sign-up and forgot-password screens.
 */
@Composable
fun AuthTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        textDecoration = TextDecoration.Underline,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .defaultMinSize(minHeight = MaterialTheme.spacing.minTouchTarget)
            .wrapContentHeight()
            .clickable(onClick = onClick, role = Role.Button),
    )
}
