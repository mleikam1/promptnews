package com.digitalturbine.promptnews.domain.model

/**
 * Signals how fresh cached results are.
 */
enum class CacheStaleness {
    FRESH,
    WARM,
    STALE,
    EXPIRED
}
