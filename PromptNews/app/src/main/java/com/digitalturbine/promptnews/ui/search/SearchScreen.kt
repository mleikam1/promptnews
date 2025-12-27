package com.digitalturbine.promptnews.ui.search

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.digitalturbine.promptnews.data.net.Http
import com.digitalturbine.promptnews.ui.PromptNewsTopBar
import com.digitalturbine.promptnews.util.Config
import com.digitalturbine.promptnews.web.ArticleWebViewActivity
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

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

private const val TAG = "SearchScreen"

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

    BackHandler(enabled = screenState == SearchScreenState.Results) {
        onBack()
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
                    entityQuery = "NFL football logo",
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
    val topicImageResolver = remember { TopicImageResolver() }

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
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        flingBehavior = rememberSnapFlingBehavior(rowState)
                    ) {
                        items(section.topics, key = { it.id }) { topic ->
                            val imageState by rememberTopicImage(topic, topicImageResolver)
                            TopicThumbnailItem(
                                title = topic.title,
                                imageUrl = imageState.url,
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

private class TopicImageResolver {
    private val cache = mutableMapOf<String, String>()
    private val cacheMutex = Mutex()

    suspend fun resolve(topic: SearchTopicUiModel): String {
        if (!topic.imageUrl.isNullOrBlank()) {
            return topic.imageUrl
        }
        val query = serpQueryFor(topic)
        val cacheKey = "${topic.topicType}:${query ?: topic.title}"
        cacheMutex.withLock {
            cache[cacheKey]?.let { return it }
        }

        val fallbackUrl = fallbackFor(topic)
        val resolved = if (query != null) {
            fetchSerpApiImageUrl(query, fallbackUrl)
        } else {
            fallbackUrl
        }
        cacheMutex.withLock {
            cache[cacheKey] = resolved
        }
        return resolved
    }

    private fun serpQueryFor(topic: SearchTopicUiModel): String? {
        val base = topic.entityQuery ?: topic.title
        return when (topic.topicType) {
            TopicType.PERSON -> "$base portrait"
            TopicType.SPORTS_LEAGUE -> if (base.contains("football", ignoreCase = true)) base else "$base football logo"
            TopicType.SPORTS_TEAM -> if (base.contains("logo", ignoreCase = true)) base else "$base logo"
            TopicType.PLACE -> "$base photo"
            TopicType.EVENT -> "$base photo"
            TopicType.CATEGORY -> null
        }
    }

    private fun fallbackFor(topic: SearchTopicUiModel): String {
        if (!topic.imageUrl.isNullOrBlank()) return topic.imageUrl
        val byEntity = topic.entityQuery?.let { entityFallbackImages[it] }
        if (!byEntity.isNullOrBlank()) return byEntity
        return when (topic.topicType) {
            TopicType.CATEGORY -> categoryImageUrls[topic.title]
            else -> topicTypeFallbackImages[topic.topicType]
        } ?: genericPlaceholderImage
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

private suspend fun fetchSerpApiImageUrl(
    query: String,
    fallbackUrl: String
): String = withContext(Dispatchers.IO) {
    if (Config.serpApiKey.isBlank()) {
        Log.w(TAG, "SerpAPI key missing. Falling back for query: $query")
        return@withContext fallbackUrl
    }
    val reqUrl = Uri.parse("https://serpapi.com/google-images-api").buildUpon()
        .appendQueryParameter("api_key", Config.serpApiKey)
        .appendQueryParameter("engine", "google_images")
        .appendQueryParameter("q", query)
        .appendQueryParameter("num", "1")
        .build()
        .toString()
    Log.d(TAG, "SerpAPI request URL: $reqUrl")
    return@withContext try {
        Http.client.newCall(Http.req(reqUrl)).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "SerpAPI request failed (${response.code}) for query: $query")
                return@use fallbackUrl
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                Log.w(TAG, "SerpAPI response empty for query: $query")
                return@use fallbackUrl
            }
            val root = JSONObject(body)
            val imageUrl = root.optJSONArray("images_results")
                ?.optJSONObject(0)
                ?.optString("original")
                ?.takeIf { it.isNotBlank() }
            if (imageUrl.isNullOrBlank()) {
                Log.w(TAG, "SerpAPI image URL missing for query: $query")
                fallbackUrl
            } else {
                Log.d(TAG, "SerpAPI image URL resolved: $imageUrl")
                imageUrl
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "SerpAPI request exception for query: $query", e)
        fallbackUrl
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

private const val genericPlaceholderImage =
    "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=800&q=80"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicThumbnailItem(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val placeholderPainter = rememberVectorPainter(Icons.Default.Image)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(min = 88.dp, max = 104.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = CircleShape,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            modifier = Modifier
                .size(82.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
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
            val placeholderPainter = rememberVectorPainter(Icons.Default.Image)
            AsyncImage(
                model = article.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter,
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
                    painter = rememberAsyncImagePainter(
                        model = article.logoUrl,
                        placeholder = placeholderPainter,
                        error = placeholderPainter,
                        fallback = placeholderPainter
                    ),
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
        val placeholderPainter = rememberVectorPainter(Icons.Default.Image)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = a.logoUrl,
                        placeholder = placeholderPainter,
                        error = placeholderPainter,
                        fallback = placeholderPainter
                    ),
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
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter,
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
