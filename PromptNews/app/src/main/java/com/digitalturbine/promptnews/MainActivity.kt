package com.digitalturbine.promptnews

import android.Manifest
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.digitalturbine.promptnews.ui.home.HomeScreen
import com.digitalturbine.promptnews.ui.history.HistoryScreen
import com.digitalturbine.promptnews.ui.search.SearchScreen
import com.digitalturbine.promptnews.util.HomePrefs
import com.digitalturbine.promptnews.data.history.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
    private val historyRepository by lazy { HistoryRepository.getInstance(applicationContext) }
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        HomePrefs.setLocationPrompted(this, true)
        if (granted) {
            resolveAndStoreLocation()
        } else {
            HomePrefs.setLocation(this, "")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            historyRepository.pruneOldEntries()
        }
        maybeRequestLocation()
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val items = listOf(Dest.Search, Dest.Home, Dest.History)
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
                                        val startId = navController.safeStartDestinationId()
                                        navController.navigate(dest.route) {
                                            launchSingleTop = true
                                            restoreState = true
                                            if (startId != null) {
                                                popUpTo(startId) { saveState = true }
                                            }
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
                            route = "${Dest.Search.route}?query={query}&source={source}",
                            arguments = listOf(
                                navArgument("query") { defaultValue = "" },
                                navArgument("source") { defaultValue = "" }
                            )
                        ) { backStackEntry ->
                            SearchScreen(
                                initialQuery = backStackEntry.arguments?.getString("query"),
                                initialSource = backStackEntry.arguments?.getString("source")
                            )
                        }
                        composable(Dest.Home.route) {
                            HomeScreen { query ->
                                if (!isGraphReady) {
                                    return@HomeScreen
                                }
                                val startId = navController.safeStartDestinationId()
                                navController.navigate(
                                    "${Dest.Search.route}?query=${Uri.encode(query)}&source=SEARCH"
                                ) {
                                    launchSingleTop = true
                                    restoreState = true
                                    if (startId != null) {
                                        popUpTo(startId) { saveState = true }
                                    }
                                }
                            }
                        }
                        composable(Dest.History.route) {
                            HistoryScreen { entry ->
                                if (!isGraphReady) {
                                    return@HistoryScreen
                                }
                                val startId = navController.safeStartDestinationId()
                                navController.navigate(
                                    "${Dest.Search.route}?query=${Uri.encode(entry.label)}&source=${entry.type.name}"
                                ) {
                                    launchSingleTop = true
                                    restoreState = true
                                    if (startId != null) {
                                        popUpTo(startId) { saveState = true }
                                    }
                                }
                            }
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

    private fun maybeRequestLocation() {
        if (!HomePrefs.wasLocationPrompted(this)) {
            if (hasCoarseLocation()) {
                HomePrefs.setLocationPrompted(this, true)
                resolveAndStoreLocation()
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            return
        }
        if (hasCoarseLocation() && HomePrefs.getLocation(this).isBlank()) {
            resolveAndStoreLocation()
        }
    }

    private fun hasCoarseLocation(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun resolveAndStoreLocation() {
        val locationManager = getSystemService(LocationManager::class.java) ?: return
        if (!hasCoarseLocation()) return
        lifecycleScope.launch {
            val location = getCoarseLocation(locationManager)
            if (location != null) {
                val label = withContext(Dispatchers.IO) { reverseGeocode(location) }
                if (!label.isNullOrBlank()) {
                    HomePrefs.setLocation(this@MainActivity, label)
                }
            }
        }
    }

    private suspend fun getCoarseLocation(locationManager: LocationManager): Location? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return suspendCancellableCoroutine { cont ->
                val provider = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    LocationManager.NETWORK_PROVIDER
                } else {
                    LocationManager.GPS_PROVIDER
                }
                locationManager.getCurrentLocation(provider, null, mainExecutor) { loc ->
                    cont.resume(loc)
                }
            }
        }
        return withContext(Dispatchers.IO) {
            val providers = locationManager.getProviders(true)
            val provider = providers.firstOrNull() ?: return@withContext null
            locationManager.getLastKnownLocation(provider)
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(location: Location): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(this, Locale.getDefault())
        val results = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        val address = results?.firstOrNull() ?: return null
        val city = address.locality ?: address.subAdminArea ?: address.subLocality
        val state = address.adminArea ?: address.subAdminArea
        val parts = listOfNotNull(city?.takeIf { it.isNotBlank() }, state?.takeIf { it.isNotBlank() })
        return parts.joinToString(", ").ifBlank { null }
    }
}

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Search : Dest("tab_search", "Search", Icons.Filled.Search)
    data object Home : Dest("tab_home", "Home", Icons.Filled.Home)
    data object History : Dest("tab_history", "History", Icons.Filled.History)
}

private fun NavController.safeStartDestinationId(): Int? {
    val id = graph.startDestinationId
    return if (id != 0) id else null
}
