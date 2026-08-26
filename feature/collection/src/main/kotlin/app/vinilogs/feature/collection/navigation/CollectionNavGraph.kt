package app.vinilogs.feature.collection.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import app.vinilogs.core.designsystem.theme.VinilogsMotion
import app.vinilogs.feature.collection.AddRecordScreen
import app.vinilogs.feature.collection.EditRecordScreen
import app.vinilogs.feature.collection.ShelfScreen
import app.vinilogs.feature.collection.StatsScreen
import app.vinilogs.feature.collection.detail.RecordDetailScreen

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
        // Shared-element continuity with the shelf's cover (per this task's brief) needs a
        // SharedTransitionLayout wrapping the top-level NavHost -- that's app/VinilogsNavHost.kt,
        // outside feature:collection's boundary (CLAUDE.md rule 2), and ShelfScreen's VinylCard
        // call site (T-15, a separate PR) would also need to opt into the same shared key. See
        // this task's PR notes. This scale+fade transition is the in-boundary approximation,
        // using AnimatedContentScope's per-destination transitions (available here regardless of
        // where the NavHost lives) and the standard 200ms transition token
        // (05-DESIGN-DIRECTION.md §6).
        composable<RecordDetailRoute>(
            enterTransition = { recordDetailEnter() },
            exitTransition = { fadeOut(tween(VinilogsMotion.TRANSITION_MS)) },
            popEnterTransition = { fadeIn(tween(VinilogsMotion.TRANSITION_MS)) },
            popExitTransition = { recordDetailPopExit() },
        ) { backStackEntry ->
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

private const val RECORD_DETAIL_COLLAPSED_SCALE = 0.92f

private fun recordDetailEnter(): EnterTransition =
    fadeIn(tween(VinilogsMotion.TRANSITION_MS)) +
        scaleIn(tween(VinilogsMotion.TRANSITION_MS), initialScale = RECORD_DETAIL_COLLAPSED_SCALE)

private fun recordDetailPopExit(): ExitTransition =
    fadeOut(tween(VinilogsMotion.TRANSITION_MS)) +
        scaleOut(tween(VinilogsMotion.TRANSITION_MS), targetScale = RECORD_DETAIL_COLLAPSED_SCALE)
