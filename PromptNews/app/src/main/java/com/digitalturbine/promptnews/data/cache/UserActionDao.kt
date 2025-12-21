package com.digitalturbine.promptnews.data.cache

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface UserActionDao {
    @Upsert
    suspend fun upsertAction(action: UserActionEntity)

    @Query("DELETE FROM user_actions WHERE actionType = :actionType AND targetId = :targetId")
    suspend fun removeAction(actionType: String, targetId: String)

    @Query("SELECT * FROM user_actions WHERE actionType = :actionType")
    suspend fun getActionsByType(actionType: String): List<UserActionEntity>

    @Query("SELECT * FROM user_actions WHERE actionType = :actionType AND targetId = :targetId")
    suspend fun getAction(actionType: String, targetId: String): UserActionEntity?
}
