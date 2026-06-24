package com.example.mdpdf.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mdpdf.SettingsRepository

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { SettingsRepository.getInstance(context) }

    var appTheme by remember { mutableStateOf(settings.appTheme) }
    var showErrors by remember { mutableStateOf(settings.showErrorsInPdf) }
    var notifications by remember { mutableStateOf(settings.notificationsEnabled) }
    var spellCheck by remember { mutableStateOf(settings.spellCheckEnabled) }
    var defaultFolder by remember { mutableStateOf(settings.defaultFolder) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            settings.defaultFolder = it.toString()
            defaultFolder = it.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("App Theme", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ThemeOption("Light", "light", appTheme) {
                    appTheme = it; settings.appTheme = it
                    (context as? Activity)?.recreate()
                }
                ThemeOption("Dark", "dark", appTheme) {
                    appTheme = it; settings.appTheme = it
                    (context as? Activity)?.recreate()
                }
                ThemeOption("Pure Black (AMOLED)", "pure_black", appTheme) {
                    appTheme = it; settings.appTheme = it
                    (context as? Activity)?.recreate()
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show errors in PDF", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = showErrors,
                        onCheckedChange = {
                            showErrors = it
                            settings.showErrorsInPdf = it
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Spellcheck", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = spellCheck,
                        onCheckedChange = {
                            spellCheck = it
                            settings.spellCheckEnabled = it
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Export notifications", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = notifications,
                        onCheckedChange = {
                            notifications = it
                            settings.notificationsEnabled = it
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text("Default Save Folder", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                if (defaultFolder.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Folder set", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = {
                            settings.defaultFolder = ""
                            defaultFolder = ""
                        }) { Text("Clear") }
                    }
                } else {
                    OutlinedButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Select Folder") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun ThemeOption(
    label: String,
    value: String,
    current: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(value) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = current == value,
            onClick = { onSelect(value) }
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}