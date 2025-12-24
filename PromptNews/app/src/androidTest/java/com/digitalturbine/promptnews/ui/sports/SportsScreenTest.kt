package com.digitalturbine.promptnews.ui.sports

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.digitalturbine.promptnews.data.sports.SportsGame
import com.digitalturbine.promptnews.data.sports.SportsResults
import com.digitalturbine.promptnews.data.sports.SportsTeam
import com.digitalturbine.promptnews.data.sports.TeamOverview
import org.junit.Rule
import org.junit.Test

class SportsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sportsScreenShowsTeamOverview() {
        val results = SportsResults(
            teamOverview = TeamOverview(
                title = "Chicago Bulls",
                ranking = "#3 East",
                thumbnail = null
            ),
            liveGame = null,
            recentGames = listOf(
                SportsGame(
                    date = "2024-04-01",
                    time = "7:00 PM",
                    league = "NBA",
                    status = "Final",
                    teams = listOf(
                        SportsTeam("Bulls", "105", null),
                        SportsTeam("Heat", "98", null)
                    ),
                    score = "105-98",
                    videoHighlights = null
                )
            ),
            upcomingGames = emptyList()
        )

        composeRule.setContent {
            MaterialTheme {
                SportsScreenContent(
                    uiState = SportsUiState.Ready("Bulls", results),
                    query = "Bulls",
                    selectedFilter = SportFilter(
                        "Basketball",
                        Icons.Filled.SportsBasketball,
                        androidx.compose.ui.graphics.Color(0xFFF97316)
                    ),
                    onQueryChange = {},
                    onFilterSelected = {},
                    onSearch = {}
                )
            }
        }

        composeRule.onNodeWithText("Chicago Bulls").assertIsDisplayed()
        composeRule.onNodeWithText("Recent Games").assertIsDisplayed()
    }
}
