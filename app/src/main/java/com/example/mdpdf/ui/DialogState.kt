package com.example.mdpdf.ui

import android.net.Uri

/**
 * Represents the current dialog/overlay state in [MdPdfScreen].
 * Replaces scattered `var showXyz` Boolean flags with a single typed state.
 */
sealed class DialogState {
    /** No dialog or overlay is shown. */
    data object None : DialogState()

    /** The overflow (options) dropdown menu is open. */
    data object OverflowMenu : DialogState()

    /** The PDF theme picker dialog is open. */
    data object ThemePicker : DialogState()

    /** The settings dialog is open. */
    data object Settings : DialogState()

    /** The image preview dialog is open with the selected URI and editable alt text. */
    data class ImagePreview(val imageUri: Uri, val altText: String) : DialogState()

    /** The recent files picker dialog is open. */
    data object RecentFiles : DialogState()
}
