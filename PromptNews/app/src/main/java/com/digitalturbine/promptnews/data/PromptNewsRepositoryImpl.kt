package com.digitalturbine.promptnews.data

import com.digitalturbine.promptnews.data.cache.CachePolicyEngine
import com.digitalturbine.promptnews.data.cache.PromptNewsDatabase
import com.digitalturbine.promptnews.data.cache.toDomain
import com.digitalturbine.promptnews.data.cache.toDomainBundle
import com.digitalturbine.promptnews.data.cache.toCacheSnapshot
import com.digitalturbine.promptnews.data.common.CacheKeyBuilder
import com.digitalturbine.promptnews.data.common.UrlCanonicalizer
import com.digitalturbine.promptnews.domain.merge.MergeableStory
import com.digitalturbine.promptnews.domain.merge.StoryMergeEngine
import com.digitalturbine.promptnews.domain.merge.StoryProvider
import com.digitalturbine.promptnews.domain.model.CacheStaleness
import com.digitalturbine.promptnews.domain.model.Prompt
import com.digitalturbine.promptnews.domain.model.PromptFilters
import com.digitalturbine.promptnews.domain.model.PromptResultBundle
import com.digitalturbine.promptnews.domain.model.Publisher
import com.digitalturbine.promptnews.domain.model.RelatedPrompt
import com.digitalturbine.promptnews.domain.model.RelatedType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.time.Instant

class PromptNewsRepositoryImpl(
    private val database: PromptNewsDatabase,
    private val cachePolicyEngine: CachePolicyEngine = CachePolicyEngine(),
    private val searchRepository: SearchRepository = SearchRepository(),
    private val storyMergeEngine: StoryMergeEngine = StoryMergeEngine(),
    private val clockMs: () -> Long = System::currentTimeMillis
) : PromptNewsRepository {

    override suspend fun fetchPromptResults(
        prompt: Prompt,
        locale: String?,
        geo: String?
    ): PromptResultBundle = withContext(Dispatchers.IO) {
        val cacheKey = CacheKeyBuilder.build(
            prompt = prompt.text,
            locale = locale,
            geo = geo,
            filters = prompt.toCacheFilters()
        )

        loadCachedBundle(cacheKey, prompt)?.let { cached ->
            return@withContext cached
        }

        fetchAndCachePromptBundle(prompt, cacheKey)
    }

    private suspend fun loadCachedBundle(
        cacheKey: String,
        fallbackPrompt: Prompt
    ): PromptResultBundle? {
        val bundleEntity = database.cachedBundleDao().getBundle(cacheKey) ?: return null
        val stories = database.cachedBundleDao().getStoriesForCacheKey(cacheKey)
            .map { it.toDomain() }
        val promptEntity = bundleEntity.promptId?.let { database.promptDao().getPrompt(it) }
        val prompt = promptEntity?.toDomain() ?: fallbackPrompt
        val bundle = bundleEntity.toDomainBundle(prompt, stories)
        val staleness = cachePolicyEngine.stalenessFor(
            CachePolicyEngine.CacheBucket.PROMPT_SEARCH,
            bundleEntity.fetchedAtMs,
            clockMs()
        )
        return bundle.copy(cacheStaleness = staleness)
    }

    private suspend fun fetchAndCachePromptBundle(
        prompt: Prompt,
        cacheKey: String
    ): PromptResultBundle {
        val nowMs = clockMs()
        val serpApiArticles = searchRepository.fetchSerpNews(prompt.text, page = 0, pageSize = 20)
        val serpApiStories = serpApiArticles.mapIndexed { index, article ->
            article.toMergeableStory(index)
        }

        // TODO: Integrate Newscatcher API responses and map them into MergeableStory instances.
        val newscatcherStories = emptyList<MergeableStory>()

        val mergeResult = storyMergeEngine.mergeStories(newscatcherStories, serpApiStories)
        val relatedPrompts = fetchRelatedPrompts(prompt.text)

        val bundle = PromptResultBundle(
            prompt = prompt,
            stories = mergeResult.stories,
            relatedPrompts = relatedPrompts,
            cacheStaleness = CacheStaleness.FRESH,
            generatedAt = Instant.ofEpochMilli(nowMs)
        )

        cachePromptBundle(cacheKey, bundle, nowMs)

        return bundle
    }

    private suspend fun fetchRelatedPrompts(prompt: String): List<RelatedPrompt> {
        val extras = searchRepository.fetchExtras(prompt)
        val followUps = extras.peopleAlsoAsk.map { suggestion ->
            RelatedPrompt(text = suggestion, type = RelatedType.FOLLOW_UP, score = 0.6)
        }
        val related = extras.relatedSearches.map { suggestion ->
            RelatedPrompt(text = suggestion, type = RelatedType.SUGGESTION, score = 0.4)
        }
        return followUps + related
    }

    private suspend fun cachePromptBundle(
        cacheKey: String,
        bundle: PromptResultBundle,
        fetchedAtMs: Long
    ) {
        val decision = cachePolicyEngine.decisionFor(
            CachePolicyEngine.CacheBucket.PROMPT_SEARCH,
            fetchedAtMs,
            fetchedAtMs
        )
        val snapshot = bundle.toCacheSnapshot(
            cacheKey = cacheKey,
            fetchedAtMs = fetchedAtMs,
            expiresAtMs = decision.hardExpiresAtMs
        )

        snapshot.prompt?.let { database.promptDao().upsert(it) }
        database.storyDao().upsertStories(snapshot.stories)
        database.cachedBundleDao().upsertBundle(snapshot.bundle)
        database.cachedBundleDao().replaceBundleItems(cacheKey, snapshot.items)
    }

    private fun Article.toMergeableStory(providerRank: Int): MergeableStory {
        val canonicalUrl = UrlCanonicalizer.canonicalize(url)
        return MergeableStory(
            canonicalUrl = canonicalUrl.ifBlank { url },
            title = title.ifBlank { null },
            summary = null,
            provider = StoryProvider.SERPAPI,
            providerRank = providerRank,
            publisher = sourceName?.takeIf { it.isNotBlank() }?.let { source ->
                Publisher(
                    name = source,
                    domain = domainFromUrl(url),
                    iconUrl = logoUrl.ifBlank { null }
                )
            },
            publishedAt = null,
            imageUrl = imageUrl.ifBlank { null },
            sentiment = null,
            namedEntities = emptyList(),
            relatedPrompts = emptyList(),
            tags = emptySet()
        )
    }

    private fun domainFromUrl(url: String): String? {
        return runCatching { URI(url).host }.getOrNull()
    }

    private fun Prompt.toCacheFilters(): Map<String, String> {
        return buildMap {
            put("intent", intent.name)
            putAll(filters.toCacheFilters())
        }
    }

    private fun PromptFilters.toCacheFilters(): Map<String, String> {
        return buildMap {
            if (sources.isNotEmpty()) {
                put("sources", sources.sortedBy { it.name }.joinToString(",") { it.name })
            }
            if (publishers.isNotEmpty()) {
                put("publishers", publishers.sorted().joinToString(","))
            }
            if (languages.isNotEmpty()) {
                put("languages", languages.sorted().joinToString(","))
            }
            if (keywords.isNotEmpty()) {
                put("keywords", keywords.sorted().joinToString(","))
            }
            fromDate?.let { put("from", it.toString()) }
            toDate?.let { put("to", it.toString()) }
            put("safeMode", safeMode.name)
            put("sortMode", sortMode.name)
        }
    }
}
