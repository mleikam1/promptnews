package com.digitalturbine.promptnews.ui.search

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    initialQuery: String? = null,
    initialSource: String? = null,
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
        runSearch(q, HistoryType.SEARCH, true)
    }
    fun runChipSearch(q: String) {
        runSearch(q, HistoryType.CHIP, true)
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

    val chips = listOf(
        ChipRowData(
            title = "Trending & Live",
            chips = listOf(
                "Taylor Swift",
                "NCAA Football Playoffs",
                "NFL Playoffs",
                "Gaza Conflict",
                "Student Loan Updates",
                "AI Layoffs",
                "Supreme Court Ruling"
            )
        ),
        ChipRowData(
            title = "News & Current Affairs",
            chips = listOf(
                "Top Stories",
                "Politics",
                "World News",
                "Business",
                "Technology",
                "Climate",
                "Education",
                "Health"
            )
        ),
        ChipRowData(
            title = "Sports",
            chips = listOf(
                "NFL",
                "NBA",
                "NCAA Football",
                "MLB",
                "Soccer",
                "UFC",
                "Formula 1",
                "Olympics"
            )
        ),
        ChipRowData(
            title = "Explore More",
            chips = listOf(
                "Space",
                "Travel",
                "Inflation",
                "Mortgage Rates",
                "Fitness",
                "Food",
                "Relationships",
                "Fashion",
                "Robotics"
            )
        )
    )

    val searchBarBottomPadding = 25.dp
    val searchBarContentPadding = 96.dp

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (ui is SearchUi.Idle) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = searchBarContentPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    TopBar()
                    Spacer(Modifier.height(16.dp))
                    chips.forEachIndexed { index, row ->
                        ChipRow(
                            title = row.title,
                            chips = row.chips,
                            onChipSelected = { runChipSearch(it) }
                        )
                        if (index != chips.lastIndex) {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = searchBarContentPadding)
                ) {
                    item {
                        TopBar()
                        Spacer(Modifier.height(12.dp))
                    }
                    item {
                        // “Finding …”
                        AnimatedVisibility(
                            visible = ui is SearchUi.Searching,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                "Finding News and Information for “$lastQuery”",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
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
    }
}

private data class ChipRowData(
    val title: String,
    val chips: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipRow(
    title: String,
    chips: List<String>,
    onChipSelected: (String) -> Unit
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(chips) { chip ->
                AssistChip(
                    onClick = { onChipSelected(chip) },
                    label = { Text(chip) },
                    shape = RoundedCornerShape(20.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "PromptNews",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "Profile"
            )
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
