package com.digitalturbine.promptnews.data.cache

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

object CacheTypeConverters {

    @TypeConverter
    fun stringListToJson(value: List<String>?): String? {
        if (value == null) return null
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun jsonToStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val array = JSONArray(value)
        return List(array.length()) { index -> array.optString(index) }
    }

    @TypeConverter
    fun relatedPromptsToJson(value: List<RelatedPromptEntity>?): String? {
        if (value == null) return null
        val array = JSONArray()
        value.forEach { prompt ->
            val obj = JSONObject()
                .put("text", prompt.text)
                .put("type", prompt.type)
                .put("score", prompt.score)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun jsonToRelatedPrompts(value: String?): List<RelatedPromptEntity> {
        if (value.isNullOrBlank()) return emptyList()
        val array = JSONArray(value)
        return List(array.length()) { index ->
            val obj = array.getJSONObject(index)
            RelatedPromptEntity(
                text = obj.optString("text"),
                type = obj.optString("type"),
                score = obj.optDouble("score")
            )
        }
    }

    @TypeConverter
    fun namedEntitiesToJson(value: List<NamedEntityEntity>?): String? {
        if (value == null) return null
        val array = JSONArray()
        value.forEach { entity ->
            val obj = JSONObject()
                .put("name", entity.name)
                .put("type", entity.type)
                .put("relevance", entity.relevance)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun jsonToNamedEntities(value: String?): List<NamedEntityEntity> {
        if (value.isNullOrBlank()) return emptyList()
        val array = JSONArray(value)
        return List(array.length()) { index ->
            val obj = array.getJSONObject(index)
            NamedEntityEntity(
                name = obj.optString("name"),
                type = obj.optString("type"),
                relevance = obj.optDouble("relevance")
            )
        }
    }

    @TypeConverter
    fun publisherToJson(value: PublisherEntity?): String? {
        if (value == null) return null
        return JSONObject()
            .put("name", value.name)
            .put("domain", value.domain)
            .put("iconUrl", value.iconUrl)
            .put("credibilityScore", value.credibilityScore)
            .toString()
    }

    @TypeConverter
    fun jsonToPublisher(value: String?): PublisherEntity? {
        if (value.isNullOrBlank()) return null
        val obj = JSONObject(value)
        return PublisherEntity(
            name = obj.optString("name"),
            domain = obj.optString("domain").ifBlank { null },
            iconUrl = obj.optString("iconUrl").ifBlank { null },
            credibilityScore = if (obj.has("credibilityScore")) obj.optDouble("credibilityScore") else null
        )
    }

    @TypeConverter
    fun sentimentToJson(value: SentimentEntity?): String? {
        if (value == null) return null
        return JSONObject()
            .put("score", value.score)
            .put("label", value.label)
            .put("confidence", value.confidence)
            .toString()
    }

    @TypeConverter
    fun jsonToSentiment(value: String?): SentimentEntity? {
        if (value.isNullOrBlank()) return null
        val obj = JSONObject(value)
        return SentimentEntity(
            score = obj.optDouble("score"),
            label = obj.optString("label"),
            confidence = obj.optDouble("confidence")
        )
    }
}
