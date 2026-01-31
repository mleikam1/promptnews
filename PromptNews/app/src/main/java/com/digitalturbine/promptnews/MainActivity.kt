package com.digitalturbine.promptnews

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.digitalturbine.promptnews.ui.home.HomeFragmentHost
import com.digitalturbine.promptnews.ui.history.HistoryScreen
import com.digitalturbine.promptnews.ui.search.CenteredLoadingStateView
import com.digitalturbine.promptnews.ui.search.SearchScreen
import com.digitalturbine.promptnews.ui.search.SearchScreenState
import com.digitalturbine.promptnews.ui.sports.SportsScreen
import com.digitalturbine.promptnews.data.sports.SportsRepository
import com.digitalturbine.promptnews.data.history.HistoryRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val historyRepository by lazy { HistoryRepository.getInstance(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            historyRepository.pruneOldEntries()
        }
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val items = listOf(Dest.Home, Dest.Search, Dest.History)
                var isGraphReady by remember { mutableStateOf(false) }

                // The crash happens when navigate() calls findStartDestination() while the graph
                // is still empty. Track readiness so we never navigate until nodes exist.
                LaunchedEffect(navController) {
                    Log.d("Nav", "startDestination=${Dest.Search.route}")
                    snapshotFlow { navController.graph }
                        .map { graph -> graph.nodes.size() > 0 }
                        .distinctUntilChanged()
                        .collect { isGraphReady = it }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            items.forEach { dest ->
                                NavigationBarItem(
                                    selected = currentDestination?.route?.startsWith(dest.route) == true,
                                    enabled = isGraphReady,
                                    onClick = {
                                        Log.d(
                                            "Nav",
                                            "startDestinationId=${navController.graph.startDestinationId} " +
                                                "nodes=${navController.graph.nodes.size()}"
                                        )
                                        if (!isGraphReady) {
                                            return@NavigationBarItem
                                        }
                                        if (currentDestination?.route?.startsWith(dest.route) == true) {
                                            return@NavigationBarItem
                                        }
                                        // Keep the existing back stack intact when switching tabs.
                                        if (!navController.popBackStack(dest.route, inclusive = false)) {
                                            navController.navigate(dest.route) { launchSingleTop = true }
                                        }
                                    },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label) }
                                )
                            }
                        }
                    }
                ) { pad ->
                    NavHost(
                        navController = navController,
                        startDestination = Dest.Search.route,
                        modifier = Modifier.padding(pad)
                    ) {
                        composable(
                            route = "${Dest.Search.route}?query={query}",
                            arguments = listOf(
                                navArgument("query") { defaultValue = "" }
                            )
                        ) { backStackEntry ->
                            SearchScreen(
                                initialQuery = backStackEntry.arguments?.getString("query"),
                                screenState = SearchScreenState.Prompt,
                                onSearchRequested = { query ->
                                    // Push results onto the back stack so back returns to prompt.
                                    navController.navigate(Dest.SearchResults.routeFor(query))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "${Dest.SearchResults.route}?query={query}",
                            arguments = listOf(
                                navArgument("query") { defaultValue = "" }
                            )
                        ) { backStackEntry ->
                            val query = backStackEntry.arguments?.getString("query").orEmpty()
                            val sportsRepository = remember { SportsRepository() }
                            val routeState by produceState<SearchResultsRoute>(initialValue = SearchResultsRoute.Loading, key1 = query) {
                                if (query.isBlank()) {
                                    value = SearchResultsRoute.News
                                    return@produceState
                                }
                                val results = runCatching { sportsRepository.fetchSportsResults(query) }.getOrNull()
                                val hasSports = results?.matches?.isNotEmpty() == true ||
                                    results?.header?.title?.isNotBlank() == true
                                value = if (hasSports) SearchResultsRoute.Sports else SearchResultsRoute.News
                            }
                            when (routeState) {
                                SearchResultsRoute.Loading -> {
                                    CenteredLoadingStateView(query = query, isLoading = true)
                                }
                                SearchResultsRoute.Sports -> {
                                    SportsScreen(
                                        query = query,
                                        showBack = true,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                SearchResultsRoute.News -> {
                                    SearchScreen(
                                        initialQuery = query,
                                        screenState = SearchScreenState.Results,
                                        onSearchRequested = { newQuery ->
                                            // Each follow-up prompt creates its own results entry.
                                            navController.navigate(Dest.SearchResults.routeFor(newQuery))
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                        composable(Dest.Home.route) {
                            HomeFragmentHost()
                        }
                        composable(Dest.Sports.route) {
                            SportsScreen(query = "Live scores", showBack = false, onBack = {})
                        }
                        composable(Dest.History.route) {
                            HistoryScreen(onEntrySelected = { entry ->
                                if (!isGraphReady) {
                                    return@HistoryScreen
                                }
                                // History taps should navigate forward and preserve back behavior.
                                navController.navigate(
                                    Dest.SearchResults.routeFor(entry.query)
                                )
                            })
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            historyRepository.pruneOldEntries()
        }
    }
}

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Search : Dest("tab_search", "Prompt", Icons.Filled.Edit)
    data object Home : Dest("tab_home", "Home", Icons.Filled.Home)
    data object Sports : Dest("tab_sports", "Sports", Icons.Filled.SportsSoccer)
    data object History : Dest("tab_history", "History", Icons.Filled.History)
    data object SearchResults : Dest("tab_search_results", "Results", Icons.Filled.Edit) {
        fun routeFor(query: String): String {
            return "$route?query=${Uri.encode(query)}"
        }
    }
}

private enum class SearchResultsRoute {
    Loading,
    Sports,
    News
}
