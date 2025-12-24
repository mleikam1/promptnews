package com.digitalturbine.promptnews.data.sports

import android.net.Uri
import com.digitalturbine.promptnews.data.net.Http
import com.digitalturbine.promptnews.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SportsRepository {
    suspend fun fetchSportsResults(query: String): SportsResults? =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isBlank()) return@withContext null
            val baseUrl = Config.sportsApiBaseUrl
            if (baseUrl.isBlank()) return@withContext null

            val url = Uri.parse(baseUrl).buildUpon()
                .appendPath("api")
                .appendPath("sports")
                .appendQueryParameter("s", trimmed)
                .build()
                .toString()

            val response = runCatching {
                Http.client.newCall(Http.req(url)).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    resp.body?.string().orEmpty().ifBlank { null }
                }
            }.getOrNull()

            response?.let { SportsParser.parse(it) }
        }
}
