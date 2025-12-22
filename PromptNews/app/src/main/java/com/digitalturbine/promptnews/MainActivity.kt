package com.digitalturbine.promptnews

import android.Manifest
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.digitalturbine.promptnews.ui.home.HomeScreen
import com.digitalturbine.promptnews.ui.search.SearchScreen
import com.digitalturbine.promptnews.util.HomePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
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
        maybeRequestLocation()
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
