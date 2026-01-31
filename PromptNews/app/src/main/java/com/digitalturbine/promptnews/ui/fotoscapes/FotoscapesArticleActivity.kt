package com.digitalturbine.promptnews.ui.fotoscapes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.digitalturbine.promptnews.ui.components.FotoscapesArticleCard
import com.digitalturbine.promptnews.ui.PromptNewsHeaderBar

class FotoscapesArticleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        if (title.isBlank()) {
            finish()
            return
        }
        val ui = FotoscapesArticleUi(
            uid = intent.getStringExtra(EXTRA_UID).orEmpty(),
            lbtype = LBTYPE_ARTICLE,
            title = title,
            body = body,
            imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)
        )
        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        PromptNewsHeaderBar(
                            showBack = true,
                            onBackClick = { onBackPressedDispatcher.onBackPressed() },
                            onProfileClick = {}
                        )
                    }
                ) { padding ->
                    FotoscapesArticleCard(ui, modifier = Modifier.padding(padding))
                }
            }
        }
    }

    companion object {
        private const val EXTRA_UID = "extra_uid"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_BODY = "extra_body"
        private const val EXTRA_IMAGE_URL = "extra_image_url"
        private const val LBTYPE_ARTICLE = "article"

        fun start(context: Context, article: FotoscapesArticleUi) {
            context.startActivity(
                Intent(context, FotoscapesArticleActivity::class.java).apply {
                    putExtra(EXTRA_UID, article.uid)
                    putExtra(EXTRA_TITLE, article.title)
                    putExtra(EXTRA_BODY, article.body)
                    putExtra(EXTRA_IMAGE_URL, article.imageUrl)
                }
            )
        }
    }
}
