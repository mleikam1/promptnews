package com.digitalturbine.promptnews.data.cache

import com.digitalturbine.promptnews.domain.model.CacheStaleness
import java.util.concurrent.TimeUnit

/**
 * Encapsulates cache freshness classification and stale-while-revalidate decisions.
 */
class CachePolicyEngine(
    private val clockMs: () -> Long = System::currentTimeMillis
) {

    enum class CacheBucket {
        HEADLINES,
        PROMPT_SEARCH,
        LOCAL_NEWS
    }

    data class CacheDecision(
        val staleness: CacheStaleness,
        val freshUntilMs: Long,
        val hardExpiresAtMs: Long,
        val shouldRefreshInBackground: Boolean,
        val mustRefresh: Boolean
    )

    private data class CachePolicy(
        val ttlMs: Long,
        val staleWhileRevalidateMs: Long
    )

    private val policies: Map<CacheBucket, CachePolicy> = mapOf(
        CacheBucket.HEADLINES to cachePolicy(minutes = 5),
        CacheBucket.PROMPT_SEARCH to cachePolicy(minutes = 30),
        CacheBucket.LOCAL_NEWS to cachePolicy(minutes = 10)
    )

    fun stalenessFor(
        bucket: CacheBucket,
        fetchedAtMs: Long,
        nowMs: Long = clockMs()
    ): CacheStaleness {
        return decisionFor(bucket, fetchedAtMs, nowMs).staleness
    }

    fun shouldRefreshInBackground(
        bucket: CacheBucket,
        fetchedAtMs: Long,
        nowMs: Long = clockMs()
    ): Boolean {
        return decisionFor(bucket, fetchedAtMs, nowMs).shouldRefreshInBackground
    }

    fun mustRefresh(
        bucket: CacheBucket,
        fetchedAtMs: Long,
        nowMs: Long = clockMs()
    ): Boolean {
        return decisionFor(bucket, fetchedAtMs, nowMs).mustRefresh
    }

    fun decisionFor(
        bucket: CacheBucket,
        fetchedAtMs: Long,
        nowMs: Long = clockMs()
    ): CacheDecision {
        val policy = policies.getValue(bucket)
        val safeNowMs = maxOf(nowMs, fetchedAtMs)
        val freshUntilMs = fetchedAtMs + policy.ttlMs
        val hardExpiresAtMs = freshUntilMs + policy.staleWhileRevalidateMs
        val staleness = when {
            safeNowMs <= freshUntilMs -> CacheStaleness.FRESH
            safeNowMs <= hardExpiresAtMs -> CacheStaleness.SOFT_STALE
            else -> CacheStaleness.HARD_STALE
        }
        return CacheDecision(
            staleness = staleness,
            freshUntilMs = freshUntilMs,
            hardExpiresAtMs = hardExpiresAtMs,
            shouldRefreshInBackground = staleness == CacheStaleness.SOFT_STALE,
            mustRefresh = staleness == CacheStaleness.HARD_STALE
        )
    }

    private fun cachePolicy(minutes: Long): CachePolicy {
        val ttlMs = TimeUnit.MINUTES.toMillis(minutes)
        return CachePolicy(
            ttlMs = ttlMs,
            staleWhileRevalidateMs = ttlMs
        )
    }
}
