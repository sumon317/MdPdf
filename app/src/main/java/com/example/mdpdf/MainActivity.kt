package com.example.mdpdf

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.mdpdf.ui.MdPdfScreen
import com.example.mdpdf.ui.theme.MdPdfTheme

class MainActivity : ComponentActivity() {

    private lateinit var settings: SettingsRepository
    private lateinit var viewModel: MdPdfViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository.getInstance(this)
        NotificationHelper.createChannel(this)

        viewModel = ViewModelProvider(this)[MdPdfViewModel::class.java]

        val theme = settings.appTheme
        applyStatusBar(theme)

        // Handle incoming view intents only on cold start (savedInstanceState == null)
        if (savedInstanceState == null) {
            val intentUri = intent?.data
            if (intentUri != null && Intent.ACTION_VIEW == intent.action) {
                handleViewIntent(intentUri, viewModel)
            }
        }

        setContent {
            MdPdfTheme(appTheme = theme) {
                MdPdfScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = intent.data
        if (uri != null && Intent.ACTION_VIEW == intent.action) {
            handleViewIntent(uri, viewModel)
        }
    }

    private fun handleViewIntent(uri: Uri, viewModel: MdPdfViewModel) {
        val mimeType = contentResolver.getType(uri)
        when {
            mimeType == "application/pdf" -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(Intent.createChooser(intent, "Open PDF"))
                } catch (_: Exception) { }
            }
            mimeType in listOf("text/markdown", "text/plain", null) -> {
                viewModel.loadMarkdownFromUri(uri) { success, _ ->
                    if (!success) {
                        Toast.makeText(this, "Failed to open file", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun applyStatusBar(theme: String) {
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
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
            }
        }
    }
}

