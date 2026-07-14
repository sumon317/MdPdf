package com.example.mdpdf.ui

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@Suppress("FunctionName")
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownWebView(
    htmlContent: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filesBaseUrl = "file://${context.filesDir.absolutePath}/"

    // Debounce: only reload the WebView after 300 ms of keystroke silence.
    // LaunchedEffect cancels and restarts whenever htmlContent changes, so the
    // delay resets on every keystroke and the WebView reloads only on pause.
    var debouncedContent by remember { mutableStateOf(htmlContent) }
    LaunchedEffect(htmlContent) {
        delay(300)
        debouncedContent = htmlContent
    }

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
                settings.allowContentAccess = false
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = false
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        Log.e(
                            "MdPdfWebView",
                            "Error(${error?.errorCode}): ${error?.description} url=${request?.url}"
                        )
                    }
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(filesBaseUrl, debouncedContent, "text/html", "UTF-8", null)
        }
    )
}
