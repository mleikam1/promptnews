package com.digitalturbine.promptnews.data.cache

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_bundles",
    indices = [Index(value = ["promptId"]) ]
)
data class CachedBundleEntity(
    @PrimaryKey
    val cacheKey: String,
    val promptId: String?,
    val fetchedAtMs: Long,
    val expiresAtMs: Long,
    val generatedAtMs: Long?,
    val nextPageToken: String?,
    val cacheStaleness: String,
    val relatedPrompts: List<RelatedPromptEntity>
)

data class RelatedPromptEntity(
    val text: String,
    val type: String,
    val score: Double
)
