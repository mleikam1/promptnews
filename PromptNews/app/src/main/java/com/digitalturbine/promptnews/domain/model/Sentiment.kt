package com.digitalturbine.promptnews.domain.model

/**
 * Sentiment score associated with a story.
 */
data class Sentiment(
    /** Numeric sentiment score, -1.0 (negative) to 1.0 (positive). */
    val score: Double = 0.0,
    /** Human-friendly sentiment label. */
    val label: String = "neutral",
    /** Confidence score for the sentiment, 0.0 - 1.0. */
    val confidence: Double = 0.0
)
