package com.digitalturbine.promptnews.data.cache

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "user_actions",
    primaryKeys = ["actionType", "targetId"],
    indices = [Index(value = ["actionType"])]
)
data class UserActionEntity(
    val actionType: String,
    val targetId: String,
    val targetType: String,
    val createdAtMs: Long?
)

enum class UserActionType {
    BOOKMARK,
    HIDE,
    FOLLOW
}
