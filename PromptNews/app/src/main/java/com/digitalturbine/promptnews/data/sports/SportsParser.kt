package com.digitalturbine.promptnews.data.sports

import org.json.JSONArray
import org.json.JSONObject

object SportsParser {
    fun parse(jsonStr: String): SportsResults? {
        if (jsonStr.isBlank()) return null
        val root = JSONObject(jsonStr)
        val sportsRoot = root.optJSONObject("sports_results") ?: root

        val header = root.optJSONObject("team_overview")?.let { overview ->
            SportsHeaderModel(
                title = overview.optString("title").ifBlank { null },
                subtitle = overview.optString("ranking").ifBlank { null },
                thumbnail = overview.optString("thumbnail").ifBlank { null },
                tabs = headerTabs(root, sportsRoot)
            )
        } ?: SportsHeaderModel(
            title = sportsRoot.firstString("title", "name")
                ?: root.optJSONObject("search_information")?.optString("query_displayed")?.ifBlank { null },
            subtitle = parseRanking(sportsRoot.opt("rankings")),
            thumbnail = sportsRoot.firstString("thumbnail", "thumbnail_url"),
            tabs = headerTabs(root, sportsRoot)
        )

        val matches = if (hasNormalizedGames(root)) {
            buildList {
                root.optJSONObject("live_game")?.let { add(parseMatch(it, SportsMatchStatusBucket.LIVE)) }
                addAll(parseMatches(root.optJSONArray("recent_games"), SportsMatchStatusBucket.COMPLETED))
                addAll(parseMatches(root.optJSONArray("upcoming_games"), SportsMatchStatusBucket.UPCOMING))
            }
        } else {
            val games = flattenGames(sportsRoot)
            games.mapNotNull { game -> parseMatch(game, null) }
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

    private fun parseMatch(obj: JSONObject, bucket: SportsMatchStatusBucket?): SportsMatchModel? {
        val teams = parseTeams(obj)
        if (teams.isEmpty()) return null

        val highlights = parseHighlights(obj)
        val scoredTeams = withWinnerFlags(teams)
        val status = obj.firstString("status", "game_status", "status_display", "stage", "result", "state")
        val date = obj.firstString("date", "start_date", "game_date")
        val time = obj.firstString("time", "start_time", "game_time", "start_time_local", "time_local")
        val context = LeagueContextModel(
            league = obj.firstString("league", "sport"),
            tournament = obj.firstString("tournament"),
            stage = obj.firstString("stage"),
            round = obj.firstString("round"),
            week = obj.firstString("week")
        )
        val resolvedBucket = bucket ?: statusBucketFor(status, scoredTeams)

        return SportsMatchModel(
            id = obj.firstString("id"),
            context = context,
            homeTeam = scoredTeams.getOrNull(0),
            awayTeam = scoredTeams.getOrNull(1),
            statusBucket = resolvedBucket,
            statusText = status,
            dateText = time ?: date,
            highlight = highlights,
            matchLink = obj.firstString("match_link", "match_link_url", "link")
        )
    }

    private fun headerTabs(root: JSONObject, sportsRoot: JSONObject): List<String> {
        val tabs = mutableListOf("Matches", "News", "Standings")
        val hasPlayers = root.optBoolean("players_available", false) ||
            root.optJSONArray("players")?.length()?.let { it > 0 } == true ||
            sportsRoot.optBoolean("players_available", false) ||
            sportsRoot.optJSONArray("players")?.length()?.let { it > 0 } == true
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

    private fun hasNormalizedGames(root: JSONObject): Boolean {
        return root.has("live_game") || root.has("recent_games") || root.has("upcoming_games")
    }

    private fun flattenGames(sportsRoot: JSONObject): List<JSONObject> {
        val buckets = listOf(
            sportsRoot.optJSONArray("live_games"),
            sportsRoot.optJSONArray("recent_games"),
            sportsRoot.optJSONArray("upcoming_games"),
            sportsRoot.optJSONArray("games"),
            sportsRoot.optJSONArray("events")
        )
        val games = mutableListOf<JSONObject>()
        buckets.forEach { array ->
            if (array != null) {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let { games.add(it) }
                }
            }
        }
        return games
    }

    private fun parseTeams(obj: JSONObject): List<TeamModel> {
        val teams = mutableListOf<TeamModel>()
        val teamsArray = obj.optJSONArray("teams")
        if (teamsArray != null) {
            for (index in 0 until teamsArray.length()) {
                val teamObj = teamsArray.optJSONObject(index) ?: continue
                parseTeam(teamObj)?.let { teams.add(it) }
            }
            return teams
        }

        obj.optJSONObject("teams")?.let { teamsObject ->
            val home = teamsObject.optJSONObject("home") ?: teamsObject.optJSONObject("home_team")
            val away = teamsObject.optJSONObject("away") ?: teamsObject.optJSONObject("away_team")
            val primaryPair = listOfNotNull(home, away)
            val fallbackPair = listOfNotNull(
                teamsObject.optJSONObject("team1"),
                teamsObject.optJSONObject("team2")
            )
            val selectedTeams = if (primaryPair.isNotEmpty()) primaryPair else fallbackPair
            selectedTeams.forEach { teamObj ->
                parseTeam(teamObj)?.let { teams.add(it) }
            }
            if (teams.isNotEmpty()) return teams
        }

        val homeTeam = obj.optJSONObject("home_team") ?: obj.optJSONObject("team1")
        val awayTeam = obj.optJSONObject("away_team") ?: obj.optJSONObject("team2")
        listOf(homeTeam, awayTeam).forEach { teamObj ->
            parseTeam(teamObj)?.let { teams.add(it) }
        }
        return teams
    }

    private fun parseTeam(teamObj: JSONObject?): TeamModel? {
        if (teamObj == null) return null
        val nested = teamObj.optJSONObject("team")
        val name = nested?.firstString("name", "team_name") ?: teamObj.firstString("name", "team_name")
        val score = teamObj.firstString("score", "points", "runs", "goals")
        val logo = nested?.firstString("thumbnail", "logo", "logo_url", "image")
            ?: teamObj.firstString("thumbnail", "logo", "logo_url", "image")
        if (name == null && score == null && logo == null) return null
        return TeamModel(
            name = name,
            score = score,
            logoUrl = logo,
            isWinner = null
        )
    }

    private fun parseHighlights(obj: JSONObject): HighlightModel? {
        val highlightObj = obj.optJSONObject("video_highlights")
            ?: obj.optJSONObject("highlights")
            ?: return null
        val link = highlightObj.firstString("link", "url")
        val thumbnail = highlightObj.firstString("thumbnail", "image")
        val duration = highlightObj.firstString("duration")
        if (link == null && thumbnail == null && duration == null) return null
        return HighlightModel(link = link, thumbnail = thumbnail, duration = duration)
    }

    private fun statusBucketFor(status: String?, teams: List<TeamModel>): SportsMatchStatusBucket {
        val normalized = status?.lowercase()
        val scorePresent = teams.size >= 2 && teams.any { !it.score.isNullOrBlank() }
        if (normalized != null) {
            if (normalized.contains("live") ||
                normalized.contains("in progress") ||
                normalized.contains("in-play") ||
                normalized.contains("period") ||
                normalized.contains("quarter") ||
                normalized.contains("inning") ||
                normalized.contains("half") ||
                normalized.contains("overtime") ||
                normalized.contains("ot") ||
                Regex("""\bq\d\b""").containsMatchIn(normalized)
            ) {
                return SportsMatchStatusBucket.LIVE
            }
            if (normalized.contains("final") ||
                normalized.contains("ended") ||
                normalized.contains("ft") ||
                normalized.contains("full") ||
                normalized.contains("completed")
            ) {
                return SportsMatchStatusBucket.COMPLETED
            }
        }
        return if (scorePresent) SportsMatchStatusBucket.COMPLETED else SportsMatchStatusBucket.UPCOMING
    }

    private fun parseRanking(rankings: Any?): String? {
        if (rankings == null) return null
        if (rankings is String) return rankings.ifBlank { null }
        if (rankings is JSONArray && rankings.length() > 0) {
            val first = rankings.opt(0)
            return parseRanking(first)
        }
        if (rankings is JSONObject) {
            return rankings.firstString("rank", "position", "name", "title")
        }
        return null
    }

    private fun JSONObject.firstString(vararg keys: String): String? {
        keys.forEach { key ->
            val value = optString(key).ifBlank { null }
            if (value != null) return value
        }
        return null
    }
}
