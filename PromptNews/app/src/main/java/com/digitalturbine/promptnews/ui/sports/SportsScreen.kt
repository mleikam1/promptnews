package com.digitalturbine.promptnews.ui.sports

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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.digitalturbine.promptnews.data.sports.HighlightModel
import com.digitalturbine.promptnews.data.sports.LeagueContextModel
import com.digitalturbine.promptnews.data.sports.SportsHeaderModel
import com.digitalturbine.promptnews.data.sports.SportsMatchModel
import com.digitalturbine.promptnews.data.sports.TeamModel
import com.digitalturbine.promptnews.data.sports.displayText

@Composable
fun SportsScreen(
    query: String?,
    vm: SportsViewModel = viewModel(),
    onMatchSelected: (SportsMatchModel) -> Unit = {},
    onHighlightSelected: (HighlightModel) -> Unit = {}
) {
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(query) {
        query?.takeIf { it.isNotBlank() }?.let { vm.search(it) }
    }

    SportsScreenContent(
        uiState = uiState,
        onMatchSelected = onMatchSelected,
        onHighlightSelected = onHighlightSelected
    )
}

@Composable
private fun SportsScreenContent(
    uiState: SportsUiState,
    onMatchSelected: (SportsMatchModel) -> Unit,
    onHighlightSelected: (HighlightModel) -> Unit
) {
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
            is SportsUiState.Error -> {
                item {
                    ErrorState(message = state.message)
                }
            }
            is SportsUiState.NoResults -> {
                item {
                    NoResultsState()
                }
            }
            is SportsUiState.Ready -> {
                val header = state.results.header
                if (header != null) {
                    item {
                        SportsHeader(header = header)
                    }
                }
                if (state.results.matches.isEmpty()) {
                    item { NoResultsState() }
                } else {
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
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Unable to load sports results.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NoResultsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "No games found for this query", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Try another team or league.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
