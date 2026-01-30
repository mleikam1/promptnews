package com.digitalturbine.promptnews.ui.fotoscapes

import android.util.Log
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.fotoscapes.FotoscapesArticle

sealed interface FotoscapesUi {
    val uid: String
    val lbtype: String
    val title: String
}

data class FotoscapesArticleUi(
    override val uid: String,
    override val lbtype: String,
    override val title: String,
    val body: String,
    val imageUrl: String?
) : FotoscapesUi

data class FotoscapesExternalUi(
    override val uid: String,
    override val lbtype: String,
    override val title: String,
    val imageUrl: String?,
    val link: String
) : FotoscapesUi

fun Article.toFotoscapesUi(): FotoscapesUi {
    val titleEn = fotoscapesTitleEn.ifBlank { title }
    val lbtypeValue = fotoscapesLbtype
    Log.d("Fotoscapes", "Render uid=$fotoscapesUid lbtype=$lbtypeValue title=$titleEn")
    return if (lbtypeValue.equals("article", ignoreCase = true)) {
        val bodyText = fotoscapesSummaryEn.ifBlank { fotoscapesBodyEn }.ifBlank { summary.orEmpty() }
        FotoscapesArticleUi(
            uid = fotoscapesUid,
            lbtype = lbtypeValue,
            title = titleEn,
            body = bodyText,
            imageUrl = fotoscapesPreviewLinks.firstOrNull()?.ifBlank { null } ?: imageUrl.ifBlank { null }
        )
    } else {
        FotoscapesExternalUi(
            uid = fotoscapesUid,
            lbtype = lbtypeValue,
            title = titleEn,
            imageUrl = fotoscapesPreviewLinks.firstOrNull()?.ifBlank { null } ?: imageUrl.ifBlank { null },
            link = fotoscapesLink.ifBlank { fotoscapesSourceLink }
        )
    }
}

fun FotoscapesArticle.toFotoscapesArticleUi(): FotoscapesArticleUi {
    Log.d("Fotoscapes", "Render uid=$id lbtype=$lbType title=$title")
    val bodyText = summary.ifBlank { body }
    return FotoscapesArticleUi(
        uid = id,
        lbtype = lbType,
        title = title,
        body = bodyText,
        imageUrl = imageUrl
    )
}
