package com.digitalturbine.promptnews.web

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class YahooResultsActivity : ComponentActivity() {

    private fun yahooUrl(q: String): String =
        Uri.parse("https://search.yahoo.com/search").buildUpon()
            .appendQueryParameter("p", q)
            .appendQueryParameter("fr2", "piv-web")
            .appendQueryParameter("fr", "none")
            .build().toString()

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val query = intent.getStringExtra("query").orEmpty()

        setContent {
            var progress by remember { mutableFloatStateOf(0f) }
            Scaffold(
                topBar = { CenterAlignedTopAppBar(title = { Text(if (query.isBlank()) "Yahoo" else query) }) }
            ) { pad ->
                Column(Modifier.padding(pad)) {
                    if (progress in 0f..0.99f) LinearProgressIndicator(progress = progress)
                    AndroidView(factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean = false

                                override fun onPageFinished(view: WebView, url: String) {
                                    progress = 1f
                                    if (Uri.parse(url).host?.contains("yahoo.com") == true) {
                                        val css = """
                                            (function(){
                                                var s=document.createElement('style');
                                                s.innerHTML=`header,#header,#ybar,#ybar-inner-wrap,#uh-search,form[role="search"],.compSearch,#app-bar,.AppHeader,.UH,.skiplinks{display:none!important} body{padding-top:0!important}`;
                                                document.head.appendChild(s);
                                            })();
                                        """.trimIndent()
                                        view.evaluateJavascript(css, null)
                                    }
                                }
                            }
                            loadUrl(yahooUrl(query))
                        }
                    })
                }
            }
        }
    }
}
