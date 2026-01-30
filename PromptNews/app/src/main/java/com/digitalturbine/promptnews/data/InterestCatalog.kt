package com.digitalturbine.promptnews.data

data class Interest(
    val id: String,
    val displayName: String,
    val endpoint: String
)

fun Interest.toFotoscapesKey(): String {
    return when (id) {
        "business" -> "business"
        "celebrities", "celebrity-homes" -> "celebrity"
        "entertainment" -> "entertainment"
        "health" -> "health"
        "sports" -> "sports"
        "technology" -> "technology"
        else -> "general"
    }
}

fun Interest.toFotoscapesSched(): String {
    return when (id) {
        "business" -> "business"
        "celebrities", "celebrity-homes" -> "celebrity"
        "entertainment" -> "entertainment"
        "games" -> "games"
        "food-drink" -> "fooddrink"
        "health" -> "health"
        "international-news" -> "world"
        "home" -> "lifestyle"
        else -> "topnews"
    }
}

data class InterestRoute(
    val id: String,
    val displayName: String,
    val sched: String
)

object InterestCatalog {
    private const val SCHED_DYNAMIC_NEWS = "dynamic-news"

    private val routes: List<InterestRoute> = listOf(
        InterestRoute("adventure", "Adventure", "adventure"),
        InterestRoute("business", "Business", "business"),
        InterestRoute("cars-rides", "Cars & Rides", "cars"),
        InterestRoute("celebrity-homes", "Celebrity Homes", "celebrity-homes"),
        InterestRoute("celebrities", "Celebrities", "celebrities"),
        InterestRoute("culture", "Culture", "culture"),
        InterestRoute("editorial-photography", "Editorial Photography", "standard"),
        InterestRoute("entertainment", "Entertainment", "dynamic-entertainment"),
        InterestRoute("fashion", "Fashion", "style"),
        InterestRoute("food-drink", "Food & Drink", "foodanddrink"),
        InterestRoute("games", "Games", "html5games"),
        InterestRoute("gear-gadgets", "Gear & Gadgets", "gearandgadgets"),
        InterestRoute("general-news", "General News", SCHED_DYNAMIC_NEWS),
        InterestRoute("health", "Health", "health"),
        InterestRoute("home", "Home", "home"),
        InterestRoute("international-news", "International News", "world"),
        InterestRoute("lifestyle", "Lifestyle", "dynamic-lifestyle"),
        InterestRoute("womens-style", "Women's Style", "womensstyle"),
        InterestRoute("mens-style", "Men's Style", "mensstyle"),
        InterestRoute("music", "Music", "music"),
        InterestRoute("news", "News", SCHED_DYNAMIC_NEWS),
        InterestRoute("outdoor-photography", "Outdoor Photography", "outdoor_photography"),
        InterestRoute("personal-finance", "Personal Finance", "personalfinance"),
        InterestRoute("politics", "Politics", "politics"),
        InterestRoute("science", "Science", "science"),
        InterestRoute("shopping", "Shopping", "shopping"),
        InterestRoute("sneakers", "Sneakers", "sneakers"),
        InterestRoute("sports", "Sports", "dynamic-sports"),
        InterestRoute("strange-news", "Strange News", "strangenews"),
        InterestRoute("tv-film", "TV & Film", "tvandfilm"),
        InterestRoute("technology", "Technology", "technology"),
        InterestRoute("top-news", "Top News", "topnews"),
        InterestRoute("wellness", "Wellness", "Wellness")
    )

    val interests: List<Interest> = routes.map { route ->
        Interest(
            id = route.id,
            displayName = route.displayName,
            endpoint = FotoscapesEndpoints.dailyEndpoint(route.sched)
        )
    }

    fun findById(id: String): Interest? {
        return routes.firstOrNull { it.id == id }?.let { route ->
            Interest(
                id = route.id,
                displayName = route.displayName,
                endpoint = FotoscapesEndpoints.dailyEndpoint(route.sched)
            )
        }
    }
}
