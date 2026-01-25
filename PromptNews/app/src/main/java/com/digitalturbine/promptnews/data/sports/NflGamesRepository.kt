package com.digitalturbine.promptnews.data.sports

import android.util.Log
import com.digitalturbine.promptnews.data.net.Http
import com.digitalturbine.promptnews.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NflGamesRepository(
    private val service: ApiSportsNflService = createService()
) {
    companion object {
        private const val TAG = "NflGamesRepository"
        private const val BASE_URL = "https://v1.american-football.api-sports.io/"

        private fun createService(): ApiSportsNflService {
            val client = Http.client.newBuilder()
                .addInterceptor { chain ->
                    // TODO: Move API-Sports calls to Worker / backend proxy.
                    val request = chain.request().newBuilder()
                        .header("x-apisports-key", Config.nflApiSportsKey)
                        .build()
                    chain.proceed(request)
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiSportsNflService::class.java)
        }
    }

    suspend fun fetchRelevantNflGames(now: LocalDate = LocalDate.now(ZoneId.of("America/New_York"))): List<NflGame> =
        withContext(Dispatchers.IO) {
            val season = now.year
            val today = now.format(DateTimeFormatter.ISO_DATE)
            val rangeEnd = now.plusDays(7).format(DateTimeFormatter.ISO_DATE)

            val todayGames = fetchAttempt(
                attempt = "today",
                season = season,
                date = today
            )
            if (todayGames.isNotEmpty()) return@withContext todayGames

            val rangeGames = fetchAttempt(
                attempt = "range",
                season = season,
                from = today,
                to = rangeEnd
            )
            if (rangeGames.isNotEmpty()) return@withContext rangeGames

            fetchAttempt(
                attempt = "last",
                season = season,
                last = 10
            )
        }

    private suspend fun fetchAttempt(
        attempt: String,
        season: Int,
        date: String? = null,
        from: String? = null,
        to: String? = null,
        last: Int? = null
    ): List<NflGame> {
        return try {
            val response = service.getGames(
                season = season,
                date = date,
                from = from,
                to = to,
                last = last
            )
            if (!response.isSuccessful) {
                Log.w(TAG, "NFL API failed attempt=$attempt code=${response.code()} message=${response.message()}")
                logAttempt(attempt, 0)
                return emptyList()
            }
            val games = response.body()?.response.orEmpty()
                .mapNotNull { mapGame(it) }
            logAttempt(attempt, games.size)
            games
        } catch (e: Exception) {
            Log.e(TAG, "NFL API exception attempt=$attempt", e)
            logAttempt(attempt, 0)
            emptyList()
        }
    }

    private fun logAttempt(attempt: String, games: Int) {
        Log.d(TAG, "[NFL API] Attempt=$attempt games=$games")
    }

    private fun mapGame(response: NflGameResponse): NflGame? {
        val game = response.game ?: return null
        val id = game.id ?: return null
        val date = parseInstant(game.date) ?: parseEpochSeconds(game.timestamp) ?: return null
        val status = game.status
        val statusShort = status?.short?.ifBlank { null }
        val statusLong = status?.long?.ifBlank { null }
        val statusClock = status?.timer?.ifBlank { null }

        val statusBucket = when (statusShort?.uppercase()) {
            "FT", "AOT", "FINAL", "POST" -> NflGameStatus.FINAL
            "Q1", "Q2", "Q3", "Q4", "OT", "HT", "LIVE" -> NflGameStatus.LIVE
            else -> NflGameStatus.SCHEDULED
        }

        val teams = response.teams
        val homeTeam = teams?.home
        val awayTeam = teams?.away
        val scores = response.scores

        val home = NflTeam(
            id = homeTeam?.id,
            name = homeTeam?.name?.ifBlank { null } ?: "TBD",
            logoUrl = homeTeam?.logo,
            score = scores?.home?.total
        )
        val away = NflTeam(
            id = awayTeam?.id,
            name = awayTeam?.name?.ifBlank { null } ?: "TBD",
            logoUrl = awayTeam?.logo,
            score = scores?.away?.total
        )

        return NflGame(
            id = id,
            league = response.league?.name?.ifBlank { null } ?: "NFL",
            status = statusBucket,
            statusShort = statusShort,
            statusLong = statusLong,
            statusClock = statusClock,
            homeTeam = home,
            awayTeam = away,
            date = date,
            venue = game.venue?.name?.ifBlank { null }
        )
    }

    private fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { Instant.parse(value) }
            .getOrNull()
    }

    private fun parseEpochSeconds(value: Long?): Instant? {
        val safeValue = value ?: return null
        if (safeValue <= 0) return null
        return Instant.ofEpochSecond(safeValue)
    }
}
