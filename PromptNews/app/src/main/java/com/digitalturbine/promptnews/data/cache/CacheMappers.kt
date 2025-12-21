package com.digitalturbine.promptnews.data.cache

import com.digitalturbine.promptnews.data.common.HashUtils
import com.digitalturbine.promptnews.data.common.UrlCanonicalizer
import com.digitalturbine.promptnews.domain.model.CacheStaleness
import com.digitalturbine.promptnews.domain.model.NamedEntity
import com.digitalturbine.promptnews.domain.model.Prompt
import com.digitalturbine.promptnews.domain.model.PromptFilters
import com.digitalturbine.promptnews.domain.model.PromptIntent
import com.digitalturbine.promptnews.domain.model.PromptResultBundle
import com.digitalturbine.promptnews.domain.model.Publisher
import com.digitalturbine.promptnews.domain.model.RelatedPrompt
import com.digitalturbine.promptnews.domain.model.RelatedType
import com.digitalturbine.promptnews.domain.model.SafeMode
import com.digitalturbine.promptnews.domain.model.Sentiment
import com.digitalturbine.promptnews.domain.model.SortMode
import com.digitalturbine.promptnews.domain.model.StorySource
import com.digitalturbine.promptnews.domain.model.UnifiedStory
import java.time.Instant

fun Prompt.toEntity(): PromptEntity {
    return PromptEntity(
        id = id,
        text = text,
        intent = intent.name,
        sources = filters.sources.map { it.name },
        publishers = filters.publishers.toList(),
        languages = filters.languages.toList(),
        keywords = filters.keywords.toList(),
        fromDateMs = filters.fromDate?.toEpochMilli(),
        toDateMs = filters.toDate?.toEpochMilli(),
        safeMode = filters.safeMode.name,
        sortMode = filters.sortMode.name,
        createdAtMs = createdAt?.toEpochMilli()
    )
}

fun PromptEntity.toDomain(): Prompt {
    return Prompt(
        id = id,
        text = text,
        intent = intent.asPromptIntent(),
        filters = PromptFilters(
            sources = sources.mapNotNull { it.asStorySourceOrNull() }.toSet(),
            publishers = publishers.toSet(),
            languages = languages.toSet(),
            keywords = keywords.toSet(),
            fromDate = fromDateMs?.let { Instant.ofEpochMilli(it) },
            toDate = toDateMs?.let { Instant.ofEpochMilli(it) },
            safeMode = safeMode.asSafeMode(),
            sortMode = sortMode.asSortMode()
        ),
        createdAt = createdAtMs?.let { Instant.ofEpochMilli(it) }
    )
}

fun UnifiedStory.toEntity(): StoryEntity {
    val canonicalUrl = UrlCanonicalizer.canonicalize(url)
    val storyId = if (canonicalUrl.isNotBlank()) {
        HashUtils.sha256(canonicalUrl)
    } else {
        id.ifBlank { HashUtils.sha256(title + source.name) }
    }
    return StoryEntity(
        storyId = storyId,
        canonicalUrl = canonicalUrl.ifBlank { url },
        title = title,
        summary = summary,
        source = source.name,
        publisher = publisher?.toEntity(),
        publishedAtMs = publishedAt?.toEpochMilli(),
        imageUrl = imageUrl,
        sentiment = sentiment?.toEntity(),
        namedEntities = namedEntities.map { it.toEntity() },
        relatedPrompts = relatedPrompts.map { it.toEntity() },
        tags = tags.toList()
    )
}

fun StoryEntity.toDomain(): UnifiedStory {
    return UnifiedStory(
        id = storyId,
        title = title,
        summary = summary,
        url = canonicalUrl,
        source = source.asStorySource(),
        publisher = publisher?.toDomain(),
        publishedAt = publishedAtMs?.let { Instant.ofEpochMilli(it) },
        imageUrl = imageUrl,
        sentiment = sentiment?.toDomain(),
        namedEntities = namedEntities.map { it.toDomain() },
        relatedPrompts = relatedPrompts.map { it.toDomain() },
        tags = tags.toSet()
    )
}

