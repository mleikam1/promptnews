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
    data class Error(val message: String) : SportsUiState()
}

class SportsViewModel(
    private val repository: SportsRepository = SportsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SportsUiState>(SportsUiState.Idle)
    val uiState: StateFlow<SportsUiState> = _uiState.asStateFlow()

    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _uiState.value = SportsUiState.Loading(trimmed)
        viewModelScope.launch {
            val results = repository.fetchSportsResults(trimmed)
            if (results == null) {
                _uiState.value = SportsUiState.Error("No results found.")
            } else {
                _uiState.value = SportsUiState.Ready(trimmed, results)
            }
        }
    }

    fun reset() {
        _uiState.value = SportsUiState.Idle
    }
}
