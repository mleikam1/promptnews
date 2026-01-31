package com.digitalturbine.promptnews.util

import android.annotation.SuppressLint
import java.time.Instant

fun isSportsIntent(query: String): Boolean {
    val t = query.lowercase()
    return Regex(
        "\\b(nfl|nba|ncaa|mlb|nhl|soccer|premier league|la liga|serie a|bundesliga|ligue 1|" +
            "mls|uefa|champions league|europa|world cup|score|scores|match|matches|standings|" +
            "playoffs|final|highlights|fixtures|kickoff)\\b"
    ).containsMatchIn(t)
}

fun isNflIntent(query: String): Boolean {
    val t = query.lowercase()
    if (Regex("\\b(nfl|super bowl|pro football|afc|nfc)\\b").containsMatchIn(t)) {
        return true
    }
    return nflTeamKeywords.any { keyword ->
        Regex("\\b${Regex.escape(keyword.lowercase())}\\b").containsMatchIn(t)
    }
}

private val nflTeamKeywords = listOf(
    "arizona cardinals",
    "atlanta falcons",
    "baltimore ravens",
    "buffalo bills",
    "carolina panthers",
    "chicago bears",
    "cincinnati bengals",
    "cleveland browns",
    "dallas cowboys",
    "denver broncos",
    "detroit lions",
    "green bay packers",
    "houston texans",
    "indianapolis colts",
    "jacksonville jaguars",
    "kansas city chiefs",
    "las vegas raiders",
    "los angeles chargers",
    "los angeles rams",
    "miami dolphins",
    "minnesota vikings",
    "new england patriots",
    "new orleans saints",
    "new york giants",
    "new york jets",
    "philadelphia eagles",
    "pittsburgh steelers",
    "san francisco 49ers",
    "seattle seahawks",
    "tampa bay buccaneers",
    "tennessee titans",
    "washington commanders",
    "cardinals",
    "falcons",
    "ravens",
    "bills",
    "panthers",
    "bears",
    "bengals",
    "browns",
    "cowboys",
    "broncos",
    "lions",
    "packers",
    "texans",
    "colts",
    "jaguars",
    "chiefs",
    "raiders",
    "chargers",
    "rams",
    "dolphins",
    "vikings",
    "patriots",
    "saints",
    "giants",
    "jets",
    "eagles",
    "steelers",
    "49ers",
    "seahawks",
    "buccaneers",
    "titans",
    "commanders"
)

@SuppressLint("NewApi")
fun Instant.toEpochMillisCompat(): Long = toEpochMilli()

fun faviconFrom(articleUrl: String?, size: Int = 64): String? {
    if (articleUrl.isNullOrBlank()) return null
    return try {
        val u = java.net.URI(articleUrl)
        "https://www.google.com/s2/favicons?domain=${u.scheme}://${u.host}&sz=$size"
    } catch (_: Exception) { null }
}
