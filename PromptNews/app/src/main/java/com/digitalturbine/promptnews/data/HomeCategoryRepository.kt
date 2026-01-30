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
    suspend fun fetchCategory(category: HomeCategory, userLocation: UserLocation?): HomeCategoryContent {
        return when (category.type) {
            HomeCategoryType.HOME -> HomeCategoryContent(local = loadLocalNews(userLocation))
            HomeCategoryType.INTEREST -> HomeCategoryContent(feed = loadFotoscapesInterest(category))
        }
    }

    suspend fun loadLocalNews(location: UserLocation?): List<Article> {
        if (location == null) {
            Log.d("LocalNews", "SerpApi location missing")
            return emptyList()
        }
        Log.d("LocalNews", "SerpApi location=${location.city}, ${location.state}")
        return serpApiRepository.fetchLocalNews(
            location = "${location.city}, ${location.state}",
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
        Log.e("FS_TRACE", "FETCH START interest=$interestKey")
        Log.d("Fotoscapes", "Fetching interest=$interestKey")
        val schedule = fotoscapesScheduleForInterest(interest)
        val results = fotoscapesRepository.fetchInterestFeed(
            interestKey = interestKey,
            limit = FEED_COUNT,
            schedule = schedule
        )
        Log.e("FS_TRACE", "FETCH END count=${results.size}")
        Log.d("Fotoscapes", "Returned count=${results.size}")
        return results
    }

    companion object {
        private const val FEED_COUNT = 10
    }

    private fun fotoscapesScheduleForInterest(interest: Interest): String {
        return when (interest.id) {
            "business" -> "business"
            "entertainment" -> "entertainment"
            "food-drink" -> "fooddrink"
            "health" -> "health"
            "celebrities", "celebrity-homes" -> "celebrity"
            else -> "general"
        }
    }
}
