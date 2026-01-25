package com.digitalturbine.promptnews.data.sports

import java.time.Instant

enum class NflGameStatus {
    SCHEDULED,
    LIVE,
    FINAL
}

data class NflTeam(
    val id: Int?,
    val name: String,
    val logoUrl: String?,
    val score: Int?
)

data class NflGame(
    val id: Int,
    val league: String,
    val status: NflGameStatus,
    val statusShort: String?,
    val statusLong: String?,
    val statusClock: String?,
    val homeTeam: NflTeam,
    val awayTeam: NflTeam,
    val date: Instant,
    val venue: String?
)
