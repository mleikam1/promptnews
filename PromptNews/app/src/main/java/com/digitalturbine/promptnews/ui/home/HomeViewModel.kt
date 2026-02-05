package com.digitalturbine.promptnews.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.UserLocation
import com.digitalturbine.promptnews.data.localnews.LocalNewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class LocalNewsState {
    data class Data(val articles: List<Article>, val isStale: Boolean) : LocalNewsState()
    data object Loading : LocalNewsState()
    data object EmptyHardFail : LocalNewsState()
}

class HomeViewModel(
    application: Application,
    private val localNewsRepository: LocalNewsRepository = LocalNewsRepository(application)
) : AndroidViewModel(application) {

    private val _localNewsState = MutableStateFlow<LocalNewsState>(LocalNewsState.Loading)
    val localNewsState: StateFlow<LocalNewsState> = _localNewsState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LocalNewsState.Loading
    )

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    private var currentPage: Int = 1
    private var currentLocation: UserLocation? = null

    fun clearLocalNews() {
        currentPage = 1
        currentLocation = null
        _localNewsState.value = LocalNewsState.EmptyHardFail
        _isLoadingMore.value = false
        _hasMore.value = false
    }

    fun onHomeEntered(location: UserLocation) {
        currentLocation = location
        viewModelScope.launch {
            emitCached(location)
            refreshInBackground(location)
        }
    }

    fun loadMoreLocalNews() {
        val location = currentLocation ?: return
        if (_isLoadingMore.value || !_hasMore.value) return
        viewModelScope.launch {
            val currentArticles = (_localNewsState.value as? LocalNewsState.Data)?.articles.orEmpty()
            val nextPage = currentPage + 1
            _isLoadingMore.value = true
            val moreItems = runCatching {
                localNewsRepository.fetchMore(location, nextPage)
            }.getOrElse { emptyList() }
            if (moreItems.isNotEmpty()) {
                currentPage = nextPage
                _localNewsState.value = LocalNewsState.Data(
                    articles = currentArticles + moreItems,
                    isStale = (_localNewsState.value as? LocalNewsState.Data)?.isStale ?: false
                )
            }
            _hasMore.value = moreItems.size == PAGE_SIZE
            _isLoadingMore.value = false
        }
    }

    private suspend fun emitCached(location: UserLocation) {
        val cached = localNewsRepository.loadCached(location)
        if (cached != null && cached.articles.isNotEmpty()) {
            _localNewsState.value = LocalNewsState.Data(
                articles = cached.articles,
                isStale = cached.isStale
            )
            _hasMore.value = cached.articles.size == PAGE_SIZE
            Log.d(TAG, "ui_render_cached")
        } else if (_localNewsState.value !is LocalNewsState.Data) {
            _localNewsState.value = LocalNewsState.Loading
        }
    }

    private suspend fun refreshInBackground(location: UserLocation) {
        when (val refreshResult = localNewsRepository.refreshLocalNews(location)) {
            is LocalNewsRepository.RefreshResult.Success -> {
                currentPage = 1
                _localNewsState.value = LocalNewsState.Data(
                    articles = refreshResult.articles,
                    isStale = false
                )
                _hasMore.value = refreshResult.articles.size == PAGE_SIZE
            }
            LocalNewsRepository.RefreshResult.Failed -> {
                if ((_localNewsState.value as? LocalNewsState.Data)?.articles.isNullOrEmpty()) {
                    _localNewsState.value = LocalNewsState.EmptyHardFail
                }
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
        private const val PAGE_SIZE = 7
    }
}
