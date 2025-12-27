package com.digitalturbine.promptnews.ui.sports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.sports.SportsHeaderModel
import com.digitalturbine.promptnews.data.sports.SportsRepository
import com.digitalturbine.promptnews.data.sports.SportsResults
import com.digitalturbine.promptnews.data.sports.SportsMatchModel
import com.digitalturbine.promptnews.network.Services
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SportsUiState {
    data object Idle : SportsUiState()
    data class Loading(val query: String) : SportsUiState()
    data class Loaded(val query: String, val results: SportsResults) : SportsUiState()
    data class Partial(
        val query: String,
        val message: String,
        val newsQuery: String,
        val news: List<Article>
    ) : SportsUiState()
    data class Fallback(
        val query: String,
        val context: SportsFallbackContext
    ) : SportsUiState()
}

data class SportsFallbackContext(
    val title: String,
    val subtitle: String,
    val cachedMatches: List<SportsMatchModel>,
    val headlines: List<String>,
    val shortcuts: List<String>
)

class SportsViewModel(
    private val repository: SportsRepository = SportsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SportsUiState>(SportsUiState.Idle)
    val uiState: StateFlow<SportsUiState> = _uiState.asStateFlow()
    private var lastResults: SportsResults? = null

    fun search(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        _uiState.value = SportsUiState.Loading(normalized)
        viewModelScope.launch {
            val results = runCatching { repository.fetchSportsResults(normalized) }.getOrNull()
            val hydratedResults = results?.withFallbackTitle(normalized)
            if (hydratedResults != null && hydratedResults.matches.isNotEmpty()) {
                lastResults = hydratedResults
                _uiState.value = SportsUiState.Loaded(normalized, hydratedResults)
                return@launch
            }

            val newsQuery = buildSportsNewsQuery(normalized)
            val news = runCatching {
                Services.fetchSerpNews(newsQuery, page = 0, pageSize = 10)
            }.getOrDefault(emptyList())

            _uiState.value = if (news.isNotEmpty()) {
                SportsUiState.Partial(
                    query = normalized,
                    message = "Live scores unavailable — showing latest sports news",
                    newsQuery = newsQuery,
                    news = news
                )
            } else {
                SportsUiState.Fallback(
                    query = normalized,
                    context = buildFallbackContext(normalized, lastResults)
                )
            }
        }
    }

    fun reset() {
        _uiState.value = SportsUiState.Idle
    }
}

private fun SportsResults.withFallbackTitle(query: String): SportsResults {
    val current = header
    val updated = (current ?: return copy(header = SportsHeaderModel(query, null, null, emptyList()))).copy(
        title = current.title ?: query,
        tabs = if (current.tabs.isEmpty()) listOf("Matches", "News", "Standings") else current.tabs
    )
    return copy(header = updated)
}

private fun buildSportsNewsQuery(query: String): String {
    val normalized = query.lowercase()
    return when {
        "nfl" in normalized -> "NFL news"
        "nba" in normalized -> "NBA news"
        "mlb" in normalized -> "MLB news"
        "nhl" in normalized -> "NHL news"
        "wnba" in normalized -> "WNBA news"
        "mls" in normalized -> "MLS news"
        "premier league" in normalized -> "Premier League news"
        "college football" in normalized || "ncaa" in normalized -> "College football news"
        "soccer" in normalized -> "Soccer news"
        else -> "Sports news"
    }
}

private fun buildFallbackContext(query: String, lastResults: SportsResults?): SportsFallbackContext {
    val normalized = query.lowercase()
    val headlines = when {
        "nfl" in normalized -> listOf("NFL week previews", "Top NFL highlights", "Injury updates to watch")
        "nba" in normalized -> listOf("NBA standings watch", "Top plays from last night", "Trade chatter roundup")
        "mlb" in normalized -> listOf("MLB series previews", "Pitching matchups", "Power rankings")
        "nhl" in normalized -> listOf("NHL playoff race", "Goalie spotlight", "League headlines")
        "soccer" in normalized -> listOf("Global soccer headlines", "Weekend fixtures", "Transfer news")
        else -> listOf("Top sports headlines", "Upcoming marquee games", "League updates")
    }
    val shortcuts = when {
        "nfl" in normalized -> listOf("Chiefs", "Cowboys", "Eagles", "49ers")
        "nba" in normalized -> listOf("Lakers", "Celtics", "Warriors", "Bucks")
        "mlb" in normalized -> listOf("Yankees", "Dodgers", "Braves", "Astros")
        "nhl" in normalized -> listOf("Rangers", "Maple Leafs", "Oilers", "Golden Knights")
        else -> listOf("NFL", "NBA", "MLB", "NHL")
    }
    return SportsFallbackContext(
        title = "Today in Sports",
        subtitle = "Quick ways to catch up while scores load.",
        cachedMatches = lastResults?.matches?.take(3).orEmpty(),
        headlines = headlines,
        shortcuts = shortcuts
    )
}
