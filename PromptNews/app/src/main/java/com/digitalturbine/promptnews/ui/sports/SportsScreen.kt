package com.digitalturbine.promptnews.ui.sports

import android.content.Intent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.digitalturbine.promptnews.data.Story
import com.digitalturbine.promptnews.data.toArticle
import com.digitalturbine.promptnews.data.sports.HighlightModel
import com.digitalturbine.promptnews.data.sports.LeagueContextModel
import com.digitalturbine.promptnews.data.sports.SportsHeaderModel
import com.digitalturbine.promptnews.data.sports.SportsMatchModel
import com.digitalturbine.promptnews.data.sports.TeamModel
import com.digitalturbine.promptnews.data.sports.displayText
import com.digitalturbine.promptnews.ui.components.HeroCard
import com.digitalturbine.promptnews.ui.components.RowCard
import com.digitalturbine.promptnews.web.ArticleWebViewActivity

@Composable
fun SportsScreen(
    query: String?,
    vm: SportsViewModel = viewModel(),
    onMatchSelected: (SportsMatchModel) -> Unit = {},
    onHighlightSelected: (HighlightModel) -> Unit = {}
) {
    val uiState by vm.uiState.collectAsState()
    val feedState by vm.feedState.collectAsState()
    val feedError by vm.feedError.collectAsState()

    LaunchedEffect(query) {
        query?.takeIf { it.isNotBlank() }?.let { vm.search(it) }
    }

    SportsScreenContent(
        uiState = uiState,
        feedState = feedState,
        feedError = feedError,
        onMatchSelected = onMatchSelected,
        onHighlightSelected = onHighlightSelected,
        onShortcutSelected = { vm.search(it) },
        onLoadMoreStories = { vm.loadMoreStories() }
    )
}

