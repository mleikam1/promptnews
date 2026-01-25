package com.digitalturbine.promptnews.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.digitalturbine.promptnews.data.sports.NflGame
import com.digitalturbine.promptnews.data.sports.NflGameStatus
import com.digitalturbine.promptnews.data.sports.NflGamesRepository
import com.digitalturbine.promptnews.util.isNflIntent
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NflScoreboard(
    query: String,
    modifier: Modifier = Modifier,
    repository: NflGamesRepository = remember { NflGamesRepository() }
) {
    if (!isNflIntent(query)) return

    val gamesState = remember { mutableStateOf<List<NflGame>?>(null) }
    val isLoading = remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        isLoading.value = true
        gamesState.value = repository.fetchRelevantNflGames()
        isLoading.value = false
    }

    val games = gamesState.value
    if (isLoading.value) {
        NflScoreboardSkeleton(modifier = modifier)
        return
    }
    if (games.isNullOrEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "NFL Scores",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(games, key = { it.id }) { game ->
                NflGameCard(game = game)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun NflGameCard(game: NflGame) {
    val (badgeText, badgeColor) = when (game.status) {
        NflGameStatus.LIVE -> "LIVE" to Color(0xFFD32F2F)
        NflGameStatus.FINAL -> "FINAL" to Color(0xFF37474F)
        NflGameStatus.SCHEDULED -> "UPCOMING" to Color(0xFF546E7A)
    }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", Locale.getDefault())
    }
    val dateText = dateFormatter.format(game.date.atZone(ZoneId.systemDefault()))

    Card(
        modifier = Modifier
            .width(240.dp)
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                val statusLine = buildStatusLine(game, dateText)
                Text(
                    text = statusLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            TeamRow(teamName = game.awayTeam.name, logoUrl = game.awayTeam.logoUrl)
            Spacer(Modifier.height(6.dp))
            TeamRow(teamName = game.homeTeam.name, logoUrl = game.homeTeam.logoUrl)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${game.awayTeam.score ?: "-"} - ${game.homeTeam.score ?: "-"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = game.venue ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TeamRow(teamName: String, logoUrl: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(logoUrl)
                .crossfade(true)
                .build(),
            contentDescription = teamName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = teamName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NflScoreboardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(2) {
                Box(
                    modifier = Modifier
                        .width(240.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

private fun buildStatusLine(game: NflGame, dateText: String): String {
    if (game.status == NflGameStatus.LIVE) {
        val details = listOfNotNull(game.statusLong, game.statusClock).joinToString(" • ")
        if (details.isNotBlank()) return details
    }
    return dateText
}
