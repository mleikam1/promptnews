package com.digitalturbine.promptnews.domain.merge

import com.digitalturbine.promptnews.domain.model.NamedEntity
import com.digitalturbine.promptnews.domain.model.Publisher
import com.digitalturbine.promptnews.domain.model.RelatedPrompt
import com.digitalturbine.promptnews.domain.model.Sentiment
import com.digitalturbine.promptnews.domain.model.StorySource
import com.digitalturbine.promptnews.domain.model.UnifiedStory
import java.security.MessageDigest
import java.time.Instant

enum class StoryProvider {
    NEWSCATCHER,
    SERPAPI
}

data class MergeableStory(
    val canonicalUrl: String,
    val title: String? = null,
    val summary: String? = null,
    val provider: StoryProvider,
    val providerRank: Int,
    val publisher: Publisher? = null,
    val publishedAt: Instant? = null,
    val imageUrl: String? = null,
    val sentiment: Sentiment? = null,
    val namedEntities: List<NamedEntity> = emptyList(),
    val relatedPrompts: List<RelatedPrompt> = emptyList(),
    val tags: Set<String> = emptySet()
)

data class StoryMergeMetadata(
    val newscatcherCount: Int,
    val serpApiCount: Int,
    val totalCount: Int,
    val uniqueCount: Int,
    val duplicateCount: Int
)

data class StoryMergeResult(
    val stories: List<UnifiedStory>,
    val metadata: StoryMergeMetadata
)

class StoryMergeEngine {
    fun mergeStories(
        newscatcherStories: List<MergeableStory>,
        serpApiStories: List<MergeableStory>
    ): StoryMergeResult {
        val allStories = newscatcherStories + serpApiStories
        val grouped = allStories
            .filter { it.canonicalUrl.isNotBlank() }
            .groupBy { canonicalUrlHash(it.canonicalUrl) }

        val mergedStories = grouped.values.mapNotNull { candidates ->
            val primary = candidates.firstOrNull { it.provider == StoryProvider.NEWSCATCHER }
            val fallback = candidates.firstOrNull { it.provider == StoryProvider.SERPAPI }
            val chosen = primary ?: fallback ?: candidates.firstOrNull()
            chosen?.let {
                mergeCandidates(it, primary, fallback)
            }
        }

        val sortedStories = mergedStories
            .sortedWith(
                compareBy<MergedStory> { it.providerRank }
                    .thenByDescending { it.story.publishedAt ?: Instant.EPOCH }
            )
            .map { it.story }

        val metadata = StoryMergeMetadata(
            newscatcherCount = newscatcherStories.size,
            serpApiCount = serpApiStories.size,
            totalCount = allStories.size,
            uniqueCount = mergedStories.size,
            duplicateCount = allStories.size - mergedStories.size
        )

        return StoryMergeResult(
            stories = sortedStories,
            metadata = metadata
        )
    }

    private fun mergeCandidates(
        chosen: MergeableStory,
        newscatcher: MergeableStory?,
        serpApi: MergeableStory?
    ): MergedStory {
        val canonicalUrl = chosen.canonicalUrl
        val id = canonicalUrlHash(canonicalUrl)
        val title = preferText(newscatcher?.title, serpApi?.title, chosen.title)
        val summary = preferText(newscatcher?.summary, serpApi?.summary, chosen.summary)
        val publisher = preferValue(newscatcher?.publisher, serpApi?.publisher, chosen.publisher)
        val publishedAt = preferValue(newscatcher?.publishedAt, serpApi?.publishedAt, chosen.publishedAt)
        val imageUrl = preferText(newscatcher?.imageUrl, serpApi?.imageUrl, chosen.imageUrl)
        val sentiment = preferValue(newscatcher?.sentiment, serpApi?.sentiment, chosen.sentiment)
        val namedEntities = preferList(newscatcher?.namedEntities, serpApi?.namedEntities, chosen.namedEntities)
        val relatedPrompts = preferList(newscatcher?.relatedPrompts, serpApi?.relatedPrompts, chosen.relatedPrompts)
        val tags = preferSet(newscatcher?.tags, serpApi?.tags, chosen.tags)
        val providerRank = listOfNotNull(newscatcher?.providerRank, serpApi?.providerRank, chosen.providerRank).minOrNull()
            ?: chosen.providerRank

        val unifiedStory = UnifiedStory(
            id = id,
            title = title,
            summary = summary,
            url = canonicalUrl,
            source = StorySource.API,
            publisher = publisher,
            publishedAt = publishedAt,
            imageUrl = imageUrl,
            sentiment = sentiment,
            namedEntities = namedEntities,
            relatedPrompts = relatedPrompts,
            tags = tags
        )

        return MergedStory(
            story = unifiedStory,
            providerRank = providerRank
        )
    }

    private fun canonicalUrlHash(canonicalUrl: String): String {
        val normalized = canonicalUrl
            .trim()
            .lowercase()
            .trimEnd { it == '/' }
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun preferText(primary: String?, fallback: String?, defaultValue: String?): String {
        return listOf(primary, fallback, defaultValue)
            .firstOrNull { !it.isNullOrBlank() }
            .orEmpty()
    }

    private fun <T> preferValue(primary: T?, fallback: T?, defaultValue: T?): T? {
        return primary ?: fallback ?: defaultValue
    }

    private fun <T> preferList(primary: List<T>?, fallback: List<T>?, defaultValue: List<T>): List<T> {
        return when {
            !primary.isNullOrEmpty() -> primary
            !fallback.isNullOrEmpty() -> fallback
            else -> defaultValue
        }
    }

    private fun <T> preferSet(primary: Set<T>?, fallback: Set<T>?, defaultValue: Set<T>): Set<T> {
        return when {
            !primary.isNullOrEmpty() -> primary
            !fallback.isNullOrEmpty() -> fallback
            else -> defaultValue
        }
    }

    private data class MergedStory(
        val story: UnifiedStory,
        val providerRank: Int
    )
}
