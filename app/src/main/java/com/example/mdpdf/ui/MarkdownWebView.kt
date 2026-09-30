package com.example.mdpdf.ui

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mdpdf.MdPdfViewModel

/**
 * Preview pane that collects [MdPdfViewModel.htmlContent] locally so that HTML
 * re-renders recompose only this small subtree instead of the whole screen.
 */
@Composable
fun MarkdownPreview(
    viewModel: MdPdfViewModel,
    modifier: Modifier = Modifier
) {
    val htmlContent by viewModel.htmlContent.collectAsStateWithLifecycle()
    MarkdownWebView(
        htmlContent = htmlContent,
        modifier = modifier
    )
}

@Suppress("FunctionName")
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
        // NOTE: the WebView is intentionally NOT keyed on htmlContent — keeping
        // the native view alive lets it reuse its in-memory asset cache across
        // reloads. Only the update lambda re-loads the document.
        update = { webView ->
            webView.loadDataWithBaseURL(filesBaseUrl, htmlContent, "text/html", "UTF-8", null)
        },
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
        }
    )
}
