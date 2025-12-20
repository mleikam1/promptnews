package com.digitalturbine.promptnews.data

private fun n(s: String) = s.trim().lowercase()

enum class RouteType { INTEREST, PUBLISHER }

data class FotoscapesRoute(val endpoint: String, val type: RouteType)

private data class Rule(val keys: List<String>, val endpoint: String)

private val interestRules = listOf(
    Rule(listOf(n("news"), n("top news"), n("general news")),
        "https://fotoscapes.com/wp/v1/daily?ckey=fb529d256155b9c6&sched=dynamic-news"),
    Rule(listOf(n("technology"), n("tech")),
        "https://fotoscapes.com/wp/v1/daily?ckey=fb529d256155b9c6&sched=dynamic-technology"),
    Rule(listOf(n("sports"), n("nfl"), n("nba")),
        "https://fotoscapes.com/wp/v1/daily?ckey=fb529d256155b9c6&sched=dynamic-sports"),
    Rule(listOf(n("business"), n("markets"), n("finance")),
        "https://fotoscapes.com/wp/v1/daily?ckey=fb529d256155b9c6&sched=business"),
    Rule(listOf(n("world"), n("international")),
        "https://fotoscapes.com/wp/v1/daily?ckey=fb529d256155b9c6&sched=world"),
)

private val publisherMap = mapOf(
    "cnn" to "https://fotoscapes.com/wp/v1/publisher/J1fk1aID?ckey=fb529d256155b9c6",
    "the verge" to "https://fotoscapes.com/wp/v1/publisher/PYfQrtX?ckey=fb529d256155b9c6",
    "nbc news" to "https://fotoscapes.com/wp/v1/publisher/6JfVerhL?ckey=fb529d256155b9c6",
    "yahoo" to "https://fotoscapes.com/wp/v1/publisher/GvfQeGCE?ckey=fb529d256155b9c6",
)

fun fotoscapesEndpointForQuery(query: String): FotoscapesRoute? {
    val q = query.lowercase()

    // publisher first
    publisherMap.entries.firstOrNull { q.contains(it.key) }?.let {
        return FotoscapesRoute(it.value, RouteType.PUBLISHER)
    }

    // interests
    interestRules.forEach { r ->
        if (r.keys.any { q.contains(it) }) {
            return FotoscapesRoute(r.endpoint, RouteType.INTEREST)
        }
    }
    return null
}
