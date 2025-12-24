package com.digitalturbine.promptnews.data.sports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SportsParserTest {

    @Test
    fun `parses normalized sports response`() {
        val payload = """
            {
              "team_overview": {
                "title": "Golden State Warriors",
                "ranking": "#4 West",
                "thumbnail": "warriors.png"
              },
              "live_game": null,
              "recent_games": [
                {
                  "date": "2024-04-02",
                  "time": "7:00 PM",
                  "league": "NBA",
                  "status": "Final",
                  "teams": [
                    {"name": "Warriors", "score": "108", "thumbnail": "gsw.png"},
                    {"name": "Kings", "score": "101", "thumbnail": "sac.png"}
                  ],
                  "score": "108-101",
                  "video_highlights": {"link": "highlights"}
                }
              ],
              "upcoming_games": []
            }
        """.trimIndent()

        val result = SportsParser.parse(payload)
        assertNotNull(result)
        requireNotNull(result)

        assertEquals("Golden State Warriors", result.teamOverview?.title)
        assertEquals(1, result.recentGames.size)
        assertEquals("NBA", result.recentGames.first().league)
    }
}
