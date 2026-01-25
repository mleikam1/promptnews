package com.digitalturbine.promptnews.data.sports

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiSportsNflService {
    @GET("games")
    suspend fun getGames(
        @Query("league") league: Int = 1,
        @Query("season") season: Int,
        @Query("date") date: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("last") last: Int? = null
    ): Response<NflGamesResponse>
}
