package com.digitalturbine.promptnews.data.trending

import android.content.Context
import android.util.Log
import com.digitalturbine.promptnews.data.net.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.jsoup.Jsoup

class TrendingPromptsRepository private constructor(context: Context) {

    companion object {
        private const val TAG = "TrendingPromptsRepo"
        private const val FILE = "trending_prompts_prefs"
        private const val KEY_TERMS = "trending_prompts_list"
        private const val KEY_LAST_UPDATED = "trending_prompts_last_updated"
        private const val MAX_TERMS = 10
        private const val REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000L
        private const val TRENDS_URL =
            "https://trends.google.com/trending?geo=US&hl=en-US&hours=24&category=4"

        @Volatile
        private var instance: TrendingPromptsRepository? = null

        fun getInstance(context: Context): TrendingPromptsRepository {
            return instance ?: synchronized(this) {
                instance ?: TrendingPromptsRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    suspend fun getTrendingTerms(forceRefresh: Boolean = false): List<String> =
        withContext(Dispatchers.IO) {
            val cached = readCachedTerms()
            val lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0L)
            val now = System.currentTimeMillis()
            val shouldRefresh = forceRefresh || cached.isEmpty() || now - lastUpdated >= REFRESH_INTERVAL_MS

            if (!shouldRefresh) return@withContext cached

            val fetched = fetchTrendingTerms()
            if (fetched.isNotEmpty()) {
                cacheTerms(fetched, now)
                return@withContext fetched
            }

            cached
        }

    private fun fetchTrendingTerms(): List<String> {
        return runCatching {
            Http.client.newCall(
                Http.req(
                    TRENDS_URL,
                    mapOf(
                        "Accept" to "text/html",
                        "Accept-Language" to "en-US,en;q=0.9"
                    )
                )
            ).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Trending request failed: ${'$'}{response.code}")
                    return emptyList()
                }
                val html = response.body?.string().orEmpty()
                if (html.isBlank()) return emptyList()
                parseTrendingTerms(html)
            }
        }.getOrElse { err ->
            Log.w(TAG, "Trending request error", err)
            emptyList()
        }
    }

    private fun parseTrendingTerms(html: String): List<String> {
        val doc = Jsoup.parse(html)
        val scriptData = doc.getElementsByTag("script")
            .asSequence()
            .map { it.data() }
            .firstOrNull { it.contains("trendingSearchesDays") }
            ?: html

        val jsonArrayText = extractJsonArray(scriptData, "\"trendingSearchesDays\"") ?: return emptyList()
        val jsonArray = runCatching { JSONArray(jsonArrayText) }.getOrNull() ?: return emptyList()
        val day = jsonArray.optJSONObject(0) ?: return emptyList()
        val searches = day.optJSONArray("trendingSearches") ?: return emptyList()

        val terms = mutableListOf<String>()
        for (i in 0 until searches.length()) {
            val item = searches.optJSONObject(i) ?: continue
            val title = item.optJSONObject("title")?.optString("query").orEmpty()
            if (title.isNotBlank()) {
                terms.add(title)
            }
        }

        return terms.distinct().take(MAX_TERMS)
    }

    private fun extractJsonArray(source: String, key: String): String? {
        val keyIndex = source.indexOf(key)
        if (keyIndex == -1) return null
        val startIndex = source.indexOf('[', keyIndex)
        if (startIndex == -1) return null

        var depth = 0
        for (i in startIndex until source.length) {
            when (source[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        return source.substring(startIndex, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun cacheTerms(terms: List<String>, updatedAt: Long) {
        val json = JSONArray().apply {
            terms.forEach { put(it) }
        }
        prefs.edit()
            .putString(KEY_TERMS, json.toString())
            .putLong(KEY_LAST_UPDATED, updatedAt)
            .apply()
    }

    private fun readCachedTerms(): List<String> {
        val json = prefs.getString(KEY_TERMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length())
                .mapNotNull { index -> array.optString(index) }
                .filter { it.isNotBlank() }
                .take(MAX_TERMS)
        }.getOrElse { emptyList() }
    }
}
