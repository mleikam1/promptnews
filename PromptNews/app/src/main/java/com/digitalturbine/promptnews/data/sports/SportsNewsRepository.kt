package com.digitalturbine.promptnews.data.sports

import com.digitalturbine.promptnews.data.SearchRepository
import com.digitalturbine.promptnews.data.Story
import com.digitalturbine.promptnews.data.toStory

class SportsNewsRepository(
    private val searchRepository: SearchRepository = SearchRepository()
) {
    suspend fun fetchSportsStories(leagueId: String, limit: Int, offset: Int): List<Story> {
        val query = buildSportsQuery(leagueId)
        return searchRepository.fetchSerpNewsByOffset(query, limit, offset)
            .map { it.toStory() }
    }

    private fun buildSportsQuery(leagueId: String): String {
        val trimmed = leagueId.trim()
        if (trimmed.isBlank()) return "Sports news"
        return if (trimmed.contains("news", ignoreCase = true)) trimmed else "$trimmed news"
    }
}
