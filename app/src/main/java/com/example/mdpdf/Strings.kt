package com.example.mdpdf

/**
 * String constants replacing `R.string.*` resources.
 *
 * Migrated from `strings.xml` to eliminate false-positive "Unresolved reference"
 * language-server diagnostics on generated R.java. Only `app_name` (used in
 * AndroidManifest.xml) and content-description strings remain in XML.
 */
@Suppress("unused", "ConstPropertyName")
object Strings {
    // Toolbar / Menu
    const val toolbarTitle = "MdPdf"
    const val btnOpen = "Open"
    const val btnExportPdf = "Export PDF"
    const val cdMore = "More options"

    // View mode labels
    const val viewModeEditor = "Edit"
    const val viewModePreview = "Preview"
    const val viewModeSplit = "Split"

    // Overflow menu
    const val menuPdfTheme = "Pdf Theme"
    const val menuShareMarkdown = "Share Markdown"
    const val menuSharePdf = "Share PDF"
    const val menuAddImage = "Add Image"
    const val menuCopyText = "Copy as plain text"
    const val copyTextDone = "Text copied to clipboard"
    const val menuRecentFiles = "Recent Files"
    const val menuSettings = "Settings"
    const val recentFilesTitle = "Recent Files"
    const val recentFilesEmpty = "No recent files"

    // Theme dialog
    const val themeDialogTitle = "Pdf Theme"
    const val themeDefault = "Default"
    const val themeAcademic = "Academic"
    const val themeDark = "Dark mode"
    const val themeMinimal = "Minimal"
    const val btnCancel = "Cancel"

    // Export dialog
    const val exportDialogTitle = "Exporting PDF"
    fun exportProgress(current: Int, total: Int) = "Page $current of $total"
    const val exportSuccess = "PDF saved"
    fun exportFailed(msg: String) = "Export failed: $msg"
    const val exportFailedGeneric = "Export failed"
    const val btnRetry = "Retry"
    const val generatingPdf = "Generating PDF for sharing\u2026"

    // Image picker
    const val imageAdded = "Image added"
    fun imageFailed(msg: String) = "Failed to add image: $msg"
    const val imagePreviewTitle = "Image Preview"
    const val imageAltLabel = "Alt text"
    const val imageAltPlaceholder = "Describe the image"
    const val btnInsert = "Insert"

    // File operations
    const val openFailed = "Failed to open file"
    const val shareMarkdownTitle = "Share Markdown"
    const val sharePdfTitle = "Share PDF"

    // Settings
    const val settingsTitle = "Settings"
    const val settingsAppTheme = "App Theme"
    const val settingsLight = "Light"
    const val settingsDark = "Dark"
    const val settingsPureBlack = "Pure Black (AMOLED)"
    const val settingsShowErrors = "Show errors in PDF"
    const val settingsSpellcheck = "Spellcheck"
    const val settingsNotifications = "Export notifications"
    const val settingsDefaultFolder = "Default Save Folder"
    const val settingsFolderSet = "Folder set"
    const val settingsClear = "Clear"
    const val settingsSelectFolder = "Select Folder"
    const val btnDone = "Done"

    // Misc
    const val openPdf = "Open PDF"

    // Accessibility (content descriptions)
    const val contentDescriptionOverflowMenu = "Open options menu"
    const val contentDescriptionViewModeEditor = "Edit mode selected"
    const val contentDescriptionViewModePreview = "Preview mode selected"
    const val contentDescriptionViewModeSplit = "Split mode selected"
    const val contentDescriptionThemeDefault = "Default theme selected"
    const val contentDescriptionThemeAcademic = "Academic theme selected"
    const val contentDescriptionThemeDark = "Dark mode selected"
    const val contentDescriptionThemeMinimal = "Minimal theme selected"
}
