package com.digitalturbine.promptnews.domain.model

import java.time.Instant

/**
 * Aggregated response for a prompt lookup.
 */
data class PromptResultBundle(
    /** Prompt used to generate the results. */
    val prompt: Prompt = Prompt(),
    /** Stories matching the prompt. */
    val stories: List<UnifiedStory> = emptyList(),
    /** Related prompt suggestions. */
    val relatedPrompts: List<RelatedPrompt> = emptyList(),
    /** Cache freshness indicator. */
    val cacheStaleness: CacheStaleness = CacheStaleness.FRESH,
    /** Timestamp the bundle was generated. */
    val generatedAt: Instant? = null,
    /** Optional token for pagination. */
    val nextPageToken: String? = null
)
