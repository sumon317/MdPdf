package com.example.mdpdf

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient

class PdfExporter(private val context: Context) {

    fun export(htmlContent: String, uri: Uri, onResult: ((Throwable?) -> Unit)? = null) {
        try {
            doExport(htmlContent, uri, onResult)
        } catch (e: Throwable) {
            Log.e("PdfExporter", "Export failed", e)
            onResult?.invoke(e)
        }
    }

    private fun doExport(
        htmlContent: String,
        uri: Uri,
        onResult: ((Throwable?) -> Unit)?
    ) {
        val density = context.resources.displayMetrics.density
        val pageWidthPx = (595f * density).toInt()
        val pageHeightPx = (842f * density).toInt()

        val webView = WebView(context.applicationContext).apply {
            layout(0, 0, pageWidthPx, pageHeightPx)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            setBackgroundColor(0xFFFFFFFF.toInt())
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            visibility = View.INVISIBLE
        }

        if (context is Activity) {
            val decor = context.window.decorView as? ViewGroup
            decor?.addView(webView, ViewGroup.LayoutParams(pageWidthPx, pageHeightPx))
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                view.postDelayed({
                    measureAndCapture(webView, pageWidthPx, pageHeightPx, uri, onResult)
                }, 5000)
            }
        }

        webView.loadDataWithBaseURL("", htmlContent, "text/html", "UTF-8", null)
    }

    private fun measureAndCapture(
        webView: WebView,
        pageWidthPx: Int,
        pageHeightPx: Int,
        uri: Uri,
        onResult: ((Throwable?) -> Unit)?
    ) {
        webView.evaluateJavascript(
            "document.body.scrollHeight * window.devicePixelRatio"
        ) { heightStr ->
            val contentHeightPx = heightStr?.trim('"', '\'')?.toFloatOrNull()?.toInt() ?: pageHeightPx
            if (contentHeightPx <= 0) {
                onResult?.invoke(Exception("Invalid content height"))
                cleanup(webView)
                return@evaluateJavascript
            }

            val pageCount = maxOf(1, (contentHeightPx + pageHeightPx - 1) / pageHeightPx)
            Log.d("PdfExporter", "contentHeight=$contentHeightPx pageCount=$pageCount")

            val document = PdfDocument()
            capturePage(webView, 0, pageCount, pageWidthPx, pageHeightPx, pageHeightPx, document, uri, onResult)
        }
    }

    private fun capturePage(
        webView: WebView,
        index: Int,
        pageCount: Int,
        pageWidthPx: Int,
        pageHeightPx: Int,
        viewportHeightPx: Int,
        document: PdfDocument,
        uri: Uri,
        onResult: ((Throwable?) -> Unit)?
    ) {
        if (index >= pageCount) {
            finish(webView, document, uri, onResult)
            return
        }

        val scrollY = index * pageHeightPx
        webView.scrollTo(0, scrollY)

        webView.postDelayed({
            try {
                // Capture the current viewport into a bitmap
                val bitmap = Bitmap.createBitmap(pageWidthPx, viewportHeightPx, Bitmap.Config.ARGB_8888)
                val bitmapCanvas = Canvas(bitmap)

                // Move the canvas to capture the right slice.
                // webView.draw() renders at the view's origin, shifted by scroll offset internally.
                webView.draw(bitmapCanvas)

                // Create the PDF page and draw the captured bitmap onto it
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPx, pageHeightPx, index + 1).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(page)
                bitmap.recycle()
            } catch (e: Throwable) {
                Log.e("PdfExporter", "Page $index failed", e)
                finish(webView, document, uri, onResult)
                return@postDelayed
            }
            capturePage(webView, index + 1, pageCount, pageWidthPx, pageHeightPx, viewportHeightPx, document, uri, onResult)
        }, 400L)
    }

    private fun finish(
        webView: WebView,
        document: PdfDocument,
        uri: Uri,
        onResult: ((Throwable?) -> Unit)?
    ) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                document.writeTo(output)
            }
            Log.d("PdfExporter", "PDF saved to $uri")
            onResult?.invoke(null)
        } catch (e: Throwable) {
            Log.e("PdfExporter", "Write failed", e)
            onResult?.invoke(e)
        } finally {
            document.close()
            cleanup(webView)
        }
    }

    private fun cleanup(webView: WebView) {
        try {
            val parent = webView.parent as? ViewGroup
            parent?.removeView(webView)
            webView.destroy()
        } catch (e: Throwable) {
            Log.w("PdfExporter", "Cleanup failed", e)
        }
    }
}
