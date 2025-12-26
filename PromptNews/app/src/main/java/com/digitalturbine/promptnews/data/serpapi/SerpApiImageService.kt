package com.digitalturbine.promptnews.data.serpapi

import android.net.Uri
import android.util.Log
import com.digitalturbine.promptnews.data.net.Http
import com.digitalturbine.promptnews.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SerpApiImageService {
    companion object {
        private const val TAG = "SerpApiImageService"
    }

    suspend fun fetchFirstImageUrl(query: String): String? = withContext(Dispatchers.IO) {
        if (Config.serpApiKey.isBlank()) return@withContext null
        val reqUrl = serpUri(mapOf("engine" to "google_images", "q" to query, "ijn" to "0"))
        runCatching {
            Http.client.newCall(Http.req(reqUrl)).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@use null
                val root = JSONObject(body)
                val arr = root.optJSONArray("images_results") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val url = firstHttp(
                        JSONObject()
                            .put("original", item.opt("original"))
                            .put("image", item.opt("image"))
                            .put("thumbnail", item.opt("thumbnail"))
                    )
                    if (!url.isNullOrBlank()) return@use tryUpscaleCdn(url)
                }
                null
            }
        }.onFailure { err ->
            Log.w(TAG, "SerpAPI image request failed", err)
        }.getOrNull()
    }

    private fun serpUri(params: Map<String, String>): String {
        val key = Config.serpApiKey
        return Uri.parse("https://serpapi.com/search.json").buildUpon().apply {
            appendQueryParameter("api_key", key)
            appendQueryParameter("hl", "en")
            appendQueryParameter("gl", "us")
            params.forEach { (k, v) -> appendQueryParameter(k, v) }
        }.build().toString()
    }

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
}
