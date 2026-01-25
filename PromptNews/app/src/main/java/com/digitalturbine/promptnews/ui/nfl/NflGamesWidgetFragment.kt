package com.digitalturbine.promptnews.ui.nfl

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

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
        val context = requireContext()
        val root = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val webView = WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isNestedScrollingEnabled = false
            setOnTouchListener { _, event ->
                event.action == MotionEvent.ACTION_MOVE
            }
            webViewClient = object : WebViewClient() {
                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    Log.d(TAG, "[NFL WIDGET] Loaded")
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    val description = error?.description?.toString().orEmpty()
                    Log.w(TAG, "[NFL WIDGET] WebView error: $description")
                    if (request?.isForMainFrame != false) {
                        Log.w(TAG, "[NFL WIDGET] Failed to load")
                        root.visibility = View.GONE
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
                        Log.w(TAG, "[NFL WIDGET] Failed to load")
                        root.visibility = View.GONE
                    }
                }
            }
        }
        root.addView(webView)
        // POC: Using API-Sports hosted widget for reliability.
        // Future: Replace with backend-resolved scoreboard API.
        val html = """
<!DOCTYPE html>
<html>
<head>
  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">
  <script type=\"module\" src=\"https://widgets.api-sports.io/3.1.0/widgets.js\"></script>
</head>
<body style=\"margin:0;padding:0;\">
  <api-sports-widget
    widget=\"games\"
    league=\"1\"
    season=\"current\"
    timezone=\"America/Chicago\"
    theme=\"light\"
    show-logos=\"true\"
    show-status=\"true\"
    show-scores=\"true\"
    api-key=\"a0e5885563df89357f88f4bcc0b5bb9b\">
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
        return root
    }
}

@Composable
fun NflGamesWidgetHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val fragmentActivity = context as? androidx.fragment.app.FragmentActivity ?: return
    val fragmentManager: FragmentManager = fragmentActivity.supportFragmentManager
    val containerId = remember { View.generateViewId() }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                id = containerId
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        update = {
            if (fragmentManager.findFragmentById(containerId) == null) {
                fragmentManager
                    .beginTransaction()
                    .replace(containerId, NflGamesWidgetFragment.newInstance())
                    .commit()
            }
        }
    )
}
