package com.digitalturbine.promptnews.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.serpapi.SerpApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.digitalturbine.promptnews.data.UserLocation

data class LocalNewsState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasFetched: Boolean = false,
    val hasLoadedMore: Boolean = false,
    val items: List<Article> = emptyList()
)

class HomeViewModel(
    private val serpApiRepository: SerpApiRepository = SerpApiRepository()
) : ViewModel() {
    private val _localNewsState = MutableStateFlow(LocalNewsState())
    val localNewsState: StateFlow<LocalNewsState> = _localNewsState.asStateFlow()

    private var lastLocation: UserLocation? = null

    fun clearLocalNews() {
        lastLocation = null
        _localNewsState.value = LocalNewsState()
    }

    fun fetchLocalNews(city: String, state: String) {
        val location = UserLocation(city = city, state = state)
        if (location == lastLocation && _localNewsState.value.hasFetched) return
        lastLocation = location
        loadLocalNews(location)
    }

    fun loadMoreLocalNews(location: UserLocation) {
        val currentState = _localNewsState.value
        if (currentState.isLoadingMore || currentState.hasLoadedMore) return
        viewModelScope.launch {
            _localNewsState.value = currentState.copy(isLoadingMore = true)
            val query = "${location.city} ${location.state} local news"
            val moreItems = serpApiRepository.fetchLocalNewsByOffset(
                location = "${location.city}, ${location.state}",
                query = query,
                limit = MORE_LIMIT,
                offset = currentState.items.size
            )
            _localNewsState.value = currentState.copy(
                isLoadingMore = false,
                hasLoadedMore = true,
                items = currentState.items + moreItems
            )
        }
    }

    private fun loadLocalNews(location: UserLocation) {
        viewModelScope.launch {
            _localNewsState.value = LocalNewsState(isLoading = true)
            val query = "${location.city} ${location.state} local news"
            val items = serpApiRepository.fetchLocalNewsByOffset(
                location = "${location.city}, ${location.state}",
                query = query,
                limit = INITIAL_LIMIT,
                offset = 0
            )
            _localNewsState.value = LocalNewsState(
                isLoading = false,
                hasFetched = true,
                items = items
            )
        }
    }

    companion object {
        private const val INITIAL_LIMIT = 4
        private const val MORE_LIMIT = 7
    }
}
