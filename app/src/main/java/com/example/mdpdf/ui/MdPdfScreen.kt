package com.example.mdpdf.ui

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.mdpdf.MarkdownParser
import com.example.mdpdf.MdTheme
import com.example.mdpdf.PdfExporter
import kotlinx.coroutines.launch

private enum class ViewMode { SPLIT, EDITOR, PREVIEW }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MdPdfScreen() {
    val context = LocalContext.current
    val parser = remember { MarkdownParser() }
    var markdownText by remember { mutableStateOf(DEFAULT_MARKDOWN) }
    var currentFileName by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf(MdTheme.DEFAULT) }
    var viewMode by remember { mutableStateOf(ViewMode.SPLIT) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val htmlContent = remember(markdownText, selectedTheme) {
        parser.toHtml(markdownText, selectedTheme)
    }
    val printHtml = remember(markdownText, selectedTheme) {
        parser.toPrintHtml(markdownText, selectedTheme)
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    markdownText = input.bufferedReader().readText()
                }
                currentFileName = extractFileName(it, context)
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to open file: ${e.message}")
                }
            }
        }
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            Toast.makeText(context, "Exporting...", Toast.LENGTH_SHORT).show()
            PdfExporter(context).export(printHtml, it) { error ->
                val msg = if (error == null) "PDF saved" else "Export failed: ${error.message}"
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MdPdf") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    TextButton(onClick = { openDocumentLauncher.launch(arrayOf("text/*", "text/markdown")) }) {
                        Text("Open")
                    }
                    TextButton(onClick = {
                        val pdfName = currentFileName.ifEmpty { "MdPdf" } + ".pdf"
                        saveDocumentLauncher.launch(pdfName)
                    }) {
                        Text("Export PDF")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MdTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = selectedTheme == theme,
                        onClick = { selectedTheme = theme },
                        label = { Text(theme.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                SegmentedButton(
                    selected = viewMode == ViewMode.EDITOR,
                    onClick = { viewMode = ViewMode.EDITOR },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 0, count = 3
                    )
                ) { Text("Editor") }
                SegmentedButton(
                    selected = viewMode == ViewMode.SPLIT,
                    onClick = { viewMode = ViewMode.SPLIT },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 1, count = 3
                    )
                ) { Text("Split") }
                SegmentedButton(
                    selected = viewMode == ViewMode.PREVIEW,
                    onClick = { viewMode = ViewMode.PREVIEW },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 2, count = 3
                    )
                ) { Text("Preview") }
            }

            when (viewMode) {
                ViewMode.EDITOR -> {
                    OutlinedTextField(
                        value = markdownText,
                        onValueChange = { markdownText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                ViewMode.PREVIEW -> {
                    MarkdownWebView(
                        htmlContent = htmlContent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                ViewMode.SPLIT -> {
                    OutlinedTextField(
                        value = markdownText,
                        onValueChange = { markdownText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    HorizontalDivider()
                    MarkdownWebView(
                        htmlContent = htmlContent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

private fun extractFileName(uri: Uri, context: android.content.Context): String {
    try {
        val docId = DocumentsContract.getDocumentId(uri)
        val segments = docId.split(":")
        val rawName = if (segments.size > 1) segments.last() else segments.first()
        val name = rawName.substringAfterLast("/")

        // Strip the current extension (.md, .markdown, .txt)
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

private val DEFAULT_MARKDOWN = """
# Welcome to MdPdf

A complete **Markdown to PDF** converter for Android, inspired by Obsidian.

## Features

- **Bold text** and *italic text*
- ~~Strikethrough~~ and `inline code`
- Tables, blockquotes, and lists
- **Syntax highlighting** for code blocks
- **Math rendering** via KaTeX — inline: \(\sum_{i=1}^n i = \frac{n(n+1)}{2}\)

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

**2×2 matrix** \(\begin{bmatrix} 1 & 2 \\ 3 & 4 \end{bmatrix}\)

\[
\begin{bmatrix}
1 & 2 \\
3 & 4
\end{bmatrix}
\]

**3×3 matrix** \(\begin{bmatrix} 1 & 2 & 3 \\ 4 & 5 & 6 \\ 7 & 8 & 9 \end{bmatrix}\)

\[
\begin{bmatrix}
1 & 2 & 3 \\
4 & 5 & 6 \\
7 & 8 & 9
\end{bmatrix}
\]

**4×4 matrix**

\[
\begin{bmatrix}
1 & 2 & 3 & 4 \\
5 & 6 & 7 & 8 \\
9 & 10 & 11 & 12 \\
13 & 14 & 15 & 16
\end{bmatrix}
\]

**Parenthesis matrix**

\[
\begin{pmatrix}
a & b \\
c & d
\end{pmatrix}
\]

**Determinant**

\[
\begin{vmatrix}
1 & 2 \\
3 & 4
\end{vmatrix}
\]
""".trimIndent()
