package app.vinilogs.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import app.vinilogs.core.designsystem.theme.vinilogsColors
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

/**
 * "User avatars are 1:1 circles at Ink100 when absent, showing initials in labelMedium"
 * (05-DESIGN-DIRECTION.md §5). [model] is passed straight to Coil, so it accepts either a
 * remote URL ([String], the shape [app.vinilogs.core.model.UserProfile.avatarUrl] stores) or a
 * locally-picked [android.net.Uri] (edit-profile's photo-picker preview, before it has been
 * uploaded) -- both render identically here, which is what lets edit-profile show a live
 * preview with this same component.
 */
@Composable
fun Avatar(
    model: Any?,
    displayName: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(MaterialTheme.vinilogsColors.placeholder),
        contentAlignment = Alignment.Center,
    ) {
        if (model == null) {
            AvatarInitials(displayName)
        } else {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { AvatarInitials(displayName) },
                error = { AvatarInitials(displayName) },
                success = { SubcomposeAsyncImageContent() },
            )
        }
    }
}

@Composable
private fun AvatarInitials(displayName: String) {
    Text(text = initialsOf(displayName), style = MaterialTheme.typography.labelMedium)
}

/** Up to two initials from [displayName]'s first two words, uppercased; blank if [displayName] is blank. */
private fun initialsOf(displayName: String): String =
    displayName
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString(separator = "")
