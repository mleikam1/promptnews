package com.digitalturbine.promptnews.domain.model

import java.time.Instant

/**
 * Normalized story representation for UI and domain use cases.
 */
data class UnifiedStory(
    /** Stable identifier for the story. */
    val id: String = "",
    /** Title or headline. */
    val title: String = "",
    /** Optional summary or snippet. */
    val summary: String? = null,
    /** Canonical URL for the story. */
    val url: String = "",
    /** Source channel for the story. */
    val source: StorySource = StorySource.UNKNOWN,
    /** Publisher metadata. */
    val publisher: Publisher? = null,
    /** Story publish timestamp, if known. */
    val publishedAt: Instant? = null,
    /** Optional hero or preview image URL. */
    val imageUrl: String? = null,
    /** Extracted sentiment metadata. */
    val sentiment: Sentiment? = null,
    /** Extracted entities for enrichment. */
    val namedEntities: List<NamedEntity> = emptyList(),
    /** Suggested follow-on prompts. */
    val relatedPrompts: List<RelatedPrompt> = emptyList(),
    /** Optional topical tags. */
    val tags: Set<String> = emptySet()
)
