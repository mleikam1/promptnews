package com.digitalturbine.promptnews.data

import android.net.Uri
import com.digitalturbine.promptnews.data.net.Http   // <-- fixed import
import com.digitalturbine.promptnews.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlin.math.max

// -----------------------------------------------------------------------------
// helpers
// -----------------------------------------------------------------------------

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

private fun ageLabelFromIso(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    val dt = runCatching { Date(iso.toLong()) }.getOrNull()
        ?: runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(iso)
        }.getOrNull()
    dt ?: return null
    val hours = max(1, ((System.currentTimeMillis() - dt.time) / (1000 * 60 * 60)).toInt())
    return if (hours <= 12) "$hours hours ago" else "Popular"
}

private fun inferInterest(title: String): String {
    val t = title.lowercase(Locale.US)
    fun has(r: String) = Regex(r).containsMatchIn(t)
    return when {
        has("\\b(nfl|nba|mlb|nhl|premier|match|score|game)\\b") -> "sports"
        has("\\b(ai|app|android|ios|google|apple|microsoft|chip|gpu|openai)\\b") -> "technology"
        has("\\b(stock|market|earnings|ipo|revenue|merger|inflation)\\b") -> "business"
        has("\\b(election|congress|parliament|white house|supreme)\\b") -> "politics"
        has("\\b(study|nasa|space|physics|biology)\\b") -> "science"
        has("\\b(health|covid|vaccine|flu)\\b") -> "health"
        has("\\b(movie|film|music|celebrity|netflix|series)\\b") -> "entertainment"
        else -> "news"
    }
}

// -----------------------------------------------------------------------------
// repository
// -----------------------------------------------------------------------------

class SearchRepository {

