package com.digitalturbine.promptnews.data

import android.net.Uri
import android.util.Log
import com.digitalturbine.promptnews.data.net.Http   // <-- fixed import
import com.digitalturbine.promptnews.data.serpapi.SerpApiMapper
import com.digitalturbine.promptnews.data.serpapi.SerpApiStoryDto
import com.digitalturbine.promptnews.domain.model.UnifiedStory
import com.digitalturbine.promptnews.util.Config
import com.digitalturbine.promptnews.util.TimeLabelFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

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

    companion object {
        private const val TAG = "SearchRepository"
    }

    private val serpApiMapper = SerpApiMapper()

    // FotoScapes (first-party)
    suspend fun fetchFotoScapes(query: String, limit: Int = 3): List<Article> =
        withContext(Dispatchers.IO) {
            val route = fotoscapesEndpointForQuery(query) ?: return@withContext emptyList()
            Http.client.newCall(Http.req(route.endpoint)).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@withContext emptyList()
                runCatching {
                    val root = JSONObject(body)
                    val status = root.optString("status")
                    val items = root.optJSONArray("items") ?: JSONArray()
                    if (status.isNotBlank() && !status.equals("ok", ignoreCase = true)) {
                        emptyList()
                    } else {
                        (0 until items.length()).asSequence().mapNotNull { i ->
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
                                interest = inferInterest(title),
                                isFotoscapes = true,
                                fotoscapesUid = j.optString("uid"),
                                fotoscapesLbtype = j.optString("lbtype"),
                                fotoscapesSourceLink = j.optString("sourceLink")
                            )
                        }.take(limit).toList()
                    }
                }.getOrElse { err ->
                    Log.w(TAG, "Failed to parse FotoScapes response", err)
                    emptyList()
                }
            }
        }

    // SerpAPI helpers
    private fun serpUri(params: Map<String, String>, location: String? = null): String {
        val key = Config.serpApiKey
        return Uri.parse("https://serpapi.com/search.json").buildUpon().apply {
            appendQueryParameter("api_key", key)
            appendQueryParameter("hl", "en")
            appendQueryParameter("gl", "us")
            if (!location.isNullOrBlank()) {
                appendQueryParameter("location", location)
            }
            params.forEach { (k, v) -> appendQueryParameter(k, v) }
        }.build().toString()
    }

    // News via SerpAPI (Google → Bing → Google tbm=nws)
    suspend fun fetchSerpNews(
        query: String,
        page: Int,
        pageSize: Int = 20,
        location: String? = null,
        useRawImageUrls: Boolean = false
    ): List<Article> =
        withContext(Dispatchers.IO) {
            if (Config.serpApiKey.isBlank()) return@withContext emptyList<Article>()

            fetchSerpNewsDtos(query, page, pageSize, location, useRawImageUrls).mapNotNull { dto ->
                val unifiedStory = serpApiMapper.toUnifiedStory(dto)
                unifiedStory.toArticle(
                    ageLabel = dto.ageLabel,
                    interest = inferInterest(dto.title)
                )
            }
        }

    suspend fun fetchSerpNewsByOffset(
        query: String,
        limit: Int,
        offset: Int,
        location: String? = null,
        useRawImageUrls: Boolean = false
    ): List<Article> =
        withContext(Dispatchers.IO) {
            if (Config.serpApiKey.isBlank()) return@withContext emptyList<Article>()

            fetchSerpNewsDtosByOffset(query, limit, offset, location, useRawImageUrls).mapNotNull { dto ->
                val unifiedStory = serpApiMapper.toUnifiedStory(dto)
                unifiedStory.toArticle(
                    ageLabel = dto.ageLabel,
                    interest = inferInterest(dto.title)
                )
            }
        }

    suspend fun fetchSerpNewsStories(
        query: String,
        page: Int,
        pageSize: Int = 20,
        location: String? = null,
        useRawImageUrls: Boolean = false
    ): List<UnifiedStory> =
        withContext(Dispatchers.IO) {
            if (Config.serpApiKey.isBlank()) return@withContext emptyList<UnifiedStory>()
            fetchSerpNewsDtos(query, page, pageSize, location, useRawImageUrls)
                .map { dto -> serpApiMapper.toUnifiedStory(dto) }
        }

    private fun fetchSerpNewsDtos(
        query: String,
        page: Int,
        pageSize: Int,
        location: String?,
        useRawImageUrls: Boolean
    ): List<SerpApiStoryDto> {
        val start = page * pageSize
        return fetchSerpNewsDtosByOffset(query, pageSize, start, location, useRawImageUrls)
    }

    private fun fetchSerpNewsDtosByOffset(
        query: String,
        limit: Int,
        offset: Int,
        location: String?,
        useRawImageUrls: Boolean
    ): List<SerpApiStoryDto> {
        fun safeRequest(url: String): String? {
            return runCatching {
                Http.client.newCall(Http.req(url)).execute().use { r ->
                    if (!r.isSuccessful) {
                        Log.w(TAG, "SerpAPI request failed (${r.code}) for $url")
                        return@use null
                    }
                    r.body?.string().orEmpty().ifBlank { null }
                }
            }.getOrElse { err ->
                Log.w(TAG, "SerpAPI request error for $url", err)
                null
            }
        }

        fun extractPublishedAt(json: JSONObject): String? {
            val published = json.opt("published_time")
            val publishedStr = when (published) {
                is String -> published
                is JSONObject -> published.optString("date")
                    .ifBlank { published.optString("iso") }
                    .ifBlank { published.optString("datetime") }
                else -> ""
            }
            return publishedStr
                .ifBlank { json.optString("date") }
                .ifBlank { json.optString("date_published") }
                .ifBlank { null }
        }

        fun parse(jsonStr: String): List<SerpApiStoryDto> {
            if (jsonStr.isBlank()) return emptyList()
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
                val link = j.optString("link").ifBlank { j.optString("url") }
                val img = firstHttp(
                    JSONObject()
                        .put("original", j.opt("original"))
                        .put("high_res", j.opt("high_res"))
                        .put("image", j.opt("image"))
                        .put("thumbnail", j.opt("thumbnail"))
                ).orEmpty()
                if (title.isBlank() || link.isBlank() || img.isBlank()) return@mapNotNull null

                val srcObject = j.opt("source")
                val srcName = when (srcObject) {
                    is JSONObject -> srcObject.optString("name")
                    is String -> srcObject
                    else -> null
                } ?: j.optString("news_site").ifBlank { null }
                val srcLogo = firstHttp(
                    JSONObject()
                        .put("icon", (srcObject as? JSONObject)?.opt("icon"))
                        .put("logo", (srcObject as? JSONObject)?.opt("logo"))
                        .put("thumbnail", (srcObject as? JSONObject)?.opt("thumbnail"))
                        .put("source_logo", j.opt("source_logo"))
                ).orEmpty()

                val age = extractPublishedAt(j)
                val imageUrl = if (useRawImageUrls) img else tryUpscaleCdn(img)
                val logoUrl = srcLogo.ifBlank { favicon(link) }

                SerpApiStoryDto(
                    title = title,
                    url = link,
                    imageUrl = imageUrl,
                    logoUrl = logoUrl,
                    sourceName = srcName,
                    ageLabel = TimeLabelFormatter.formatTimeLabel(age)
                )
            }
        }

        val start = offset.toString()
        val num = limit.toString()

        var out: List<SerpApiStoryDto> = emptyList()

        safeRequest(serpUri(mapOf(
            "engine" to "google_news", "q" to query, "num" to num, "start" to start
        ), location))?.let { out = parse(it) }
        if (out.isNotEmpty()) return out

        safeRequest(serpUri(mapOf(
            "engine" to "bing_news", "q" to query, "count" to num, "first" to start, "cc" to "US"
        ), location))?.let { out = parse(it) }
        if (out.isNotEmpty()) return out

        safeRequest(serpUri(mapOf(
            "engine" to "google", "tbm" to "nws", "q" to query, "num" to num, "start" to start
        ), location))?.let { out = parse(it) }

        return out
    }

    private fun UnifiedStory.toArticle(ageLabel: String?, interest: String): Article {
        return Article(
            title = title,
            url = url,
            imageUrl = imageUrl.orEmpty(),
            logoUrl = publisher?.iconUrl.orEmpty(),
            sourceName = publisher?.name,
            ageLabel = ageLabel,
            interest = interest,
            isFotoscapes = false
        )
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
                    val body = r.body?.string().orEmpty()
                    if (body.isBlank()) return@use
                    val root  = JSONObject(body)
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
            if (str.isBlank()) return emptyList()
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

        val shorts = runCatching {
            Http.client.newCall(
                Http.req(serpUri(mapOf("engine" to "google_videos", "q" to "site:youtube.com/shorts $query")))
            ).execute().use { if (it.isSuccessful) parse(it.body?.string().orEmpty()) else emptyList() }
        }.getOrElse { err ->
            Log.w(TAG, "SerpAPI clips request failed for shorts", err)
            emptyList()
        }

        val general = runCatching {
            Http.client.newCall(
                Http.req(serpUri(mapOf("engine" to "google_videos", "q" to query)))
            ).execute().use { if (it.isSuccessful) parse(it.body?.string().orEmpty()) else emptyList() }
        }.getOrElse { err ->
            Log.w(TAG, "SerpAPI clips request failed for general", err)
            emptyList()
        }

        (shorts + general).distinctBy { it.url }.take(18)
    }

    // Images (SerpAPI)
    suspend fun fetchImages(query: String): List<String> = withContext(Dispatchers.IO) {
        if (Config.serpApiKey.isBlank()) return@withContext emptyList<String>()
        val reqUrl = serpUri(mapOf("engine" to "google_images", "q" to query, "ijn" to "0"))
        val out = mutableListOf<String>()
        runCatching {
            Http.client.newCall(Http.req(reqUrl)).execute().use { r ->
                if (!r.isSuccessful) return@use
                val body = r.body?.string().orEmpty()
                if (body.isBlank()) return@use
                val root = JSONObject(body)
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
        }.onFailure { err ->
            Log.w(TAG, "SerpAPI images request failed", err)
        }
        out
    }

    // People Also Ask + Related (SerpAPI)
    suspend fun fetchExtras(query: String): Extras = withContext(Dispatchers.IO) {
        if (Config.serpApiKey.isBlank()) return@withContext Extras()
        val reqUrl = serpUri(mapOf("engine" to "google", "q" to query))
        var people: List<String> = emptyList()
        var related: List<String> = emptyList()

        runCatching {
            Http.client.newCall(Http.req(reqUrl)).execute().use { r ->
                if (!r.isSuccessful) return@use
                val body = r.body?.string().orEmpty()
                if (body.isBlank()) return@use
                val root = JSONObject(body)

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
        }.onFailure { err ->
            Log.w(TAG, "SerpAPI extras request failed", err)
        }

        Extras(peopleAlsoAsk = people, relatedSearches = related)
    }
}
