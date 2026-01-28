package com.digitalturbine.promptnews.data.fotoscapes

import android.net.Uri
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.Interest
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
        Http.client.newCall(Http.req(interest.endpoint)).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext emptyList()
            val body = resp.body?.string().orEmpty()
            if (body.isBlank()) return@withContext emptyList()
            val root = JSONObject(body)
            val items = root.optJSONArray("items") ?: JSONArray()

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
                    interest = interest.displayName,
                    isFotoscapes = true,
                    fotoscapesUid = j.optString("uid"),
                    fotoscapesLbtype = j.optString("lbtype")
                )
            }
        }
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
