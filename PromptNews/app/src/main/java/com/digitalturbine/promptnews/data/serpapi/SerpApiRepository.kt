package com.digitalturbine.promptnews.data.serpapi

import android.util.Log
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SerpApiRepository(
    private val searchRepository: SearchRepository = SearchRepository()
) {
    suspend fun fetchLocalNews(location: String?, query: String): List<Article> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Local request location=$location")
            if (location.isNullOrBlank()) {
                Log.d(TAG, "Local request missing location, aborting")
                return@withContext emptyList()
            }
            val results = searchRepository.fetchSerpNewsByOffset(
                query = query,
                limit = LOCAL_LIMIT,
                offset = 0,
                location = location
            )
            Log.d(TAG, "Local results=${results.size}")
            results
        }

    suspend fun fetchLocalNewsByOffset(
        location: String?,
        query: String,
        limit: Int,
        offset: Int
    ): List<Article> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Local request location=$location offset=$offset limit=$limit")
        if (location.isNullOrBlank()) {
            Log.d(TAG, "Local request missing location, aborting")
            return@withContext emptyList()
        }
        val results = searchRepository.fetchSerpNewsByOffset(
            query = query,
            limit = limit,
            offset = offset,
            location = location
        )
        Log.d(TAG, "Local results=${results.size}")
        results
    }

    companion object {
        private const val TAG = "SerpApi"
        private const val LOCAL_LIMIT = 10
    }
}
