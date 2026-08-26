package app.vinilogs.feature.auth.navigation

import kotlinx.serialization.Serializable

// Route shapes match the nav graph in 02-ARCHITECTURE.md §5:
//   auth (start if signed out)
//    ├── signIn → signUp → forgotPassword
//   main ...
//    └── profile ──→ editProfile → settings

/** Nested graph containing [SignInRoute], [SignUpRoute], [ForgotPasswordRoute]. */
@Serializable
data object AuthGraphRoute

@Serializable
data object SignInRoute

@Serializable
data object SignUpRoute

@Serializable
data object ForgotPasswordRoute

/** Nested graph for the "Profile" bottom-bar tab — [ProfileRoute] is its start destination. */
@Serializable
data object ProfileGraphRoute

/** The signed-in user's own profile — third bottom-bar destination. */
@Serializable
data object ProfileRoute

@Serializable
data object EditProfileRoute

@Serializable
data object SettingsRoute
