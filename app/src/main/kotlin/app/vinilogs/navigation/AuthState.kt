package app.vinilogs.navigation

/** Which top-level graph [VinilogsNavHost] should show, derived from `AuthRepository.currentUser`. */
sealed interface AuthState {
    /** No value has arrived from `AuthRepository.currentUser` yet. */
    data object Loading : AuthState

    data object SignedOut : AuthState

    data object SignedIn : AuthState
}
