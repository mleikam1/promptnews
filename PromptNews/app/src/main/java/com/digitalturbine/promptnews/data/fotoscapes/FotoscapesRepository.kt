package com.digitalturbine.promptnews.data.fotoscapes

import android.util.Log
import com.digitalturbine.promptnews.data.FotoscapesEndpoints
import com.digitalturbine.promptnews.data.Interest
import com.digitalturbine.promptnews.data.toFotoscapesKey
import com.digitalturbine.promptnews.data.toFotoscapesSched
import com.digitalturbine.promptnews.data.net.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class FotoscapesRepository {

    suspend fun fetchInterestSections(interests: List<Interest>): List<InterestSectionResult> =
        coroutineScope {
            interests.map { interest ->
                async { InterestSectionResult(interest, fetchInterest(interest)) }
            }.awaitAll()
        }

    private suspend fun fetchInterest(interest: Interest): List<FotoscapesArticle> = withContext(Dispatchers.IO) {
        fetchInterestFeed(
            interest = interest,
            limit = DEFAULT_LIMIT
        )
    }

    suspend fun fetchInterestFeed(
        interest: Interest,
        limit: Int
    ): List<FotoscapesArticle> = withContext(Dispatchers.IO) {
        val sched = interest.toFotoscapesSched()
        Log.e("FS_TRACE", "USING sched=$sched")
        val interestKey = interest.toFotoscapesKey()
        val items = fetchContent(
            category = interestKey,
            interest = interestKey,
            limit = limit,
            schedule = sched,
            geo = null
        )
        Log.e("FS_TRACE", "RESPONSE size=${items.size}")
        Log.d(TAG, "Interest=$interestKey rawCount=${items.size}")
        items
    }

    suspend fun fetchContent(
        category: String,
        interest: String?,
        limit: Int,
        schedule: String,
        geo: String?
    ): List<FotoscapesArticle> = withContext(Dispatchers.IO) {
        val url = FotoscapesEndpoints.contentEndpoint(
            category = category,
            interest = interest,
            limit = limit,
            schedule = schedule,
            geo = geo
        )
        Log.e("FS_TRACE", "REQUEST url=$url")
        Log.d(
            TAG,
            "Request params: category=$category interest=$interest limit=$limit schedule=$schedule geo=$geo url=$url"
        )
        Http.client.newCall(Http.req(url)).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext emptyList()
            val body = resp.body?.string().orEmpty()
            Log.d(TAG, "Response body: $body")
            if (body.isBlank()) return@withContext emptyList()
            return@withContext runCatching {
                val root = JSONObject(body)
                val items = root.optJSONArray("items") ?: JSONArray()
                Log.d(TAG, "Response count (raw): ${items.length()}")
                val mappedItems = (0 until items.length()).map { i ->
                    val j = items.optJSONObject(i) ?: JSONObject()
                    val title = localizedText(j, "title")
                    val summary = localizedText(j, "summary")
                    val body = localizedText(j, "body")
                    val articleUrl = j.optString("link").ifBlank { j.optString("sourceLink") }
                    val previews = previewLinks(j)
                    val imageUrl = previews.firstOrNull()

                    FotoscapesArticle(
                        id = j.optString("uid"),
                        lbType = j.optString("lbtype").ifBlank { "unknown" },
                        title = title,
                        summary = summary,
                        body = body,
                        imageUrl = imageUrl,
                        articleUrl = articleUrl
                    )
                }
                Log.d("FS_MAP", "Mapped ${mappedItems.size} Fotoscapes items")
                Log.d(TAG, "Response count (mapped): ${mappedItems.size}")
                mappedItems
            }.getOrElse { err ->
                Log.w(TAG, "Failed to parse FotoScapes response", err)
                emptyList()
            }
        }
    }

    companion object {
        private const val TAG = "Fotoscapes"
        private const val DEFAULT_LIMIT = 10
    }
}

data class InterestSectionResult(
    val interest: Interest,
    val articles: List<FotoscapesArticle>
)

private fun localizedText(obj: JSONObject, key: String): String {
    return when (val value = obj.opt(key)) {
        is JSONObject -> value.optString("en")
        is String -> value
        else -> ""
    }
}

private fun previewLinks(obj: JSONObject): List<String> {
    val previews = obj.optJSONArray("previews") ?: return emptyList()
    return (0 until previews.length()).mapNotNull { index ->
        when (val item = previews.opt(index)) {
            is JSONObject -> item.optString("link")
            is String -> item
            else -> null
        }?.takeIf { it.isNotBlank() }
    }
}
