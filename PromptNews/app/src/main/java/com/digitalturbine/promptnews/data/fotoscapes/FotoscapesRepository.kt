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
        Log.d(TAG, "Interest=$interestKey rawCount=${items.size}")
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
                (0 until items.length()).map { i ->
                    val j = items.optJSONObject(i) ?: JSONObject()
                    val title = localizedText(j, "title")
                    val summary = localizedText(j, "summary")
                    val body = localizedText(j, "body")
                    val link = j.optString("link").ifBlank { j.optString("sourceLink") }
                    val previews = previewLinks(j)
                    val img = previews.firstOrNull().orEmpty()
                    val age = TimeLabelFormatter.formatTimeLabel(j.optString("publishOn"))
                    val owner = j.optString("owner").orEmpty()

                    Article(
                        title = title,
                        url = link,
                        imageUrl = if (img.isBlank()) "" else tryUpscaleCdn(img),
                        logoUrl = j.optString("brandLogo"),
                        logoUrlDark = j.optString("brandLogoDark"),
                        sourceName = owner.ifBlank { null },
                        ageLabel = age,
                        summary = summary.ifBlank { body }.ifBlank { "" },
                        interest = interest.orEmpty(),
                        isFotoscapes = true,
                        fotoscapesUid = j.optString("uid"),
                        fotoscapesLbtype = j.optString("lbtype"),
                        fotoscapesSourceLink = j.optString("sourceLink"),
                        fotoscapesTitleEn = title,
                        fotoscapesSummaryEn = summary,
                        fotoscapesBodyEn = body,
                        fotoscapesPreviewLinks = previews,
                        fotoscapesLink = link
                    )
                }.also { articles ->
                    Log.d(TAG, "Response count (mapped): ${articles.size}")
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
