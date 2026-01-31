package com.digitalturbine.promptnews.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.Clip
import com.digitalturbine.promptnews.data.Extras
import com.digitalturbine.promptnews.data.SearchRepository
import com.digitalturbine.promptnews.data.SearchUi
import com.digitalturbine.promptnews.data.cache.SearchCache
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    application: Application,
    private val repo: SearchRepository = SearchRepository()
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SearchViewModel"
    }

    private val _ui = MutableStateFlow<SearchUi>(SearchUi.Idle)
    val ui: StateFlow<SearchUi> = _ui

    private var currentQuery: String = ""
    private var page = 0
    private var loadMoreJob: Job? = null

    fun runSearch(query: String, screenName: String = "Prompt") {
        val q = query.trim()
        if (q.isEmpty()) return
        currentQuery = q
        page = 0
        _ui.value = SearchUi.Searching(q)
        Log.d(TAG, "$screenName search executed: '$q'")

        viewModelScope.launch {
            val cachedResult = SearchCache.get(getApplication(), q, screenName)
            if (cachedResult != null) {
                emitSearchReady(
                    query = q,
                    list = cachedResult.results,
                    clips = emptyList(),
                    images = emptyList(),
                    extras = Extras(),
                    screenName = screenName,
                    source = "cache"
                )
            }

            val serpResults = if (cachedResult == null) {
                val result = runCatching {
                    repo.fetchSerpNews(q, page = 0, pageSize = 20)
                }
                val serp = result.getOrElse { err ->
                    _ui.value = SearchUi.Error(err.message ?: "Search failed.")
                    return@launch
                }
                val fs = if (serp.isEmpty()) {
                    Log.d(TAG, "SerpAPI empty; triggering FotoScapes fallback for query=$q")
                    repo.fetchFotoScapes(q, limit = 3)
                } else {
                    emptyList()
                }
                val list = (serp + fs).distinctBy { article ->
                    article.url.ifBlank { article.fotoscapesUid.ifBlank { article.title } }
                }
                SearchCache.put(getApplication(), q, list, screenName)
                list
            } else {
                cachedResult.results
            }

            val clips = runCatching { repo.fetchClips(q) }.getOrElse { emptyList() }
            val images = runCatching { repo.fetchImages(q) }.getOrElse { emptyList() }
            val extras = runCatching { repo.fetchExtras(q) }.getOrElse { Extras() }

            emitSearchReady(
                query = q,
                list = serpResults,
                clips = clips,
                images = images,
                extras = extras,
                screenName = screenName,
                source = if (cachedResult == null) "network" else "cache-refresh"
            )
            page = 1
        }
    }

    fun loadMore() {
        val state = _ui.value as? SearchUi.Ready ?: return
        if (loadMoreJob?.isActive == true) return

        loadMoreJob = viewModelScope.launch {
            runCatching {
                val next = repo.fetchSerpNews(currentQuery, page = page, pageSize = 20)
                page += 1
                val dedup = (state.rows + next).distinctBy { it.url }
                _ui.value = state.copy(rows = dedup, hasMore = next.isNotEmpty())
            }.onFailure { err ->
                _ui.value = SearchUi.Error(err.message ?: "Search failed.")
            }
        }
    }

    private fun emitSearchReady(
        query: String,
        list: List<Article>,
        clips: List<Clip>,
        images: List<String>,
        extras: Extras,
        screenName: String,
        source: String
    ) {
        val hero = list.firstOrNull()
        val rows = list.drop(1).take(4)
        _ui.value = SearchUi.Ready(
            query = query,
            hero = hero,
            rows = rows,
            hasMore = list.size > 5,
            clips = clips,
            images = images,
            extras = extras
        )
        Log.d(TAG, "$screenName emitted ${list.size} results ($source)")
    }
}
