package com.digitalturbine.promptnews.domain.model

/**
 * Suggested prompt derived from a result set.
 */
data class RelatedPrompt(
    /** Suggested query text. */
    val text: String = "",
    /** Classification for the related prompt. */
    val type: RelatedType = RelatedType.SUGGESTION,
    /** Relative score, 0.0 - 1.0. */
    val score: Double = 0.0
)
