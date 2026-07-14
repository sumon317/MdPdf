package com.example.mdpdf

import android.content.Context
import androidx.annotation.VisibleForTesting

/**
 * Thread-safe singleton backed by [SharedPreferences] for
 * persisting user settings (theme, spellcheck, notifications, etc.).
 *
 * Use [getInstance] to obtain the singleton with the application context.
 * Call [resetForTesting] before each test to clear the cached instance.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    var appTheme: String
        get() = prefs.getString(Constants.PREF_APP_THEME, Constants.DEFAULT_APP_THEME) ?: Constants.DEFAULT_APP_THEME
        set(value) = prefs.edit().putString(Constants.PREF_APP_THEME, value).apply()

    var markdownTheme: String
        get() = prefs.getString(Constants.PREF_MARKDOWN_THEME, Constants.DEFAULT_MARKDOWN_THEME)
            ?: Constants.DEFAULT_MARKDOWN_THEME
        set(value) = prefs.edit().putString(Constants.PREF_MARKDOWN_THEME, value).apply()

    var showErrorsInPdf: Boolean
        get() = prefs.getBoolean(Constants.PREF_SHOW_ERRORS, true)
        set(value) = prefs.edit().putBoolean(Constants.PREF_SHOW_ERRORS, value).apply()

    var defaultFolder: String
        get() = prefs.getString(Constants.PREF_DEFAULT_FOLDER, Constants.DEFAULT_SAVE_FOLDER)
            ?: Constants.DEFAULT_SAVE_FOLDER
        set(value) = prefs.edit().putString(Constants.PREF_DEFAULT_FOLDER, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(Constants.PREF_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(Constants.PREF_NOTIFICATIONS, value).apply()

    var spellCheckEnabled: Boolean
        get() = prefs.getBoolean(Constants.PREF_SPELLCHECK, true)
        set(value) = prefs.edit().putBoolean(Constants.PREF_SPELLCHECK, value).apply()

    var recentFiles: List<String>
        get() = prefs.getStringSet(Constants.PREF_RECENT_FILES, emptySet())?.toList() ?: emptyList()
        set(value) = prefs.edit().putStringSet(Constants.PREF_RECENT_FILES, value.toSet()).apply()

    fun addRecentFile(name: String) {
        val current = recentFiles.toMutableList()
        current.remove(name) // dedupe
        current.add(0, name) // newest first
        recentFiles = current.take(10) // keep last 10
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }

        @VisibleForTesting
        fun resetForTesting() {
            instance = null
        }
    }
}
