package com.digitalturbine.promptnews.data

import com.digitalturbine.promptnews.data.fotoscapes.FotoscapesRepository
import com.digitalturbine.promptnews.ui.home.HomeCategory
import com.digitalturbine.promptnews.ui.home.HomeCategoryType

data class HomeCategoryContent(
    val local: List<Article> = emptyList(),
    val feed: List<Article> = emptyList()
)

class HomeCategoryRepository(
    private val searchRepository: SearchRepository = SearchRepository(),
    private val fotoscapesRepository: FotoscapesRepository = FotoscapesRepository()
) {
    suspend fun fetchCategory(category: HomeCategory, locationLabel: String): HomeCategoryContent {
        return when (category.type) {
            HomeCategoryType.HOME -> HomeCategoryContent(local = fetchLocalNews(locationLabel))
            HomeCategoryType.INTEREST -> HomeCategoryContent(feed = fetchInterestFeed(category))
        }
    }

    private suspend fun fetchLocalNews(locationLabel: String): List<Article> {
        val query = if (locationLabel.isBlank()) "local news" else "local news $locationLabel"
        return searchRepository.fetchSerpNews(
            query = query,
            page = 0,
            pageSize = LOCAL_COUNT,
            location = locationLabel,
            useRawImageUrls = true
        )
    }

    private suspend fun fetchInterestFeed(category: HomeCategory): List<Article> {
        val interest = if (category.endpoint.isNotBlank()) {
            Interest(category.id, category.displayName, category.endpoint)
        } else {
            InterestCatalog.findById(category.id)
        } ?: return emptyList()

        return fotoscapesRepository.fetchInterestSections(listOf(interest))
            .firstOrNull()
            ?.articles
            .orEmpty()
    }

    companion object {
        private const val LOCAL_COUNT = 5
    }
}
