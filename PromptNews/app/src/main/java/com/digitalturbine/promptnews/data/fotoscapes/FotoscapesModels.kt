package com.digitalturbine.promptnews.data.fotoscapes

data class FotoscapesArticle(
    val id: String,
    val lbType: String,
    val title: String,
    val summary: String,
    val body: String,
    val imageUrl: String?,
    val articleUrl: String
)
