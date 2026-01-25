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

        assertEquals("Golden State Warriors", result.header?.title)
        assertEquals(1, result.matches.size)
        assertEquals("NBA", result.matches.first().context?.league)
    }

    @Test
    fun `parses serpapi sports_results with upcoming games`() {
        val payload = """
            {
              "sports_results": {
                "title": "Los Angeles Lakers",
                "thumbnail": "lakers.png",
                "rankings": "#4 West",
                "upcoming_games": [
                  {
                    "status": "Scheduled",
                    "date": "2024-10-01",
                    "teams": [
                      {"name": "Lakers", "thumbnail": "lal.png"},
                      {"name": "Celtics", "thumbnail": "bos.png"}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val result = SportsParser.parse(payload)
        assertNotNull(result)
        requireNotNull(result)

        assertEquals("Los Angeles Lakers", result.header?.title)
        assertEquals("#4 West", result.header?.subtitle)
        assertEquals(1, result.matches.size)
        assertEquals(SportsMatchStatusBucket.UPCOMING, result.matches.first().statusBucket)
        assertEquals("Lakers", result.matches.first().homeTeam?.name)
    }
}
