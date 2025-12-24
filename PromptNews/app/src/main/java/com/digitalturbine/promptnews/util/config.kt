package com.digitalturbine.promptnews.util

import com.digitalturbine.promptnews.BuildConfig
import com.digitalturbine.promptnews.data.common.RateLimitConfig
import com.digitalturbine.promptnews.domain.merge.StoryProvider

/**
 * Centralized configuration and API keys.
 *
 * We *try* to read keys from BuildConfig (if they're defined with buildConfigField),
 * but we don't depend on those symbols at compile time. If not present or blank,
 * we fall back to the local constants below so the app still runs.
 */
object Config {

    // ---- Local fallbacks (your current keys) --------------------------------
    private const val SERP_API_FALLBACK: String =
        "d314d50129060440c039a90701193541056ee3f0d11da024d9a3a8918a479773"

    private const val YT_API_FALLBACK: String =
        "AIzaSyAxxe3jz-SAK7AQE4tCDPv31pMXN6qexg0"

    private const val SPORTS_API_BASE_URL_FALLBACK: String =
        "http://10.0.2.2:8080"

    // ---- Helper: read optional BuildConfig fields safely --------------------
    private fun buildConfigStringOrNull(fieldName: String): String? = runCatching {
        val field = BuildConfig::class.java.getField(fieldName) // may not exist
        (field.get(null) as? String)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    // ---- Public accessors used throughout the app --------------------------
    /** SerpAPI key; empty string disables SerpAPI-backed features. */
    val serpApiKey: String
        get() = buildConfigStringOrNull("SERPAPI_KEY") ?: SERP_API_FALLBACK

    /** YouTube Data API key; empty string disables YouTube-backed clips. */
    val youtubeApiKey: String
        get() = buildConfigStringOrNull("YT_API_KEY") ?: YT_API_FALLBACK

    /** Base URL for the Sports backend; empty string disables sports scores. */
    val sportsApiBaseUrl: String
        get() = buildConfigStringOrNull("SPORTS_API_BASE_URL") ?: SPORTS_API_BASE_URL_FALLBACK

    /** Feature flag: merge SerpAPI YouTube engine results into the Clips rail. */
    const val USE_SERP_FOR_YOUTUBE_BOOST: Boolean = true

    val providerRateLimits: Map<StoryProvider, RateLimitConfig> = mapOf(
        StoryProvider.SERPAPI to RateLimitConfig(
            capacity = 60,
            refillTokens = 60,
            refillPeriodMs = 60_000L
        ),
        StoryProvider.NEWSCATCHER to RateLimitConfig(
            capacity = 30,
            refillTokens = 30,
            refillPeriodMs = 60_000L
        )
    )
}
