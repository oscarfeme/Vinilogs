package app.vinilogs.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import app.vinilogs.feature.auth.navigation.AuthGraphRoute
import app.vinilogs.feature.auth.navigation.ProfileGraphRoute
import app.vinilogs.feature.auth.navigation.authGraph
import app.vinilogs.feature.auth.navigation.profileGraph
import app.vinilogs.feature.collection.navigation.ShelfGraphRoute
import app.vinilogs.feature.collection.navigation.shelfGraph
import app.vinilogs.feature.discovery.navigation.DiscoverGraphRoute
import app.vinilogs.feature.discovery.navigation.discoverGraph
import kotlin.reflect.KClass

/**
 * The single-activity nav host: signed-out ([AuthGraphRoute]) and signed-in
 * ([MainGraphRoute], three bottom-bar tabs) as the two top-level graphs.
 *
 * The signed-out/signed-in choice is hardcoded to [AuthGraphRoute] for now —
 * T-09 replaces this with real auth-state routing (00-README.md T-09).
 */
@Composable
fun VinilogsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination.isInTab(ShelfGraphRoute::class) ||
        currentDestination.isInTab(DiscoverGraphRoute::class) ||
        currentDestination.isInTab(ProfileGraphRoute::class)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                VinilogsBottomBar(
                    currentDestination = currentDestination,
                    onTabSelected = { route -> navController.navigateToTab(route) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AuthGraphRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            authGraph(
                navController = navController,
                onAuthenticated = {
                    navController.navigate(MainGraphRoute) {
                        popUpTo(AuthGraphRoute) { inclusive = true }
                    }
                },
            )
            navigation<MainGraphRoute>(startDestination = ShelfGraphRoute) {
                shelfGraph(navController)
                discoverGraph(navController)
                profileGraph(navController)
            }
        }
    }
}

@Composable
private fun VinilogsBottomBar(
    currentDestination: NavDestination?,
    onTabSelected: (Any) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentDestination.isInTab(ShelfGraphRoute::class),
            onClick = { onTabSelected(ShelfGraphRoute) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Shelf") },
        )
        NavigationBarItem(
            selected = currentDestination.isInTab(DiscoverGraphRoute::class),
            onClick = { onTabSelected(DiscoverGraphRoute) },
            icon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text("Discover") },
        )
        NavigationBarItem(
            selected = currentDestination.isInTab(ProfileGraphRoute::class),
            onClick = { onTabSelected(ProfileGraphRoute) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text("Profile") },
        )
    }
}

/** True while the current destination is anywhere inside tab graph [route], not only at its root. */
private fun NavDestination?.isInTab(route: KClass<*>): Boolean =
    this?.hierarchy?.any { it.hasRoute(route) } == true

/** Standard bottom-nav switch: preserves each tab's own back stack. */
private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
