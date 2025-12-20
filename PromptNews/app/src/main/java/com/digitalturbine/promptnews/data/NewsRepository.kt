package com.digitalturbine.promptnews.data

import android.content.Context
import com.digitalturbine.promptnews.data.net.Http
import com.digitalturbine.promptnews.data.og.ImageResolver
import com.digitalturbine.promptnews.data.rss.GoogleNewsRss
import com.digitalturbine.promptnews.data.rss.YoutubeRss
import com.digitalturbine.promptnews.data.suggest.GoogleSuggest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Lean repository:
 *  - Google News RSS
 *  - Missing image enrichment via OG/Twitter/JSON-LD
 *  - YouTube RSS for clips
 *  - Google Autocomplete for "Dive Deeper"
 */
class NewsRepository(context: Context) {

    private val http = Http.client
    private val rss = GoogleNewsRss(http)
    private val yt = YoutubeRss(http)
    private val suggest = GoogleSuggest(http)
    private val imageResolver = ImageResolver(http)

    suspend fun searchAll(query: String): SearchUi.Ready = withContext(Dispatchers.IO) {
        // 1) News
        val baseArticles: List<Article> = rss.search(query, limit = 30)

        // 2) Enrich missing images (capped concurrency)
        val enrichedArticles = enrichImages(baseArticles)

        // 3) Rails
        val imagesRail: List<String> = enrichedArticles.asSequence()
            .map { it.imageUrl }
            .filter { it.isNotBlank() }
            .distinct()
            .take(14)
            .toList()

        val clips: List<Clip> = yt.search(query, limit = 12)

        val peopleAlsoAsk: List<String> = suggest.suggestions(query, limit = 8)
        val related: List<String> = suggest.suggestions("$query news", limit = 8)
            .filterNot { it in peopleAlsoAsk }
            .take(8)

        // 4) Hero + 4 rows
        val hero = enrichedArticles.firstOrNull()
        val rows = if (enrichedArticles.size > 1) enrichedArticles.drop(1).take(4) else emptyList()
        val hasMore = enrichedArticles.size > (1 + rows.size)

        SearchUi.Ready(
            query = query,
            hero = hero,
            rows = rows,
            hasMore = hasMore,
            clips = clips,
            images = imagesRail,
            extras = Extras(
                peopleAlsoAsk = peopleAlsoAsk,
                relatedSearches = related
            )
        )
    }

    private suspend fun enrichImages(articles: List<Article>): List<Article> = coroutineScope {
        val gate = Semaphore(4)
        articles.map { a ->
            if (a.imageUrl.isNotBlank()) return@map async { a }
            async(Dispatchers.IO) {
                gate.withPermit {
                    val resolved = imageResolver.resolve(a.url)
                    if (resolved.isNullOrBlank()) a else a.copy(imageUrl = resolved)
                }
            }
        }.awaitAll()
    }
}
