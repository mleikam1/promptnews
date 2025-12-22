package com.digitalturbine.promptnews.network

import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.Clip
import com.digitalturbine.promptnews.data.Extras
import com.digitalturbine.promptnews.data.SearchRepository
import com.digitalturbine.promptnews.data.net.Http     // <-- fixed import
import com.digitalturbine.promptnews.data.rss.parseGoogleNewsRss
import com.digitalturbine.promptnews.util.ageLabelFrom
import com.digitalturbine.promptnews.util.faviconFrom
import com.digitalturbine.promptnews.util.inferInterestFromTitle
import com.digitalturbine.promptnews.util.toEpochMillisCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder
import kotlin.math.min

/**
 * Thin service layer that talks to SearchRepository and maps RSS to the app's Article model.
 */
object Services {

    private val repo = SearchRepository()

    /** FotoScapes rail (first section). */
    suspend fun fetchFsArticles(query: String, limit: Int = 3): List<Article> =
        repo.fetchFotoScapes(query, limit)

    /** General news via SerpAPI (Google/Bing/Google tbm=nws fallback). */
    suspend fun fetchSerpNews(query: String, page: Int = 0, pageSize: Int = 20): List<Article> =
        repo.fetchSerpNews(query, page, pageSize)

    /** Short clips (YouTube first, SerpAPI fallback). */
    suspend fun fetchClips(query: String): List<Clip> =
        repo.fetchClips(query)

    /** Image gallery for the query. */
    suspend fun fetchImages(query: String): List<String> =
        repo.fetchImages(query)

    /** People Also Ask + Similar searches. */
    suspend fun fetchPaaAndRelated(query: String): Extras =
        repo.fetchExtras(query)

    /**
     * Local/keyword news via Google News RSS, mapped into the unified Article model.
     */
    suspend fun fetchGoogleNewsRss(query: String, limit: Int = 20): MutableList<Article> =
        withContext(Dispatchers.IO) {
            val rssUrl =
                "https://news.google.com/rss/search?q=${URLEncoder.encode(query, "UTF-8")}&hl=en-US&gl=US&ceid=US:en"

            Http.client.newCall(Http.req(rssUrl)).execute().use { res ->
                if (!res.isSuccessful) return@withContext mutableListOf<Article>()
                val xml = res.body?.string().orEmpty()
                val rssItems = parseGoogleNewsRss(xml)

                val out = mutableListOf<Article>()
                for (it in rssItems) {
                    val age = it.published?.toEpochMillisCompat()?.let { ms -> ageLabelFrom(ms) }
                    out += Article(
                        title = it.title,
                        url = it.link,
                        imageUrl = it.imageUrl.orEmpty(),
                        logoUrl = faviconFrom(it.link).orEmpty(),
                        sourceName = it.source.ifBlank { null },
                        ageLabel = age,
                        interest = inferInterestFromTitle(it.title),
                        isFotoscapes = false
                    )
                    if (out.size >= limit) break
                }
                out
            }
        }

    /** Optional: enrich a few items with OG/Twitter images if the feed lacked one. */
    suspend fun enrichOgImages(items: MutableList<Article>, limit: Int = 6) =
        withContext(Dispatchers.IO) {
            for (i in 0 until min(items.size, limit)) {
                val a = items[i]
                if (a.imageUrl.isNotBlank()) continue
                runCatching {
                    val doc = Jsoup.connect(a.url)
                        .userAgent("PromptNews/1.0 (Android)")
                        .timeout(10_000)
                        .get()

                    fun og(name: String) =
                        doc.selectFirst("""meta[property="$name"], meta[name="$name"]""")
                            ?.attr("content")

                    val img = og("og:image") ?: og("twitter:image")
                    if (!img.isNullOrBlank()) {
                        items[i] = a.copy(imageUrl = img)
                    }
                }
            }
        }
}
