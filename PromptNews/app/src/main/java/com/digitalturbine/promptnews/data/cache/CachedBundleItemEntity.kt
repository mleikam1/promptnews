package com.digitalturbine.promptnews.data.cache

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "cached_bundle_items",
    primaryKeys = ["cacheKey", "storyId"],
    indices = [Index(value = ["cacheKey"]), Index(value = ["storyId"]) ]
)
data class CachedBundleItemEntity(
    val cacheKey: String,
    val storyId: String,
    val sortOrder: Int
)
