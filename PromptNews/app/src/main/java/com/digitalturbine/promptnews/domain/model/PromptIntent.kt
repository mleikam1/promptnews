package com.digitalturbine.promptnews.domain.model

/**
 * High-level intent inferred for a prompt.
 */
enum class PromptIntent {
    /** General search intent. */
    SEARCH,

    /** Request to summarize a topic or story. */
    SUMMARIZE,

    /** Request to explain a concept or event. */
    EXPLAIN,

    /** Request to compare topics or stories. */
    COMPARE,

    /** Request to discover trending or related content. */
    DISCOVER,

    /** Intent could not be determined. */
    UNKNOWN
}
