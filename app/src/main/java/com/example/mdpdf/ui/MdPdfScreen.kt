package com.example.mdpdf.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mdpdf.MarkdownParser
import com.example.mdpdf.MdPdfViewModel
import com.example.mdpdf.MdTheme
import com.example.mdpdf.NotificationHelper
import com.example.mdpdf.PdfExporter
import com.example.mdpdf.SettingsRepository
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MdPdfScreen(viewModel: MdPdfViewModel) {
    val context = LocalContext.current
    val settings = remember { SettingsRepository.getInstance(context) }

    val markdownText by viewModel.markdownText.collectAsStateWithLifecycle()
    val currentFileName by viewModel.currentFileName.collectAsStateWithLifecycle()
    val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val htmlContent by viewModel.htmlContent.collectAsStateWithLifecycle()
    val parser = viewModel.parser

    var exporting by remember { mutableStateOf(false) }
    var exportCurrent by remember { mutableIntStateOf(0) }
    var exportTotal by remember { mutableIntStateOf(1) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationIdRef = remember { mutableIntStateOf(1001) }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }

    fun executeExport(uri: Uri) {
        exporting = true
        exportCurrent = 0
        exportTotal = 1
        val nid = notificationIdRef.intValue++
        val printHtml = parser.toPrintHtml(markdownText, selectedTheme)
        PdfExporter(context).export(
            htmlContent = printHtml,
            uri = uri,
            onProgress = { current, total ->
                exportCurrent = current
                exportTotal = total
                if (settings.notificationsEnabled) {
                    NotificationHelper.showExportProgress(context, nid, current, total)
                }
            },
            onResult = { error ->
                exporting = false
                if (error == null) {
                    if (settings.notificationsEnabled) {
                        NotificationHelper.showExportComplete(context, nid, uri)
                    }
                    Toast.makeText(context, "PDF saved", Toast.LENGTH_LONG).show()
                } else {
                    if (settings.notificationsEnabled) {
                        NotificationHelper.showExportError(context, nid)
                    }
                    Toast.makeText(context, "Export failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingExportUri?.let { uri ->
            executeExport(uri)
            pendingExportUri = null
        }
    }

    fun startExport(uri: Uri) {
        if (settings.notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                executeExport(uri)
            } else {
                pendingExportUri = uri
                notificationPermissionLauncher.launch(permission)
            }
        } else {
            executeExport(uri)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.loadMarkdownFromUri(it) { success, _ ->
                if (!success) {
                    Toast.makeText(context, "Failed to open file", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { startExport(it) }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { contentUri ->
            try {
                val imagesDir = File(context.filesDir, "images")
                imagesDir.mkdirs()
                val mimeType = context.contentResolver.getType(contentUri)
                val ext = when {
                    mimeType?.contains("png") == true -> "png"
                    mimeType?.contains("gif") == true -> "gif"
                    mimeType?.contains("webp") == true -> "webp"
                    else -> "jpg"
                }
                val fileName = "img_${System.currentTimeMillis()}.$ext"
                val destFile = File(imagesDir, fileName)
                context.contentResolver.openInputStream(contentUri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.updateMarkdownText(markdownText + "\n![Image](images/$fileName)\n")
                Toast.makeText(context, "Image added", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to add image: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (exporting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Exporting PDF") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = { exportCurrent.toFloat() / exportTotal.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Page $exportCurrent of $exportTotal")
                }
            },
            confirmButton = {}
        )
    }
    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        ViewMode.entries.forEach { mode ->
                            TextButton(
                                onClick = { viewModel.updateViewMode(mode) },
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text(
                                    mode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (viewMode == mode) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (viewMode == mode) FontWeight.Bold
                                    else FontWeight.Normal
                                )
                            }
                        }
                    }
                    TextButton(onClick = { openDocumentLauncher.launch(arrayOf("text/*", "text/markdown")) }) {
                        Text("Open")
                    }
                    TextButton(onClick = {
                        val pdfName = currentFileName.ifEmpty { "MdPdf" } + ".pdf"
                        val defaultFolderStr = settings.defaultFolder
                        if (defaultFolderStr.isNotEmpty()) {
                            try {
                                val treeUri = Uri.parse(defaultFolderStr)
                                val documentFile = DocumentFile.fromTreeUri(context, treeUri)
                                val pdfFile = documentFile?.createFile("application/pdf", pdfName.removeSuffix(".pdf"))
                                if (pdfFile != null) {
                                    startExport(pdfFile.uri)
                                } else {
                                    saveDocumentLauncher.launch(pdfName)
                                }
                            } catch (_: Exception) {
                                saveDocumentLauncher.launch(pdfName)
                            }
                        } else {
                            saveDocumentLauncher.launch(pdfName)
                        }
                    }) {
                        Text("Export PDF")
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Pdf Theme", fontWeight = FontWeight.Medium) },
                                trailingIcon = { Text(selectedTheme.label, style = MaterialTheme.typography.labelSmall) },
                                onClick = {
                                    showOverflowMenu = false
                                    showThemeMenu = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Markdown") },
                                onClick = {
                                    showOverflowMenu = false
                                    shareMarkdown(context, markdownText, currentFileName)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share PDF") },
                                onClick = {
                                    showOverflowMenu = false
                                    sharePdf(context, markdownText, selectedTheme, parser)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add Image") },
                                onClick = {
                                    showOverflowMenu = false
                                    imagePickerLauncher.launch("image/*")
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    showOverflowMenu = false
                                    showSettings = true
                                }
                            )
                        }
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
            if (showThemeMenu) {
                AlertDialog(
                    onDismissRequest = { showThemeMenu = false },
                    title = { Text("Pdf Theme") },
                    text = {
                        Column {
                            MdTheme.entries.forEach { theme ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedTheme == theme,
                                        onClick = {
                                            viewModel.updateSelectedTheme(theme)
                                            showThemeMenu = false
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(theme.label, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showThemeMenu = false }) { Text("Cancel") }
                    }
                )
            }

            when (viewMode) {
                ViewMode.EDITOR -> {
                    OutlinedTextField(
                        value = markdownText,
                        onValueChange = { viewModel.updateMarkdownText(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = settings.spellCheckEnabled)
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
                        onValueChange = { viewModel.updateMarkdownText(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = settings.spellCheckEnabled)
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

private fun shareMarkdown(context: android.content.Context, text: String, fileName: String) {
    val name = fileName.ifEmpty { "document" }
    val file = File(context.cacheDir, "$name.md")
    file.writeText(text)
    val uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/markdown"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Markdown"))
}

private fun sharePdf(context: android.content.Context, markdownText: String, theme: MdTheme, parser: MarkdownParser) {
    val printHtml = parser.toPrintHtml(markdownText, theme)
    val name = "MdPdf_shared.pdf"
    val tempFile = File(context.cacheDir, name)
    try {
        tempFile.createNewFile()
    } catch (_: Exception) { }
    val uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", tempFile
    )
    val toast = Toast.makeText(context, "Generating PDF for sharing…", Toast.LENGTH_SHORT)
    toast.show()
    PdfExporter(context).export(
        htmlContent = printHtml,
        uri = uri,
        onResult = { error ->
            toast.cancel()
            if (error == null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share PDF"))
            } else {
                Toast.makeText(context, "Failed to create PDF: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    )
}