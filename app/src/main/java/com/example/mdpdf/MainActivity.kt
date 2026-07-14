package com.example.mdpdf

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        IntentHandler.applyStatusBar(window, theme)

        // Handle incoming view intents only on cold start (savedInstanceState == null)
        if (savedInstanceState == null) {
            val intentUri = intent?.data
            if (intentUri != null && Intent.ACTION_VIEW == intent.action) {
                IntentHandler.handleViewIntent(this, intentUri, viewModel)
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
            IntentHandler.handleViewIntent(this, uri, viewModel)
        }
    }
}
