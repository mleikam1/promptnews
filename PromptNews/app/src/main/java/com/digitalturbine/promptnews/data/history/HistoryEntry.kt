package com.digitalturbine.promptnews.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_entries")
data class HistoryEntry(
    @PrimaryKey val id: String,
    val query: String,
    val timestampMs: Long
)
