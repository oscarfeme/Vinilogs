package app.vinilogs.core.model

/** The editable subset of [UserProfile] (FR-A4: display name, avatar, bio, location; FR-A5: privacy). */
data class ProfileUpdate(
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val location: String?,
    val isPublic: Boolean,
)
