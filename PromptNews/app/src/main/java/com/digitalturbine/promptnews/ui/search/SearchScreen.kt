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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.rememberAsyncImagePainter
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.Clip
import com.digitalturbine.promptnews.data.SearchRepository
import com.digitalturbine.promptnews.data.SearchUi
import com.digitalturbine.promptnews.data.history.HistoryRepository
import com.digitalturbine.promptnews.data.history.HistoryType
import com.digitalturbine.promptnews.web.ArticleWebViewActivity
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.launch
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SearchScreenState {
    Prompt,
    Results
}

enum class TopicType {
    PERSON,
    SPORTS_LEAGUE,
    SPORTS_TEAM,
    PLACE,
    EVENT,
    CATEGORY
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
                    searchQuery = "Taylor Swift",
                    badge = "Trending",
                    topicType = TopicType.PERSON,
                    entityQuery = "Taylor Swift",
                    imageUrl = "https://upload.wikimedia.org/wikipedia/commons/6/61/Taylor_Swift_at_the_2023_MTV_Video_Music_Awards_2.png"
                ),
                SearchTopicUiModel(
                    id = "trending-live-scores",
                    title = "Live Scores",
                    searchQuery = "Live Scores",
                    badge = "Live",
                    topicType = TopicType.EVENT,
                    entityQuery = "sports scoreboard",
                    imageUrl = "https://images.unsplash.com/photo-1517649763962-0c623066013b?auto=format&fit=crop&w=800&q=80"
                ),
                SearchTopicUiModel(
                    id = "trending-supreme-court",
                    title = "Supreme Court",
                    searchQuery = "Supreme Court",
                    badge = "Trending",
                    topicType = TopicType.PLACE,
                    entityQuery = "Supreme Court of the United States"
                ),
                SearchTopicUiModel(
                    id = "trending-gaza-conflict",
                    title = "Gaza Conflict",
                    searchQuery = "Gaza Conflict",
                    badge = "Live",
                    topicType = TopicType.EVENT,
                    entityQuery = "breaking news crowd"
                )
            )
        ),
        SearchTopicSection(
            title = "Entertainment",
            topics = listOf(
                SearchTopicUiModel(
                    id = "entertainment-movies",
                    title = "Movies",
                    searchQuery = "Movies",
                    badge = "Popular",
                    topicType = TopicType.CATEGORY,
                    entityQuery = null
                ),
                SearchTopicUiModel(
                    id = "entertainment-tv-shows",
                    title = "TV Shows",
                    searchQuery = "TV Shows",
                    badge = null,
                    topicType = TopicType.CATEGORY,
                    entityQuery = null
                ),
                SearchTopicUiModel(
                    id = "entertainment-music",
                    title = "Music",
                    searchQuery = "Music",
                    badge = "Trending",
                    topicType = TopicType.CATEGORY,
                    entityQuery = null
                ),
                SearchTopicUiModel(
                    id = "entertainment-celebrities",
                    title = "Celebrities",
                    searchQuery = "Celebrities",
                    badge = null,
                    topicType = TopicType.CATEGORY,
                    entityQuery = null
                )
            )
        ),
        SearchTopicSection(
            title = "Sports",
            topics = listOf(
                SearchTopicUiModel(
                    id = "sports-nfl",
                    title = "NFL",
                    searchQuery = "NFL",
                    badge = null,
                    topicType = TopicType.SPORTS_LEAGUE,
                    entityQuery = "NFL logo",
                    imageUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/a/a2/National_Football_League_logo.svg/512px-National_Football_League_logo.svg.png"
                ),
                SearchTopicUiModel(
                    id = "sports-los-angeles-lakers",
                    title = "Los Angeles Lakers",
                    searchQuery = "Los Angeles Lakers",
                    badge = null,
                    topicType = TopicType.SPORTS_TEAM,
                    entityQuery = "Los Angeles Lakers logo"
                ),
                SearchTopicUiModel(
                    id = "sports-ncaa-football",
                    title = "NCAA Football",
                    searchQuery = "NCAA Football",
                    badge = "Live",
                    topicType = TopicType.EVENT,
                    entityQuery = "football stadium night"
                ),
                SearchTopicUiModel(
                    id = "sports-soccer",
                    title = "Soccer",
                    searchQuery = "Soccer",
                    badge = null,
                    topicType = TopicType.CATEGORY,
                    entityQuery = null
                )
            )
        ),
        SearchTopicSection(
            title = "News & World",
            topics = listOf(
                SearchTopicUiModel(
                    id = "news-politics",
                    title = "Politics",
                    searchQuery = "Politics",
                    badge = "Trending",
                    topicType = TopicType.CATEGORY,
                    entityQuery = null
                ),
                SearchTopicUiModel(
                    id = "news-business",
                    title = "Business",
                    searchQuery = "Business",
                    badge = null,
                    topicType = TopicType.CATEGORY,
                    entityQuery = null
                ),
                SearchTopicUiModel(
                    id = "news-technology",
                    title = "Technology",
                    searchQuery = "Technology",
                    badge = null,
                    topicType = TopicType.CATEGORY,
                    entityQuery = null
                ),
                SearchTopicUiModel(
                    id = "news-climate",
                    title = "Climate",
                    searchQuery = "Climate",
                    badge = "Live",
                    topicType = TopicType.EVENT,
                    entityQuery = "climate protest crowd"
                )
            )
        ),
        SearchTopicSection(
            title = "Explore More",
            topics = listOf(
                SearchTopicUiModel(
                    id = "explore-space",
                    title = "Space",
                    searchQuery = "Space",
                    badge = null,
                    topicType = TopicType.CATEGORY,
                    entityQuery = null
                ),
                SearchTopicUiModel(
                    id = "explore-travel",
                    title = "Travel",
                    searchQuery = "Travel",
                    badge = null,
                    topicType = TopicType.CATEGORY,
                    entityQuery = null
                ),
                SearchTopicUiModel(
                    id = "explore-science",
                    title = "Science",
                    searchQuery = "Science",
                    badge = "Popular",
                    topicType = TopicType.CATEGORY,
                    entityQuery = null
                ),
                SearchTopicUiModel(
                    id = "explore-ai",
                    title = "AI",
                    searchQuery = "AI",
                    badge = "Trending",
                    topicType = TopicType.CATEGORY,
                    entityQuery = null
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
    val topicImageResolver = remember { TopicImageResolver(SearchRepository()) }

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
                                imageResolver = topicImageResolver,
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
    val searchQuery: String,
    val badge: String?,
    val topicType: TopicType,
    val entityQuery: String?,
    val imageUrl: String? = null
)

private data class SearchTopicSection(
    val title: String,
    val topics: List<SearchTopicUiModel>
)

private data class TopicImageState(
    val url: String?,
    val isLoading: Boolean
)

private class TopicImageResolver(
    private val repo: SearchRepository
) {
    private val cache = mutableMapOf<String, String>()
    private val cacheMutex = Mutex()

    suspend fun resolve(topic: SearchTopicUiModel): String {
        topic.imageUrl?.let { return it }
        val query = serpQueryFor(topic)
        val cacheKey = "${topic.topicType}:${query ?: topic.title}"
        cacheMutex.withLock {
            cache[cacheKey]?.let { return it }
        }

        val serpImage = query?.let { repo.fetchImages(it).firstOrNull() }
        val resolved = serpImage ?: fallbackFor(topic)
        cacheMutex.withLock {
            cache[cacheKey] = resolved
        }
        return resolved
    }

    private fun serpQueryFor(topic: SearchTopicUiModel): String? {
        val base = topic.entityQuery ?: return null
        return when (topic.topicType) {
            TopicType.PERSON -> "$base portrait photo"
            TopicType.SPORTS_LEAGUE,
            TopicType.SPORTS_TEAM -> if (base.contains("logo", ignoreCase = true)) base else "$base logo"
            TopicType.PLACE -> "$base photo"
            TopicType.EVENT,
            TopicType.CATEGORY -> base
        }
    }

    private fun fallbackFor(topic: SearchTopicUiModel): String {
        val byEntity = topic.entityQuery?.let { entityFallbackImages[it] }
        if (!byEntity.isNullOrBlank()) return byEntity
        return when (topic.topicType) {
            TopicType.CATEGORY -> categoryImageUrls[topic.title]
            else -> topicTypeFallbackImages[topic.topicType]
        } ?: topicTypeFallbackImages.getValue(TopicType.CATEGORY)
    }
}

@Composable
private fun rememberTopicImage(
    topic: SearchTopicUiModel,
    resolver: TopicImageResolver
): State<TopicImageState> {
    return produceState(
        initialValue = TopicImageState(url = null, isLoading = true),
        key1 = topic.id,
        key2 = topic.imageUrl,
        key3 = topic.entityQuery ?: topic.topicType
    ) {
        val resolved = resolver.resolve(topic)
        value = TopicImageState(url = resolved, isLoading = false)
    }
}

private val categoryImageUrls = mapOf(
    "Movies" to "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=800&q=80",
    "TV Shows" to "https://images.unsplash.com/photo-1524985069026-dd778a71c7b4?auto=format&fit=crop&w=800&q=80",
    "Music" to "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=800&q=80",
    "Celebrities" to "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&w=800&q=80",
    "Soccer" to "https://images.unsplash.com/photo-1504309092620-4d0ec726efa4?auto=format&fit=crop&w=800&q=80",
    "Politics" to "https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=800&q=80",
    "Business" to "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=800&q=80",
    "Technology" to "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=800&q=80",
    "Space" to "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?auto=format&fit=crop&w=800&q=80",
    "Travel" to "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=800&q=80",
    "Science" to "https://images.unsplash.com/photo-1517976487492-5750f3195933?auto=format&fit=crop&w=800&q=80",
    "AI" to "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=800&q=80"
)

private val entityFallbackImages = mapOf(
    "Supreme Court of the United States" to "https://upload.wikimedia.org/wikipedia/commons/1/1b/US_Supreme_Court_Building.jpg",
    "Los Angeles Lakers logo" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3c/Los_Angeles_Lakers_logo.svg/512px-Los_Angeles_Lakers_logo.svg.png",
    "sports scoreboard" to "https://images.unsplash.com/photo-1517649763962-0c623066013b?auto=format&fit=crop&w=800&q=80",
    "breaking news crowd" to "https://images.unsplash.com/photo-1495020689067-958852a7765e?auto=format&fit=crop&w=800&q=80",
    "football stadium night" to "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?auto=format&fit=crop&w=800&q=80",
    "climate protest crowd" to "https://images.unsplash.com/photo-1497493292307-31c376b6e479?auto=format&fit=crop&w=800&q=80"
)

private val topicTypeFallbackImages = mapOf(
    TopicType.PERSON to "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=800&q=80",
    TopicType.SPORTS_LEAGUE to "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?auto=format&fit=crop&w=800&q=80",
    TopicType.SPORTS_TEAM to "https://images.unsplash.com/photo-1517649763962-0c623066013b?auto=format&fit=crop&w=800&q=80",
    TopicType.PLACE to "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=800&q=80",
    TopicType.EVENT to "https://images.unsplash.com/photo-1495020689067-958852a7765e?auto=format&fit=crop&w=800&q=80",
    TopicType.CATEGORY to "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=800&q=80"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicCarouselCard(
    topic: SearchTopicUiModel,
    imageResolver: TopicImageResolver,
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
            val imageState by rememberTopicImage(topic, imageResolver)
            if (imageState.isLoading) {
                ShimmerPlaceholder(Modifier.fillMaxSize())
            } else {
                SubcomposeAsyncImage(
                    model = imageState.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> ShimmerPlaceholder(Modifier.fillMaxSize())
                        is AsyncImagePainter.State.Error -> ErrorPlaceholder(Modifier.fillMaxSize())
                        else -> SubcomposeAsyncImageContent()
                    }
                }
            }
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

@Composable
private fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val shimmerOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        )
    )
    val shimmerColors = listOf(
        Color(0xFF1F1F1F),
        Color(0xFF3A3A3A),
        Color(0xFF1F1F1F)
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(shimmerOffset - 200f, 0f),
        end = Offset(shimmerOffset, 600f)
    )
    Box(modifier.background(brush))
}

@Composable
private fun ErrorPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier.background(
            Brush.linearGradient(
                colors = listOf(Color(0xFF2D2D2D), Color(0xFF4A4A4A))
            )
        )
    )
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
