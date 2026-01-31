package com.digitalturbine.promptnews.ui.home

import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digitalturbine.promptnews.R
import com.digitalturbine.promptnews.ui.components.HeroCard
import com.digitalturbine.promptnews.ui.components.RowCard
import com.digitalturbine.promptnews.util.HomePrefs
import com.digitalturbine.promptnews.web.ArticleWebViewActivity

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf(HomePrefs.getUserLocation(context)) }
    val localNewsState by viewModel.localNewsState.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()

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
                }
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    LaunchedEffect(userLocation) {
        viewModel.loadForLocation(userLocation)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        if (userLocation == null) {
            item {
                Text(
                    text = stringResource(R.string.home_location_required),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            when {
                localNewsState.isLoading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                localNewsState.hasFetched && localNewsState.items.isEmpty() -> {
                    item {
                        Text(
                            text = stringResource(R.string.home_no_local_stories),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    val newsItems = localNewsState.items
                    val hero = newsItems.firstOrNull()
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
                    items(newsItems.drop(1)) { article ->
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
                !localNewsState.hasLoadedMore &&
                localNewsState.items.size >= 4 &&
                !localNewsState.isLoading
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        OutlinedButton(
                            onClick = {
                                viewModel.loadMoreLocalNews(location)
                            },
                            enabled = !localNewsState.isLoadingMore
                        ) {
                            if (localNewsState.isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(text = stringResource(R.string.home_loading_more))
                            } else {
                                Text(text = stringResource(R.string.home_more_local_news))
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.home_local_weather),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            val weatherData = weatherState.data
            when {
                userLocation == null -> {
                    Text(
                        text = stringResource(R.string.home_location_required),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                weatherState.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                weatherData != null -> {
                    WeatherCard(data = weatherData)
                }
                else -> {
                    if (weatherState.hasFetched) {
                        Text(
                            text = stringResource(R.string.home_weather_unavailable),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(data: WeatherData) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = data.city,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = data.temperature,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = data.condition,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = data.highLow,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
