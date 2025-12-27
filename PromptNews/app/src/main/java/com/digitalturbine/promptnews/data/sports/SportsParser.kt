package com.digitalturbine.promptnews.data.sports

import org.json.JSONArray
import org.json.JSONObject

object SportsParser {
    fun parse(jsonStr: String): SportsResults? {
        if (jsonStr.isBlank()) return null
        val root = JSONObject(jsonStr)

        val header = root.optJSONObject("team_overview")?.let { overview ->
            SportsHeaderModel(
                title = overview.optString("title").ifBlank { null },
                subtitle = overview.optString("ranking").ifBlank { null },
                thumbnail = overview.optString("thumbnail").ifBlank { null },
                tabs = headerTabs(root)
            )
        } ?: SportsHeaderModel(
            title = null,
            subtitle = null,
            thumbnail = null,
            tabs = headerTabs(root)
        )

        val matches = buildList {
            root.optJSONObject("live_game")?.let { add(parseMatch(it, SportsMatchStatusBucket.LIVE)) }
            addAll(parseMatches(root.optJSONArray("recent_games"), SportsMatchStatusBucket.COMPLETED))
            addAll(parseMatches(root.optJSONArray("upcoming_games"), SportsMatchStatusBucket.UPCOMING))
        }

        return SportsResults(
            header = header,
            matches = matches
        )
    }

    private fun parseMatches(array: JSONArray?, bucket: SportsMatchStatusBucket): List<SportsMatchModel> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { parseMatch(it, bucket) }
        }
    }

    private fun parseMatch(obj: JSONObject, bucket: SportsMatchStatusBucket): SportsMatchModel {
        val teams = obj.optJSONArray("teams")?.let { teamsArray ->
            (0 until teamsArray.length()).mapNotNull { index ->
                val teamObj = teamsArray.optJSONObject(index) ?: return@mapNotNull null
                TeamModel(
                    name = teamObj.optString("name").ifBlank { null },
                    score = teamObj.optString("score").ifBlank { null },
                    logoUrl = teamObj.optString("thumbnail").ifBlank { null },
                    isWinner = null
                )
            }
        } ?: emptyList()

        val highlights = obj.optJSONObject("video_highlights")?.let { highlightObj ->
            val link = highlightObj.optString("link").ifBlank { null }
            val thumbnail = highlightObj.optString("thumbnail").ifBlank { null }
            val duration = highlightObj.optString("duration").ifBlank { null }
            if (link == null && thumbnail == null && duration == null) {
                null
            } else {
                HighlightModel(link = link, thumbnail = thumbnail, duration = duration)
            }
        }

        val scoredTeams = withWinnerFlags(teams)
        val status = obj.optString("status").ifBlank { null }
        val date = obj.optString("date").ifBlank { null }
        val time = obj.optString("time").ifBlank { null }
        val context = LeagueContextModel(
            league = obj.optString("league").ifBlank { null },
            tournament = obj.optString("tournament").ifBlank { null },
            stage = obj.optString("stage").ifBlank { null },
            round = obj.optString("round").ifBlank { null },
            week = obj.optString("week").ifBlank { null }
        )

        return SportsMatchModel(
            id = obj.optString("id").ifBlank { null },
            context = context,
            homeTeam = scoredTeams.getOrNull(0),
            awayTeam = scoredTeams.getOrNull(1),
            statusBucket = bucket,
            statusText = status,
            dateText = time ?: date,
            highlight = highlights,
            matchLink = obj.optString("match_link").ifBlank { null }
        )
    }

    private fun headerTabs(root: JSONObject): List<String> {
        val tabs = mutableListOf("Matches", "News", "Standings")
        val hasPlayers = root.optBoolean("players_available", false) ||
            root.optJSONArray("players")?.length()?.let { it > 0 } == true
        if (hasPlayers) {
            tabs.add("Players")
        }
        return tabs
    }

    private fun withWinnerFlags(teams: List<TeamModel>): List<TeamModel> {
        val scores = teams.map { it.score?.toIntOrNull() }
        if (scores.size < 2 || scores.any { it == null }) {
            return teams
        }
        val first = scores[0]!!
        val second = scores[1]!!
        return teams.mapIndexed { index, team ->
            val isWinner = if (first == second) null else {
                if (index == 0) first > second else second > first
            }
            team.copy(isWinner = isWinner)
        }
    }
}
