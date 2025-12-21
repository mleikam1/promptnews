package com.digitalturbine.promptnews.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey
    val storyId: String,
    val canonicalUrl: String,
    val title: String,
    val summary: String?,
    val source: String,
    val publisher: PublisherEntity?,
    val publishedAtMs: Long?,
    val imageUrl: String?,
    val sentiment: SentimentEntity?,
    val namedEntities: List<NamedEntityEntity>,
    val relatedPrompts: List<RelatedPromptEntity>,
    val tags: List<String>
)

data class PublisherEntity(
    val name: String,
    val domain: String?,
    val iconUrl: String?,
    val credibilityScore: Double?
)

data class SentimentEntity(
    val score: Double,
    val label: String,
    val confidence: Double
)

data class NamedEntityEntity(
    val name: String,
    val type: String,
    val relevance: Double
)
