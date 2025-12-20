package com.digitalturbine.promptnews.util

import java.util.concurrent.TimeUnit
import kotlin.math.max

fun inferInterestFromTitle(title: String): String {
    val t = title.lowercase()
    fun has(re: String) = Regex(re).containsMatchIn(t)
    return when {
        has("\\b(nfl|nba|mlb|nhl|mls|premier league|score|match|final|world cup)\\b") -> "sports"
        has("\\b(ai|startup|app|android|ios|google|apple|microsoft|chip|gpu|openai)\\b") -> "technology"
        has("\\b(stock|market|earnings|ipo|revenue|profit|merger|acquisition|inflation)\\b") -> "business"
        has("\\b(bill|election|congress|parliament|white house|court|supreme)\\b") -> "politics"
        has("\\b(study|research|space|nasa|quantum|physics|biology)\\b") -> "science"
        has("\\b(health|covid|flu|hospital|vaccine)\\b") -> "health"
        has("\\b(game|xbox|playstation|nintendo|steam|fortnite)\\b") -> "gaming"
        has("\\b(forecast|storm|hurricane|tornado|heatwave|rain|snow)\\b") -> "weather"
        has("\\b(movie|film|music|celebrity|oscars|grammys|series|netflix)\\b") -> "entertainment"
        else -> "news"
    }
}

fun ageLabelFrom(publishedAtMillis: Long?): String? {
    if (publishedAtMillis == null) return null
    val diffHrs = max(1, TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - publishedAtMillis).toInt())
    return if (diffHrs <= 12) "$diffHrs hours ago" else "Popular"
}

fun faviconFrom(articleUrl: String?, size: Int = 64): String? {
    if (articleUrl.isNullOrBlank()) return null
    return try {
        val u = java.net.URI(articleUrl)
        "https://www.google.com/s2/favicons?domain=${u.scheme}://${u.host}&sz=$size"
    } catch (_: Exception) { null }
}
