package com.digitalturbine.promptnews.data

import com.digitalturbine.promptnews.data.fotoscapes.FotoscapesRepository
import com.digitalturbine.promptnews.ui.home.HomeCategory
import com.digitalturbine.promptnews.ui.home.HomeCategoryType

data class HomeCategoryContent(
    val local: List<Article> = emptyList(),
    val feed: List<Article> = emptyList()
)

class HomeCategoryRepository(
    private val fotoscapesRepository: FotoscapesRepository = FotoscapesRepository()
) {
    suspend fun fetchCategory(category: HomeCategory, locationLabel: String): HomeCategoryContent {
        return when (category.type) {
            HomeCategoryType.HOME -> HomeCategoryContent(local = fetchLocalNews(locationLabel))
            HomeCategoryType.INTEREST -> HomeCategoryContent(feed = fetchInterestFeed(category))
        }
    }

    private suspend fun fetchLocalNews(locationLabel: String): List<Article> {
        return fotoscapesRepository.fetchContent(
            category = "local",
            interest = null,
            limit = LOCAL_COUNT,
            schedule = FOTOSCAPES_SCHEDULE,
            geo = locationLabel.ifBlank { null }
        )
    }

    private suspend fun fetchInterestFeed(category: HomeCategory): List<Article> {
        val interest = if (category.id.isNotBlank()) {
            Interest(category.id, category.displayName, category.endpoint)
        } else {
            InterestCatalog.findById(category.id)
        } ?: return emptyList()

        val mappedCategory = mapInterestToFotoscapesCategory(interest.id)
        return fotoscapesRepository.fetchContent(
            category = mappedCategory,
            interest = interest.id,
            limit = FEED_COUNT,
            schedule = FOTOSCAPES_SCHEDULE,
            geo = null
        )
    }

    companion object {
        private const val LOCAL_COUNT = 10
        private const val FEED_COUNT = 10
        private const val FOTOSCAPES_SCHEDULE = "promptnews"
    }
}

private fun mapInterestToFotoscapesCategory(interestId: String): String {
    return when (interestId.lowercase()) {
        "sports" -> "sports"
        "business", "personal-finance", "markets", "finance" -> "business"
        "technology", "tech", "science", "games", "gear-gadgets" -> "technology"
        "entertainment", "celebrities", "tv-film", "music", "culture" -> "entertainment"
        "news", "general-news", "top-news", "international-news", "politics", "world" -> "news"
        "health", "wellness", "lifestyle", "fashion", "womens-style", "mens-style", "food-drink", "home" -> "lifestyle"
        else -> "general"
    }
}
