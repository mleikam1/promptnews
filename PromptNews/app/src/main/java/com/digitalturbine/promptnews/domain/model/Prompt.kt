package com.digitalturbine.promptnews.domain.model

import java.time.Instant

/**
 * A user prompt captured for news discovery.
 */
data class Prompt(
    /** Stable identifier for the prompt. */
    val id: String = "",
    /** Natural language prompt text. */
    val text: String = "",
    /** Inferred intent for the prompt. */
    val intent: PromptIntent = PromptIntent.SEARCH,
    /** Optional filters that scope the results. */
    val filters: PromptFilters = PromptFilters(),
    /** Timestamp the prompt was created, if known. */
    val createdAt: Instant? = null
)
