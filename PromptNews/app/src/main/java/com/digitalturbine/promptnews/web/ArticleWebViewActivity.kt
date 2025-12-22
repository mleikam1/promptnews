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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

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
            var errorMessage by remember { mutableStateOf<String?>(null) }
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("PromptNews") },
                        navigationIcon = {
                            IconButton(onClick = { onBackPressedDispatcher.onBackPressed() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                }
            ) { pad ->
                Column(Modifier.padding(pad)) {
                    if (progress in 0f..0.99f) LinearProgressIndicator(progress = progress)
                    errorMessage?.let { msg ->
                        Text(text = msg)
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
                                    errorMessage = "Unable to load this page."
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
                                    errorMessage = "Unable to load this page."
                                }

                                override fun onPageCommitVisible(view: WebView?, url: String?) {
                                    progress = 1f
                                    errorMessage = null
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
