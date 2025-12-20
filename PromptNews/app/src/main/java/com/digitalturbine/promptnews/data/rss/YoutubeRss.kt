package com.digitalturbine.promptnews.data.rss

import com.digitalturbine.promptnews.data.Clip
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * YouTube public search Atom feed:
 *   https://www.youtube.com/feeds/videos.xml?search_query=<query>
 *
 * We build a thumbnail from the videoId:
 *   https://i.ytimg.com/vi/<id>/hqdefault.jpg
 *
 * Matches your data models in package com.digitalturbine.promptnews.data (Clip).
 */
class YoutubeRss(private val http: OkHttpClient) {

    fun search(query: String, limit: Int = 12): List<Clip> {
        val url = "https://www.youtube.com/feeds/videos.xml?search_query=" +
                URLEncoder.encode(query, "UTF-8")

        val req = Request.Builder().url(url).get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val stream = resp.body?.byteStream() ?: return emptyList()

            val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            val doc = factory.newDocumentBuilder().parse(stream)

            val entries = doc.getElementsByTagName("entry")
            val out = ArrayList<Clip>(minOf(limit, entries.length))

            for (i in 0 until entries.length) {
                val entry = entries.item(i) as? Element ?: continue

                val title = entry.firstText("title") ?: continue

                // <link href="...">
                val linkHref =
                    entry.firstElement("link")?.getAttribute("href")?.takeIf { it.isNotBlank() }
                        ?: entry.firstElementNS(ATOM_NS, "link")?.getAttribute("href")?.takeIf { it.isNotBlank() }
                        ?: continue

                // yt:videoId (namespaced) – try common namespaces
                val videoId =
                    entry.firstText("yt:videoId")
                        ?: entry.firstTextNS(YT_NS, "videoId")
                        ?: entry.firstTextNS(GDATA_NS, "videoId")

                // Try deriving a thumbnail from videoId; fallback to media:thumbnail if present
                val derivedThumb = videoId?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }
                val mediaThumb = entry.firstAttr("media:thumbnail", "url")
                    ?: entry.firstAttrNS(MRSS_NS, "thumbnail", "url")

                val thumbnail = (derivedThumb ?: mediaThumb) ?: ""

                out.add(
                    Clip(
                        title = title.trim(),
                        url = linkHref,
                        thumbnail = thumbnail,
                        source = "YouTube"
                    )
                )
                if (out.size >= limit) break
            }
            return out
        }
    }

    private companion object {
        const val ATOM_NS = "http://www.w3.org/2005/Atom"
        const val YT_NS = "http://www.youtube.com/xml/schemas/2015"
        const val GDATA_NS = "http://gdata.youtube.com/schemas/2007"
        const val MRSS_NS = "http://search.yahoo.com/mrss/"
    }
}

/* ---------------- XML helpers ---------------- */

private fun Element.firstElement(tag: String): Element? {
    val nodes = this.getElementsByTagName(tag)
    for (i in 0 until nodes.length) {
        val n = nodes.item(i)
        if (n.nodeType == Node.ELEMENT_NODE) return n as Element
    }
    return null
}

private fun Element.firstElementNS(ns: String, local: String): Element? {
    val nodes = this.getElementsByTagNameNS(ns, local)
    if (nodes.length == 0) return null
    val n = nodes.item(0)
    return if (n.nodeType == Node.ELEMENT_NODE) n as Element else null
}

private fun Element.firstText(tag: String): String? =
    firstElement(tag)?.textContent?.trim()?.ifBlank { null }

private fun Element.firstTextNS(ns: String, local: String): String? =
    firstElementNS(ns, local)?.textContent?.trim()?.ifBlank { null }

private fun Element.firstAttr(tag: String, attr: String): String? =
    firstElement(tag)?.getAttribute(attr)?.takeIf { it.isNotBlank() }

private fun Element.firstAttrNS(ns: String, local: String, attr: String): String? =
    firstElementNS(ns, local)?.getAttribute(attr)?.takeIf { it.isNotBlank() }
