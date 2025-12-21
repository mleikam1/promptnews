package com.digitalturbine.promptnews.data.common

import java.net.URI

object UrlCanonicalizer {

    fun canonicalize(url: String): String {
        if (url.isBlank()) return url
        return try {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase()
            val host = uri.host?.lowercase()
            val filteredQuery = filterQuery(uri.rawQuery)
            val normalized = URI(
                scheme,
                uri.userInfo,
                host,
                uri.port,
                uri.rawPath,
                filteredQuery,
                null
            )
            normalized.toString()
        } catch (exception: Exception) {
            url
        }
    }

    private fun filterQuery(rawQuery: String?): String? {
        if (rawQuery.isNullOrBlank()) return null
        val keptParams = rawQuery.split("&")
            .filter { it.isNotEmpty() }
            .mapNotNull { param ->
                val name = param.substringBefore("=", param)
                if (shouldDropParam(name)) null else param
            }
        return if (keptParams.isEmpty()) null else keptParams.joinToString("&")
    }

    private fun shouldDropParam(name: String): Boolean {
        val normalized = name.lowercase()
        return normalized.startsWith("utm_") || normalized == "gclid" || normalized == "fbclid"
    }
}
