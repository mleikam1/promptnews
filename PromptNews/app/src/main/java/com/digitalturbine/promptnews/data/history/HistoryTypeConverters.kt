package com.digitalturbine.promptnews.data.history

import androidx.room.TypeConverter

object HistoryTypeConverters {
    @TypeConverter
    fun fromHistoryEntryType(value: HistoryEntryType?): String? = value?.name

    @TypeConverter
    fun toHistoryEntryType(value: String?): HistoryEntryType? =
        value?.let { HistoryEntryType.valueOf(it) }
}
