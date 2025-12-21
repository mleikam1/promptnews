package com.digitalturbine.promptnews.data.common

import com.digitalturbine.promptnews.domain.merge.StoryProvider

data class RateLimitConfig(
    val capacity: Int,
    val refillTokens: Int,
    val refillPeriodMs: Long
)

class RateLimiter(
    private val limits: Map<StoryProvider, RateLimitConfig>,
    private val clockMs: () -> Long = System::currentTimeMillis
) {
    private data class Bucket(
        var tokens: Int,
        var lastRefillMs: Long
    )

    private val buckets = limits.mapValues { (_, config) ->
        Bucket(tokens = config.capacity, lastRefillMs = clockMs())
    }

    @Synchronized
    fun tryAcquire(provider: StoryProvider): Boolean {
        val config = limits[provider] ?: return true
        val bucket = buckets[provider] ?: return true
        refillBucket(bucket, config)
        if (bucket.tokens <= 0) return false
        bucket.tokens -= 1
        return true
    }

    private fun refillBucket(bucket: Bucket, config: RateLimitConfig) {
        if (config.refillPeriodMs <= 0 || config.refillTokens <= 0) return
        val now = clockMs()
        val elapsed = now - bucket.lastRefillMs
        if (elapsed < config.refillPeriodMs) return
        val periods = (elapsed / config.refillPeriodMs).toInt()
        if (periods <= 0) return
        val refillAmount = periods * config.refillTokens
        bucket.tokens = (bucket.tokens + refillAmount).coerceAtMost(config.capacity)
        bucket.lastRefillMs += periods * config.refillPeriodMs
    }
}
