package com.digitalturbine.promptnews.di

import android.app.Application
import android.content.Context
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.SearchRepository
import com.digitalturbine.promptnews.data.cache.CachedSearchResult
import com.digitalturbine.promptnews.data.cache.SearchCache
import com.digitalturbine.promptnews.data.serpapi.SerpApiImageService
import com.digitalturbine.promptnews.data.serpapi.SerpApiRepository
import com.digitalturbine.promptnews.ui.search.SearchViewModelFactory

object AppGraph {
    @Volatile
    private var app: Application? = null

    fun init(application: Application) {
        if (app == null) {
            app = application
        }
    }

    private fun requireApp(): Application {
        return requireNotNull(app) { "AppGraph.init(application) must be called first." }
    }

    val serpApiImageService: SerpApiImageService by lazy {
        SerpApiImageService()
    }

    val searchRepository: SearchRepository by lazy {
        SearchRepository()
    }

    val serpApiRepository: SerpApiRepository by lazy {
        SerpApiRepository(searchRepository)
    }

    val searchCacheStore: SearchCacheStore by lazy {
        SearchCacheStore(requireApp())
    }

    val searchViewModelFactory: SearchViewModelFactory by lazy {
        SearchViewModelFactory(requireApp(), searchRepository)
    }
}

class SearchCacheStore(private val context: Context) {
    fun get(query: String, screenName: String): CachedSearchResult? {
        return SearchCache.get(context, query, screenName)
    }

    fun put(query: String, results: List<Article>, screenName: String) {
        SearchCache.put(context, query, results, screenName)
    }
}
