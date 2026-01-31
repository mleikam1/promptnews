package com.digitalturbine.promptnews.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.digitalturbine.promptnews.data.SerpNewsItem
import com.digitalturbine.promptnews.data.UserLocation
import com.digitalturbine.promptnews.data.cache.SearchCache
import com.digitalturbine.promptnews.data.serpapi.SerpApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val serpApiRepository: SerpApiRepository = SerpApiRepository()
) : AndroidViewModel(application) {
    private val _localNewsItems = MutableStateFlow<List<SerpNewsItem>>(emptyList())
    val localNewsItems: StateFlow<List<SerpNewsItem>> = _localNewsItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasFetched = MutableStateFlow(false)
    val hasFetched: StateFlow<Boolean> = _hasFetched.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var currentPage: Int = 1

    fun clearLocalNews() {
        currentPage = 1
        _localNewsItems.value = emptyList()
        _isLoading.value = false
        _isLoadingMore.value = false
        _hasFetched.value = false
        _hasMore.value = false
    }

    fun fetchLocalNews(city: String, state: String) {
        val location = UserLocation(city = city, state = state)
        loadLocalNews(location)
    }

    fun loadMoreLocalNews(location: UserLocation) {
        if (_isLoadingMore.value || !_hasMore.value) return
        viewModelScope.launch {
            val nextPage = currentPage + 1
            _isLoadingMore.value = true
            val query = "${location.city} ${location.state} local news"
            val result = runCatching {
                serpApiRepository.fetchLocalNewsPage(
                    location = "${location.city}, ${location.state}",
                    query = query,
                    page = nextPage,
                    pageSize = PAGE_SIZE
                )
            }
            val moreItems = result.getOrElse { emptyList() }
            if (result.isSuccess) {
                currentPage = nextPage
                _localNewsItems.value = _localNewsItems.value + moreItems
                _hasMore.value = moreItems.size == PAGE_SIZE
            }
            _isLoadingMore.value = false
        }
    }

    private fun loadLocalNews(location: UserLocation) {
        viewModelScope.launch {
            currentPage = 1
            _isLoading.value = true
            _hasFetched.value = false
            _hasMore.value = false
            _localNewsItems.value = emptyList()
            val query = "${location.city} ${location.state} local news"
            val cacheResult = SearchCache.get(getApplication(), query, "Home")
            if (cacheResult != null) {
                _localNewsItems.value = cacheResult.results
                _hasFetched.value = true
                _hasMore.value = cacheResult.results.size == PAGE_SIZE
                _isLoading.value = false
                Log.d(TAG, "HomeLocalNews emitted ${cacheResult.results.size} results (cache)")
                return@launch
            }

            Log.d(TAG, "HomeLocalNews fetching fresh results for '$query'")
            val items = runCatching {
                serpApiRepository.fetchLocalNewsPage(
                    location = "${location.city}, ${location.state}",
                    query = query,
                    page = currentPage,
                    pageSize = PAGE_SIZE
                )
            }.getOrElse { emptyList() }
            SearchCache.put(getApplication(), query, items, "Home")
            _localNewsItems.value = items
            _hasFetched.value = true
            _hasMore.value = items.size == PAGE_SIZE
            _isLoading.value = false
            Log.d(TAG, "HomeLocalNews emitted ${items.size} results (network)")
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
        private const val PAGE_SIZE = 7
    }
}