    // FotoScapes (first-party)
    suspend fun fetchFotoScapes(query: String, limit: Int = 3): List<Article> =
        withContext(Dispatchers.IO) {
            val route = fotoscapesEndpointForQuery(query) ?: return@withContext emptyList()
            Http.client.newCall(Http.req(route.endpoint)).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val root = JSONObject(resp.body?.string().orEmpty())
                val items = root.optJSONArray("items") ?: JSONArray()

                (0 until items.length()).asSequence().mapNotNull { i ->
                    val j = items.optJSONObject(i) ?: return@mapNotNull null
                    val title = j.opt("title")?.let { if (it is String) it else (it as? JSONObject)?.optString("en") }.orEmpty()
                    val link = j.optString("link").ifBlank { j.optString("sourceLink") }
                    val img  = firstHttp(j.opt("previews")) ?: firstHttp(j.opt("images")) ?: ""
                    if (title.isBlank() || link.isBlank() || img.isBlank()) return@mapNotNull null

                    val logo = (j.optString("brandLogoDark").ifBlank { j.optString("brandLogo") }).ifBlank { favicon(link) }
                    val age  = ageLabelFromIso(j.optString("publishOn").ifBlank { j.optString("scheduledOn") })

                    Article(
                        title = title,
                        url = link,
                        imageUrl = tryUpscaleCdn(img),
                        logoUrl = logo,
                        sourceName = j.optString("owner").ifBlank { null },
                        ageLabel = age,
                        interest = inferInterest(title),
                        isFotoscapes = true
                    )
                }.take(limit).toList()
            }
        }

    // SerpAPI helpers
    private fun serpUri(params: Map<String, String>): String {
        val key = Config.serpApiKey
        return Uri.parse("https://serpapi.com/search.json").buildUpon().apply {
            appendQueryParameter("api_key", key)
            appendQueryParameter("hl", "en")
            appendQueryParameter("gl", "us")
            params.forEach { (k, v) -> appendQueryParameter(k, v) }
        }.build().toString()
    }

    // News via SerpAPI (Google → Bing → Google tbm=nws)
    suspend fun fetchSerpNews(query: String, page: Int, pageSize: Int = 20): List<Article> =
        withContext(Dispatchers.IO) {
            if (Config.serpApiKey.isBlank()) return@withContext emptyList<Article>()

            fun parse(jsonStr: String): List<Article> {
                val root = JSONObject(jsonStr)
                val candidates =
                    root.optJSONArray("news_results")
                        ?: root.optJSONArray("top_stories")
                        ?: root.optJSONArray("inline_results")
                        ?: root.optJSONArray("organic_results")
                        ?: JSONArray()

                return (0 until candidates.length()).mapNotNull { i ->
                    val j = candidates.optJSONObject(i) ?: return@mapNotNull null
                    val title = j.optString("title")
                    val link  = j.optString("link").ifBlank { j.optString("url") }
                    val img   = firstHttp(
                        JSONObject()
                            .put("original", j.opt("original"))
                            .put("high_res", j.opt("high_res"))
                            .put("image", j.opt("image"))
                            .put("thumbnail", j.opt("thumbnail"))
                    ).orEmpty()
                    if (title.isBlank() || link.isBlank() || img.isBlank()) return@mapNotNull null

                    val srcName = when (val s = j.opt("source")) {
                        is JSONObject -> s.optString("name")
                        is String -> s
                        else -> null
                    } ?: j.optString("news_site").ifBlank { null }

                    val age = j.optString("date").ifBlank { j.optString("date_published") }

                    Article(
                        title = title,
                        url = link,
                        imageUrl = tryUpscaleCdn(img),
                        logoUrl = favicon(link),
                        sourceName = srcName,
                        ageLabel = if (age.isBlank()) null else age,
                        interest = inferInterest(title),
                        isFotoscapes = false
                    )
                }
            }

            val start = (page * pageSize).toString()
            val num   = pageSize.toString()

            var out: List<Article> = emptyList()

            Http.client.newCall(Http.req(serpUri(mapOf(
                "engine" to "google_news", "q" to query, "num" to num, "start" to start
            )))).execute().use { r ->
                if (r.isSuccessful) out = parse(r.body?.string().orEmpty())
            }
            if (out.isNotEmpty()) return@withContext out

            Http.client.newCall(Http.req(serpUri(mapOf(
                "engine" to "bing_news", "q" to query, "count" to num, "first" to start, "cc" to "US"
            )))).execute().use { r ->
                if (r.isSuccessful) out = parse(r.body?.string().orEmpty())
            }
            if (out.isNotEmpty()) return@withContext out

            Http.client.newCall(Http.req(serpUri(mapOf(
                "engine" to "google", "tbm" to "nws", "q" to query, "num" to num, "start" to start
            )))).execute().use { r ->
                if (r.isSuccessful) out = parse(r.body?.string().orEmpty())
            }

            out
        }

    // Clips (YouTube first, SerpAPI fallback)
    suspend fun fetchClips(query: String): List<Clip> = withContext(Dispatchers.IO) {
        // YouTube Data API
        if (Config.youtubeApiKey.isNotBlank()) {
            val u = Uri.parse("https://www.googleapis.com/youtube/v3/search").buildUpon()
                .appendQueryParameter("part", "snippet")
                .appendQueryParameter("maxResults", "18")
                .appendQueryParameter("q", query)
                .appendQueryParameter("type", "video")
                .appendQueryParameter("videoDuration", "short")
                .appendQueryParameter("key", Config.youtubeApiKey)
                .build().toString()

            Http.client.newCall(Http.req(u)).execute().use { r ->
                if (r.isSuccessful) {
                    val root  = JSONObject(r.body?.string().orEmpty())
                    val items = root.optJSONArray("items") ?: JSONArray()
                    val out   = mutableListOf<Clip>()
                    for (i in 0 until items.length()) {
                        val it = items.optJSONObject(i) ?: continue
                        val id = it.optJSONObject("id")?.optString("videoId").orEmpty()
                        val sn = it.optJSONObject("snippet") ?: continue
                        val title = sn.optString("title")
                        if (id.isBlank() || title.isBlank()) continue
                        val url   = "https://www.youtube.com/watch?v=$id"
                        val thumb = "https://i.ytimg.com/vi/$id/maxresdefault.jpg"
                        out += Clip(title = title, thumbnail = thumb, url = url, source = "YouTube")
                    }
                    if (out.isNotEmpty()) return@withContext out
                }
            }
        }

        // Fallback: SerpAPI google_videos
        if (Config.serpApiKey.isBlank()) return@withContext emptyList<Clip>()

        fun parse(str: String): List<Clip> {
            val root = JSONObject(str)
            val list = root.optJSONArray("video_results")
                ?: root.optJSONArray("inline_videos")
                ?: JSONArray()
            val out = mutableListOf<Clip>()
            for (i in 0 until list.length()) {
                val j = list.optJSONObject(i) ?: continue
                val title = j.optString("title")
                val link  = j.optString("link").ifBlank { j.optString("url") }
                val thumb = firstHttp(JSONObject().put("original", j.opt("thumbnail")).put("image", j.opt("image"))) ?: ""
                if (title.isBlank() || link.isBlank() || thumb.isBlank()) continue
                out += Clip(title = title, thumbnail = tryUpscaleCdn(thumb), url = link, source = j.optString("source","Video"))
            }
            return out
        }

        val shorts = Http.client.newCall(
            Http.req(serpUri(mapOf("engine" to "google_videos", "q" to "site:youtube.com/shorts $query")))
        ).execute().use { if (it.isSuccessful) parse(it.body?.string().orEmpty()) else emptyList() }

        val general = Http.client.newCall(
            Http.req(serpUri(mapOf("engine" to "google_videos", "q" to query)))
        ).execute().use { if (it.isSuccessful) parse(it.body?.string().orEmpty()) else emptyList() }

        (shorts + general).distinctBy { it.url }.take(18)
    }

    // Images (SerpAPI)
    suspend fun fetchImages(query: String): List<String> = withContext(Dispatchers.IO) {
        if (Config.serpApiKey.isBlank()) return@withContext emptyList<String>()
        val reqUrl = serpUri(mapOf("engine" to "google_images", "q" to query, "ijn" to "0"))
        val out = mutableListOf<String>()
        Http.client.newCall(Http.req(reqUrl)).execute().use { r ->
            if (!r.isSuccessful) return@use
            val root = JSONObject(r.body?.string().orEmpty())
            val arr  = root.optJSONArray("images_results") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val j = arr.optJSONObject(i) ?: continue
                val u = firstHttp(JSONObject()
                    .put("original", j.opt("original"))
                    .put("image", j.opt("image"))
                    .put("thumbnail", j.opt("thumbnail")))
                if (!u.isNullOrBlank()) out += tryUpscaleCdn(u)
                if (out.size >= 10) break
            }
        }
        out
    }

    // People Also Ask + Related (SerpAPI)
    suspend fun fetchExtras(query: String): Extras = withContext(Dispatchers.IO) {
        if (Config.serpApiKey.isBlank()) return@withContext Extras()
        val reqUrl = serpUri(mapOf("engine" to "google", "q" to query))
        var people: List<String> = emptyList()
        var related: List<String> = emptyList()

        Http.client.newCall(Http.req(reqUrl)).execute().use { r ->
            if (!r.isSuccessful) return@use
            val root = JSONObject(r.body?.string().orEmpty())

            fun toStrings(arr: JSONArray?): List<String> {
                if (arr == null) return emptyList()
                val ret = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val j = arr.optJSONObject(i) ?: continue
                    val s = j.optString("question").ifBlank { j.optString("title") }
                    if (s.isNotBlank()) ret += s
                }
                return ret
            }

            people = toStrings(root.optJSONArray("related_questions")
                ?: root.optJSONArray("people_also_ask")).take(4)
            related = toStrings(root.optJSONArray("related_searches")).take(10)
        }

        Extras(peopleAlsoAsk = people, relatedSearches = related)
    }
}
