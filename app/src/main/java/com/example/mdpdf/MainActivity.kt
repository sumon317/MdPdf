package com.example.mdpdf

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.mdpdf.ui.MdPdfScreen
import com.example.mdpdf.ui.theme.MdPdfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.parseColor("#6B8F3A")
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        setContent {
            MdPdfTheme {
                MdPdfScreen()
            }
        }
    }
}
