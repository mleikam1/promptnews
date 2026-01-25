package com.digitalturbine.promptnews.data.sports

import android.net.Uri
import android.util.Log
import com.digitalturbine.promptnews.data.net.Http
import com.digitalturbine.promptnews.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ApiSportsNflService(
    private val baseUrl: String = "https://v1.american-football.api-sports.io"
) {
    companion object {
        private const val TAG = "ApiSportsNflService"
    }

    suspend fun fetchGames(query: String, now: Instant = Instant.now()): List<NflGame> =
        withContext(Dispatchers.IO) {
            val zone = ZoneId.of("America/New_York")
            val today = LocalDate.now(zone)
            val from = today.minusDays(1)
            val to = today.plusDays(7)
            val season = if (today.monthValue <= 2) today.year - 1 else today.year

            val uri = Uri.parse("$baseUrl/games").buildUpon()
                .appendQueryParameter("season", season.toString())
                .appendQueryParameter("from", from.format(DateTimeFormatter.ISO_DATE))
                .appendQueryParameter("to", to.format(DateTimeFormatter.ISO_DATE))
                .build()
                .toString()

            val headers = mapOf("x-apisports-key" to Config.nflApiSportsKey)

            // TODO: Replace this direct API-Sports call with GET /sports/nfl/scores from a Worker/backend.
            return@withContext try {
                Http.client.newCall(Http.req(uri, headers)).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "NFL scores request failed (${response.code}) for query: $query")
                        return@use emptyList()
                    }
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) {
                        Log.w(TAG, "NFL scores response empty for query: $query")
                        return@use emptyList()
                    }
                    parseGames(body)
                }
            } catch (e: Exception) {
                Log.e(TAG, "NFL scores request exception for query: $query", e)
                emptyList()
            }
        }

    private fun parseGames(body: String): List<NflGame> {
        val root = JSONObject(body)
        val response = root.optJSONArray("response") ?: return emptyList()
        val games = mutableListOf<NflGame>()
        for (i in 0 until response.length()) {
            val item = response.optJSONObject(i) ?: continue
            val leagueName = item.optJSONObject("league")?.optString("name")?.ifBlank { null } ?: "NFL"
            val gameObj = item.optJSONObject("game") ?: continue
            val gameId = gameObj.optInt("id", -1)
            if (gameId == -1) continue
            val statusObj = gameObj.optJSONObject("status")
            val statusShort = statusObj?.optString("short")?.ifBlank { null }
            val statusLong = statusObj?.optString("long")?.ifBlank { null }
            val statusClock = statusObj?.optString("timer")?.ifBlank { null }
            val date = parseInstant(gameObj.optString("date"))
                ?: parseEpochSeconds(gameObj.optLong("timestamp", 0)) ?: continue
            val venue = gameObj.optJSONObject("venue")?.optString("name")?.ifBlank { null }

            val teams = item.optJSONObject("teams")
            val homeTeam = teams?.optJSONObject("home")
            val awayTeam = teams?.optJSONObject("away")

            val scores = item.optJSONObject("scores")

            val home = NflTeam(
                id = homeTeam?.optInt("id"),
                name = homeTeam?.optString("name").orEmpty().ifBlank { "TBD" },
                logoUrl = homeTeam?.optString("logo"),
                score = parseScore(scores, "home")
            )
            val away = NflTeam(
                id = awayTeam?.optInt("id"),
                name = awayTeam?.optString("name").orEmpty().ifBlank { "TBD" },
                logoUrl = awayTeam?.optString("logo"),
                score = parseScore(scores, "away")
            )

            val statusBucket = when (statusShort?.uppercase()) {
                "FT", "AOT", "FINAL", "POST" -> NflGameStatus.FINAL
                "Q1", "Q2", "Q3", "Q4", "OT", "HT", "LIVE" -> NflGameStatus.LIVE
                else -> NflGameStatus.SCHEDULED
            }

            games += NflGame(
                id = gameId,
                league = leagueName,
                status = statusBucket,
                statusShort = statusShort,
                statusLong = statusLong,
                statusClock = statusClock,
                homeTeam = home,
                awayTeam = away,
                date = date,
                venue = venue
            )
        }
        return games
    }

    private fun parseScore(scores: JSONObject?, side: String): Int? {
        val scoreObj = scores?.optJSONObject(side) ?: return null
        val total = scoreObj.optInt("total", -1)
        if (total >= 0) return total
        val points = scoreObj.optInt("points", -1)
        return points.takeIf { it >= 0 }
    }

    private fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { Instant.parse(value) }
            .getOrNull()
    }

    private fun parseEpochSeconds(value: Long): Instant? {
        if (value <= 0) return null
        return Instant.ofEpochSecond(value)
    }
}