@Composable
private fun SportsScreenContent(
    uiState: SportsUiState,
    feedState: SportsFeedState,
    feedError: String?,
    onMatchSelected: (SportsMatchModel) -> Unit,
    onHighlightSelected: (HighlightModel) -> Unit,
    onShortcutSelected: (String) -> Unit,
    onLoadMoreStories: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        when (val state = uiState) {
            is SportsUiState.Idle -> {
                item {
                    EmptySportsState()
                }
            }
            is SportsUiState.Loading -> {
                item {
                    SportsHeader(
                        header = SportsHeaderModel(
                            title = state.query,
                            subtitle = null,
                            thumbnail = null,
                            tabs = listOf("Matches", "News", "Standings")
                        )
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
                item { SportsSkeleton() }
            }
            is SportsUiState.Loaded -> {
                val header = state.results.header
                if (header != null) {
                    item {
                        SportsHeader(header = header)
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
                items(state.results.matches) { match ->
                    MatchCard(
                        match = match,
                        onMatchSelected = onMatchSelected,
                        onHighlightSelected = onHighlightSelected
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            is SportsUiState.Partial -> {
                item {
                    SportsHeader(
                        header = SportsHeaderModel(
                            title = state.query,
                            subtitle = "Latest sports updates",
                            thumbnail = null,
                            tabs = listOf("News")
                        )
                    )
                }
                item { InlineFallbackMessage(message = state.message) }
                item { Spacer(Modifier.height(8.dp)) }
            }
            is SportsUiState.Fallback -> {
                item {
                    TodayInSportsFallback(
                        context = state.context,
                        onMatchSelected = onMatchSelected,
                        onHighlightSelected = onHighlightSelected,
                        onShortcutSelected = onShortcutSelected
                    )
                }
            }
        }
        if (feedState.stories.isEmpty() && feedState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        if (feedState.stories.isNotEmpty()) {
            item { Spacer(Modifier.height(16.dp)) }
            item {
                Text(
                    text = "Latest Stories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
            feedState.stories.firstOrNull()?.let { story ->
                item {
                    val article = story.toArticle()
                    HeroCard(
                        article = article,
                        onClick = {
                            context.startActivity(
                                Intent(context, ArticleWebViewActivity::class.java)
                                    .putExtra("url", article.url)
                            )
                        }
                    )
                }
                if (feedState.stories.size > 1) {
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
            items(feedState.stories.drop(1), key = { it.url }) { story ->
                SportsStoryRow(
                    story = story,
                    onArticleSelected = { url ->
                        context.startActivity(
                            Intent(context, ArticleWebViewActivity::class.java)
                                .putExtra("url", url)
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                SportsFeedFooter(
                    isLoading = feedState.isLoading,
                    canLoadMore = feedState.canLoadMore,
                    errorMessage = feedError,
                    onLoadMore = onLoadMoreStories
                )
            }
        }
    }
}

@Composable
private fun SportsHeader(header: SportsHeaderModel) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = header.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = header.title.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    header.subtitle?.let { subtitle ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            TabsRow(tabs = header.tabs)
        }
    }
}

@Composable
private fun TabsRow(tabs: List<String>) {
    val items = if (tabs.isEmpty()) listOf("Matches", "News", "Standings") else tabs
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEachIndexed { index, title ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                if (index == 0) {
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(28.dp)
                            .background(MaterialTheme.colorScheme.onPrimary)
                    )
                } else {
                    Spacer(Modifier.height(3.dp))
                }
            }
        }
    }
}

@Composable
private fun MatchCard(
    match: SportsMatchModel,
    onMatchSelected: (SportsMatchModel) -> Unit,
    onHighlightSelected: (HighlightModel) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onMatchSelected(match) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            MatchContextRow(context = match.context)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TeamRow(team = match.homeTeam)
                    TeamRow(team = match.awayTeam)
                }
                Spacer(Modifier.width(12.dp))
                StatusDateColumn(status = match.statusText, date = match.dateText)
                match.highlight?.let { highlight ->
                    Spacer(Modifier.width(12.dp))
                    HighlightThumbnail(highlight = highlight, onClick = { onHighlightSelected(highlight) })
                }
            }
        }
    }
}

@Composable
private fun MatchContextRow(context: LeagueContextModel?) {
    val text = context.displayText() ?: return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun TeamRow(team: TeamModel?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        AsyncImage(
            model = team?.logoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = team?.name ?: "TBD",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (team?.isWinner == true) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = team?.score ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (team?.isWinner == true) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun StatusDateColumn(status: String?, date: String?) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = status ?: "TBD",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        date?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HighlightThumbnail(
    highlight: HighlightModel,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 64.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = highlight.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.1f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        highlight.duration?.let { duration ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
            ) {
                Text(
                    text = duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptySportsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Find the latest scores, highlights, and standings.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Run a sports prompt or search to see live match updates.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InlineFallbackMessage(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun SportsStoryRow(story: Story, onArticleSelected: (String) -> Unit) {
    val article = story.toArticle()
    RowCard(a = article, onClick = { onArticleSelected(article.url) })
}

@Composable
private fun SportsFeedFooter(
    isLoading: Boolean,
    canLoadMore: Boolean,
    errorMessage: String?,
    onLoadMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (canLoadMore) {
            OutlinedButton(
                onClick = onLoadMore,
                enabled = !isLoading,
                shape = RoundedCornerShape(24.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text("More")
            }
        } else {
            Text(
                text = "You're all caught up",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        errorMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TodayInSportsFallback(
    context: SportsFallbackContext,
    onMatchSelected: (SportsMatchModel) -> Unit,
    onHighlightSelected: (HighlightModel) -> Unit,
    onShortcutSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = context.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = context.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (context.cachedMatches.isNotEmpty()) {
            Text(
                text = "Upcoming Games",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            context.cachedMatches.forEach { match ->
                MatchCard(
                    match = match,
                    onMatchSelected = onMatchSelected,
                    onHighlightSelected = onHighlightSelected
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "League Headlines",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            context.headlines.forEach { headline ->
                Text(
                    text = "• $headline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Team shortcuts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(context.shortcuts) { shortcut ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { onShortcutSelected(shortcut) }
                    ) {
                        Text(
                            text = shortcut,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SportsSkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shimmer by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(3) {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = shimmer))
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = shimmer * 0.9f))
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
