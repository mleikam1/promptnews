package com.digitalturbine.promptnews.util

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.max

class InterestTracker private constructor(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordInteraction(query: String?) {
        val normalized = normalizeQuery(query)
        if (normalized.isBlank()) return

        applyDecayIfNeeded()
        val keywords = extractKeywords(normalized)
        val scores = loadScores()
        val history = loadHistory()

        keywords.forEach { keyword ->
            scores[keyword] = (scores[keyword] ?: 0) + 1
        }

        history.add(keywords)
        trimHistory(history, scores)

        saveScores(scores)
        saveHistory(history)

        Log.d(
            TAG,
            "Personalization: updated interests for query=\"$normalized\" keywords=$keywords scores=$scores"
        )
    }

    fun getSuggestedPrompts(): List<String> {
        applyDecayIfNeeded()
        val history = loadHistory()
        if (history.size < MIN_INTERACTIONS) {
            Log.d(TAG, "Personalization: suggestions hidden (insufficient data, interactions=${history.size})")
            return emptyList()
        }

        val scores = loadScores().filterValues { it > 0 }
        if (scores.isEmpty()) {
            Log.d(TAG, "Personalization: suggestions hidden (no keyword scores)")
            return emptyList()
        }

        val topKeywords = scores.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(MAX_TOP_KEYWORDS)
            .map { it.key }

        if (topKeywords.isEmpty()) {
            Log.d(TAG, "Personalization: suggestions hidden (no top keywords)")
            return emptyList()
        }

        val suggestions = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        topKeywords.forEach { keyword ->
            suggestionOptionsFor(keyword).forEach { option ->
                if (suggestions.size < MAX_SUGGESTIONS && seen.add(option)) {
                    suggestions.add(option)
                }
            }
        }

        if (suggestions.size < MAX_SUGGESTIONS) {
            val fallbackKeyword = topKeywords.first()
            fallbackOptionsFor(fallbackKeyword).forEach { option ->
                if (suggestions.size < MAX_SUGGESTIONS && seen.add(option)) {
                    suggestions.add(option)
                }
            }
        }

        if (suggestions.size < MAX_SUGGESTIONS && topKeywords.size > 1) {
            val fallbackKeyword = topKeywords.last()
            fallbackOptionsFor(fallbackKeyword).forEach { option ->
                if (suggestions.size < MAX_SUGGESTIONS && seen.add(option)) {
                    suggestions.add(option)
                }
            }
        }

        if (suggestions.size < MAX_SUGGESTIONS) {
            Log.d(TAG, "Personalization: suggestions hidden (insufficient suggestions)")
            return emptyList()
        }

        val topKeywordScores = topKeywords.joinToString(
            prefix = "[",
            postfix = "]"
        ) { keyword -> "$keyword=${scores[keyword] ?: 0}" }
        Log.d(TAG, "Personalization: top keywords = $topKeywordScores suggestions=$suggestions")

        return suggestions.take(MAX_SUGGESTIONS)
    }

    private fun normalizeQuery(query: String?): String {
        return query?.trim()?.lowercase(Locale.getDefault()).orEmpty()
    }

    private fun extractKeywords(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val tokens = query.split(Regex("\\W+"))
        return tokens
            .filter { it.isNotBlank() }
            .map { it.lowercase(Locale.getDefault()) }
            .filter { KEYWORDS.contains(it) }
            .distinct()
    }

    private fun trimHistory(
        history: MutableList<List<String>>,
        scores: MutableMap<String, Int>
    ) {
        while (history.size > MAX_HISTORY) {
            val removed = history.removeAt(0)
            removed.forEach { keyword ->
                scores[keyword] = max(0, (scores[keyword] ?: 0) - 1)
            }
        }
    }

    private fun loadScores(): MutableMap<String, Int> {
        val raw = prefs.getString(KEY_SCORES, null) ?: return mutableMapOf()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { key -> json.optInt(key, 0) }.toMutableMap()
        }.getOrElse { mutableMapOf() }
    }

    private fun saveScores(scores: Map<String, Int>) {
        val json = JSONObject()
        scores.forEach { (key, value) ->
            json.put(key, value)
        }
        prefs.edit().putString(KEY_SCORES, json.toString()).apply()
    }

    private fun loadHistory(): MutableList<List<String>> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return mutableListOf()
        return runCatching {
            val array = JSONArray(raw)
            val list = mutableListOf<List<String>>()
            for (index in 0 until array.length()) {
                val entry = array.optJSONArray(index) ?: JSONArray()
                val keywords = mutableListOf<String>()
                for (k in 0 until entry.length()) {
                    val keyword = entry.optString(k)
                    if (keyword.isNotBlank()) {
                        keywords.add(keyword)
                    }
                }
                list.add(keywords)
            }
            list
        }.getOrElse { mutableListOf() }
    }

    private fun saveHistory(history: List<List<String>>) {
        val array = JSONArray()
        history.forEach { entry ->
            val keywords = JSONArray()
            entry.forEach { keyword -> keywords.put(keyword) }
            array.put(keywords)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    private fun applyDecayIfNeeded(nowMs: Long = System.currentTimeMillis()) {
        val lastDecay = prefs.getLong(KEY_LAST_DECAY, 0L)
        if (lastDecay == 0L) {
            prefs.edit().putLong(KEY_LAST_DECAY, nowMs).apply()
            return
        }
        if (nowMs <= lastDecay) return
        val steps = ((nowMs - lastDecay) / MILLIS_IN_DAY).toInt()
        if (steps <= 0) return

        val scores = loadScores()
        if (scores.isNotEmpty()) {
            scores.keys.forEach { key ->
                scores[key] = max(0, (scores[key] ?: 0) - steps)
            }
            saveScores(scores)
        }
        prefs.edit().putLong(KEY_LAST_DECAY, lastDecay + steps * MILLIS_IN_DAY).apply()
    }

    private fun suggestionOptionsFor(keyword: String): List<String> {
        return when (keyword) {
            "sports" -> listOf("Top sports headlines today", "Latest sports news", "Sports scores today")
            "nfl" -> listOf("Latest NFL news", "NFL scores today", "NFL trade updates")
            "nba" -> listOf("Latest NBA news", "NBA scores today", "NBA playoff updates")
            "mlb" -> listOf("Latest MLB news", "MLB scores today", "MLB highlights today")
            "football" -> listOf("Latest football news", "Football scores today", "College football updates")
            "basketball" -> listOf("Latest basketball news", "Basketball highlights today", "Top basketball headlines")
            "tech" -> listOf("Top tech headlines today", "Latest gadgets and tech news", "Tech industry updates")
            "ai" -> listOf("Breaking AI news", "AI research updates", "AI product launches")
            "politics" -> listOf("Politics headlines today", "Election updates", "Policy news today")
            "finance" -> listOf("Stock market updates", "Finance news today", "Market movers today")
            "weather" -> listOf("Weather forecast updates", "Severe weather alerts", "Storm tracking updates")
            "entertainment" -> listOf("Entertainment news today", "Celebrity news updates", "Movie and TV headlines")
            else -> listOf("Top headlines today", "Breaking news updates", "Latest news today")
        }
    }

    private fun fallbackOptionsFor(keyword: String): List<String> {
        val display = KEYWORD_DISPLAY_NAME[keyword] ?: keyword
        return listOf(
            "Latest $display news",
            "Top $display headlines today",
            "Breaking $display news"
        )
    }

    companion object {
        private const val TAG = "Personalization"
        private const val PREFS_NAME = "interest_tracker"
        private const val KEY_SCORES = "scores_json"
        private const val KEY_HISTORY = "history_json"
        private const val KEY_LAST_DECAY = "last_decay_ms"
        private const val MAX_HISTORY = 50
        private const val MIN_INTERACTIONS = 3
        private const val MAX_SUGGESTIONS = 3
        private const val MAX_TOP_KEYWORDS = 2
        private const val MILLIS_IN_DAY = 24 * 60 * 60 * 1000L
        private val KEYWORDS = setOf(
            "sports",
            "nfl",
            "nba",
            "mlb",
            "football",
            "basketball",
            "tech",
            "ai",
            "politics",
            "finance",
            "weather",
            "entertainment"
        )
        private val KEYWORD_DISPLAY_NAME = mapOf(
            "sports" to "sports",
            "nfl" to "NFL",
            "nba" to "NBA",
            "mlb" to "MLB",
            "football" to "football",
            "basketball" to "basketball",
            "tech" to "tech",
            "ai" to "AI",
            "politics" to "politics",
            "finance" to "finance",
            "weather" to "weather",
            "entertainment" to "entertainment"
        )

        @Volatile
        private var instance: InterestTracker? = null

        fun getInstance(context: Context): InterestTracker {
            return instance ?: synchronized(this) {
                instance ?: InterestTracker(context.applicationContext).also { instance = it }
            }
        }
    }
}
