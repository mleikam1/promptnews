package com.digitalturbine.promptnews.ui.home

import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.rss.GoogleNewsRepository
import com.digitalturbine.promptnews.ui.PromptNewsTopBar
import com.digitalturbine.promptnews.ui.components.HeroCard
import com.digitalturbine.promptnews.ui.components.RowCard
import com.digitalturbine.promptnews.util.HomePrefs
import com.digitalturbine.promptnews.util.TimeLabelFormatter
import com.digitalturbine.promptnews.web.ArticleWebViewActivity

private sealed class HomeFeedState {
    data object Loading : HomeFeedState()
    data class Ready(
        val topStories: List<Article>,
        val localStories: List<Article>,
        val locationLabel: String
    ) : HomeFeedState()
    data class Error(val message: String) : HomeFeedState()
}

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    val locationLabel = rememberLocationLabel()

    val feedState by produceState<HomeFeedState>(
        initialValue = HomeFeedState.Loading,
        key1 = locationLabel
    ) {
        val topResult = runCatching { GoogleNewsRepository.topStories() }
        val localResult = if (locationLabel.isNotBlank()) {
            runCatching { GoogleNewsRepository.localNews(locationLabel) }
        } else {
            Result.success(emptyList())
        }

        if (topResult.isFailure && localResult.isFailure) {
            value = HomeFeedState.Error("Unable to load headlines.")
            return@produceState
        }

        value = HomeFeedState.Ready(
            topStories = topResult.getOrDefault(emptyList()).toUiArticles("top"),
            localStories = localResult.getOrDefault(emptyList()).toUiArticles("local"),
            locationLabel = locationLabel
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PromptNewsTopBar(
            title = "Home",
            showBack = false,
            onBack = {},
            showProfileIcon = true
        )
        when (val state = feedState) {
            HomeFeedState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is HomeFeedState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
            is HomeFeedState.Ready -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    newsSection(
                        title = "Top Stories",
                        articles = state.topStories,
                        emptyMessage = "No top stories available right now."
                    )
                    val localTitle = if (state.locationLabel.isNotBlank()) {
                        "Local News • ${state.locationLabel}"
                    } else {
                        "Local News"
                    }
                    newsSection(
                        title = localTitle,
                        articles = state.localStories,
                        emptyMessage = if (state.locationLabel.isBlank()) {
                            "Enable location access to see local headlines."
                        } else {
                            "No local stories available right now."
                        }
                    )
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun rememberLocationLabel(): String {
    val ctx = LocalContext.current
    val prefs = remember { HomePrefs.getPrefs(ctx) }
    var label by remember { mutableStateOf(HomePrefs.getLocation(ctx)) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == HomePrefs.KEY_LOCATION) {
                label = HomePrefs.getLocation(ctx)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    LaunchedEffect(ctx) {
        label = HomePrefs.getLocation(ctx)
    }

    return label
}

private fun List<com.digitalturbine.promptnews.data.rss.Article>.toUiArticles(
    interestLabel: String
): List<Article> {
    return map { article ->
        Article(
            title = article.title,
            url = article.link,
            imageUrl = article.imageUrl.orEmpty(),
            logoUrl = "",
            sourceName = article.source,
            ageLabel = TimeLabelFormatter.formatTimeLabel(article.published?.toEpochMilli()),
            interest = interestLabel,
            isFotoscapes = false
        )
    }
}

private fun openArticle(ctx: android.content.Context, url: String) {
    if (url.isBlank()) return
    ctx.startActivity(Intent(ctx, ArticleWebViewActivity::class.java).putExtra("url", url))
}

private fun androidx.compose.foundation.lazy.LazyListScope.newsSection(
    title: String,
    articles: List<Article>,
    emptyMessage: String
) {
    item(key = "header-$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(12.dp))
    }

    if (articles.isEmpty()) {
        item(key = "empty-$title") {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        return
    }

    val hero = articles.first()
    val rows = articles.drop(1)

    item(key = "hero-$title") {
        HeroCard(article = hero) {
            openArticle(LocalContext.current, hero.url)
        }
        Spacer(Modifier.height(8.dp))
    }

    items(rows, key = { article -> "$title-${article.url}" }) { article ->
        RowCard(a = article) { openArticle(LocalContext.current, article.url) }
        HorizontalDivider(thickness = 0.5.dp)
    }
    item(key = "footer-$title") { Spacer(Modifier.height(16.dp)) }
}
