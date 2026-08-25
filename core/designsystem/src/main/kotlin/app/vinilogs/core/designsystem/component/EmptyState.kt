package app.vinilogs.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.vinilogs.core.designsystem.theme.spacing

/**
 * "Empty states are written, not decorated. Each one names the next action...
 * with the action as a button." (02-ARCHITECTURE.md §6). [message] and
 * [actionLabel] are supplied by the caller — this component owns layout, not
 * copy, so callers keep using their own (eventually localised, T-30) strings.
 */
@Composable
fun EmptyState(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                // 48dp: large enough to read as an illustration, not a button.
                modifier = Modifier
                    .size(48.dp)
                    .padding(bottom = MaterialTheme.spacing.md),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onAction,
            modifier = Modifier.padding(top = MaterialTheme.spacing.md),
        ) {
            Text(actionLabel)
        }
    }
}
