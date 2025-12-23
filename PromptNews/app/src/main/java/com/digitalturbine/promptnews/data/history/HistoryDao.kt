package com.digitalturbine.promptnews.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry)

    @Query("SELECT * FROM history_entries WHERE timestampMs >= :sinceMs ORDER BY timestampMs DESC")
    fun observeEntriesSince(sinceMs: Long): Flow<List<HistoryEntry>>

    @Query("DELETE FROM history_entries WHERE timestampMs < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)
}
