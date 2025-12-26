package com.digitalturbine.promptnews.ui.sports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.digitalturbine.promptnews.data.sports.SportsGame
import com.digitalturbine.promptnews.data.sports.SportsResults
import com.digitalturbine.promptnews.data.sports.SportsTeam

data class SportFilter(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color
)

private val sportFilters = listOf(
    SportFilter("All", Icons.Filled.Star, Color(0xFF2563EB)),
    SportFilter("Basketball", Icons.Filled.SportsBasketball, Color(0xFFF97316)),
    SportFilter("Football", Icons.Filled.SportsFootball, Color(0xFFEF4444)),
    SportFilter("Soccer", Icons.Filled.SportsSoccer, Color(0xFF16A34A)),
    SportFilter("Baseball", Icons.Filled.SportsBaseball, Color(0xFF0EA5E9)),
    SportFilter("Hockey", Icons.Filled.SportsHockey, Color(0xFF6366F1))
)

private val teamSuggestions = listOf(
    "Los Angeles Lakers",
    "Golden State Warriors",
    "Boston Celtics",
    "Dallas Cowboys",
    "Kansas City Chiefs",
    "New York Yankees",
    "Los Angeles Dodgers",
    "Manchester City",
    "FC Barcelona",
    "Real Madrid",
    "Paris Saint-Germain",
    "Toronto Maple Leafs",
    "Edmonton Oilers",
    "Inter Miami"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsSearchScreen(
    vm: SportsViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(sportFilters.first()) }

    SportsScreenContent(
        uiState = uiState,
        query = query,
        selectedFilter = selectedFilter,
        onQueryChange = { query = it },
        onFilterSelected = { selectedFilter = it },
        onSearch = { vm.search(it, selectedFilter) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SportsScreenContent(
    uiState: SportsUiState,
    query: String,
    selectedFilter: SportFilter,
    onQueryChange: (String) -> Unit,
    onFilterSelected: (SportFilter) -> Unit,
    onSearch: (String) -> Unit
) {
    var selectedGame by remember { mutableStateOf<SportsGame?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val suggestions = remember(query) {
        if (query.isBlank()) {
            emptyList()
        } else {
            teamSuggestions.filter { it.contains(query, ignoreCase = true) }.take(6)
        }
    }

    if (selectedGame != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedGame = null },
            sheetState = sheetState
        ) {
            GameDetailModal(game = selectedGame!!)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Sports Scores",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(12.dp))
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search for a team or sport") },
                    leadingIcon = {
                        IconButton(onClick = { onSearch(query) }) {
                            Icon(Icons.Filled.SportsSoccer, contentDescription = null)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearch(query) }
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = selectedFilter.accent
                )
            )
            AnimatedVisibility(
                visible = suggestions.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        Text(
                            text = suggestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onQueryChange(suggestion)
                                    onSearch(suggestion)
                                }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sportFilters) { filter ->
                    AssistChip(
                        onClick = { onFilterSelected(filter) },
                        label = { Text(filter.name) },
                        leadingIcon = {
                            Icon(filter.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (filter == selectedFilter) {
                                filter.accent.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            leadingIconContentColor = filter.accent
                        )
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        when (val state = uiState) {
            is SportsUiState.Idle -> {
                item {
                    EmptySportsState()
                }
            }
            is SportsUiState.Loading -> {
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
                val accent = selectedFilter.accent
                item {
                    TeamOverviewCard(results = state.results, accent = accent)
                }
                item {
                    Spacer(Modifier.height(16.dp))
                }
                state.results.liveGame?.let { game ->
                    item {
                        LiveMatchHero(game = game, accent = accent)
                        Spacer(Modifier.height(16.dp))
                    }
                }
                item {
                    GamesCarousel(
                        title = "Recent Games",
                        games = state.results.recentGames,
                        accent = accent,
                        onGameSelected = { selectedGame = it }
                    )
                }
                item {
                    Spacer(Modifier.height(12.dp))
                }
                item {
                    GamesCarousel(
                        title = "Upcoming Games",
                        games = state.results.upcomingGames,
                        accent = accent,
                        onGameSelected = { selectedGame = it }
                    )
                }
                item {
                    Spacer(Modifier.height(20.dp))
                }
                item {
                    StatsSection(results = state.results, accent = accent)
                }
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
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Search for a team to get live match updates.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        Text(text = "No results found.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Try another team or adjust the filter.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TeamOverviewCard(results: SportsResults, accent: Color) {
    val overview = results.teamOverview
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                AsyncImage(
                    model = overview?.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = overview?.title ?: "Team overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    overview?.ranking?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Ranking: $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = { }) {
                    Text("Follow")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { }) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(accent)
        )
    }
}

@Composable
private fun LiveMatchHero(game: SportsGame, accent: Color) {
    val transition = rememberInfiniteTransition(label = "livePulse")
    val pulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "livePulseAlpha"
    )
    val gradient = Brush.horizontalGradient(
        listOf(accent.copy(alpha = pulse), MaterialTheme.colorScheme.surface)
    )

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(16.dp)
        ) {
            Text(
                text = game.status ?: "Live",
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TeamScoreBlock(
                    team = game.teams.firstOrNull(),
                    accent = accent,
                    modifier = Modifier.fillMaxWidth(0.48f)
                )
                TeamScoreBlock(
                    team = game.teams.getOrNull(1),
                    accent = accent,
                    alignEnd = true,
                    modifier = Modifier.fillMaxWidth(0.48f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = game.league ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Play highlights")
            }
        }
    }
}

@Composable
private fun TeamScoreBlock(
    team: SportsTeam?,
    accent: Color,
    alignEnd: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!alignEnd) {
                Image(
                    painter = rememberAsyncImagePainter(team?.thumbnail),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = team?.name ?: "TBD",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (alignEnd) {
                Spacer(Modifier.width(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(team?.thumbnail),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        AnimatedContent(
            targetState = team?.score ?: "-",
            label = "score"
        ) { score ->
            Text(
                text = score,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
        }
    }
}

@Composable
private fun GamesCarousel(
    title: String,
    games: List<SportsGame>,
    accent: Color,
    onGameSelected: (SportsGame) -> Unit
) {
    if (games.isEmpty()) {
        return
    }
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(games) { game ->
            GameCard(game = game, accent = accent, onClick = { onGameSelected(game) })
        }
    }
}

@Composable
private fun GameCard(game: SportsGame, accent: Color, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(240.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = accent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = game.league ?: "League",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    text = listOfNotNull(game.date, game.time).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            GameTeamsRow(game.teams)
            Spacer(Modifier.height(8.dp))
            Text(
                text = game.score ?: game.status.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            game.videoHighlights?.link?.let {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = accent)
                    Spacer(Modifier.width(6.dp))
                    Text("Highlights", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun GameTeamsRow(teams: List<SportsTeam>) {
    teams.take(2).forEachIndexed { index, team ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(team.thumbnail),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = team.name ?: "TBD",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(text = team.score ?: "-", style = MaterialTheme.typography.bodyMedium)
        }
        if (index == 0) {
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun GameDetailModal(game: SportsGame) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = game.league ?: "Game details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = listOfNotNull(game.date, game.time, game.status).joinToString(" • "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text("Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Timeline data will appear here when available.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text("Video highlights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        game.videoHighlights?.link?.let { link ->
            Text(text = link, color = MaterialTheme.colorScheme.primary)
        } ?: Text(text = "No highlights available yet.")
        Spacer(Modifier.height(16.dp))
        Text("Stadium info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Venue details will show once provided by the league feed.")
        Spacer(Modifier.height(16.dp))
        Text("Stats breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("No detailed stats returned for this matchup yet.")
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun StatsSection(results: SportsResults, accent: Color) {
    val teams = results.recentGames.firstOrNull()?.teams ?: emptyList()
    val primaryTeam = teams.firstOrNull()?.name
    val winLoss = calculateWinLoss(primaryTeam, results.recentGames)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text("Standings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = primaryTeam?.let { "$it standings" } ?: "Standings",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = results.teamOverview?.ranking?.let { "Current rank: $it" }
                        ?: "Ranking unavailable",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Form & streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Recent form", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        winLoss.form.forEach { symbol ->
                            Surface(
                                shape = CircleShape,
                                color = if (symbol == "W") accent.copy(alpha = 0.2f) else Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = symbol,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = if (symbol == "W") accent else Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Record", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("${winLoss.wins}-${winLoss.losses}", fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Head-to-head", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                results.recentGames.take(3).forEachIndexed { index, game ->
                    Text(
                        text = listOfNotNull(game.date, game.score).joinToString(" • ")
                            .ifBlank { "Matchup details unavailable" },
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (index != 2) {
                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(6.dp))
                    }
                }
                if (results.recentGames.isEmpty()) {
                    Text("No recent head-to-head data yet.")
                }
            }
        }
    }
}

private data class WinLossSummary(
    val wins: Int,
    val losses: Int,
    val form: List<String>
)

private fun calculateWinLoss(teamName: String?, games: List<SportsGame>): WinLossSummary {
    if (teamName == null) return WinLossSummary(0, 0, listOf("-", "-", "-"))
    var wins = 0
    var losses = 0
    val form = mutableListOf<String>()

    games.take(5).forEach { game ->
        val team = game.teams.firstOrNull { it.name == teamName }
        val opponent = game.teams.firstOrNull { it.name != teamName }
        val teamScore = team?.score?.toIntOrNull()
        val oppScore = opponent?.score?.toIntOrNull()
        if (teamScore != null && oppScore != null) {
            if (teamScore >= oppScore) {
                wins += 1
                form.add("W")
            } else {
                losses += 1
                form.add("L")
            }
        }
    }
    if (form.isEmpty()) {
        form.addAll(listOf("-", "-", "-"))
    }
    return WinLossSummary(wins, losses, form)
}

@Composable
private fun SportsSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(3) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(16.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
