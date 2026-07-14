package com.example.mdpdf.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.example.mdpdf.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Suppress("FunctionName")
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

    var dialogState: DialogState by remember { mutableStateOf(DialogState.None) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val notificationIdRef = remember { mutableIntStateOf(1001) }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }

    fun executeExport(uri: Uri) {
        val nid = notificationIdRef.intValue++
        val printHtml = parser.toPrintHtml(markdownText, selectedTheme)

        scope.launch {
            // Show a persistent progress snackbar without blocking the export coroutine.
            val snackbarJob = launch {
                snackbarHostState.showSnackbar(
                    message = Strings.exportProgress(0, 1),
                    duration = SnackbarDuration.Indefinite
                )
            }
            try {
                val error = PdfExporter(context).export(
                    htmlContent = printHtml,
                    uri = uri,
                    onProgress = { current, total ->
                        if (settings.notificationsEnabled) {
                            NotificationHelper.showExportProgress(context, nid, current, total)
                        }
                    }
                )
                // Dismiss the progress snackbar before showing the result.
                snackbarJob.cancel()
                snackbarHostState.currentSnackbarData?.dismiss()

                if (error == null) {
                    if (settings.notificationsEnabled) {
                        NotificationHelper.showExportComplete(context, nid, uri)
                    }
                    snackbarHostState.showSnackbar(
                        message = Strings.exportSuccess,
                        duration = SnackbarDuration.Short
                    )
                } else {
                    if (settings.notificationsEnabled) {
                        NotificationHelper.showExportError(context, nid)
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = Strings.exportFailed(error.message ?: ""),
                        actionLabel = Strings.btnRetry,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        executeExport(uri)
                    }
                }
            } catch (e: Throwable) {
                snackbarJob.cancel()
                snackbarHostState.currentSnackbarData?.dismiss()
                if (settings.notificationsEnabled) {
                    NotificationHelper.showExportError(context, nid)
                }
                val result = snackbarHostState.showSnackbar(
                    message = Strings.exportFailed(e.message ?: ""),
                    actionLabel = Strings.btnRetry,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    executeExport(uri)
                }
            }
        }
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
                    Toast.makeText(context, Strings.openFailed, Toast.LENGTH_LONG).show()
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
            dialogState = DialogState.ImagePreview(imageUri = contentUri, altText = "")
        }
    }

    // --- Dialog state driven rendering ---
    when (dialogState) {
        is DialogState.ImagePreview -> {
            val state = dialogState as DialogState.ImagePreview
            var altText by remember(state) { mutableStateOf(state.altText) }
            AlertDialog(
                onDismissRequest = { dialogState = DialogState.None },
                title = { Text(Strings.imagePreviewTitle) },
                text = {
                    Column {
                        Text(
                            text = Strings.imageAltLabel,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = altText,
                            onValueChange = { altText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(Strings.imageAltPlaceholder) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        dialogState = DialogState.None
                        val imageUri = state.imageUri
                        scope.launch(Dispatchers.IO) {
                            try {
                                val imagesDir = File(context.filesDir, "images")
                                imagesDir.mkdirs()
                                val mimeType = context.contentResolver.getType(imageUri)
                                val ext = when {
                                    mimeType?.contains("png") == true -> "png"
                                    mimeType?.contains("gif") == true -> "gif"
                                    mimeType?.contains("webp") == true -> "webp"
                                    else -> "jpg"
                                }
                                val fileName = "img_${System.currentTimeMillis()}.$ext"
                                val destFile = File(imagesDir, fileName)
                                context.contentResolver.openInputStream(imageUri)?.use { input ->
                                    destFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                val alt = altText.ifEmpty { "Image" }
                                withContext(Dispatchers.Main.immediate) {
                                    viewModel.updateMarkdownText("$markdownText\n![$alt](images/$fileName)\n")
                                    Toast.makeText(context, Strings.imageAdded, Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main.immediate) {
                                    Toast.makeText(
                                        context,
                                        Strings.imageFailed(e.message ?: ""),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }) {
                        Text(Strings.btnInsert)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialogState = DialogState.None }) {
                        Text(Strings.btnCancel)
                    }
                }
            )
        }

        is DialogState.Settings -> {
            SettingsDialog(onDismiss = { dialogState = DialogState.None })
        }

        is DialogState.ThemePicker -> {
            AlertDialog(
                onDismissRequest = { dialogState = DialogState.None },
                title = { Text(Strings.themeDialogTitle) },
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
                                        dialogState = DialogState.None
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(theme.label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        dialogState = DialogState.None
                    }) { Text(Strings.btnCancel) }
                }
            )
        }

        is DialogState.RecentFiles -> {
            val recent = settings.recentFileEntries
            AlertDialog(
                onDismissRequest = { dialogState = DialogState.None },
                title = { Text(Strings.recentFilesTitle) },
                text = {
                    if (recent.isEmpty()) {
                        Text(Strings.recentFilesEmpty)
                    } else {
                        Column {
                            recent.forEach { recentFile ->
                                TextButton(
                                    onClick = {
                                        dialogState = DialogState.None
                                        val uri = Uri.parse(recentFile.uriString)
                                        viewModel.loadMarkdownFromUri(uri) { success, _ ->
                                            if (!success) {
                                                Toast.makeText(
                                                    context,
                                                    Strings.openFailed,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        recentFile.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { dialogState = DialogState.None }) {
                        Text(Strings.btnDone)
                    }
                }
            )
        }

        else -> { /* No dialog — handled by Scaffold overflow menu */
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.toolbarTitle) },
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
                    Box {
                        IconButton(
                            onClick = { dialogState = DialogState.OverflowMenu },
                            modifier = Modifier.testTag("overflowMenuButton")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = Strings.cdMore)
                        }
                        DropdownMenu(
                            expanded = dialogState == DialogState.OverflowMenu,
                            onDismissRequest = { dialogState = DialogState.None }
                        ) {
                            // Open file
                            DropdownMenuItem(
                                text = { Text(Strings.btnOpen) },
                                onClick = {
                                    dialogState = DialogState.None
                                    openDocumentLauncher.launch(arrayOf("text/*", "text/markdown"))
                                }
                            )
                            // Export PDF (respects default folder setting)
                            DropdownMenuItem(
                                text = { Text(Strings.btnExportPdf) },
                                onClick = {
                                    dialogState = DialogState.None
                                    val pdfName = currentFileName.ifEmpty { "MdPdf" } + ".pdf"
                                    val defaultFolderStr = settings.defaultFolder
                                    if (defaultFolderStr.isNotEmpty()) {
                                        try {
                                            val treeUri = Uri.parse(defaultFolderStr)
                                            val documentFile = DocumentFile.fromTreeUri(context, treeUri)
                                            val pdfFile = documentFile?.createFile(
                                                "application/pdf",
                                                pdfName.removeSuffix(".pdf")
                                            )
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
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        Strings.menuPdfTheme,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                trailingIcon = {
                                    Text(
                                        selectedTheme.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                onClick = {
                                    dialogState = DialogState.ThemePicker
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.menuShareMarkdown) },
                                onClick = {
                                    dialogState = DialogState.None
                                    shareMarkdown(context, markdownText, currentFileName)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.menuSharePdf) },
                                onClick = {
                                    dialogState = DialogState.None
                                    sharePdf(context, scope, markdownText, selectedTheme, parser)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.menuCopyText) },
                                onClick = {
                                    dialogState = DialogState.None
                                    val clipboard =
                                        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    @Suppress("UsePropertyAccessSyntax")
                                    clipboard.setPrimaryClip(ClipData.newPlainText("markdown", markdownText))
                                    Toast.makeText(
                                        context,
                                        Strings.copyTextDone,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.menuAddImage) },
                                onClick = {
                                    dialogState = DialogState.None
                                    imagePickerLauncher.launch("image/*")
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(Strings.menuRecentFiles) },
                                onClick = {
                                    dialogState = DialogState.RecentFiles
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.menuSettings) },
                                onClick = {
                                    dialogState = DialogState.Settings
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
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = settings.spellCheckEnabled,
                            imeAction = ImeAction.Done
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
                        onValueChange = { viewModel.updateMarkdownText(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = settings.spellCheckEnabled,
                            imeAction = ImeAction.Done
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
    context.startActivity(Intent.createChooser(intent, Strings.shareMarkdownTitle))
}

private fun sharePdf(
    context: android.content.Context,
    scope: CoroutineScope,
    markdownText: String,
    theme: MdTheme,
    parser: MarkdownParser
) {
    val printHtml = parser.toPrintHtml(markdownText, theme)
    val name = "MdPdf_shared.pdf"
    val tempFile = File(context.cacheDir, name)
    try {
        tempFile.createNewFile()
    } catch (_: Exception) {
    }
    val uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", tempFile
    )
    val toast = Toast.makeText(context, Strings.generatingPdf, Toast.LENGTH_SHORT)
    toast.show()
    scope.launch {
        val error = PdfExporter(context).export(
            htmlContent = printHtml,
            uri = uri
        )
        toast.cancel()
        if (error == null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, Strings.sharePdfTitle))
        } else {
            Toast.makeText(
                context,
                Strings.exportFailed(error.message ?: ""),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
