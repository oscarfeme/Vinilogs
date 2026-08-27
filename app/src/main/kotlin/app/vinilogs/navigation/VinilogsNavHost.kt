package app.vinilogs.navigation

import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import app.vinilogs.core.designsystem.component.LoadingState
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
 * The signed-out/signed-in choice is driven by [AuthStateViewModel], which
 * wraps `AuthRepository.currentUser` (T-09, 02-ARCHITECTURE.md §5). Nothing is
 * shown until the first value arrives, so the graph is never mounted with a
 * guess that then has to be corrected.
 */
@Composable
fun VinilogsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authStateViewModel: AuthStateViewModel = hiltViewModel(),
) {
    val authState by authStateViewModel.authState.collectAsState()

    when (authState) {
        AuthState.Loading -> LoadingState(modifier = modifier.fillMaxSize())
        AuthState.SignedOut, AuthState.SignedIn -> {
            // `remember`ed so a later SignedIn <-> SignedOut flip (e.g. sign-out,
            // handled below) never changes NavHost's startDestination -- Compose
            // Navigation only honours that on first composition.
            val initialStartDestination = remember {
                if (authState == AuthState.SignedIn) MainGraphRoute else AuthGraphRoute
            }
            VinilogsMainNavHost(
                startDestination = initialStartDestination,
                authState = authState,
                modifier = modifier,
                navController = navController,
            )
        }
    }
}

@Composable
private fun VinilogsMainNavHost(
    startDestination: Any,
    authState: AuthState,
    modifier: Modifier,
    navController: NavHostController,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination.isInTab(ShelfGraphRoute::class) ||
        currentDestination.isInTab(DiscoverGraphRoute::class) ||
        currentDestination.isInTab(ProfileGraphRoute::class)

    // Reacts to a sign-out that happens once the main graph is already showing
    // (FR-A2). There is no sign-out UI yet (T-19), but wiring this now means
    // the moment one calls `AuthRepository.signOut()`, routing back to the
    // auth graph is already correct with no further nav-host changes needed.
    LaunchedEffect(authState) {
        if (authState == AuthState.SignedOut && currentDestination.isInTab(MainGraphRoute::class)) {
            navController.navigate(AuthGraphRoute) {
                popUpTo(MainGraphRoute) { inclusive = true }
            }
        }
    }

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
            startDestination = startDestination,
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
private fun NavDestination?.isInTab(route: KClass<*>): Boolean = this?.hierarchy?.any { it.hasRoute(route) } == true

/** Standard bottom-nav switch: preserves each tab's own back stack. */
private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
