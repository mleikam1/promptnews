package com.digitalturbine.promptnews.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Geocoder
import android.location.LocationManager
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.digitalturbine.promptnews.R
import com.digitalturbine.promptnews.data.UserLocation
import com.digitalturbine.promptnews.di.AppGraph
import com.digitalturbine.promptnews.ui.components.HeroCard
import com.digitalturbine.promptnews.ui.components.RowCard
import com.digitalturbine.promptnews.util.HomePrefs
import com.digitalturbine.promptnews.web.ArticleWebViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(navController: NavController) {
    val navBackStackEntry = remember {
        navController.getBackStackEntry("tab_home")
    }
    val viewModel: HomeViewModel = viewModel(
        navBackStackEntry,
        factory = AppGraph.homeViewModelFactory
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userLocation by remember { mutableStateOf(HomePrefs.getUserLocation(context)) }
    var userName by remember { mutableStateOf(HomePrefs.getUserName(context)) }
    val localNewsItems by viewModel.localNewsItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasFetched by viewModel.hasFetched.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    var permissionDenied by remember { mutableStateOf(false) }
    var showNamePrompt by remember { mutableStateOf(!HomePrefs.hasSeenNamePrompt(context)) }
    var nameInput by remember { mutableStateOf(userName.orEmpty()) }

    fun fetchLocationAndNews() {
        if (!hasLocationPermission(context)) {
            return
        }
        scope.launch {
            val resolvedLocation = resolveUserLocation(context) ?: HomePrefs.getUserLocation(context)
            if (resolvedLocation != null) {
                HomePrefs.setUserLocation(context, resolvedLocation)
                userLocation = resolvedLocation
                viewModel.fetchLocalNews(resolvedLocation.city, resolvedLocation.state)
            }
        }
    }

    val coarseLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        HomePrefs.setLocationPrompted(context, true)
        if (granted && hasLocationPermission(context)) {
            permissionDenied = false
            fetchLocationAndNews()
        } else {
            permissionDenied = true
            HomePrefs.setUserLocation(context, null)
            userLocation = null
            viewModel.clearLocalNews()
        }
    }

    val fineLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        HomePrefs.setLocationPrompted(context, true)
        if (granted || hasLocationPermission(context)) {
            permissionDenied = false
            fetchLocationAndNews()
        } else {
            coarseLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    DisposableEffect(context) {
        val prefs = HomePrefs.getPrefs(context)
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (
                    key == HomePrefs.KEY_LOCATION ||
                    key == HomePrefs.KEY_LOCATION_CITY ||
                    key == HomePrefs.KEY_LOCATION_STATE
                ) {
                    userLocation = HomePrefs.getUserLocation(context)
                } else if (key == HomePrefs.KEY_USER_NAME) {
                    userName = HomePrefs.getUserName(context)
                }
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    LaunchedEffect(Unit) {
        when {
            hasLocationPermission(context) -> {
                permissionDenied = false
                fetchLocationAndNews()
            }
            !HomePrefs.wasLocationPrompted(context) -> {
                fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                permissionDenied = true
            }
        }
    }

    if (showNamePrompt) {
        AlertDialog(
            onDismissRequest = {
                HomePrefs.saveUserName(context, "")
                HomePrefs.setHasSeenNamePrompt(context, true)
                showNamePrompt = false
            },
            title = { Text(text = stringResource(R.string.home_name_prompt_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(R.string.home_name_prompt_body))
                    TextField(
                        value = nameInput,
                        onValueChange = { nameInput = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        HomePrefs.saveUserName(context, nameInput)
                        HomePrefs.setHasSeenNamePrompt(context, true)
                        userName = HomePrefs.getUserName(context)
                        showNamePrompt = false
                    }
                ) {
                    Text(text = stringResource(R.string.home_name_prompt_continue))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        HomePrefs.saveUserName(context, "")
                        HomePrefs.setHasSeenNamePrompt(context, true)
                        userName = null
                        showNamePrompt = false
                    }
                ) {
                    Text(text = stringResource(R.string.home_name_prompt_skip))
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = getGreeting(userName),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                Text(
                    text = stringResource(R.string.home_local_stories),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                userLocation?.let { location ->
                    Text(
                        text = "${location.city}, ${location.state}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (permissionDenied) {
                item {
                    Text(
                        text = stringResource(R.string.home_location_required),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                when {
                    isLoading -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    hasFetched && localNewsItems.isEmpty() -> {
                        item {
                            Text(
                                text = stringResource(R.string.home_no_local_stories),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    else -> {
                        val hero = localNewsItems.firstOrNull()
                        if (hero != null) {
                            item {
                                HeroCard(hero) {
                                    context.startActivity(
                                        Intent(context, ArticleWebViewActivity::class.java)
                                            .putExtra("url", hero.url)
                                    )
                                }
                            }
                        }
                        items(localNewsItems.drop(1)) { article ->
                            RowCard(article) {
                                context.startActivity(
                                    Intent(context, ArticleWebViewActivity::class.java)
                                        .putExtra("url", article.url)
                                )
                            }
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
                val location = userLocation
                if (
                    location != null &&
                    hasMore &&
                    localNewsItems.isNotEmpty()
                ) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.loadMoreLocalNews(location)
                                },
                                enabled = !isLoadingMore
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(text = stringResource(R.string.home_more_local_news))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getGreeting(userName: String?): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        in 18..20 -> "Good Evening"
        else -> "Goodnight"
    }
    val trimmedName = userName?.trim().orEmpty()
    return if (trimmedName.isNotEmpty()) {
        "$greeting, $trimmedName"
    } else {
        greeting
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}

@Suppress("DEPRECATION")
private suspend fun resolveUserLocation(context: android.content.Context): UserLocation? {
    val locationManager = context.getSystemService(LocationManager::class.java) ?: return null
    if (!hasLocationPermission(context)) return null
    return withContext(Dispatchers.IO) {
        val providers = locationManager.getProviders(true)
        val location = providers.firstNotNullOfOrNull { provider ->
            locationManager.getLastKnownLocation(provider)
        } ?: return@withContext null
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.getDefault())
        val results = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        val address = results?.firstOrNull() ?: return@withContext null
        val city = address.locality ?: address.subAdminArea ?: address.subLocality
        val state = address.adminArea ?: address.subAdminArea
        val resolvedCity = city?.takeIf { it.isNotBlank() }
        val resolvedState = state?.takeIf { it.isNotBlank() }
        if (resolvedCity.isNullOrBlank() || resolvedState.isNullOrBlank()) {
            null
        } else {
            UserLocation(city = resolvedCity, state = resolvedState)
        }
    }
}
