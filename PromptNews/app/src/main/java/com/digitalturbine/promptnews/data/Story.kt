package com.digitalturbine.promptnews.data

data class Story(
    val title: String,
    val url: String,
    val imageUrl: String,
    val logoUrl: String,
    val sourceName: String? = null,
    val ageLabel: String? = null,
    val interest: String = "news",
    val isFotoscapes: Boolean = false
)

fun Article.toStory(): Story = Story(
    title = title,
    url = url,
    imageUrl = imageUrl,
    logoUrl = logoUrl,
    sourceName = sourceName,
    ageLabel = ageLabel,
    interest = interest,
    isFotoscapes = isFotoscapes
)

fun Story.toArticle(): Article = Article(
    title = title,
    url = url,
    imageUrl = imageUrl,
    logoUrl = logoUrl,
    sourceName = sourceName,
    ageLabel = ageLabel,
    interest = interest,
    isFotoscapes = isFotoscapes
)
