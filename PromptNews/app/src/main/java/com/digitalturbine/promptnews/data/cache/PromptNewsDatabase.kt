package com.digitalturbine.promptnews.data.cache

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PromptEntity::class,
        CachedBundleEntity::class,
        CachedBundleItemEntity::class,
        StoryEntity::class,
        UserActionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(CacheTypeConverters::class)
abstract class PromptNewsDatabase : RoomDatabase() {
    abstract fun promptDao(): PromptDao
    abstract fun cachedBundleDao(): CachedBundleDao
    abstract fun storyDao(): StoryDao
    abstract fun userActionDao(): UserActionDao
}
