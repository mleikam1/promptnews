package com.digitalturbine.promptnews.domain.model

import java.time.Instant

/**
 * Optional filters applied when resolving a prompt.
 */
data class PromptFilters(
    /** Requested sources to include. Empty means no restriction. */
    val sources: Set<StorySource> = emptySet(),
    /** Preferred publishers by name. */
    val publishers: Set<String> = emptySet(),
    /** Preferred language codes (BCP-47). */
    val languages: Set<String> = emptySet(),
    /** Optional keyword constraints. */
    val keywords: Set<String> = emptySet(),
    /** Start of the time window to search. */
    val fromDate: Instant? = null,
    /** End of the time window to search. */
    val toDate: Instant? = null,
    /** Safety filtering level. */
    val safeMode: SafeMode = SafeMode.MODERATE,
    /** Sort strategy for matching results. */
    val sortMode: SortMode = SortMode.RELEVANCE
)
