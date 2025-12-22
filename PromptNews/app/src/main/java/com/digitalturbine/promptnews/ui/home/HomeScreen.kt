package com.digitalturbine.promptnews.ui.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import com.digitalturbine.promptnews.data.rss.Article
import com.digitalturbine.promptnews.data.rss.GoogleNewsRepository
import com.digitalturbine.promptnews.util.HomePrefs
import com.digitalturbine.promptnews.util.ageLabelFrom
import com.digitalturbine.promptnews.util.toEpochMillisCompat
import com.digitalturbine.promptnews.web.ArticleWebViewActivity
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onSearch: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { HomePrefs.getPrefs(ctx) }

    var top by remember { mutableStateOf<List<Article>>(emptyList()) }
    var local by remember { mutableStateOf<List<Article>>(emptyList()) }
    var showAllLocal by remember { mutableStateOf(false) }
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

    // Load feeds
    LaunchedEffect(Unit) {
        scope.launch { top = GoogleNewsRepository.topStories() }
    }

    LaunchedEffect(location) {
        if (location.isNotBlank()) {
            scope.launch { local = GoogleNewsRepository.localNews(location) }
        } else {
            local = emptyList()
        }
    }

    val heroTop = top.firstOrNull()
    val listTop = top.drop(1).take(4)

    val heroLocal = local.firstOrNull()
    val listLocal = local.drop(1).let { if (showAllLocal) it else it.take(4) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        // Trending topics row
        item {
            TrendingChipsRow(
                topics = listOf(
                    "A.I.", "Taylor Swift", "NFL", "Space X",
                    "Bitcoin", "K-Pop", "NBA trade rumors",
                    "U.S. election", "Weather radar", "Fortnite"
                ),
                onTopicClick = { q ->
                    onSearch(q)
                }
            )
        }

        // Search header
        item {
            Surface(tonalElevation = 4.dp) {
                SearchHeader { q -> onSearch(q) }
            }
        }

        // Top Stories
        if (heroTop != null) {
            item { SectionTitle("Top Stories") }
            item { HeroCard(heroTop) { openArticle(ctx, it) } }
        }
        itemsIndexed(listTop, key = { index, art -> "${art.link}-$index" }) { _, art ->
            SmallArticleRow(art, onClick = { openArticle(ctx, art.link) })
        }

        // Local News
        if (heroLocal != null) {
            item { Spacer(Modifier.height(16.dp)) }
            item { SectionTitle("Local News • $location") }
            item { HeroCard(heroLocal) { openArticle(ctx, it) } }
        }
        itemsIndexed(listLocal, key = { index, art -> "${art.link}-$index" }) { _, art ->
            SmallArticleRow(art, onClick = { openArticle(ctx, art.link) })
        }

        if (!showAllLocal && local.size > 5) {
            item {
                MoreButton(onClick = { showAllLocal = true })
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TrendingChipsRow(
    topics: List<String>,
    onTopicClick: (String) -> Unit
) {
    val chipBlue = Color(0xFF1E88E5)
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
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
                        containerColor = chipBlue,
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
private fun HeroCard(article: Article, onClickLink: (String) -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClickLink(article.link) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            if (!article.imageUrl.isNullOrBlank()) {
                Box {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = article.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    CategoryBadge(
                        label = "News",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    )
                }
            }
            Column(Modifier.padding(16.dp)) {
                val age = remember(article.published) {
                    article.published?.toEpochMillisCompat()?.let { ageLabelFrom(it) }
                }
                if (!age.isNullOrBlank()) {
                    Text(
                        text = age,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = article.title,
                    // ✅ fixed typo here
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SmallArticleRow(article: Article, onClick: () -> Unit) {
    val age = remember(article.published) {
        article.published?.toEpochMillisCompat()?.let { ageLabelFrom(it) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = article.source.takeIf { it.isNotBlank() } ?: "News",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!age.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = age,
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
                modifier = Modifier
                    .size(width = 92.dp, height = 76.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(8.dp))
            CategoryBadge(label = "News")
        }
    }
}

@Composable
private fun CategoryBadge(
    label: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = MaterialTheme.typography.labelMedium.fontSize
) {
    Surface(
        color = Color(0xFF1E88E5),
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

@Composable
private fun MoreButton(
    onClick: () -> Unit,
    text: String = "More local news"
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

private fun openArticle(ctx: android.content.Context, url: String) {
    ctx.startActivity(Intent(ctx, ArticleWebViewActivity::class.java).putExtra("url", url))
}
