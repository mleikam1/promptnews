package com.digitalturbine.promptnews.data.sports

data class TeamModel(
    val name: String?,
    val logoUrl: String?,
    val score: String?,
    val isWinner: Boolean?
)

data class LeagueContextModel(
    val league: String?,
    val tournament: String?,
    val stage: String?,
    val round: String?,
    val week: String?
)

data class HighlightModel(
    val link: String?,
    val thumbnail: String?,
    val duration: String?
)

enum class SportsMatchStatusBucket {
    LIVE,
    COMPLETED,
    UPCOMING
}

data class SportsMatchModel(
    val id: String?,
    val context: LeagueContextModel?,
    val homeTeam: TeamModel?,
    val awayTeam: TeamModel?,
    val statusBucket: SportsMatchStatusBucket,
    val statusText: String?,
    val dateText: String?,
    val highlight: HighlightModel?,
    val matchLink: String?
)

data class SportsHeaderModel(
    val title: String?,
    val subtitle: String?,
    val thumbnail: String?,
    val tabs: List<String>
)

data class SportsResults(
    val header: SportsHeaderModel?,
    val matches: List<SportsMatchModel>
)

fun LeagueContextModel?.displayText(): String? {
    if (this == null) return null
    val primary = tournament ?: league
    val secondary = listOfNotNull(stage, round, week)
    val parts = buildList {
        primary?.let { add(it) }
        addAll(secondary)
    }
    return parts.joinToString(" · ").ifBlank { null }
}
