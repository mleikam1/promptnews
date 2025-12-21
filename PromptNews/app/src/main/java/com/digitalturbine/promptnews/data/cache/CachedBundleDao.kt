package com.digitalturbine.promptnews.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface CachedBundleDao {
    @Upsert
    suspend fun upsertBundle(bundle: CachedBundleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBundleItems(items: List<CachedBundleItemEntity>)

    @Query("DELETE FROM cached_bundle_items WHERE cacheKey = :cacheKey")
    suspend fun clearBundleItems(cacheKey: String)

    @Query("SELECT * FROM cached_bundles WHERE cacheKey = :cacheKey")
    suspend fun getBundle(cacheKey: String): CachedBundleEntity?

    @Query(
        """
        SELECT stories.*
        FROM stories
        INNER JOIN cached_bundle_items
            ON stories.storyId = cached_bundle_items.storyId
        WHERE cached_bundle_items.cacheKey = :cacheKey
        ORDER BY cached_bundle_items.sortOrder
        """
    )
    suspend fun getStoriesForCacheKey(cacheKey: String): List<StoryEntity>

    @Transaction
    suspend fun replaceBundleItems(cacheKey: String, items: List<CachedBundleItemEntity>) {
        clearBundleItems(cacheKey)
        if (items.isNotEmpty()) {
            insertBundleItems(items)
        }
    }
}
