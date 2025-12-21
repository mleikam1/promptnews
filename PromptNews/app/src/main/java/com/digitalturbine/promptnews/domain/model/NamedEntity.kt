package com.digitalturbine.promptnews.domain.model

/**
 * Named entity extracted from a story.
 */
data class NamedEntity(
    /** Entity text as it appears in content. */
    val name: String = "",
    /** Entity type label (e.g., person, location). */
    val type: String = "unknown",
    /** Relevance score, 0.0 - 1.0. */
    val relevance: Double = 0.0
)
