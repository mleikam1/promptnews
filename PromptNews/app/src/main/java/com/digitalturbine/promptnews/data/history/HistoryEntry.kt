package com.digitalturbine.promptnews.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HistoryEntryType {
    QUERY,
    ARTICLE_CLICK
}

@Entity(tableName = "history_entries")
data class HistoryEntry(
    @PrimaryKey val id: String,
    val type: HistoryEntryType,
    val query: String?,
    val url: String?,
    val title: String?,
    val timestampMs: Long
)
