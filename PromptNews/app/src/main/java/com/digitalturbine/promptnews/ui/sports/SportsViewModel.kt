package com.digitalturbine.promptnews.ui.sports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalturbine.promptnews.data.Story
import com.digitalturbine.promptnews.data.sports.SportsHeaderModel
import com.digitalturbine.promptnews.data.sports.SportsNewsRepository
import com.digitalturbine.promptnews.data.sports.SportsRepository
import com.digitalturbine.promptnews.data.sports.SportsResults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed class SportsUiState {
    data object Idle : SportsUiState()
    data class Loading(val query: String) : SportsUiState()
    data class Loaded(
        val query: String,
        val results: SportsResults,
        val inlineMessage: String? = null
    ) : SportsUiState()
}

data class SportsFeedState(
    val stories: List<Story> = emptyList(),
    val isLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val offset: Int = 0
)

class SportsViewModel(
    private val repository: SportsRepository = SportsRepository(),
    private val newsRepository: SportsNewsRepository = SportsNewsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SportsUiState>(SportsUiState.Idle)
    val uiState: StateFlow<SportsUiState> = _uiState.asStateFlow()
    private val _feedState = MutableStateFlow(SportsFeedState())
    val feedState: StateFlow<SportsFeedState> = _feedState.asStateFlow()
    private val _feedError = MutableStateFlow<String?>(null)
    val feedError: StateFlow<String?> = _feedError.asStateFlow()
    private var lastResults: SportsResults? = null
    private var currentQuery: String = ""
    private var loadMoreJob: Job? = null

    fun search(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        currentQuery = normalized
        _uiState.value = SportsUiState.Loading(normalized)
        _feedError.value = null
        _feedState.value = SportsFeedState(isLoading = true, canLoadMore = true, offset = 0)
        loadMoreJob?.cancel()
        viewModelScope.launch {
            val results = runCatching { repository.fetchSportsResults(normalized) }.getOrNull()
            val hydratedResults = results?.withFallbackTitle(normalized)
            if (hydratedResults != null) {
                lastResults = hydratedResults
            }
            val header = hydratedResults?.header ?: SportsHeaderModel(normalized, null, null, emptyList())
            val matches = hydratedResults?.matches ?: lastResults?.matches.orEmpty()
            val inlineMessage = if (hydratedResults == null) {
                "Live scores unavailable — showing today’s completed and upcoming games"
            } else {
                null
            }
            _uiState.value = SportsUiState.Loaded(
                query = normalized,
                results = SportsResults(header = header, matches = matches),
                inlineMessage = inlineMessage
            )
        }
        viewModelScope.launch {
            loadInitialStories(normalized)
        }
    }

    fun loadMoreStories() {
        val state = _feedState.value
        if (state.isLoading || !state.canLoadMore || currentQuery.isBlank()) return
        _feedState.value = state.copy(isLoading = true)
        _feedError.value = null

        loadMoreJob = viewModelScope.launch {
            runCatching {
                newsRepository.fetchSportsStories(
                    leagueId = currentQuery,
                    limit = LOAD_MORE_LIMIT,
                    offset = state.offset
                )
            }.onSuccess { next ->
                val merged = (state.stories + next).distinctBy { it.url }
                val canLoadMore = next.size == LOAD_MORE_LIMIT
                _feedState.value = state.copy(
                    stories = merged,
                    isLoading = false,
                    canLoadMore = canLoadMore,
                    offset = state.offset + LOAD_MORE_OFFSET_INCREMENT
                )
            }.onFailure {
                _feedState.value = state.copy(isLoading = false)
                _feedError.value = LOAD_MORE_ERROR_MESSAGE
            }
        }
    }

    private suspend fun loadInitialStories(query: String) {
        runCatching {
            newsRepository.fetchSportsStories(
                leagueId = query,
                limit = INITIAL_LIMIT,
                offset = 0
            )
        }.onSuccess { stories ->
            _feedState.value = _feedState.value.copy(
                stories = stories,
                isLoading = false,
                canLoadMore = stories.size == INITIAL_LIMIT,
                offset = LOAD_MORE_OFFSET_INCREMENT
            )
        }.onFailure {
            _feedState.value = _feedState.value.copy(isLoading = false, canLoadMore = false)
        }
    }

    fun reset() {
        _uiState.value = SportsUiState.Idle
        _feedState.value = SportsFeedState()
        _feedError.value = null
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

private const val INITIAL_LIMIT = 7
private const val LOAD_MORE_LIMIT = 10
private const val LOAD_MORE_OFFSET_INCREMENT = 10
private const val LOAD_MORE_ERROR_MESSAGE = "Unable to load more stories. Tap to retry."
