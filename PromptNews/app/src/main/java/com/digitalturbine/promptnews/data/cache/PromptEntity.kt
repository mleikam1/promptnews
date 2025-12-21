package com.digitalturbine.promptnews.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val intent: String,
    val sources: List<String>,
    val publishers: List<String>,
    val languages: List<String>,
    val keywords: List<String>,
    val fromDateMs: Long?,
    val toDateMs: Long?,
    val safeMode: String,
    val sortMode: String,
    val createdAtMs: Long?
)
