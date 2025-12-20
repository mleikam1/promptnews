package com.digitalturbine.promptnews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.digitalturbine.promptnews.ui.home.HomeScreen
import com.digitalturbine.promptnews.ui.search.SearchScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val items = listOf(Dest.Search, Dest.Home, Dest.Following)

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            items.forEach { dest ->
                                NavigationBarItem(
                                    selected = currentDestination?.route == dest.route,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
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
                        composable(Dest.Search.route) { SearchScreen() }
                        composable(Dest.Home.route) { HomeScreen() }
                        composable(Dest.Following.route) { FollowingScreen() }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowingScreen() {
    Box(Modifier.fillMaxSize()) {
        Text(text = "Following (coming soon)", style = MaterialTheme.typography.titleMedium)
    }
}

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Search : Dest("tab_search", "Search", Icons.Filled.Search)
    data object Home : Dest("tab_home", "Home", Icons.Filled.Home)
    data object Following : Dest("tab_following", "Following", Icons.Filled.FavoriteBorder)
}
