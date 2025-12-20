package com.digitalturbine.promptnews.data

data class Article(
    val title: String,
    val url: String,
    val imageUrl: String,
    val logoUrl: String,
    val sourceName: String? = null,
    val ageLabel: String? = null,
    val interest: String = "news",
    val isFotoscapes: Boolean = false
)

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
