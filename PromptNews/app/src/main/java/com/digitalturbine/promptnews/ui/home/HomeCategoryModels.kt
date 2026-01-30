package com.digitalturbine.promptnews.ui.home

import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.Interest

enum class HomeCategoryType {
    HOME,
    INTEREST
}

data class HomeCategory(
    val id: String,
    val displayName: String,
    val type: HomeCategoryType,
    val endpoint: String
) {
    companion object {
        fun home(): HomeCategory = HomeCategory(
            id = "home",
            displayName = "Local",
            type = HomeCategoryType.HOME,
            endpoint = ""
        )

        fun interest(interest: Interest): HomeCategory = HomeCategory(
            id = interest.id,
            displayName = interest.displayName,
            type = HomeCategoryType.INTEREST,
            endpoint = interest.endpoint
        )
    }
}

sealed class HomeFeedItem {
    data class Hero(val article: Article, val readTime: String, val meta: String) : HomeFeedItem()
    data class SectionHeader(val title: String) : HomeFeedItem()
    data class SmallCard(val article: Article) : HomeFeedItem()
    data class FeedCard(val article: Article) : HomeFeedItem()
    data class FotoscapesArticle(val article: Article) : HomeFeedItem()
    data class TrendingPulse(val rank: Int, val article: Article, val indicator: TrendIndicator) : HomeFeedItem()
    data class CtaButton(val label: String) : HomeFeedItem()
}

enum class TrendIndicator {
    UP,
    DOWN,
    FLAT
}
