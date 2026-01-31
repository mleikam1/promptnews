package com.digitalturbine.promptnews.ui.search

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.Clip
import com.digitalturbine.promptnews.data.SearchUi
import com.digitalturbine.promptnews.data.isFotoscapesStory
import com.digitalturbine.promptnews.data.history.HistoryRepository
import com.digitalturbine.promptnews.data.history.HistoryType
import com.digitalturbine.promptnews.ui.PromptNewsTopBar
import com.digitalturbine.promptnews.util.isNflIntent
import com.digitalturbine.promptnews.ui.components.HeroCard
import com.digitalturbine.promptnews.ui.components.FotoscapesArticleCard
import com.digitalturbine.promptnews.ui.components.RowCard
import com.digitalturbine.promptnews.ui.fotoscapes.FotoscapesArticleUi
import com.digitalturbine.promptnews.ui.fotoscapes.FotoscapesExternalUi
import com.digitalturbine.promptnews.ui.fotoscapes.toFotoscapesUi
import com.digitalturbine.promptnews.ui.fotoscapes.FotoscapesArticleActivity
import com.digitalturbine.promptnews.web.ArticleWebViewActivity
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

private val DEFAULT_TRENDING_PROMPTS = listOf(
    "Breaking news today",
    "NFL scores today",
    "Taylor Swift news",
    "Stock market today",
    "Best movies streaming now",
    "AI news today",
    "Celebrity news",
    "Election updates",
    "Top tech news"
)

