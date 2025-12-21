package com.digitalturbine.promptnews.data.common

object CacheKeyBuilder {

    private const val VERSION = "v1"

    fun build(
        prompt: String,
        locale: String?,
        geo: String?,
        filters: Map<String, String> = emptyMap()
    ): String {
        val normalizedPrompt = normalizePrompt(prompt)
        val normalizedLocale = normalizeOptional(locale)
        val normalizedGeo = normalizeOptional(geo)
        val normalizedFilters = normalizeFilters(filters)

        return listOf(
            "version=$VERSION",
            "prompt=${escape(normalizedPrompt)}",
            "locale=${escape(normalizedLocale)}",
            "geo=${escape(normalizedGeo)}",
            "filters=${escape(normalizedFilters)}"
        ).joinToString("|")
    }

    private fun normalizePrompt(prompt: String): String {
        return prompt.trim().lowercase().replace(Regex("\\s+"), " ")
    }

    private fun normalizeOptional(value: String?): String {
        return value?.trim()?.lowercase().orEmpty()
    }

    private fun normalizeFilters(filters: Map<String, String>): String {
        if (filters.isEmpty()) return ""
        return filters.toSortedMap().entries.joinToString("&") { (key, value) ->
            "${key.trim().lowercase()}=${value.trim().lowercase()}"
        }
    }

    private fun escape(value: String): String {
        return value.replace("|", "%7C")
    }
}
