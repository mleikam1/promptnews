package com.digitalturbine.promptnews.data.rss

import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.util.TimeLabelFormatter
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Element
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Google News RSS → your Article model.
 * We fill:
 *  - title, url
 *  - imageUrl (from media:content/enclosure when present, else "")
 *  - sourceName (from <source>)
 *  - ageLabel (relative time if today, else "Popular")
 *  - logoUrl = "" (leave for your UI to map if desired)
 *  - isFotoscapes = false
 */
class GoogleNewsRss(private val http: OkHttpClient) {

    private fun endpoint(q: String) =
        "https://news.google.com/rss/search?q=${URLEncoder.encode(q, "UTF-8")}&hl=en-US&gl=US&ceid=US:en"

    fun search(q: String, limit: Int = 30): List<Article> {
        val req = Request.Builder().url(endpoint(q)).get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.byteStream() ?: return emptyList()

            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(body)
            val items = doc.getElementsByTagName("item")

            val out = ArrayList<Article>(minOf(limit, items.length))
            for (i in 0 until items.length) {
                val node = items.item(i) as? Element ?: continue
                val title = node.text("title") ?: continue
                val link = node.text("link") ?: continue
                val source = node.text("source")
                val pub = node.text("pubDate")
                val epoch = pub?.let { parseRfc822(it) }
                val age = TimeLabelFormatter.formatTimeLabel(epoch)

                val img = node.attr("media:content", "url")
                    ?: node.attr("media:thumbnail", "url")
                    ?: node.attr("enclosure", "url")
                    ?: ""

                out += Article(
                    title = title.stripTags(),
                    url = link,
                    imageUrl = img,
                    logoUrl = "",
                    sourceName = source?.stripTags(),
                    ageLabel = age,
                    isFotoscapes = false
                )

                if (out.size >= limit) break
            }
            return out
        }
    }

    /* ---------- helpers ---------- */

    private fun Element.text(tag: String): String? =
        (getElementsByTagName(tag).item(0) as? Element)?.textContent?.trim()?.ifBlank { null }

    private fun Element.attr(tag: String, attr: String): String? {
        val nodes = getElementsByTagName(tag)
        for (i in 0 until nodes.length) {
            val e = nodes.item(i) as? Element ?: continue
            val v = e.getAttribute(attr)
            if (!v.isNullOrBlank()) return v
        }
        return null
    }

    private fun String.stripTags(): String = replace(Regex("<.*?>"), "").trim()

    private fun parseRfc822(s: String): Long? {
        val fmts = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm Z"
        )
        for (f in fmts) {
            try {
                val sdf = SimpleDateFormat(f, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                val d: Date? = sdf.parse(s)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return null
    }

}
