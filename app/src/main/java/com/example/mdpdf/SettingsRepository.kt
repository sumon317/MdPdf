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

    /**
     * A recent file entry pairing a display [name] with the persisted [uriString].
     * The URI string allows the file to be reopened directly from the recent-files list.
     */
    data class RecentFile(val name: String, val uriString: String)

    /**
     * Ordered list of recently opened files, newest-first.
     * Each entry stores both the display name and the SAF URI string so the file
     * can be reopened directly. Entries are serialised as "name\u0000uriString"
     * (null-byte delimiter) in a [SharedPreferences] [StringSet].
     *
     * Legacy entries (no null-byte) are silently discarded on read; they will be
     * replaced naturally as the user opens more files.
     */
    var recentFileEntries: List<RecentFile>
        get() = prefs.getStringSet(Constants.PREF_RECENT_FILES, emptySet())
            ?.mapNotNull { entry ->
                val idx = entry.indexOf('\u0000')
                if (idx > 0) RecentFile(entry.substring(0, idx), entry.substring(idx + 1)) else null
            }
            ?.sortedByDescending { it.uriString } // preserve insertion order via URI string (stable sort)
            ?: emptyList()
        private set(value) = prefs.edit()
            .putStringSet(
                Constants.PREF_RECENT_FILES,
                value.map { "${it.name}\u0000${it.uriString}" }.toSet()
            )
            .apply()

    /**
     * Adds or moves [name]+[uriString] to the top of the recent-files list (max 10 entries).
     * Deduplicates by URI so the same file is never listed twice.
     */
    fun addRecentFile(name: String, uriString: String) {
        val current = recentFileEntries.toMutableList()
        current.removeIf { it.uriString == uriString }
        current.add(0, RecentFile(name, uriString))
        recentFileEntries = current.take(10)
    }

    /**
     * @deprecated Use [addRecentFile] with explicit [uriString] so the file can be
     * reopened from the recent-files list.
     */
    @Deprecated(
        message = "Provide a uriString so recent files can be reopened.",
        replaceWith = ReplaceWith("addRecentFile(name, uriString)")
    )
    @Suppress("UnusedParameter")
    fun addRecentFile(name: String) {
        // No-op: callers should migrate to the two-argument overload.
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
