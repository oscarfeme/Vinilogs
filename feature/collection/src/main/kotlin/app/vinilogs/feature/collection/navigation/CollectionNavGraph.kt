package app.vinilogs.feature.collection.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import app.vinilogs.feature.collection.RecordDetailScreen
import app.vinilogs.feature.collection.ShelfScreen
import app.vinilogs.feature.collection.StatsScreen
import app.vinilogs.feature.collection.addedit.AddRecordScreen
import app.vinilogs.feature.collection.addedit.EditRecordScreen

/** The "Shelf" bottom-bar tab. */
fun NavGraphBuilder.shelfGraph(navController: NavController) {
    navigation<ShelfGraphRoute>(startDestination = ShelfRoute) {
        composable<ShelfRoute> {
            ShelfScreen(
                onRecordClick = { recordId -> navController.navigate(RecordDetailRoute(recordId)) },
                onAddRecordClick = { navController.navigate(AddRecordRoute) },
                onStatsClick = { navController.navigate(StatsRoute) },
            )
        }
        composable<RecordDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<RecordDetailRoute>()
            RecordDetailScreen(
                recordId = route.recordId,
                onEditClick = { navController.navigate(EditRecordRoute(route.recordId)) },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<EditRecordRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EditRecordRoute>()
            EditRecordScreen(
                recordId = route.recordId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<AddRecordRoute> {
            AddRecordScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<StatsRoute> {
            StatsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
