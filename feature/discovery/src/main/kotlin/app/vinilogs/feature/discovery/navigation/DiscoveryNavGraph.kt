package app.vinilogs.feature.discovery.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import app.vinilogs.feature.discovery.DiscoverScreen
import app.vinilogs.feature.discovery.PublicProfileScreen
import app.vinilogs.feature.discovery.PublicRecordScreen
import app.vinilogs.feature.discovery.SharedRecordsScreen

/** The "Discover" bottom-bar tab. */
fun NavGraphBuilder.discoverGraph(navController: NavController) {
    navigation<DiscoverGraphRoute>(startDestination = DiscoverRoute) {
        composable<DiscoverRoute> {
            DiscoverScreen(
                onUserClick = { uid -> navController.navigate(PublicProfileRoute(uid)) },
            )
        }
        composable<PublicProfileRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PublicProfileRoute>()
            PublicProfileScreen(
                uid = route.uid,
                onRecordClick = { recordId ->
                    navController.navigate(PublicRecordRoute(route.uid, recordId))
                },
                onSharedRecordsClick = { navController.navigate(SharedRecordsRoute(route.uid)) },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<PublicRecordRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PublicRecordRoute>()
            PublicRecordScreen(
                uid = route.uid,
                recordId = route.recordId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<SharedRecordsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<SharedRecordsRoute>()
            SharedRecordsScreen(
                uid = route.uid,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
