package com.digitalturbine.promptnews.ui.sports

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.digitalturbine.promptnews.data.sports.LeagueContextModel
import com.digitalturbine.promptnews.data.sports.SportsHeaderModel
import com.digitalturbine.promptnews.data.sports.SportsMatchModel
import com.digitalturbine.promptnews.data.sports.SportsMatchStatusBucket
import com.digitalturbine.promptnews.data.sports.SportsResults
import com.digitalturbine.promptnews.data.sports.TeamModel
import org.junit.Rule
import org.junit.Test

class SportsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sportsScreenShowsTeamOverview() {
        val results = SportsResults(
            header = SportsHeaderModel(
                title = "Chicago Bulls",
                subtitle = "#3 East",
                thumbnail = null,
                tabs = listOf("Matches", "News", "Standings")
            ),
            matches = listOf(
                SportsMatchModel(
                    id = "bulls-heat",
                    context = LeagueContextModel(
                        league = "NBA",
                        tournament = null,
                        stage = "Final",
                        stadium = null,
                        round = null,
                        week = null
                    ),
                    homeTeam = TeamModel("Bulls", null, "105", true),
                    awayTeam = TeamModel("Heat", null, "98", false),
                    statusBucket = SportsMatchStatusBucket.COMPLETED,
                    statusText = "Final",
                    dateText = "7:00 PM",
                    highlight = null,
                    matchLink = null
                )
            )
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
        composeRule.onNodeWithText("Matches").assertIsDisplayed()
    }
}
