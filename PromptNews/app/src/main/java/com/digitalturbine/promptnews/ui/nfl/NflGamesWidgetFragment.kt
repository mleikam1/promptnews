package com.digitalturbine.promptnews.ui.nfl

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment

class NflGamesWidgetFragment : Fragment() {

    companion object {
        private const val TAG = "NflGamesWidget"
        fun newInstance() = NflGamesWidgetFragment()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val webView = WebView(requireContext()).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = object : WebViewClient() {
                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    Log.d(TAG, "[NFL WIDGET] WebView loaded")
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    val description = error?.description?.toString().orEmpty()
                    Log.w(TAG, "[NFL WIDGET] WebView error: $description")
                    if (request?.isForMainFrame != false) {
                        hideContainer(this@apply)
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    val status = errorResponse?.statusCode?.toString().orEmpty()
                    Log.w(TAG, "[NFL WIDGET] WebView error: HTTP $status")
                    if (request?.isForMainFrame != false) {
                        hideContainer(this@apply)
                    }
                }
            }
        }
        // POC: Using API-Sports hosted widget for reliability.
        // Future: Replace with backend-resolved scoreboard API.
        val html = """
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <script type="module" src="https://widgets.api-sports.io/3.1.0/widgets.js"></script>
</head>
<body style="margin:0;padding:0;">
  <api-sports-widget
    widget="games"
    league="1"
    season="current"
    timezone="America/Chicago"
    theme="light"
    show-logos="true"
    show-status="true"
    show-scores="true"
    api-key="a0e5885563df89357f88f4bcc0b5bb9b">
  </api-sports-widget>
</body>
</html>
""".trimIndent()
        webView.loadDataWithBaseURL(
            "https://widgets.api-sports.io",
            html,
            "text/html",
            "UTF-8",
            null
        )
        return webView
    }

    private fun hideContainer(webView: WebView) {
        Log.w(TAG, "[NFL WIDGET] WebView error")
        (webView.parent as? View)?.visibility = View.GONE
        webView.visibility = View.GONE
    }
}
