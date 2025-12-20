package com.digitalturbine.promptnews.data.suggest

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONTokener
import java.net.URLEncoder

/**
 * Unofficial Google autocomplete endpoint used for "related searches":
 * https://suggestqueries.google.com/complete/search?client=firefox&q=<query>
 *
 * Returns: [ "<query>", [ "suggestion1", "suggestion2", ... ] ]
 * Keep it gentle (cache + do not spam).
 */
class GoogleSuggest(private val http: OkHttpClient) {

    fun suggestions(query: String, limit: Int = 8): List<String> {
        val url =
            "https://suggestqueries.google.com/complete/search?client=firefox&q=${URLEncoder.encode(query, "UTF-8")}"
        val req = Request.Builder().url(url).get().build()
        return runCatching {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val arr = JSONTokener(body).nextValue() as? JSONArray ?: return emptyList()
                val list = (arr.optJSONArray(1) ?: return emptyList())
                (0 until list.length())
                    .asSequence()
                    .mapNotNull { list.optString(it) }
                    .filter { it.isNotBlank() }
                    .take(limit)
                    .toList()
            }
        }.getOrDefault(emptyList())
    }
}
