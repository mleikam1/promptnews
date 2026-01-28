package com.digitalturbine.promptnews.data

object FotoscapesEndpoints {
    private const val baseDailyEndpoint =
        "https://fotoscapes.com/wp/v1/daily?ckey=fb529d256155b9c6&sched="

    fun dailyEndpoint(sched: String): String = "$baseDailyEndpoint$sched"
}