fun PromptResultBundle.toCacheSnapshot(
    cacheKey: String,
    fetchedAtMs: Long,
    expiresAtMs: Long
): CachedBundleSnapshot {
    val promptEntity = prompt.takeIf { it.id.isNotBlank() || it.text.isNotBlank() }?.toEntity()
    val storyEntities = stories.map { it.toEntity() }
    val items = storyEntities.mapIndexed { index, story ->
        CachedBundleItemEntity(
            cacheKey = cacheKey,
            storyId = story.storyId,
            sortOrder = index
        )
    }
    val bundleEntity = CachedBundleEntity(
        cacheKey = cacheKey,
        promptId = promptEntity?.id,
        fetchedAtMs = fetchedAtMs,
        expiresAtMs = expiresAtMs,
        generatedAtMs = generatedAt?.toEpochMilli(),
        nextPageToken = nextPageToken,
        cacheStaleness = cacheStaleness.name,
        relatedPrompts = relatedPrompts.map { it.toEntity() }
    )
    return CachedBundleSnapshot(
        prompt = promptEntity,
        bundle = bundleEntity,
        items = items,
        stories = storyEntities
    )
}

fun CachedBundleEntity.toDomainBundle(
    prompt: Prompt?,
    stories: List<UnifiedStory>
): PromptResultBundle {
    return PromptResultBundle(
        prompt = prompt ?: Prompt(),
        stories = stories,
        relatedPrompts = relatedPrompts.map { it.toDomain() },
        cacheStaleness = cacheStaleness.asCacheStaleness(),
        generatedAt = generatedAtMs?.let { Instant.ofEpochMilli(it) },
        nextPageToken = nextPageToken
    )
}

data class CachedBundleSnapshot(
    val prompt: PromptEntity?,
    val bundle: CachedBundleEntity,
    val items: List<CachedBundleItemEntity>,
    val stories: List<StoryEntity>
)

private fun Publisher.toEntity(): PublisherEntity {
    return PublisherEntity(
        name = name,
        domain = domain,
        iconUrl = iconUrl,
        credibilityScore = credibilityScore
    )
}

private fun PublisherEntity.toDomain(): Publisher {
    return Publisher(
        name = name,
        domain = domain,
        iconUrl = iconUrl,
        credibilityScore = credibilityScore
    )
}

private fun Sentiment.toEntity(): SentimentEntity {
    return SentimentEntity(
        score = score,
        label = label,
        confidence = confidence
    )
}

private fun SentimentEntity.toDomain(): Sentiment {
    return Sentiment(
        score = score,
        label = label,
        confidence = confidence
    )
}

private fun NamedEntity.toEntity(): NamedEntityEntity {
    return NamedEntityEntity(
        name = name,
        type = type,
        relevance = relevance
    )
}

private fun NamedEntityEntity.toDomain(): NamedEntity {
    return NamedEntity(
        name = name,
        type = type,
        relevance = relevance
    )
}

private fun RelatedPrompt.toEntity(): RelatedPromptEntity {
    return RelatedPromptEntity(
        text = text,
        type = type.name,
        score = score
    )
}

private fun RelatedPromptEntity.toDomain(): RelatedPrompt {
    return RelatedPrompt(
        text = text,
        type = type.asRelatedType(),
        score = score
    )
}

private fun String.asPromptIntent(): PromptIntent =
    PromptIntent.values().firstOrNull { it.name == this } ?: PromptIntent.SEARCH

private fun String.asStorySourceOrNull(): StorySource? =
    StorySource.values().firstOrNull { it.name == this }

private fun String.asStorySource(): StorySource =
    StorySource.values().firstOrNull { it.name == this } ?: StorySource.UNKNOWN

private fun String.asSafeMode(): SafeMode =
    SafeMode.values().firstOrNull { it.name == this } ?: SafeMode.MODERATE

private fun String.asSortMode(): SortMode =
    SortMode.values().firstOrNull { it.name == this } ?: SortMode.RELEVANCE

private fun String.asRelatedType(): RelatedType =
    RelatedType.values().firstOrNull { it.name == this } ?: RelatedType.SUGGESTION

private fun String.asCacheStaleness(): CacheStaleness {
    return when (this) {
        CacheStaleness.FRESH.name -> CacheStaleness.FRESH
        CacheStaleness.SOFT_STALE.name -> CacheStaleness.SOFT_STALE
        CacheStaleness.HARD_STALE.name -> CacheStaleness.HARD_STALE
        "WARM" -> CacheStaleness.SOFT_STALE
        "STALE", "EXPIRED" -> CacheStaleness.HARD_STALE
        else -> CacheStaleness.FRESH
    }
}
