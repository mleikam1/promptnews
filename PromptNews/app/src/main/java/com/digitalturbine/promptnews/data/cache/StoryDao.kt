package com.digitalturbine.promptnews.data.cache

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface StoryDao {
    @Upsert
    suspend fun upsertStories(stories: List<StoryEntity>)

    @Query("SELECT * FROM stories WHERE storyId = :storyId")
    suspend fun getStory(storyId: String): StoryEntity?

    @Query("SELECT * FROM stories WHERE storyId IN (:storyIds)")
    suspend fun getStories(storyIds: List<String>): List<StoryEntity>
}
