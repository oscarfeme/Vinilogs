package app.vinilogs.core.model

import java.time.Instant

/** The `users/{uid}` profile document, as seen by profile and discovery screens (§3). */
data class UserProfile(
    val uid: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val location: String?,
    val isPublic: Boolean,
    val recordCount: Int,
    val createdAt: Instant,
)
