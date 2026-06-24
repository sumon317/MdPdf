package com.example.mdpdf.ui

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownWebView(
    htmlContent: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filesBaseUrl = "file://${context.filesDir.absolutePath}/"

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = false
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = false
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                        Log.e("MdPdfWebView", "Error($errorCode): $description url=$failingUrl")
                    }
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(filesBaseUrl, htmlContent, "text/html", "UTF-8", null)
        }
    )
}