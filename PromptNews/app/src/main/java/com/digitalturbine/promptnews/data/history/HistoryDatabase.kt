package com.digitalturbine.promptnews.data.history

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

private const val HISTORY_DB_NAME = "history.db"

@Database(
    entities = [HistoryEntry::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(HistoryTypeConverters::class)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: HistoryDatabase? = null

        fun getInstance(context: Context): HistoryDatabase {
            return instance ?: synchronized(this) {
                instance
                    ?: Room.databaseBuilder(
                        context.applicationContext,
                        HistoryDatabase::class.java,
                        HISTORY_DB_NAME
                    ).build().also { instance = it }
            }
        }
    }
}

class HistoryTypeConverters {
    @TypeConverter
    fun toType(value: String?): HistoryType? {
        return value?.let { HistoryType.valueOf(it) }
    }

    @TypeConverter
    fun fromType(type: HistoryType?): String? {
        return type?.name
    }
}
