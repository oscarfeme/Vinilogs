package app.vinilogs.feature.discovery.navigation

import kotlinx.serialization.Serializable

// Route shapes match the nav graph in 02-ARCHITECTURE.md §5:
//   discover ──→ profile/{uid} ──→ publicRecord/{uid}/{id}
//                            └──→ sharedRecords/{uid}

/** Nested graph for the "Discover" bottom-bar tab — [DiscoverRoute] is its start destination. */
@Serializable
data object DiscoverGraphRoute

@Serializable
data object DiscoverRoute

/**
 * Another user's public profile. Named distinctly from feature:auth's
 * [app.vinilogs.feature.auth.navigation.ProfileRoute] (the signed-in user's
 * own profile, argument-free) even though 02-ARCHITECTURE.md §5 labels both
 * just "profile" — the two are different screens in different modules.
 */
@Serializable
data class PublicProfileRoute(val uid: String)

@Serializable
data class PublicRecordRoute(val uid: String, val recordId: String)

@Serializable
data class SharedRecordsRoute(val uid: String)
