package com.digitalturbine.promptnews.data.sports

data class NflGamesResponse(
    val response: List<NflGameResponse>?
)

data class NflGameResponse(
    val game: NflGameInfo?,
    val league: NflLeagueInfo?,
    val teams: NflTeamsResponse?,
    val scores: NflScoresResponse?
)

data class NflGameInfo(
    val id: Int?,
    val date: String?,
    val timestamp: Long?,
    val status: NflGameStatusResponse?,
    val venue: NflVenueInfo?
)

data class NflGameStatusResponse(
    val short: String?,
    val long: String?,
    val timer: String?
)

data class NflLeagueInfo(
    val name: String?
)

data class NflVenueInfo(
    val name: String?
)

data class NflTeamsResponse(
    val home: NflTeamInfo?,
    val away: NflTeamInfo?
)

data class NflTeamInfo(
    val id: Int?,
    val name: String?,
    val logo: String?
)

data class NflScoresResponse(
    val home: NflScoreInfo?,
    val away: NflScoreInfo?
)

data class NflScoreInfo(
    val total: Int?
)