enum class SearchScreenState {
    Prompt,
    Results
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String? = null,
    initialSource: String? = null,
    screenState: SearchScreenState,
    onSearchRequested: (String, HistoryType) -> Unit,
    onBack: () -> Unit,
    vm: SearchViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current
    val historyRepository = remember(ctx) { HistoryRepository.getInstance(ctx) }
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }
    var lastQuery by remember { mutableStateOf("") }
    fun openFotoscapesArticle(ui: FotoscapesArticleUi) {
        FotoscapesArticleActivity.start(ctx, ui)
    }

    fun openArticle(article: Article) {
        if (article.fotoscapesLbtype.equals("article", ignoreCase = true)) {
            val ui = article.toFotoscapesUi()
            if (ui is FotoscapesArticleUi) {
                openFotoscapesArticle(ui)
            }
            return
        }
        val url = if (article.isFotoscapesStory()) {
            article.fotoscapesLink
        } else {
            article.url
        }
        if (url.isBlank()) return
        ctx.startActivity(Intent(ctx, ArticleWebViewActivity::class.java).putExtra("url", url))
    }

    fun openArticle(url: String) {
        if (url.isBlank()) return
        ctx.startActivity(Intent(ctx, ArticleWebViewActivity::class.java).putExtra("url", url))
    }
    fun runSearch(q: String, type: HistoryType, recordHistory: Boolean) {
        val trimmed = q.trim()
        if (trimmed.isBlank()) return
        text = trimmed
        vm.runSearch(trimmed)
        if (recordHistory) {
            scope.launch {
                historyRepository.addEntry(type, trimmed)
            }
        }
    }
    fun runSearchFromInput(q: String) {
        val trimmed = q.trim()
        if (trimmed.isBlank()) return
        onSearchRequested(trimmed, HistoryType.SEARCH)
    }
    fun runChipSearch(q: String) {
        val trimmed = q.trim()
        if (trimmed.isBlank()) return
        onSearchRequested(trimmed, HistoryType.CHIP)
    }

    LaunchedEffect(ui) {
        when (ui) {
            is SearchUi.Searching -> {
                lastQuery = (ui as SearchUi.Searching).query
            }
            is SearchUi.Ready -> lastQuery = (ui as SearchUi.Ready).query
            else -> {}
        }
    }

    LaunchedEffect(initialQuery) {
        val trimmed = initialQuery?.trim().orEmpty()
        if (trimmed.isNotBlank() && trimmed != lastQuery) {
            val type = initialSource?.let { source -> runCatching { HistoryType.valueOf(source) }.getOrNull() }
            runSearch(trimmed, type ?: HistoryType.SEARCH, type != null)
        }
    }

    BackHandler(enabled = screenState == SearchScreenState.Results) {
        onBack()
    }

    val searchBarBottomPadding = 25.dp
    val searchBarContentPadding = 96.dp
    val contentBottomPadding = if (screenState == SearchScreenState.Prompt) {
        searchBarContentPadding
    } else {
        12.dp
    }
    Column(modifier = Modifier.fillMaxSize()) {
        PromptNewsTopBar(
            showBack = screenState == SearchScreenState.Results,
            onBack = onBack
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
        if (ui is SearchUi.Idle && screenState == SearchScreenState.Prompt) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = contentBottomPadding)
            ) {
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = trendingTitleForToday(),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                maxItemsInEachRow = 3
                            ) {
                                DEFAULT_TRENDING_PROMPTS.forEach { prompt ->
                                    TrendingPill(
                                        text = prompt,
                                        onClick = { runChipSearch(prompt) }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = contentBottomPadding)
            ) {
                item {
                    Spacer(Modifier.height(12.dp))
                }
                item {
                    Spacer(Modifier.height(8.dp))
                }

                when (val s = ui) {
                    is SearchUi.Ready -> {
                        if (isNflIntent(s.query)) {
                            item {
                                NflWidgetPlaceholder()
                            }
                        }
                        // Hero
                        s.hero?.let { hero ->
                            item {
                                if (hero.fotoscapesLbtype.equals("article", ignoreCase = true)) {
                                    val ui = remember(hero) { hero.toFotoscapesUi() }
                                    if (ui is FotoscapesArticleUi) {
                                        FotoscapesArticleCard(ui, onClick = { openFotoscapesArticle(ui) })
                                    }
                                } else if (hero.isFotoscapesStory()) {
                                    val ui = remember(hero) { hero.toFotoscapesUi() }
                                    if (ui is FotoscapesExternalUi) {
                                        HeroCard(hero) { openArticle(ui.link) }
                                    }
                                } else {
                                    HeroCard(hero) { openArticle(hero) }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                        // list
                        items(s.rows) { a ->
                            if (a.fotoscapesLbtype.equals("article", ignoreCase = true)) {
                                val ui = remember(a) { a.toFotoscapesUi() }
                                if (ui is FotoscapesArticleUi) {
                                    FotoscapesArticleCard(ui, onClick = { openFotoscapesArticle(ui) })
                                }
                            } else if (a.isFotoscapesStory()) {
                                val ui = remember(a) { a.toFotoscapesUi() }
                                if (ui is FotoscapesExternalUi) {
                                    RowCard(a, onClick = { openArticle(ui.link) })
                                }
                            } else {
                                RowCard(a, onClick = { openArticle(a) })
                            }
                            HorizontalDivider(thickness = 0.5.dp)
                        }

                        // More button
                        item {
                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                OutlinedButton(
                                    onClick = { vm.loadMore() },
                                    enabled = s.hasMore,
                                    shape = RoundedCornerShape(24.dp)
                                ) { Text(if (s.hasMore) "More" else "No more") }
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        // Clips rail
                        if (s.clips.isNotEmpty()) {
                            item {
                                Text(
                                    "Clips",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(s.clips) { c -> ClipCard(c) { openArticle(c.url) } }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }

                        // Images rail
                        if (s.images.isNotEmpty()) {
                            item {
                                Text(
                                    "Images",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            item {
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(s.images) { u ->
                                        AsyncImage(
                                            model = u,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            placeholder = rememberVectorPainter(Icons.Default.Image),
                                            error = rememberVectorPainter(Icons.Default.Image),
                                            fallback = rememberVectorPainter(Icons.Default.Image),
                                            modifier = Modifier
                                                .size(110.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { openArticle(u) }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(18.dp))
                            }
                        }

                        // Dive deeper
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    "Want to Dive Deeper On this?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "People also ask",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        items(s.extras.peopleAlsoAsk) { q ->
                            ElevatedCard(
                                onClick = { runSearchFromInput(q) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(q, modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        // Similar searches chips
                        if (s.extras.relatedSearches.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Similar searches",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            item {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    s.extras.relatedSearches.forEach { chip ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFFF0ECF2))
                                                .clickable { runChipSearch(chip) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                chip,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                    is SearchUi.Error -> {
                        item { Text("Error: ${s.message}") }
                    }
                    is SearchUi.Idle -> Unit
                    is SearchUi.Searching -> Unit
                }
            }
        }
        if (screenState == SearchScreenState.Prompt) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = searchBarBottomPadding)
            ) {
                SearchInputField(
                    text = text,
                    onValueChange = { text = it },
                    onSearch = { runSearchFromInput(text) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                )
            }
        }
            CenteredLoadingStateView(
                query = lastQuery,
                isLoading = ui is SearchUi.Searching
            )
        }
    }
}


@Composable
private fun NflWidgetPlaceholder() {
    // NFL widget temporarily disabled – class not present in this build.
}

@Composable
private fun TrendingPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = modifier
            .heightIn(min = 32.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

private fun trendingTitleForToday(): String {
    val calendar = Calendar.getInstance()
    val month = calendar.getDisplayName(
        Calendar.MONTH,
        Calendar.LONG,
        Locale.getDefault()
    ) ?: ""

    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val suffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }

    return "Trending Prompts for $month $day$suffix"
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchInputField(
    text: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = text,
        onValueChange = onValueChange,
        placeholder = { Text("Your next discovery starts here") },
        leadingIcon = {
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Prompt search"
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() }
        ),
        modifier = modifier,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}


@Composable
private fun ClipCard(c: Clip, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(190.dp)
    ) {
        Box {
            val placeholderPainter = rememberVectorPainter(Icons.Default.Image)
            AsyncImage(
                model = c.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
            )
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) { Text("New", color = Color.White, fontWeight = FontWeight.ExtraBold) }
            Text(
                c.title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
