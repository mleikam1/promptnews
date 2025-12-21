package com.digitalturbine.promptnews.domain.model

/**
 * Publisher metadata for a story.
 */
data class Publisher(
    /** Display name of the publisher. */
    val name: String = "",
    /** Host/domain of the publisher, if available. */
    val domain: String? = null,
    /** URL to a favicon or brand image. */
    val iconUrl: String? = null,
    /** Optional credibility score, 0.0 - 1.0. */
    val credibilityScore: Double? = null
)
