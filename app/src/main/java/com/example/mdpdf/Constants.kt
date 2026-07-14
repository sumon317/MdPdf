package com.example.mdpdf

/**
 * Centralized constants used across the app.
 * Preference keys, notification channel identifiers, file provider authority, etc.
 */
object Constants {
    // SharedPreferences keys
    const val PREFS_NAME = "mdpdf_settings"
    const val PREF_APP_THEME = "app_theme"
    const val PREF_MARKDOWN_THEME = "markdown_theme"
    const val PREF_SHOW_ERRORS = "show_errors"
    const val PREF_DEFAULT_FOLDER = "default_folder"
    const val PREF_NOTIFICATIONS = "notifications"
    const val PREF_SPELLCHECK = "spellcheck"
    const val PREF_RECENT_FILES = "recent_files"

    // SavedStateHandle keys
    const val STATE_KEY_MARKDOWN_TEXT = "markdown_text"
    const val STATE_KEY_CURRENT_FILE_NAME = "current_file_name"
    const val STATE_KEY_CURRENT_FILE_URI = "current_file_uri"
    const val STATE_KEY_VIEW_MODE = "view_mode"
    const val STATE_KEY_SELECTED_THEME = "selected_theme"

    // Notification channel
    const val NOTIFICATION_CHANNEL_ID = "mdpdf_export"
    const val NOTIFICATION_CHANNEL_NAME = "Export Progress"
    const val NOTIFICATION_CHANNEL_DESC = "PDF export notifications"

    // FileProvider
    const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"

    // Defaults
    const val DEFAULT_APP_THEME = "light"
    const val DEFAULT_MARKDOWN_THEME = "DEFAULT"
    const val DEFAULT_SAVE_FOLDER = ""
    const val DEFAULT_FILENAME_EMPTY = "MdPdf"

    // Timeouts
    const val HTML_RENDER_TIMEOUT_MS = 800L
    const val SHARING_TOAST_DURATION_MS = 2000L
}
