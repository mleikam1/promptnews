package com.digitalturbine.promptnews.data.sports

data class TeamOverview(
    val title: String?,
    val ranking: String?,
    val thumbnail: String?
)

data class VideoHighlights(
    val link: String?,
    val thumbnail: String?
)

data class SportsTeam(
    val name: String?,
    val score: String?,
    val thumbnail: String?
)

data class SportsGame(
    val date: String?,
    val time: String?,
    val league: String?,
    val status: String?,
    val teams: List<SportsTeam>,
    val score: String?,
    val videoHighlights: VideoHighlights?
)

data class SportsResults(
    val teamOverview: TeamOverview?,
    val liveGame: SportsGame?,
    val recentGames: List<SportsGame>,
    val upcomingGames: List<SportsGame>
)
