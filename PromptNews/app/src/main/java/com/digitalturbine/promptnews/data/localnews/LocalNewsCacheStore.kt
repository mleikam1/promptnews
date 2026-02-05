package com.digitalturbine.promptnews.data.localnews

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert

private const val LOCAL_NEWS_DB_NAME = "local_news_cache.db"

@Entity(tableName = "cached_local_news")
data class CachedLocalNews(
    @PrimaryKey val locationKey: String,
    val articlesJson: String,
    val fetchedAt: Long
)

@Dao
interface CachedLocalNewsDao {
    @Query("SELECT * FROM cached_local_news WHERE locationKey = :locationKey LIMIT 1")
    suspend fun getByLocation(locationKey: String): CachedLocalNews?

    @Upsert
    suspend fun upsert(entry: CachedLocalNews)

    @Query("DELETE FROM cached_local_news WHERE fetchedAt < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)
}

@Database(entities = [CachedLocalNews::class], version = 1, exportSchema = true)
abstract class LocalNewsCacheDatabase : RoomDatabase() {
    abstract fun cachedLocalNewsDao(): CachedLocalNewsDao

    companion object {
        @Volatile
        private var instance: LocalNewsCacheDatabase? = null

        fun getInstance(context: Context): LocalNewsCacheDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalNewsCacheDatabase::class.java,
                    LOCAL_NEWS_DB_NAME
                ).fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
