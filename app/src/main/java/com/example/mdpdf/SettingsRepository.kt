package com.example.mdpdf

import android.content.Context


class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("mdpdf_settings", Context.MODE_PRIVATE)

    var appTheme: String
        get() = prefs.getString("app_theme", "light") ?: "light"
        set(value) = prefs.edit().putString("app_theme", value).apply()

    var markdownTheme: String
        get() = prefs.getString("markdown_theme", "DEFAULT") ?: "DEFAULT"
        set(value) = prefs.edit().putString("markdown_theme", value).apply()

    var showErrorsInPdf: Boolean
        get() = prefs.getBoolean("show_errors", true)
        set(value) = prefs.edit().putBoolean("show_errors", value).apply()

    var defaultFolder: String
        get() = prefs.getString("default_folder", "") ?: ""
        set(value) = prefs.edit().putString("default_folder", value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications", true)
        set(value) = prefs.edit().putBoolean("notifications", value).apply()

    var spellCheckEnabled: Boolean
        get() = prefs.getBoolean("spellcheck", true)
        set(value) = prefs.edit().putBoolean("spellcheck", value).apply()

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
