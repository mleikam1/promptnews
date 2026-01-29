package com.digitalturbine.promptnews.data

import android.util.Log
import com.digitalturbine.promptnews.data.fotoscapes.FotoscapesRepository
import com.digitalturbine.promptnews.data.serpapi.SerpApiRepository
import com.digitalturbine.promptnews.ui.home.HomeCategory
import com.digitalturbine.promptnews.ui.home.HomeCategoryType

data class HomeCategoryContent(
    val local: List<Article> = emptyList(),
    val feed: List<Article> = emptyList()
)

class HomeCategoryRepository(
    private val fotoscapesRepository: FotoscapesRepository = FotoscapesRepository(),
    private val serpApiRepository: SerpApiRepository = SerpApiRepository()
) {
    suspend fun fetchCategory(category: HomeCategory, locationLabel: String): HomeCategoryContent {
        return when (category.type) {
            HomeCategoryType.HOME -> HomeCategoryContent(local = loadLocalNews(locationLabel))
            HomeCategoryType.INTEREST -> HomeCategoryContent(feed = loadFotoscapesInterest(category))
        }
    }

    suspend fun loadLocalNews(locationLabel: String): List<Article> {
        return serpApiRepository.fetchLocalNews(
            location = locationLabel,
            query = "local news"
        )
    }

    suspend fun loadFotoscapesInterest(category: HomeCategory): List<Article> {
        val interest = if (category.id.isNotBlank()) {
            Interest(category.id, category.displayName, category.endpoint)
        } else {
            InterestCatalog.findById(category.id)
        } ?: return emptyList()

        val interestKey = interest.toFotoscapesKey()
        val results = fotoscapesRepository.fetchInterestFeed(
            interestKey = interestKey,
            limit = FEED_COUNT,
            schedule = FOTOSCAPES_SCHEDULE
        )
        Log.d("Fotoscapes", "Interest=$interestKey count=${results.size}")
        return results
    }

    companion object {
        private const val FEED_COUNT = 10
        private const val FOTOSCAPES_SCHEDULE = "promptnews"
    }
}
