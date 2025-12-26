package com.digitalturbine.promptnews.ui.search

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.Clip
import com.digitalturbine.promptnews.data.SearchUi
import com.digitalturbine.promptnews.data.history.HistoryRepository
import com.digitalturbine.promptnews.data.history.HistoryType
import com.digitalturbine.promptnews.web.ArticleWebViewActivity
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.launch

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
    vm: SearchViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current
    val historyRepository = remember(ctx) { HistoryRepository.getInstance(ctx) }
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }
    var lastQuery by remember { mutableStateOf("") }

    fun openArticle(url: String) {
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

    val topicSections = listOf(
        SearchTopicSection(
            title = "Trending Now",
            topics = listOf(
                SearchTopicUiModel(
                    id = "trending-taylor-swift",
                    title = "Taylor Swift",
                    imageUrl = "https://images.unsplash.com/photo-1454922915609-78549ad709bb?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Taylor Swift",
                    badge = "Trending"
                ),
                SearchTopicUiModel(
                    id = "trending-live-scores",
                    title = "Live Scores",
                    imageUrl = "https://images.unsplash.com/photo-1517649763962-0c623066013b?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Live Scores",
                    badge = "Live"
                ),
                SearchTopicUiModel(
                    id = "trending-supreme-court",
                    title = "Supreme Court",
                    imageUrl = "https://images.unsplash.com/photo-1477281765962-ef34e8bb0967?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Supreme Court",
                    badge = "Trending"
                ),
                SearchTopicUiModel(
                    id = "trending-gaza-conflict",
                    title = "Gaza Conflict",
                    imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Gaza Conflict",
                    badge = "Live"
                )
            )
        ),
        SearchTopicSection(
            title = "Entertainment",
            topics = listOf(
                SearchTopicUiModel(
                    id = "entertainment-movies",
                    title = "Movies",
                    imageUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Movies",
                    badge = "Popular"
                ),
                SearchTopicUiModel(
                    id = "entertainment-tv-shows",
                    title = "TV Shows",
                    imageUrl = "https://images.unsplash.com/photo-1524985069026-dd778a71c7b4?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "TV Shows",
                    badge = null
                ),
                SearchTopicUiModel(
                    id = "entertainment-music",
                    title = "Music",
                    imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Music",
                    badge = "Trending"
                ),
                SearchTopicUiModel(
                    id = "entertainment-celebrities",
                    title = "Celebrities",
                    imageUrl = "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Celebrities",
                    badge = null
                )
            )
        ),
        SearchTopicSection(
            title = "Sports",
            topics = listOf(
                SearchTopicUiModel(
                    id = "sports-nfl",
                    title = "NFL",
                    imageUrl = "https://images.unsplash.com/photo-1521412644187-c49fa049e84d?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "NFL",
                    badge = null
                ),
                SearchTopicUiModel(
                    id = "sports-nba",
                    title = "NBA",
                    imageUrl = "https://images.unsplash.com/photo-1504450758481-7338eba7524a?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "NBA",
                    badge = null
                ),
                SearchTopicUiModel(
                    id = "sports-ncaa-football",
                    title = "NCAA Football",
                    imageUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "NCAA Football",
                    badge = "Live"
                ),
                SearchTopicUiModel(
                    id = "sports-soccer",
                    title = "Soccer",
                    imageUrl = "https://images.unsplash.com/photo-1504309092620-4d0ec726efa4?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Soccer",
                    badge = null
                )
            )
        ),
        SearchTopicSection(
            title = "News & World",
            topics = listOf(
                SearchTopicUiModel(
                    id = "news-politics",
                    title = "Politics",
                    imageUrl = "https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Politics",
                    badge = "Trending"
                ),
                SearchTopicUiModel(
                    id = "news-business",
                    title = "Business",
                    imageUrl = "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Business",
                    badge = null
                ),
                SearchTopicUiModel(
                    id = "news-technology",
                    title = "Technology",
                    imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Technology",
                    badge = null
                ),
                SearchTopicUiModel(
                    id = "news-climate",
                    title = "Climate",
                    imageUrl = "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Climate",
                    badge = "Live"
                )
            )
        ),
        SearchTopicSection(
            title = "Explore More",
            topics = listOf(
                SearchTopicUiModel(
                    id = "explore-space",
                    title = "Space",
                    imageUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Space",
                    badge = null
                ),
                SearchTopicUiModel(
                    id = "explore-travel",
                    title = "Travel",
                    imageUrl = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Travel",
                    badge = null
                ),
                SearchTopicUiModel(
                    id = "explore-science",
                    title = "Science",
                    imageUrl = "https://images.unsplash.com/photo-1517976487492-5750f3195933?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "Science",
                    badge = "Popular"
                ),
                SearchTopicUiModel(
                    id = "explore-ai",
                    title = "AI",
                    imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=800&q=80",
                    searchQuery = "AI",
                    badge = "Trending"
                )
            )
        )
    )

    val searchBarBottomPadding = 25.dp
    val searchBarContentPadding = 96.dp
    val contentBottomPadding = if (screenState == SearchScreenState.Prompt) {
        searchBarContentPadding
    } else {
        12.dp
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (ui is SearchUi.Idle && screenState == SearchScreenState.Prompt) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = contentBottomPadding)
            ) {
                item { Spacer(Modifier.height(16.dp)) }
                items(topicSections, key = { it.title }) { section ->
                    Text(
                        section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(12.dp))
                    val rowState = rememberLazyListState()
                    LazyRow(
                        state = rowState,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        flingBehavior = rememberSnapFlingBehavior(rowState)
                    ) {
                        items(section.topics, key = { it.id }) { topic ->
                            TopicCarouselCard(
                                topic = topic,
                                onClick = { runChipSearch(topic.searchQuery) }
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
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
                        // Hero
                        s.hero?.let { hero ->
                            item {
                                HeroCard(hero) { openArticle(hero.url) }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                        // list
                        items(s.rows) { a ->
                            RowCard(a, onClick = { openArticle(a.url) })
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
                                        AssistChip(
                                            onClick = { runChipSearch(chip) },
                                            label = { Text(chip) },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = Color(0xFFF0ECF2)
                                            )
                                        )
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

private data class SearchTopicUiModel(
    val id: String,
    val title: String,
    val imageUrl: String,
    val searchQuery: String,
    val badge: String?
)

private data class SearchTopicSection(
    val title: String,
    val topics: List<SearchTopicUiModel>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicCarouselCard(
    topic: SearchTopicUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .width(190.dp)
            .aspectRatio(3f / 4f)
    ) {
        Box {
            AsyncImage(
                model = topic.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xAA000000))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                topic.badge?.let { badge ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2563EB), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            badge.uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    topic.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
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
private fun HeroCard(article: Article, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            AsyncImage(
                model = article.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
            Row(
                Modifier
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(article.logoUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                )
                article.ageLabel?.let { ageLabel ->
                    Spacer(Modifier.width(6.dp))
                    Text(ageLabel, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                article.title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(Color(0xFF2563EB), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    article.interest.replaceFirstChar { it.uppercase() },
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun RowCard(a: Article, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = rememberAsyncImagePainter(a.logoUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                )
                a.sourceName?.let { sourceName ->
                    Spacer(Modifier.width(6.dp))
                    Text(
                        sourceName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                a.title,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            a.ageLabel?.let { ageLabel ->
                Spacer(Modifier.height(4.dp))
                Text(ageLabel, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            AsyncImage(
                model = a.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 118.dp, height = 84.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFF2563EB), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    a.interest.replaceFirstChar { it.uppercase() },
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ClipCard(c: Clip, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(190.dp)
    ) {
        Box {
            AsyncImage(
                model = c.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
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
