package com.digitalturbine.promptnews.web

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebResourceError
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.digitalturbine.promptnews.ui.PromptNewsHeaderBar

class ArticleWebViewActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ArticleWebView"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialUrl = intent.getStringExtra("url") ?: "about:blank"
        Log.d(TAG, "Loading URL: $initialUrl")

        setContent {
            var progress by remember { mutableFloatStateOf(0f) }
            var isLoading by remember { mutableStateOf(true) }
            var hasError by remember { mutableStateOf(false) }
            var hasContent by remember { mutableStateOf(false) }
            Scaffold(
                topBar = {
                    PromptNewsHeaderBar(
                        showBack = true,
                        onBackClick = { onBackPressedDispatcher.onBackPressed() },
                        onProfileClick = {}
                    )
                }
            ) { pad ->
                Column(Modifier.padding(pad)) {
                    if (isLoading && progress in 0f..0.99f) {
                        LinearProgressIndicator(progress = progress)
                    }
                    if (hasError && !isLoading && !hasContent) {
                        Text(text = "Unable to load this page.")
                    }
                    AndroidView(factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    return false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    val description = error?.description?.toString().orEmpty()
                                    Log.w(TAG, "WebView error for ${request?.url}: $description")
                                    val isMainFrame = request?.isForMainFrame == true
                                    val errorCode = error?.errorCode
                                    val isTransientError = errorCode == ERROR_HOST_LOOKUP ||
                                        errorCode == ERROR_TIMEOUT
                                    if (isMainFrame && !isTransientError) {
                                        hasError = true
                                    }
                                }

                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    errorResponse: WebResourceResponse?
                                ) {
                                    Log.w(
                                        TAG,
                                        "WebView HTTP ${errorResponse?.statusCode} for ${request?.url}"
                                    )
                                    if (request?.isForMainFrame == true) {
                                        hasError = true
                                    }
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    isLoading = true
                                    progress = 0f
                                    hasError = false
                                    hasContent = false
                                }

                                override fun onPageCommitVisible(view: WebView?, url: String?) {
                                    progress = 1f
                                    hasContent = true
                                    hasError = false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }
                            }
                            Log.d(TAG, "loadUrl($initialUrl)")
                            loadUrl(initialUrl)
                        }
                    })
                }
            }
        }
    }
}
