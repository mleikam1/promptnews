package com.digitalturbine.promptnews.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalturbine.promptnews.data.SearchRepository
import com.digitalturbine.promptnews.data.SearchUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repo: SearchRepository = SearchRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow<SearchUi>(SearchUi.Idle)
    val ui: StateFlow<SearchUi> = _ui

    private var currentQuery: String = ""
    private var page = 0
    private var loadMoreJob: Job? = null

    fun runSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        currentQuery = q
        page = 0
        _ui.value = SearchUi.Searching(q)

        viewModelScope.launch {
            val fs = repo.fetchFotoScapes(q, limit = 3)
            val serp = repo.fetchSerpNews(q, page = 0, pageSize = 20)

            val list = (fs + serp).distinctBy { it.url }
            val hero = list.firstOrNull()
            val rows = list.drop(1).take(4)

            val clips = repo.fetchClips(q)
            val images = repo.fetchImages(q)
            val extras = repo.fetchExtras(q)

            _ui.value = SearchUi.Ready(
                query = q,
                hero = hero,
                rows = rows,
                hasMore = list.size > 5, // initial > hero+4
                clips = clips,
                images = images,
                extras = extras
            )
            page = 1
        }
    }

    fun loadMore() {
        val state = _ui.value as? SearchUi.Ready ?: return
        if (loadMoreJob?.isActive == true) return

        loadMoreJob = viewModelScope.launch {
            val next = repo.fetchSerpNews(currentQuery, page = page, pageSize = 20)
            page += 1
            val dedup = (state.rows + next).distinctBy { it.url }
            _ui.value = state.copy(rows = dedup, hasMore = next.isNotEmpty())
        }
    }
}