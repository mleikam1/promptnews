package com.digitalturbine.promptnews.data.sports

import org.json.JSONArray
import org.json.JSONObject

object SportsParser {
    fun parse(jsonStr: String): SportsResults? {
        if (jsonStr.isBlank()) return null
        val root = JSONObject(jsonStr)

        val teamOverview = root.optJSONObject("team_overview")?.let { overview ->
            TeamOverview(
                title = overview.optString("title").ifBlank { null },
                ranking = overview.optString("ranking").ifBlank { null },
                thumbnail = overview.optString("thumbnail").ifBlank { null }
            )
        }

        val liveGame = root.optJSONObject("live_game")?.let { parseGame(it) }
        val recentGames = parseGames(root.optJSONArray("recent_games"))
        val upcomingGames = parseGames(root.optJSONArray("upcoming_games"))

        return SportsResults(
            teamOverview = teamOverview,
            liveGame = liveGame,
            recentGames = recentGames,
            upcomingGames = upcomingGames
        )
    }

    private fun parseGames(array: JSONArray?): List<SportsGame> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { parseGame(it) }
        }
    }

    private fun parseGame(obj: JSONObject): SportsGame {
        val teams = obj.optJSONArray("teams")?.let { teamsArray ->
            (0 until teamsArray.length()).mapNotNull { index ->
                val teamObj = teamsArray.optJSONObject(index) ?: return@mapNotNull null
                SportsTeam(
                    name = teamObj.optString("name").ifBlank { null },
                    score = teamObj.optString("score").ifBlank { null },
                    thumbnail = teamObj.optString("thumbnail").ifBlank { null }
                )
            }
        } ?: emptyList()

        val highlights = obj.optJSONObject("video_highlights")?.let { highlightObj ->
            val link = highlightObj.optString("link").ifBlank { null }
            val thumbnail = highlightObj.optString("thumbnail").ifBlank { null }
            if (link == null && thumbnail == null) {
                null
            } else {
                VideoHighlights(link = link, thumbnail = thumbnail)
            }
        }

        return SportsGame(
            date = obj.optString("date").ifBlank { null },
            time = obj.optString("time").ifBlank { null },
            league = obj.optString("league").ifBlank { null },
            status = obj.optString("status").ifBlank { null },
            teams = teams,
            score = obj.optString("score").ifBlank { null },
            videoHighlights = highlights
        )
    }
}
