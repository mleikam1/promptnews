package com.digitalturbine.promptnews.data

data class Interest(
    val id: String,
    val displayName: String,
    val endpoint: String
)

object InterestCatalog {
    private const val baseEndpoint =
        "https://fotoscapes.com/wp/v1/daily?ckey=fb529d256155b9c6&sched="

    private fun endpoint(sched: String) = "$baseEndpoint$sched"

    val interests: List<Interest> = listOf(
        Interest("adventure", "Adventure", endpoint("adventure")),
        Interest("business", "Business", endpoint("business")),
        Interest("cars-rides", "Cars & Rides", endpoint("cars")),
        Interest("celebrity-homes", "Celebrity Homes", endpoint("celebrity-homes")),
        Interest("celebrities", "Celebrities", endpoint("celebrities")),
        Interest("culture", "Culture", endpoint("culture")),
        Interest("editorial-photography", "Editorial Photography", endpoint("standard")),
        Interest("entertainment", "Entertainment", endpoint("dynamic-entertainment")),
        Interest("fashion", "Fashion", endpoint("style")),
        Interest("food-drink", "Food & Drink", endpoint("foodanddrink")),
        Interest("games", "Games", endpoint("html5games")),
        Interest("gear-gadgets", "Gear & Gadgets", endpoint("gearandgadgets")),
        Interest("general-news", "General News", endpoint("dynamic-news")),
        Interest("health", "Health", endpoint("health")),
        Interest("home", "Home", endpoint("home")),
        Interest("international-news", "International News", endpoint("world")),
        Interest("lifestyle", "Lifestyle", endpoint("dynamic-lifestyle")),
        Interest("womens-style", "Women's Style", endpoint("womensstyle")),
        Interest("mens-style", "Men's Style", endpoint("mensstyle")),
        Interest("music", "Music", endpoint("music")),
        Interest("news", "News", endpoint("dynamic-news")),
        Interest("outdoor-photography", "Outdoor Photography", endpoint("outdoor_photography")),
        Interest("personal-finance", "Personal Finance", endpoint("personalfinance")),
        Interest("politics", "Politics", endpoint("politics")),
        Interest("science", "Science", endpoint("science")),
        Interest("shopping", "Shopping", endpoint("shopping")),
        Interest("sneakers", "Sneakers", endpoint("sneakers")),
        Interest("sports", "Sports", endpoint("dynamic-sports")),
        Interest("strange-news", "Strange News", endpoint("strangenews")),
        Interest("tv-film", "TV & Film", endpoint("tvandfilm")),
        Interest("technology", "Technology", endpoint("technology")),
        Interest("top-news", "Top News", endpoint("topnews")),
        Interest("wellness", "Wellness", endpoint("wellness"))
    )
}
