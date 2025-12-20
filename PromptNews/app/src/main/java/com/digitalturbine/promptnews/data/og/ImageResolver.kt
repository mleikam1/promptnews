package com.digitalturbine.promptnews.data.og

import android.util.LruCache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

/**
 * Very small HTML scraper to resolve a representative image for an article:
 * - meta[property=og:image], meta[name=twitter:image]
 * - JSON-LD "image"
 * - fallback: first <img> with plausible URL
 *
 * Results are cached in a tiny in-memory LRU. OkHttp provides disk cache.
 */
class ImageResolver(private val http: OkHttpClient) {

    private val cache = object : LruCache<String, String>(100) {}

    fun resolve(url: String): String? {
        cache.get(url)?.let { return it }
        return runCatching {
            val req = Request.Builder().url(url).get().build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val html = resp.body?.string() ?: return null
                val doc = Jsoup.parse(html, url)

                // 1) OpenGraph / Twitter
                val og = metaContent(doc, "property", "og:image")
                    ?: metaContent(doc, "name", "twitter:image")
                if (!og.isNullOrBlank()) return og.also { cache.put(url, it) }

                // 2) JSON-LD
                jsonLdImage(doc)?.let { img ->
                    return img.also { cache.put(url, it) }
                }

                // 3) Fallback: first <img> with absolute http(s)
                doc.select("img[src]").firstOrNull { it.absUrl("src").startsWith("http") }?.let {
                    val v = it.absUrl("src")
                    if (v.isNotBlank()) {
                        cache.put(url, v)
                        return v
                    }
                }
                null
            }
        }.getOrNull()
    }

    private fun metaContent(doc: Document, attr: String, key: String): String? {
        val els: Elements = doc.select("meta[$attr=$key], meta[$attr=${key}:secure_url]")
        return els.firstOrNull()?.attr("content")?.takeIf { it.isNotBlank() }
    }

    private fun jsonLdImage(doc: Document): String? {
        val scripts = doc.select("script[type=application/ld+json]")
        scripts.forEach { el ->
            val raw = el.data()
            runCatching {
                val any = org.json.JSONTokener(raw).nextValue()
                when (any) {
                    is JSONObject -> extractFromJson(any)
                    is JSONArray -> (0 until any.length()).asSequence()
                        .mapNotNull { extractFromJson(any.optJSONObject(it)) }
                        .firstOrNull()
                    else -> null
                }?.let { return it }
            }
        }
        return null
    }

    private fun extractFromJson(o: JSONObject?): String? {
        if (o == null) return null
        // direct "image" may be string, array, or object {url:...}
        when (val img = o.opt("image")) {
            is String -> return img
            is JSONArray -> {
                for (i in 0 until img.length()) {
                    val s = img.optString(i)
                    if (s.isNotBlank()) return s
                }
            }
            is JSONObject -> {
                val u = img.optString("url")
                if (u.isNotBlank()) return u
            }
        }
        // nested "thumbnailUrl" or "thumbnail"
        o.optString("thumbnailUrl").takeIf { it.isNotBlank() }?.let { return it }
        o.optString("thumbnail").takeIf { it.isNotBlank() }?.let { return it }
        return null
    }
}
