package com.digitalturbine.promptnews.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HistoryType {
    CHIP,
    SEARCH
}

@Entity(tableName = "history_entries")
data class HistoryEntry(
    @PrimaryKey val id: String,
    val type: HistoryType,
    val label: String,
    val timestampMs: Long
)
