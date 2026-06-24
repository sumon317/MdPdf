package com.example.mdpdf

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.mdpdf.ui.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MdPdfViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val settings = SettingsRepository.getInstance(application)
    val parser = MarkdownParser()

    companion object {
        private const val KEY_MARKDOWN_TEXT = "markdown_text"
        private const val KEY_CURRENT_FILE_NAME = "current_file_name"
        private const val KEY_CURRENT_FILE_URI = "current_file_uri"
        private const val KEY_VIEW_MODE = "view_mode"
        private const val KEY_SELECTED_THEME = "selected_theme"

        private val DEFAULT_MARKDOWN = """
            # Welcome to MdPdf

            A complete **Markdown to PDF** converter for Android, inspired by Obsidian.

            ## Features

            - **Bold text** and *italic text*
            - ~~Strikethrough~~ and `inline code`
            - Tables, blockquotes, and lists
            - **Syntax highlighting** for code blocks
            - **Math rendering** via KaTeX — inline: \(\sum_{i=1}^n i = \frac{n(n+1)}{2}\)

            ## Checkboxes

            - [x] Completed task
            - [ ] Pending task
            - [ ] Another pending task

            ## Code Blocks

            ```kotlin
            fun factorial(n: Int): Int {
                return if (n <= 1) 1 else n * factorial(n - 1)
            }

            fun main() {
                println("Hello, MdPdf!")
                for (i in 1..5) {
                    println(i.toString() + "! = " + factorial(i))
                }
            }
            ```

            ## Blockquotes

            > The best way to predict the future is to create it.
            > — Peter Drucker

            ## Tables

            | Feature | Status | Version |
            |---------|--------|---------|
            | Editor | ✅ | 1.0 |
            | Preview | ✅ | 1.0 |
            | PDF Export | ✅ | 1.0 |
            | Syntax Highlighting | ✅ | 1.0 |
            | Math Rendering | ✅ | 1.0 |

            ## Math Display

            \[
            \sum_{i=1}^n i = \frac{n(n+1)}{2}
            \]

            **Subscripts and superscripts:**

            - \(x^2\) — superscript with `^`
            - \(x_i\) — subscript with `_`
            - \(x_i^2\) — both
            - \(x_{ij}\) — multi-char subscript
            - \(e^{i\pi} + 1 = 0\) — Euler's identity
            - \(\int_0^\infty e^{-x^2} dx = \frac{\sqrt{\pi}}{2}\) — integral

            ## Matrices

            **3×3 matrix** \(\begin{bmatrix} 1 & 2 & 3 \\ 4 & 5 & 6 \\ 7 & 8 & 9 \end{bmatrix}\)

            \[
            \begin{bmatrix}
            1 & 2 & 3 \\
            4 & 5 & 6 \\
            7 & 8 & 9
            \end{bmatrix}
            \]

            **Parenthesis matrix**

            \[
            \begin{pmatrix}
            a & b & c \\
            d & e & f \\
            g & h & i
            \end{pmatrix}
            \]

            **Determinant**

            \[
            \begin{vmatrix}
            1 & 2 & 3 \\
            4 & 5 & 6 \\
            7 & 8 & 9
            \end{vmatrix}
            \]
        """.trimIndent()
    }

    val markdownText = savedStateHandle.getStateFlow(KEY_MARKDOWN_TEXT, DEFAULT_MARKDOWN)
    val currentFileName = savedStateHandle.getStateFlow(KEY_CURRENT_FILE_NAME, "")
    val currentFileUri = savedStateHandle.getStateFlow(KEY_CURRENT_FILE_URI, "")

    val viewMode: StateFlow<ViewMode> = savedStateHandle.getStateFlow(KEY_VIEW_MODE, ViewMode.SPLIT.name)
        .combine(MutableStateFlow(ViewMode.SPLIT)) { name, _ ->
            try { ViewMode.valueOf(name) } catch (_: Exception) { ViewMode.SPLIT }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ViewMode.SPLIT)

    val selectedTheme: StateFlow<MdTheme> = savedStateHandle.getStateFlow(KEY_SELECTED_THEME, settings.markdownTheme)
        .combine(MutableStateFlow(MdTheme.DEFAULT)) { name, _ ->
            try { MdTheme.valueOf(name) } catch (_: Exception) { MdTheme.DEFAULT }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, MdTheme.DEFAULT)

    val htmlContent: StateFlow<String> = combine(markdownText, selectedTheme) { text, theme ->
        parser.toHtml(text, theme, settings.showErrorsInPdf)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    fun updateMarkdownText(text: String) {
        savedStateHandle[KEY_MARKDOWN_TEXT] = text
    }

    fun updateCurrentFileName(name: String) {
        savedStateHandle[KEY_CURRENT_FILE_NAME] = name
    }

    fun updateCurrentFileUri(uri: Uri?) {
        savedStateHandle[KEY_CURRENT_FILE_URI] = uri?.toString() ?: ""
    }

    fun updateViewMode(mode: ViewMode) {
        savedStateHandle[KEY_VIEW_MODE] = mode.name
    }

    fun updateSelectedTheme(theme: MdTheme) {
        savedStateHandle[KEY_SELECTED_THEME] = theme.name
        settings.markdownTheme = theme.name
    }

    fun loadMarkdownFromUri(uri: Uri, onResult: (Boolean, String) -> Unit) {
        try {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                val text = input.bufferedReader().readText()
                updateMarkdownText(text)
                updateCurrentFileUri(uri)
                val fileName = extractFileName(uri, getApplication())
                updateCurrentFileName(fileName)
                onResult(true, fileName)
            }
        } catch (e: Exception) {
            onResult(false, e.message ?: "Unknown error")
        }
    }

    private fun extractFileName(uri: Uri, context: Context): String {
        try {
            val docId = DocumentsContract.getDocumentId(uri)
            val segments = docId.split(":")
            val rawName = if (segments.size > 1) segments.last() else segments.first()
            val name = rawName.substringAfterLast("/")
            val extensions = listOf(".md", ".markdown", ".mkd", ".mdwn", ".mdtxt", ".txt")
            for (ext in extensions) {
                if (name.endsWith(ext, ignoreCase = true)) {
                    return name.removeSuffix(ext)
                }
            }
            return name
        } catch (_: Exception) {
            val path = uri.lastPathSegment ?: ""
            val name = path.substringAfterLast("/")
            val dot = name.lastIndexOf('.')
            return if (dot > 0) name.substring(0, dot) else name
        }
    }
}
