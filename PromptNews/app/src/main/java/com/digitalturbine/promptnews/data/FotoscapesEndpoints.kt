package com.digitalturbine.promptnews.data

import android.net.Uri

object FotoscapesEndpoints {
    private const val CKEY = "fb529d256155b9c6"
    private const val BASE_DAILY_ENDPOINT = "https://fotoscapes.com/wp/v1/daily"
    private const val BASE_CONTENT_ENDPOINT = "https://fotoscapes.com/wp/v1/content"

    fun dailyEndpoint(sched: String): String = Uri.parse(BASE_DAILY_ENDPOINT).buildUpon()
        .appendQueryParameter("ckey", CKEY)
        .appendQueryParameter("sched", sched)
        .build()
        .toString()

    fun contentEndpoint(
        category: String,
        limit: Int,
        schedule: String,
        geo: String?
    ): String = Uri.parse(BASE_CONTENT_ENDPOINT).buildUpon()
        .appendQueryParameter("ckey", CKEY)
        .appendQueryParameter("category", category)
        .appendQueryParameter("limit", limit.toString())
        .appendQueryParameter("sched", schedule)
        .apply {
            if (!geo.isNullOrBlank()) {
                appendQueryParameter("geo", geo)
            }
        }
        .build()
        .toString()
}
