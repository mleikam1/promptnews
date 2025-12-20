package com.digitalturbine.promptnews.data.rss

import com.digitalturbine.promptnews.data.net.Http   // <-- fixed import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

object GoogleNewsRepository {

    private const val BASE = "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en"

    private fun localUrl(location: String): String {
        val geo = URLEncoder.encode(location, "UTF-8")
        return "https://news.google.com/rss/headlines/section/geo/$geo?hl=en-US&gl=US&ceid=US:en"
    }

    /** Top stories for the US. */
    suspend fun topStories(): List<Article> = fetch(BASE)

    /** Local news by city/state name like "Overland Park, Kansas". */
    suspend fun localNews(location: String): List<Article> = fetch(localUrl(location))

    private suspend fun fetch(url: String): List<Article> = withContext(Dispatchers.IO) {
        Http.client.newCall(Http.req(url)).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            parseGoogleNewsRss(body)
        }
    }
}
