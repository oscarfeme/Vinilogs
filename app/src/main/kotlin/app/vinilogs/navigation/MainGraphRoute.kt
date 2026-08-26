package app.vinilogs.navigation

import kotlinx.serialization.Serializable

/**
 * Wraps the three signed-in bottom-bar tab graphs (shelf, discover, profile).
 * Owned here, not by any feature module — it's pure integration, matching no
 * single feature's screens.
 */
@Serializable
data object MainGraphRoute
