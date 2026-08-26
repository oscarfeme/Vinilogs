package app.vinilogs.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import app.vinilogs.feature.auth.EditProfileScreen
import app.vinilogs.feature.auth.ForgotPasswordScreen
import app.vinilogs.feature.auth.ProfileScreen
import app.vinilogs.feature.auth.SettingsScreen
import app.vinilogs.feature.auth.SignInScreen
import app.vinilogs.feature.auth.SignUpScreen

/** The signed-out flow. [onAuthenticated] is called on a successful sign in or sign up. */
fun NavGraphBuilder.authGraph(
    navController: NavController,
    onAuthenticated: () -> Unit,
) {
    navigation<AuthGraphRoute>(startDestination = SignInRoute) {
        composable<SignInRoute> {
            SignInScreen(
                onSignInSuccess = onAuthenticated,
                onNavigateToSignUp = { navController.navigate(SignUpRoute) },
                onNavigateToForgotPassword = { navController.navigate(ForgotPasswordRoute) },
            )
        }
        composable<SignUpRoute> {
            SignUpScreen(
                onSignUpSuccess = onAuthenticated,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<ForgotPasswordRoute> {
            ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}

/** The "Profile" bottom-bar tab — the signed-in user's own profile. */
fun NavGraphBuilder.profileGraph(navController: NavController) {
    navigation<ProfileGraphRoute>(startDestination = ProfileRoute) {
        composable<ProfileRoute> {
            ProfileScreen(
                onNavigateToEditProfile = { navController.navigate(EditProfileRoute) },
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<EditProfileRoute> {
            EditProfileScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
