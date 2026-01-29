package com.digitalturbine.promptnews.data.fotoscapes

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
            return@withContext runCatching {
                val root = JSONObject(body)
                val status = root.optString("status")
                val items = root.optJSONArray("items") ?: JSONArray()
                Log.d(TAG, "Response status: $status count=${items.length()}")
                if (status.isNotBlank() && !status.equals("ok", ignoreCase = true)) {
                    emptyList()
                } else {
                    (0 until items.length()).mapNotNull { i ->
                        val j = items.optJSONObject(i) ?: return@mapNotNull null
                        val title = localizedText(j, "title")
                        val summary = localizedText(j, "summary")
                        val link = j.optString("link")
                        if (link.isBlank()) return@mapNotNull null
                        val img = previewLink(j)
                        val age = TimeLabelFormatter.formatTimeLabel(j.optString("publishOn"))

                        Article(
                            title = title,
                            url = link,
                            imageUrl = if (img.isBlank()) "" else tryUpscaleCdn(img),
                            logoUrl = j.optString("brandLogo"),
                            logoUrlDark = j.optString("brandLogoDark"),
                            sourceName = j.optString("owner").ifBlank { null },
                            ageLabel = age,
                            summary = summary.ifBlank { null },
                            interest = interest ?: "",
                            isFotoscapes = true,
                            fotoscapesUid = j.optString("uid"),
                            fotoscapesLbtype = j.optString("lbtype"),
                            fotoscapesSourceLink = j.optString("sourceLink")
                        )
                    }.also { articles ->
                        Log.d(TAG, "Response count: ${articles.size}")
                    }
                }
            }.getOrElse { err ->
                Log.w(TAG, "Failed to parse FotoScapes response", err)
                emptyList()
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

private fun tryUpscaleCdn(url: String): String =
    url.replace(Regex("=w\\d{2,4}(-h\\d{2,4})?(-no)?"), "=w1200-h800")
        .replace(Regex("=s\\d{2,4}"), "=s1200")

private fun localizedText(obj: JSONObject, key: String): String {
    return when (val value = obj.opt(key)) {
        is JSONObject -> value.optString("en")
        is String -> value
        else -> ""
    }
}

private fun previewLink(obj: JSONObject): String {
    val previews = obj.optJSONArray("previews") ?: return ""
    return when (val first = previews.opt(0)) {
        is JSONObject -> first.optString("link")
        is String -> first
        else -> ""
    }
}
