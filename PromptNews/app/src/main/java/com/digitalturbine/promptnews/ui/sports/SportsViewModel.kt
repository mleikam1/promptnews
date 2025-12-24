package com.digitalturbine.promptnews.ui.sports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalturbine.promptnews.data.sports.SportsRepository
import com.digitalturbine.promptnews.data.sports.SportsResults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SportsUiState {
    data object Idle : SportsUiState()
    data class Loading(val query: String) : SportsUiState()
    data class Ready(val query: String, val results: SportsResults) : SportsUiState()
    data class NoResults(val query: String) : SportsUiState()
    data class Error(val message: String) : SportsUiState()
}

class SportsViewModel(
    private val repository: SportsRepository = SportsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SportsUiState>(SportsUiState.Idle)
    val uiState: StateFlow<SportsUiState> = _uiState.asStateFlow()

    fun search(query: String, filter: SportFilter) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return
        val normalizedFilter = filter.name.lowercase()
        val searchQuery = if (normalizedFilter == "all") {
            normalized
        } else {
            "$normalized $normalizedFilter"
        }
        _uiState.value = SportsUiState.Loading(searchQuery)
        viewModelScope.launch {
            val results = repository.fetchSportsResults(searchQuery)
            _uiState.value = when {
                results == null -> SportsUiState.Error("Unable to load sports results.")
                results.isEmpty() -> SportsUiState.NoResults(searchQuery)
                else -> SportsUiState.Ready(searchQuery, results)
            }
        }
    }

    fun reset() {
        _uiState.value = SportsUiState.Idle
    }
}

private fun SportsResults.isEmpty(): Boolean {
    val overview = teamOverview
    val hasOverview = overview?.title != null || overview?.ranking != null || overview?.thumbnail != null
    return !hasOverview &&
        liveGame == null &&
        recentGames.isEmpty() &&
        upcomingGames.isEmpty()
}
