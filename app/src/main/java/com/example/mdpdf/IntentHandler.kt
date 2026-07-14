package com.example.mdpdf

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.core.view.WindowCompat

/**
 * Handles incoming VIEW intents and status bar theming for [MainActivity].
 * Extracted from MainActivity to keep the Activity class focused on lifecycle.
 */
object IntentHandler {

    /** Route a VIEW intent URI to the appropriate handler (PDF viewer or markdown loader). */
    fun handleViewIntent(context: Context, uri: Uri, viewModel: MdPdfViewModel) {
        val mimeType = context.contentResolver.getType(uri)
        when (mimeType) {
            "application/pdf" -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.startActivity(Intent.createChooser(intent, Strings.openPdf))
                } catch (_: Exception) {
                }
            }

            "text/markdown", "text/plain", null -> {
                viewModel.loadMarkdownFromUri(uri) { success, _ ->
                    if (!success) {
                        Toast.makeText(context, Strings.openFailed, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /** Apply status bar color and light/dark icon tint based on app theme string. */
    fun applyStatusBar(window: android.view.Window, theme: String) {
        @Suppress("DEPRECATION")
        when (theme) {
            "dark" -> {
                window.statusBarColor = Color.parseColor("#1C1C1A")
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
            }

            "pure_black" -> {
                window.statusBarColor = Color.parseColor("#000000")
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
            }

            else -> {
                window.statusBarColor = Color.parseColor("#5F8B4A")
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
            }
        }
    }
}
