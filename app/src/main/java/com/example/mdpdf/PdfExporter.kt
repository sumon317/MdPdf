package com.example.mdpdf

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Exports rendered HTML to a paginated PDF document via [WebView] capture.
 *
 * Uses an off-screen WebView to render the HTML, then captures each
 * page as a bitmap and writes them into an Android [PdfDocument].
 * Progress is reported per page, and cancellation is honoured on
 * Activity destruction.
 *
 * @param context Must be an [Activity] context for WebView attachment.
 */
class PdfExporter(private val context: Context) {

    suspend fun export(
        htmlContent: String,
        uri: Uri,
        onProgress: ((Int, Int) -> Unit)? = null,
        onResult: ((Throwable?) -> Unit)? = null
    ): Throwable? = withContext(Dispatchers.IO) {
        val activity = context as? Activity
        if (activity == null) {
            return@withContext Exception("Export requires an Activity context")
        }

        val env = ExportEnv(htmlContent, uri, activity, onProgress, onResult)

        val lifecycle = (activity as? LifecycleOwner)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                if (env.isCancelled.compareAndSet(false, true)) {
                    cleanup(env, null)
                    env.onResult?.invoke(Exception("Activity destroyed during export"))
                }
            }
        }
        env.lifecycle = lifecycle
        env.observer = observer
        lifecycle?.addObserver(observer)

        try {
            runExport(env)
            null
        } catch (e: Throwable) {
            Log.e("PdfExporter", "Export failed", e)
            if (env.isCancelled.compareAndSet(false, true)) {
                cleanup(env, null)
                onResult?.invoke(e)
            }
            e
        }
    }

    private class ExportEnv(
        val htmlContent: String,
        val uri: Uri,
        val activity: Activity,
        val onProgress: ((Int, Int) -> Unit)?,
        val onResult: ((Throwable?) -> Unit)?,
        val isCancelled: AtomicBoolean = AtomicBoolean(false),
        var lifecycle: Lifecycle? = null,
        var observer: LifecycleEventObserver? = null,
        var webView: WebView? = null
    )

    private fun runExport(env: ExportEnv) {
        val density = 2.0f
        val pageWidthPx = (595f * density).toInt()
        val pageHeightPx = (842f * density).toInt()
        val filesDir = env.activity.filesDir.absolutePath
        val baseUrl = "file://$filesDir/"

        val webView = WebView(env.activity).apply {
            layoutParams = ViewGroup.LayoutParams(pageWidthPx, pageHeightPx)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true

            setBackgroundColor(0xFFFFFFFF.toInt())
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            alpha = 0f
            clearCache(true)
        }
        env.webView = webView

        if (env.isCancelled.get()) {
            cleanup(env, webView)
            return
        }

        val decor = env.activity.window.decorView as? ViewGroup
        decor?.addView(webView)

        var started = false
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (started) return
                started = true
                view.postDelayed({
                    if (!env.isCancelled.get()) {
                        measureAndCapture(env, webView, pageWidthPx, pageHeightPx)
                    }
                }, 5000)
            }
        }

        webView.loadDataWithBaseURL(baseUrl, env.htmlContent, "text/html", "UTF-8", null)
    }

    private fun measureAndCapture(
        env: ExportEnv,
        webView: WebView,
        pageWidthPx: Int,
        pageHeightPx: Int
    ) {
        if (env.isCancelled.get()) return
        webView.evaluateJavascript(
            "document.body.scrollHeight"
        ) { heightStr ->
            if (env.isCancelled.get()) return@evaluateJavascript
            val contentHeightPx = heightStr?.trim('"', '\'')?.toFloatOrNull()?.toInt() ?: pageHeightPx
            if (contentHeightPx <= 0) {
                if (env.isCancelled.compareAndSet(false, true)) {
                    cleanup(env, webView)
                    env.onResult?.invoke(Exception("Invalid content height: $heightStr"))
                }
                return@evaluateJavascript
            }

            val pageCount = maxOf(1, (contentHeightPx + pageHeightPx - 1) / pageHeightPx)
            Log.d("PdfExporter", "contentHeight=$contentHeightPx pageCount=$pageCount")

            val document = PdfDocument()
            env.onProgress?.invoke(0, pageCount)
            capturePage(env, webView, 0, pageCount, pageWidthPx, pageHeightPx, document)
        }
    }

    private fun capturePage(
        env: ExportEnv,
        webView: WebView,
        index: Int,
        pageCount: Int,
        pageWidthPx: Int,
        pageHeightPx: Int,
        document: PdfDocument
    ) {
        if (env.isCancelled.get()) {
            document.close()
            return
        }
        if (index >= pageCount) {
            finish(env, webView, document)
            return
        }

        val scrollY = index * pageHeightPx
        webView.scrollTo(0, scrollY)
        scheduleCapture(env, webView, index, pageCount, pageWidthPx, pageHeightPx, document)
    }

    private fun scheduleCapture(
        env: ExportEnv,
        webView: WebView,
        index: Int,
        pageCount: Int,
        pageWidthPx: Int,
        pageHeightPx: Int,
        document: PdfDocument
    ) {
        if (env.isCancelled.get()) {
            document.close()
            return
        }
        val done = AtomicBoolean(false)
        val id = System.nanoTime()

        webView.postVisualStateCallback(id, object : WebView.VisualStateCallback() {
            override fun onComplete(callbackId: Long) {
                if (callbackId != id) return
                if (env.isCancelled.get()) {
                    document.close()
                    return
                }
                if (!done.compareAndSet(false, true)) return
                doCapture(env, webView, index, pageCount, pageWidthPx, pageHeightPx, document)
            }
        })

        webView.postDelayed({
            if (env.isCancelled.get()) {
                document.close()
                return@postDelayed
            }
            if (done.compareAndSet(false, true)) {
                Log.w("PdfExporter", "postVisualStateCallback timeout, using fallback")
                doCapture(env, webView, index, pageCount, pageWidthPx, pageHeightPx, document)
            }
        }, 800L)
    }

    private fun doCapture(
        env: ExportEnv,
        webView: WebView,
        index: Int,
        pageCount: Int,
        pageWidthPx: Int,
        pageHeightPx: Int,
        document: PdfDocument
    ) {
        if (env.isCancelled.get()) {
            document.close()
            return
        }
        try {
            val bitmap = Bitmap.createBitmap(pageWidthPx, pageHeightPx, Bitmap.Config.ARGB_8888)
            val bitmapCanvas = Canvas(bitmap)
            webView.draw(bitmapCanvas)

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPx, pageHeightPx, index + 1).create()
            val page = document.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            document.finishPage(page)
            bitmap.recycle()

            env.onProgress?.invoke(index + 1, pageCount)
        } catch (e: Throwable) {
            Log.e("PdfExporter", "Page $index failed", e)
            if (env.isCancelled.compareAndSet(false, true)) {
                document.close()
                cleanup(env, webView)
                env.onResult?.invoke(e)
            }
            return
        }
        capturePage(env, webView, index + 1, pageCount, pageWidthPx, pageHeightPx, document)
    }

    private fun finish(
        env: ExportEnv,
        webView: WebView,
        document: PdfDocument
    ) {
        if (env.isCancelled.compareAndSet(false, true)) {
            try {
                env.activity.contentResolver.openOutputStream(env.uri)?.use { output ->
                    document.writeTo(output)
                }
                Log.d("PdfExporter", "PDF saved to ${env.uri}")
                env.onResult?.invoke(null)
            } catch (e: Throwable) {
                Log.e("PdfExporter", "Write failed", e)
                env.onResult?.invoke(e)
            } finally {
                document.close()
                cleanup(env, webView)
            }
        } else {
            document.close()
        }
    }

    private fun cleanup(env: ExportEnv, webView: WebView?) {
        try {
            env.lifecycle?.let { lf ->
                env.observer?.let { obs ->
                    lf.removeObserver(obs)
                }
            }
            env.lifecycle = null
            env.observer = null

            val wv = webView ?: env.webView
            if (wv != null) {
                val parent = wv.parent as? ViewGroup
                parent?.removeView(wv)
                wv.destroy()
            }
            env.webView = null
        } catch (e: Throwable) {
            Log.w("PdfExporter", "Cleanup failed", e)
        }
    }
}
