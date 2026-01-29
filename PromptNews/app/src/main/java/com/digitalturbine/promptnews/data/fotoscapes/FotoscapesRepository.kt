package com.digitalturbine.promptnews.data.fotoscapes

import android.net.Uri
import android.util.Log
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.FotoscapesEndpoints
import com.digitalturbine.promptnews.data.Interest
import com.digitalturbine.promptnews.data.toFotoscapesKey
import com.digitalturbine.promptnews.data.net.Http
import com.digitalturbine.promptnews.util.TimeLabelFormatter
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

    private suspend fun fetchInterest(interest: Interest): List<Article> = withContext(Dispatchers.IO) {
        fetchInterestFeed(
            interestKey = interest.toFotoscapesKey(),
            limit = DEFAULT_LIMIT,
            schedule = DEFAULT_SCHEDULE
        )
    }

    suspend fun fetchInterestFeed(
        interestKey: String,
        limit: Int,
        schedule: String
    ): List<Article> = withContext(Dispatchers.IO) {
        val items = fetchContent(
            category = interestKey,
            interest = interestKey,
            limit = limit,
            schedule = schedule,
            geo = null
        )
        Log.d(TAG, "Interest=$interestKey count=${items.size}")
        items
    }

    suspend fun fetchContent(
        category: String,
        interest: String?,
        limit: Int,
        schedule: String,
        geo: String?
    ): List<Article> = withContext(Dispatchers.IO) {
        val url = FotoscapesEndpoints.contentEndpoint(
            category = category,
            interest = interest,
            limit = limit,
            schedule = schedule,
            geo = geo
        )
        Log.d(
            TAG,
            "Request params: category=$category interest=$interest limit=$limit schedule=$schedule geo=$geo url=$url"
        )
        Http.client.newCall(Http.req(url)).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext emptyList()
            val body = resp.body?.string().orEmpty()
            Log.d(TAG, "Response body: $body")
            if (body.isBlank()) return@withContext emptyList()
            val root = JSONObject(body)
            val status = root.optString("status")
            val items = root.optJSONArray("items") ?: JSONArray()
            Log.d(TAG, "Response status: $status count=${items.length()}")

            (0 until items.length()).mapNotNull { i ->
                val j = items.optJSONObject(i) ?: return@mapNotNull null
                val title = j.opt("title")?.let { if (it is String) it else (it as? JSONObject)?.optString("en") }
                    .orEmpty()
                val link = j.optString("link").ifBlank { j.optString("sourceLink") }
                val img = firstHttp(j.opt("previews")) ?: firstHttp(j.opt("images")) ?: ""
                if (title.isBlank() || link.isBlank() || img.isBlank()) return@mapNotNull null

                val logo = j.optString("brandLogoDark").ifBlank { j.optString("brandLogo") }
                    .ifBlank { favicon(link) }
                val age = TimeLabelFormatter.formatTimeLabel(
                    j.optString("publishOn").ifBlank { j.optString("scheduledOn") }
                )

                Article(
                    title = title,
                    url = link,
                    imageUrl = tryUpscaleCdn(img),
                    logoUrl = logo,
                    sourceName = j.optString("owner").ifBlank { null },
                    ageLabel = age,
                    interest = interest ?: "",
                    isFotoscapes = true,
                    fotoscapesUid = j.optString("uid"),
                    fotoscapesLbtype = j.optString("lbtype")
                )
            }.also { articles ->
                Log.d(TAG, "Response count: ${articles.size}")
            }
        }
    }

    companion object {
        private const val TAG = "Fotoscapes"
        private const val DEFAULT_SCHEDULE = "promptnews"
        private const val DEFAULT_LIMIT = 10
    }
}

data class InterestSectionResult(
    val interest: Interest,
    val articles: List<Article>
)

private fun favicon(url: String, size: Int = 64): String = runCatching {
    val u = Uri.parse(url)
    "https://www.google.com/s2/favicons?domain=${u.scheme}://${u.host}&sz=$size"
}.getOrElse { "" }

private fun firstHttp(obj: Any?): String? {
    when (obj) {
        is String -> if (obj.startsWith("http")) return obj
        is JSONObject -> {
            for (k in obj.keys()) {
                val v = obj.opt(k)
                firstHttp(v)?.let { return it }
            }
        }
        is JSONArray -> {
            for (i in 0 until obj.length()) {
                firstHttp(obj.opt(i))?.let { return it }
            }
        }
    }
    return null
}

private fun tryUpscaleCdn(url: String): String =
    url.replace(Regex("=w\\d{2,4}(-h\\d{2,4})?(-no)?"), "=w1200-h800")
        .replace(Regex("=s\\d{2,4}"), "=s1200")
