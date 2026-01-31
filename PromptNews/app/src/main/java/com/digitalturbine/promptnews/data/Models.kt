package com.digitalturbine.promptnews.data

data class Article(
    val title: String,
    val url: String,
    val imageUrl: String,
    val logoUrl: String,
    val logoUrlDark: String = "",
    val sourceName: String? = null,
    val ageLabel: String? = null,
    val summary: String? = null,
    val isFotoscapes: Boolean = false,
    val fotoscapesUid: String = "",
    val fotoscapesLbtype: String = "",
    val fotoscapesSourceLink: String = "",
    val fotoscapesTitleEn: String = "",
    val fotoscapesSummaryEn: String = "",
    val fotoscapesBodyEn: String = "",
    val fotoscapesPreviewLinks: List<String> = emptyList(),
    val fotoscapesLink: String = ""
)

fun Article.isFotoscapesStory(): Boolean {
    return isFotoscapes ||
        url.contains("fotoscapes.com/lookbook", ignoreCase = true) ||
        fotoscapesUid.isNotBlank() ||
        fotoscapesLbtype.isNotBlank()
}

fun Article.logoUrlForTheme(isDark: Boolean): String {
    return if (isDark && logoUrlDark.isNotBlank()) {
        logoUrlDark
    } else {
        logoUrl
    }
}

data class Clip(
    val title: String,
    val thumbnail: String,
    val url: String,
    val source: String = "YouTube"
)

data class Extras(
    val peopleAlsoAsk: List<String> = emptyList(),
    val relatedSearches: List<String> = emptyList()
)

sealed interface SearchUi {
    object Idle : SearchUi
    data class Searching(val query: String) : SearchUi
    data class Ready(
        val query: String,
        val hero: Article?,
        val rows: List<Article>,
        val hasMore: Boolean,
        val clips: List<Clip>,
        val images: List<String>,
        val extras: Extras
    ) : SearchUi
    data class Error(val message: String) : SearchUi
}
