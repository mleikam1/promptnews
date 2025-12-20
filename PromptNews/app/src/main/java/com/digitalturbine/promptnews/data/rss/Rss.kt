package com.digitalturbine.promptnews.data.rss

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Article(
    val title: String,
    val source: String,
    val link: String,
    val imageUrl: String?,
    val published: Instant?
)

/**
 * Some feeds return RFC1123 with textual zones (GMT) and some use numeric offsets.
 * We try a few common variants to be resilient.
 */
private val RFC1123_FORMATTERS: List<DateTimeFormatter> = listOf(
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm zzz", Locale.US),
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss xxxx", Locale.US), // numeric offset
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm xxxx", Locale.US)
)

private fun parsePubDate(raw: String): Instant? {
    for (fmt in RFC1123_FORMATTERS) {
        try {
            return ZonedDateTime.parse(raw, fmt).toInstant()
        } catch (_: Throwable) {
            // try next format
        }
    }
    return null
}

internal fun parseGoogleNewsRss(xml: String): List<Article> {
    val doc: Document = Jsoup.parse(xml, "", Parser.xmlParser())

    return doc.select("item").map { item ->
        val title = item.selectFirst("title")?.text().orEmpty()
        val link = item.selectFirst("link")?.text().orEmpty()

        val source = item.selectFirst("source")?.text()
            ?: item.selectFirst("dc|creator")?.text().orEmpty()

        val pub = item.selectFirst("pubDate")?.text()?.let(::parsePubDate)

        // Google News often embeds an <img> inside <description>.
        val imageFromDesc = item.selectFirst("description")?.let { d ->
            val html = Jsoup.parse(d.text())
            html.selectFirst("img")?.attr("src")
        }

        // Some feeds provide <media:content url="...">
        val imageFromMedia = item.selectFirst("media|content")?.attr("url")

        Article(
            title = title,
            source = source,
            link = link,
            imageUrl = imageFromMedia ?: imageFromDesc,
            published = pub
        )
    }
}
