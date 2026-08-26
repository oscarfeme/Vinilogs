package app.vinilogs.core.model

/** The signed-in auth identity exposed by `AuthRepository.currentUser` (02-ARCHITECTURE.md §4). */
data class User(
    val uid: String,
    val email: String,
    val displayName: String,
)
