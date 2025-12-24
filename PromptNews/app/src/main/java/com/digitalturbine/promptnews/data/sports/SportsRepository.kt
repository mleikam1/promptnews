package com.digitalturbine.promptnews.data.sports

import android.net.Uri
import android.util.Log
import com.digitalturbine.promptnews.data.net.Http
import com.digitalturbine.promptnews.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SportsRepository {
    suspend fun fetchSportsResults(query: String): SportsResults? =
        withContext(Dispatchers.IO) {
            val normalized = query.trim().lowercase()
            if (normalized.isBlank()) return@withContext null
            val baseUrl = Config.sportsApiBaseUrl
            if (baseUrl.isBlank()) return@withContext null

            val url = Uri.parse(baseUrl).buildUpon()
                .appendPath("api")
                .appendPath("sports")
                .appendQueryParameter("s", normalized)
                .build()
                .toString()

            val response = runCatching {
                Http.client.newCall(Http.req(url)).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    resp.body?.string().orEmpty().ifBlank { null }
                }
            }.getOrNull()

            Log.d(TAG, "Sports API response: $response")

            response?.let { SportsParser.parse(it) }
        }

    private companion object {
        const val TAG = "SportsRepository"
    }
}
