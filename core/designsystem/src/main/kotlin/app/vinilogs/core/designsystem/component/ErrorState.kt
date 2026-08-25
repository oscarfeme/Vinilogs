package app.vinilogs.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.vinilogs.core.designsystem.theme.VinilogsTheme

/**
 * Generic failure shell. A primary action (typically retry) is required; a
 * secondary action is optional and exists mainly so a catalogue-lookup
 * failure can offer "Add manually" as a first-class escape hatch alongside
 * retry (FR-B1: "graceful... offline and rate-limited states that always
 * offer manual entry as the way forward").
 */
@Composable
fun ErrorState(
    message: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(VinilogsTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onPrimaryAction,
            modifier = Modifier.padding(top = VinilogsTheme.spacing.md),
        ) {
            Text(primaryActionLabel)
        }
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            TextButton(
                onClick = onSecondaryAction,
                modifier = Modifier.padding(top = VinilogsTheme.spacing.xs),
            ) {
                Text(secondaryActionLabel)
            }
        }
    }
}
