package com.digitalturbine.promptnews.data.cache

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.digitalturbine.promptnews.data.Article
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class CachedSearchResult(
    val results: List<Article>,
    val timestampMillis: Long
)

object SearchCache {
    private const val TAG = "SearchCache"
    private const val PREFS_NAME = "search_cache"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 10
    private const val TTL_MS = 15 * 60 * 1000L

    private val memoryCache = mutableMapOf<String, CachedSearchResult>()
    @Volatile
    private var prefs: SharedPreferences? = null

    fun get(context: Context, query: String, screenName: String): CachedSearchResult? {
        val key = normalizeQuery(query)
        val now = System.currentTimeMillis()
        val memory = memoryCache[key]
        if (memory != null) {
            if (isFresh(memory, now)) {
                Log.d(TAG, "SearchCache HIT ($screenName): '$key'")
                return memory
            }
            memoryCache.remove(key)
        }

        val diskEntries = readDiskEntries(context)
        val disk = diskEntries[key]
        if (disk != null && isFresh(disk, now)) {
            memoryCache[key] = disk
            Log.d(TAG, "SearchCache HIT ($screenName): '$key'")
            return disk
        }

        if (disk != null) {
            diskEntries.remove(key)
            writeDiskEntries(context, diskEntries)
        }

        Log.d(TAG, "SearchCache MISS ($screenName): '$key'")
        return null
    }

    fun put(context: Context, query: String, results: List<Article>, screenName: String) {
        val key = normalizeQuery(query)
        val now = System.currentTimeMillis()
        val entry = CachedSearchResult(results = results, timestampMillis = now)
        memoryCache[key] = entry

        val diskEntries = readDiskEntries(context)
        diskEntries[key] = entry
        pruneExpired(diskEntries, now)
        val trimmed = keepNewest(diskEntries, MAX_ENTRIES)
        writeDiskEntries(context, trimmed)

        Log.d(TAG, "SearchCache STORE ($screenName): '$key' (${results.size})")
    }

    fun normalizeQuery(query: String): String {
        return query
            .lowercase(Locale.getDefault())
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun isFresh(entry: CachedSearchResult, now: Long): Boolean {
        return now - entry.timestampMillis < TTL_MS
    }

    private fun ensurePrefs(context: Context): SharedPreferences {
        return prefs ?: synchronized(this) {
            prefs ?: context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .also { prefs = it }
        }
    }

    private fun readDiskEntries(context: Context): MutableMap<String, CachedSearchResult> {
        val prefs = ensurePrefs(context)
        val raw = prefs.getString(KEY_ENTRIES, null).orEmpty()
        if (raw.isBlank()) return mutableMapOf()
        return runCatching {
            val array = JSONArray(raw)
            val map = mutableMapOf<String, CachedSearchResult>()
            for (i in 0 until array.length()) {
                val entry = array.optJSONObject(i) ?: continue
                val key = entry.optString("key")
                val timestamp = entry.optLong("timestamp")
                val resultsArray = entry.optJSONArray("results") ?: JSONArray()
                val results = parseArticles(resultsArray)
                if (key.isNotBlank()) {
                    map[key] = CachedSearchResult(results = results, timestampMillis = timestamp)
                }
            }
            map
        }.getOrElse {
            mutableMapOf()
        }
    }

    private fun writeDiskEntries(context: Context, entries: Map<String, CachedSearchResult>) {
        val prefs = ensurePrefs(context)
        val array = JSONArray()
        entries.forEach { (key, value) ->
            val obj = JSONObject()
                .put("key", key)
                .put("timestamp", value.timestampMillis)
                .put("results", serializeArticles(value.results))
            array.put(obj)
        }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    private fun pruneExpired(entries: MutableMap<String, CachedSearchResult>, now: Long) {
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!isFresh(entry.value, now)) {
                iterator.remove()
            }
        }
    }

    private fun keepNewest(
        entries: MutableMap<String, CachedSearchResult>,
        maxEntries: Int
    ): Map<String, CachedSearchResult> {
        if (entries.size <= maxEntries) return entries
        val sorted = entries.entries.sortedByDescending { it.value.timestampMillis }.take(maxEntries)
        return sorted.associate { it.toPair() }
    }

    private fun serializeArticles(articles: List<Article>): JSONArray {
        val array = JSONArray()
        articles.forEach { article ->
            val obj = JSONObject()
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
                .put(
                    "fotoscapesPreviewLinks",
                    JSONArray(article.fotoscapesPreviewLinks)
                )
            array.put(obj)
        }
        return array
    }

    private fun parseArticles(array: JSONArray): List<Article> {
        val results = mutableListOf<Article>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val previewLinksArray = obj.optJSONArray("fotoscapesPreviewLinks") ?: JSONArray()
            val previewLinks = mutableListOf<String>()
            for (j in 0 until previewLinksArray.length()) {
                previewLinks += previewLinksArray.optString(j)
            }
            results += Article(
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
        }
        return results
    }
}
