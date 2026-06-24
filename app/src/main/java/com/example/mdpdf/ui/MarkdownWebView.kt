package com.example.mdpdf.ui

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MarkdownWebView(
    htmlContent: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                        Log.e("MdPdfWebView", "Error($errorCode): $description url=$failingUrl")
                    }
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://cdnjs.cloudflare.com/", htmlContent, "text/html", "UTF-8", null)
        }
    )
}
