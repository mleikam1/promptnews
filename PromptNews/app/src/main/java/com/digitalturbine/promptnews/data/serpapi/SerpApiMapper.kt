package com.digitalturbine.promptnews.data.serpapi

import com.digitalturbine.promptnews.domain.model.Publisher
import com.digitalturbine.promptnews.domain.model.StorySource
import com.digitalturbine.promptnews.domain.model.UnifiedStory
import java.net.URI

data class SerpApiStoryDto(
    val title: String,
    val url: String,
    val imageUrl: String,
    val logoUrl: String,
    val sourceName: String? = null,
    val ageLabel: String? = null
)

class SerpApiMapper {
    fun toUnifiedStory(dto: SerpApiStoryDto): UnifiedStory {
        val publisher = dto.sourceName?.takeIf { it.isNotBlank() }?.let { source ->
            Publisher(
                name = source,
                domain = domainFromUrl(dto.url),
                iconUrl = dto.logoUrl.ifBlank { null }
            )
        }

        return UnifiedStory(
            title = dto.title,
            summary = null,
            url = dto.url,
            source = StorySource.API,
            publisher = publisher,
            publishedAt = null,
            imageUrl = dto.imageUrl.ifBlank { null }
        )
    }

    private fun domainFromUrl(url: String): String? {
        return runCatching { URI(url).host }.getOrNull()
    }
}
