package com.digitalturbine.promptnews.data.cache

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PromptDao {
    @Upsert
    suspend fun upsert(prompt: PromptEntity)

    @Query("SELECT * FROM prompts WHERE id = :id")
    suspend fun getPrompt(id: String): PromptEntity?
}
