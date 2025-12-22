package com.digitalturbine.promptnews.ui.home

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.network.Services
import com.digitalturbine.promptnews.util.HomePrefs
import com.digitalturbine.promptnews.web.ArticleWebViewActivity
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun HomeScreen(onSearch: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { HomePrefs.getPrefs(ctx) }

    var top by remember { mutableStateOf<List<Article>>(emptyList()) }
    var local by remember { mutableStateOf<List<Article>>(emptyList()) }
    var location by remember { mutableStateOf(HomePrefs.getLocation(ctx)) }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == HomePrefs.KEY_LOCATION) {
                location = HomePrefs.getLocation(ctx)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            top = runCatching { Services.fetchFsArticles("top news", limit = 5) }.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(location) {
        if (location.isNotBlank()) {
            val query = "$location news"
            scope.launch {
                local = runCatching { Services.fetchSerpNews(query, page = 0, pageSize = 8) }.getOrDefault(emptyList())
            }
        } else {
            local = emptyList()
        }
    }

    val heroTop = top.firstOrNull()
    val listTop = top.drop(1).take(4)

    val heroLocal = local.firstOrNull()
    val listLocal = local.drop(1).take(6)

    val defaultBadge = "News"
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        item {
            TrendingChipsRow(
                topics = listOf(
                    "A.I.", "Taylor Swift", "NFL", "Space X",
                    "Bitcoin", "K-Pop", "NBA trade rumors",
                    "U.S. election", "Weather radar", "Fortnite"
                ),
                onTopicClick = { q -> onSearch(q) }
            )
        }

        item {
            Surface(tonalElevation = 4.dp) {
                SearchHeader { q -> onSearch(q) }
            }
        }

        if (heroTop != null) {
            item { SectionTitle("Top Stories") }
            item {
                val badgeLabel = heroTop.interest
                    .takeIf { it.isNotBlank() }
                    ?.replaceFirstChar { it.uppercase() }
                    ?: defaultBadge
                HeroCard(
                    article = heroTop,
                    badgeLabel = badgeLabel,
                    badgeColor = badgeColorFor(badgeLabel)
                ) { openArticle(ctx, heroTop.url) }
            }
        }
        itemsIndexed(listTop, key = { index, art -> "${art.url}-$index" }) { index, art ->
            NewsRowCard(
                article = art,
                badgeLabel = defaultBadge,
                badgeColor = badgeColorFor(defaultBadge),
                showDivider = index != listTop.lastIndex,
                onClick = { openArticle(ctx, art.url) }
            )
        }

        if (location.isNotBlank() && heroLocal != null) {
            item { Spacer(Modifier.height(16.dp)) }
            item { SectionTitle("Local News • $location") }
            item {
                HeroCard(
                    article = heroLocal,
                    badgeLabel = defaultBadge,
                    badgeColor = badgeColorFor(defaultBadge)
                ) { openArticle(ctx, heroLocal.url) }
            }
        }
        itemsIndexed(listLocal, key = { index, art -> "${art.url}-$index" }) { index, art ->
            NewsRowCard(
                article = art,
                badgeLabel = defaultBadge,
                badgeColor = badgeColorFor(defaultBadge),
                showDivider = index != listLocal.lastIndex,
                onClick = { openArticle(ctx, art.url) }
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TrendingChipsRow(
    topics: List<String>,
    onTopicClick: (String) -> Unit
) {
    val chipRed = Color(0xFFE53935)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Trending search topics",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(topics) { t ->
                AssistChip(
                    onClick = { onTopicClick(t) },
                    label = { Text(t) },
                    shape = RoundedCornerShape(24.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = chipRed,
                        labelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun SearchHeader(onSearch: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxWidth(), tonalElevation = 0.dp) {
        TextField(
            value = q,
            onValueChange = { q = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(54.dp),
            placeholder = { Text("search") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    val s = q.trim()
                    if (s.isNotEmpty()) onSearch(s)
                }
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF1EEF4),
                unfocusedContainerColor = Color(0xFFF1EEF4),
                disabledContainerColor = Color(0xFFF1EEF4),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun HeroCard(
    article: Article,
    badgeLabel: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box {
            AsyncImage(
                model = article.imageUrl,
                contentDescription = article.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (article.logoUrl.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(article.logoUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                    )
                }
                val sourceName = article.sourceName
                if (!sourceName.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = sourceName,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            CategoryBadge(
                label = badgeLabel,
                containerColor = badgeColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )
            Text(
                text = article.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun NewsRowCard(
    article: Article,
    badgeLabel: String,
    badgeColor: Color,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (article.logoUrl.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(article.logoUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                        )
                    }
                    val sourceName = article.sourceName
                    if (!sourceName.isNullOrBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = sourceName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                val ageLabel = article.ageLabel
                if (!ageLabel.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = ageLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 92.dp, height = 76.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(8.dp))
                CategoryBadge(
                    label = badgeLabel,
                    containerColor = badgeColor
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun CategoryBadge(
    label: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = MaterialTheme.typography.labelMedium.fontSize,
    containerColor: Color = Color(0xFF1E88E5)
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = Color.White,
            style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private fun badgeColorFor(label: String): Color {
    return when (label.lowercase(Locale.US)) {
        "entertainment" -> Color(0xFF8E24AA)
        "sports" -> Color(0xFFD32F2F)
        "technology" -> Color(0xFF00897B)
        "business" -> Color(0xFF6D4C41)
        "politics" -> Color(0xFF5E35B1)
        "science" -> Color(0xFF3949AB)
        "health" -> Color(0xFF2E7D32)
        else -> Color(0xFF1E88E5)
    }
}

private fun openArticle(ctx: android.content.Context, url: String) {
    if (url.isBlank()) return
    ctx.startActivity(Intent(ctx, ArticleWebViewActivity::class.java).putExtra("url", url))
}
