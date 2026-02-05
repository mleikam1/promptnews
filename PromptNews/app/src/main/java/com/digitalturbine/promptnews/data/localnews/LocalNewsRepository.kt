package com.digitalturbine.promptnews.data.localnews

import android.content.Context
import android.util.Log
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.UserLocation
import com.digitalturbine.promptnews.data.SearchRepository
import com.digitalturbine.promptnews.data.serpapi.SerpApiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class LocalNewsRepository(
    context: Context,
    private val searchRepository: SearchRepository = SearchRepository(),
    private val serpApiRepository: SerpApiRepository = SerpApiRepository(searchRepository),
    private val localNewsDao: CachedLocalNewsDao =
        LocalNewsCacheDatabase.getInstance(context).cachedLocalNewsDao()
) {
    private val inMemoryLocalNews = mutableMapOf<String, CachePayload>()

    suspend fun loadCached(location: UserLocation): CachedResult? = withContext(Dispatchers.IO) {
        val key = location.cacheKey()
        val now = System.currentTimeMillis()

        val memory = inMemoryLocalNews[key]
        if (memory != null) {
            Log.d(TAG, "cache_hit_memory")
            return@withContext CachedResult(memory.articles, isStale = now - memory.fetchedAt > CACHE_TTL_MS)
        }

        val disk = localNewsDao.getByLocation(key) ?: return@withContext null
        val articles = parseArticles(disk.articlesJson)
        inMemoryLocalNews[key] = CachePayload(articles = articles, fetchedAt = disk.fetchedAt)
        Log.d(TAG, "cache_hit_disk")
        CachedResult(articles = articles, isStale = now - disk.fetchedAt > CACHE_TTL_MS)
    }

    suspend fun refreshLocalNews(location: UserLocation): RefreshResult = withContext(Dispatchers.IO) {
        val query = "${location.city} ${location.state} local news"
        val locationText = "${location.city}, ${location.state}"

        val fotoscapes = fetchWithTimeoutRetry("fotoscapes") {
            searchRepository.fetchFotoScapes(query = query, limit = PAGE_SIZE)
        }
        if (fotoscapes.isNotEmpty()) {
            Log.d(TAG, "fetch_success_fotoscapes")
            save(location, fotoscapes)
            return@withContext RefreshResult.Success(fotoscapes)
        }

        val serpApi = fetchWithTimeoutRetry("serpapi") {
            serpApiRepository.fetchLocalNewsPage(
                location = locationText,
                query = query,
                page = 1,
                pageSize = PAGE_SIZE
            )
        }
        if (serpApi.isNotEmpty()) {
            Log.d(TAG, "fetch_fallback_serpapi")
            save(location, serpApi)
            return@withContext RefreshResult.Success(serpApi)
        }

        RefreshResult.Failed
    }

    suspend fun fetchMore(location: UserLocation, page: Int): List<Article> = withContext(Dispatchers.IO) {
        serpApiRepository.fetchLocalNewsPage(
            location = "${location.city}, ${location.state}",
            query = "${location.city} ${location.state} local news",
            page = page,
            pageSize = PAGE_SIZE
        )
    }

    private suspend fun fetchWithTimeoutRetry(source: String, block: suspend () -> List<Article>): List<Article> {
        repeat(2) { attempt ->
            val result = runCatching {
                withTimeout(NETWORK_TIMEOUT_MS) { block() }
            }
            if (result.isSuccess) {
                return result.getOrNull().orEmpty()
            }
            val throwable = result.exceptionOrNull()
            if (throwable != null && attempt == 0) {
                Log.w(TAG, "fetch_timeout source=$source retrying", throwable)
            } else if (throwable != null) {
                Log.w(TAG, "fetch_timeout source=$source", throwable)
            }
            Log.d(TAG, "fetch_timeout")
        }
        return emptyList()
    }

    private suspend fun save(location: UserLocation, articles: List<Article>) {
        val key = location.cacheKey()
        val now = System.currentTimeMillis()
        val payload = CachePayload(articles = articles, fetchedAt = now)
        inMemoryLocalNews[key] = payload
        localNewsDao.upsert(
            CachedLocalNews(
                locationKey = key,
                articlesJson = serializeArticles(articles),
                fetchedAt = now
            )
        )
        localNewsDao.deleteOlderThan(now - DISK_RETENTION_MS)
    }

    private fun serializeArticles(articles: List<Article>): String {
        val array = JSONArray()
        articles.forEach { article ->
            array.put(
                JSONObject()
                    .put("title", article.title)
                    .put("url", article.url)
                    .put("imageUrl", article.imageUrl)
                    .put("logoUrl", article.logoUrl)
                    .put("logoUrlDark", article.logoUrlDark)
                    .put("sourceName", article.sourceName)
                    .put("ageLabel", article.ageLabel)
                    .put("summary", article.summary)
                    .put("isFotoscapes", article.isFotoscapes)
                    .put("fotoscapesUid", article.fotoscapesUid)
                    .put("fotoscapesLbtype", article.fotoscapesLbtype)
                    .put("fotoscapesSourceLink", article.fotoscapesSourceLink)
                    .put("fotoscapesTitleEn", article.fotoscapesTitleEn)
                    .put("fotoscapesSummaryEn", article.fotoscapesSummaryEn)
                    .put("fotoscapesBodyEn", article.fotoscapesBodyEn)
                    .put("fotoscapesLink", article.fotoscapesLink)
                    .put("fotoscapesPreviewLinks", JSONArray(article.fotoscapesPreviewLinks))
            )
        }
        return array.toString()
    }

    private fun parseArticles(json: String): List<Article> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val previewLinksArray = obj.optJSONArray("fotoscapesPreviewLinks") ?: JSONArray()
                    val previewLinks = mutableListOf<String>()
                    for (previewIndex in 0 until previewLinksArray.length()) {
                        previewLinks += previewLinksArray.optString(previewIndex)
                    }
                    add(
                        Article(
                            title = obj.optString("title"),
                            url = obj.optString("url"),
                            imageUrl = obj.optString("imageUrl"),
                            logoUrl = obj.optString("logoUrl"),
                            logoUrlDark = obj.optString("logoUrlDark"),
                            sourceName = obj.optString("sourceName").ifBlank { null },
                            ageLabel = obj.optString("ageLabel").ifBlank { null },
                            summary = obj.optString("summary").ifBlank { null },
                            isFotoscapes = obj.optBoolean("isFotoscapes"),
                            fotoscapesUid = obj.optString("fotoscapesUid"),
                            fotoscapesLbtype = obj.optString("fotoscapesLbtype"),
                            fotoscapesSourceLink = obj.optString("fotoscapesSourceLink"),
                            fotoscapesTitleEn = obj.optString("fotoscapesTitleEn"),
                            fotoscapesSummaryEn = obj.optString("fotoscapesSummaryEn"),
                            fotoscapesBodyEn = obj.optString("fotoscapesBodyEn"),
                            fotoscapesPreviewLinks = previewLinks,
                            fotoscapesLink = obj.optString("fotoscapesLink")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun UserLocation.cacheKey(): String {
        return "${city.trim().lowercase(Locale.US)}|${state.trim().lowercase(Locale.US)}"
    }

    data class CachedResult(
        val articles: List<Article>,
        val isStale: Boolean
    )

    sealed class RefreshResult {
        data class Success(val articles: List<Article>) : RefreshResult()
        data object Failed : RefreshResult()
    }

    private data class CachePayload(
        val articles: List<Article>,
        val fetchedAt: Long
    )

    companion object {
        private const val TAG = "LocalNewsRepository"
        private const val PAGE_SIZE = 7
        private const val NETWORK_TIMEOUT_MS = 7_000L
        private const val CACHE_TTL_MS = 30 * 60 * 1000L
        private const val DISK_RETENTION_MS = 2 * 24 * 60 * 60 * 1000L
    }
}
