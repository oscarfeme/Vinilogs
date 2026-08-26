package app.vinilogs.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Top bar used across every screen. [onNavigateBack] null renders no
 * navigation icon (top-level / bottom-bar destinations); non-null renders a
 * back arrow. [navigationIconContentDescription] is required whenever
 * [onNavigateBack] is set so the back action always has an accessible label
 * (NFR-7) — callers supply their own localised string.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VinilogsTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    navigationIconContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        modifier = modifier,
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = navigationIconContentDescription,
                    )
                }
            }
        },
        actions = actions,
    )
}
