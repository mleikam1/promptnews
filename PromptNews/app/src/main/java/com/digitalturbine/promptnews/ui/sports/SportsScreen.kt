package com.digitalturbine.promptnews.ui.sports

import android.content.Intent
import android.util.Log
import android.view.ViewGroup
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.digitalturbine.promptnews.R
import com.digitalturbine.promptnews.data.Story
import com.digitalturbine.promptnews.data.toArticle
import com.digitalturbine.promptnews.data.sports.HighlightModel
import com.digitalturbine.promptnews.data.sports.LeagueContextModel
import com.digitalturbine.promptnews.data.sports.SportsHeaderModel
import com.digitalturbine.promptnews.data.sports.SportsMatchModel
import com.digitalturbine.promptnews.data.sports.SportsMatchStatusBucket
import com.digitalturbine.promptnews.data.sports.TeamModel
import com.digitalturbine.promptnews.data.sports.displayText
import com.digitalturbine.promptnews.ui.components.HeroCard
import com.digitalturbine.promptnews.ui.components.RowCard
import com.digitalturbine.promptnews.ui.nfl.NflGamesWidgetFragment
import com.digitalturbine.promptnews.util.isSportsIntent
import com.digitalturbine.promptnews.util.isNflIntent
import com.digitalturbine.promptnews.web.ArticleWebViewActivity

@Composable
fun SportsScreen(
    query: String?,
    vm: SportsViewModel = viewModel(factory = SportsViewModelFactory()),
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
    onLoadMoreStories: () -> Unit
) {
    val context = LocalContext.current
    val showWidget = when (uiState) {
        is SportsUiState.Idle -> true
        is SportsUiState.Loading -> isSportsIntent(uiState.query) || isNflIntent(uiState.query)
        is SportsUiState.Loaded -> isSportsIntent(uiState.query) || isNflIntent(uiState.query)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NflGamesWidgetContainer(
            showWidget = showWidget,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            when (val state = uiState) {
                is SportsUiState.Idle -> {
                    item {
                        SportsHeader(
                            header = SportsHeaderModel(
                                title = "Sports",
                                subtitle = "Latest updates · Scores · News",
                                thumbnail = null,
                                tabs = emptyList()
                            )
                        )
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                    item { EmptySportsState() }
                }
                is SportsUiState.Loading -> {
                    item {
                        SportsHeader(
                            header = SportsHeaderModel(
                                title = state.query,
                                subtitle = "Latest updates · Scores · News",
                                thumbnail = null,
                                tabs = emptyList()
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
                            SportsHeader(header = header.copy(subtitle = "Latest updates · Scores · News"))
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                    state.inlineMessage?.let { message ->
                        item { InlineFallbackMessage(message = message) }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                    item {
                        GamesModule(
                            matches = state.results.matches,
                            onMatchSelected = onMatchSelected,
                            onHighlightSelected = onHighlightSelected
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
                        modifier = Modifier.fillMaxWidth()
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
}

@Composable
private fun NflGamesWidgetContainer(
    showWidget: Boolean,
    modifier: Modifier = Modifier
) {
    if (!showWidget) return
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity ?: return
    val fragmentManager = fragmentActivity.supportFragmentManager
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = R.id.nfl_games_widget_container
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                if (fragmentManager.findFragmentById(R.id.nfl_games_widget_container) == null) {
                    fragmentManager
                        .beginTransaction()
                        .replace(
                            R.id.nfl_games_widget_container,
                            NflGamesWidgetFragment.newInstance()
                        )
                        .commit()
                    Log.d("NflGamesWidget", "[NFL WIDGET] Injected into SportsScreen")
                }
            }
        }
    )
}

@Composable
private fun SportsHeader(header: SportsHeaderModel) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = header.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                header.subtitle?.let { subtitle ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                StatusDateColumn(
                    status = match.statusLabel(),
                    date = match.dateText
                )
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

private fun SportsMatchModel.statusLabel(): String {
    return when (statusBucket) {
        SportsMatchStatusBucket.LIVE -> statusText?.ifBlank { "LIVE" } ?: "LIVE"
        SportsMatchStatusBucket.COMPLETED -> statusText?.ifBlank { "FINAL" } ?: "FINAL"
        SportsMatchStatusBucket.UPCOMING -> statusText?.ifBlank { "UPCOMING" } ?: "UPCOMING"
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
            .padding(vertical = 32.dp),
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
            .padding(vertical = 12.dp),
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
private fun GamesModule(
    matches: List<SportsMatchModel>,
    onMatchSelected: (SportsMatchModel) -> Unit,
    onHighlightSelected: (HighlightModel) -> Unit
) {
    val liveGames = matches.filter { it.statusBucket == SportsMatchStatusBucket.LIVE }
    val completedGames = matches.filter { it.statusBucket == SportsMatchStatusBucket.COMPLETED }
    val upcomingGames = matches.filter { it.statusBucket == SportsMatchStatusBucket.UPCOMING }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Games",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        if (liveGames.isEmpty() && completedGames.isEmpty() && upcomingGames.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No games scheduled today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
            return
        }
        GamesSection(
            title = "Live Games",
            matches = liveGames,
            onMatchSelected = onMatchSelected,
            onHighlightSelected = onHighlightSelected
        )
        GamesSection(
            title = "Completed Games",
            matches = completedGames,
            onMatchSelected = onMatchSelected,
            onHighlightSelected = onHighlightSelected
        )
        GamesSection(
            title = "Upcoming Games",
            matches = upcomingGames,
            onMatchSelected = onMatchSelected,
            onHighlightSelected = onHighlightSelected
        )
    }
}

@Composable
private fun GamesSection(
    title: String,
    matches: List<SportsMatchModel>,
    onMatchSelected: (SportsMatchModel) -> Unit,
    onHighlightSelected: (HighlightModel) -> Unit
) {
    if (matches.isEmpty()) return
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    matches.forEach { match ->
        MatchCard(
            match = match,
            onMatchSelected = onMatchSelected,
            onHighlightSelected = onHighlightSelected
        )
        Spacer(Modifier.height(12.dp))
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
            .padding(vertical = 12.dp),
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
