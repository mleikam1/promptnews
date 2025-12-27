package com.digitalturbine.promptnews.ui.sports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalturbine.promptnews.data.sports.SportsHeaderModel
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

    fun search(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        _uiState.value = SportsUiState.Loading(normalized)
        viewModelScope.launch {
            val results = repository.fetchSportsResults(normalized)
            _uiState.value = when {
                results == null -> SportsUiState.Error("Unable to load sports results.")
                results.isEmpty() -> SportsUiState.NoResults(normalized)
                else -> SportsUiState.Ready(normalized, results.withFallbackTitle(normalized))
            }
        }
    }

    fun reset() {
        _uiState.value = SportsUiState.Idle
    }
}

private fun SportsResults.isEmpty(): Boolean {
    val hasHeader = header?.let { it.title != null || it.subtitle != null || it.thumbnail != null } ?: false
    return !hasHeader && matches.isEmpty()
}

private fun SportsResults.withFallbackTitle(query: String): SportsResults {
    val current = header
    val updated = (current ?: return copy(header = SportsHeaderModel(query, null, null, emptyList()))).copy(
        title = current.title ?: query,
        tabs = if (current.tabs.isEmpty()) listOf("Matches", "News", "Standings") else current.tabs
    )
    return copy(header = updated)
}
