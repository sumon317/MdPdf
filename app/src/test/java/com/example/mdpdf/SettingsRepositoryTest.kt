package com.example.mdpdf

import android.content.Context
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var settings: SettingsRepository

    @Before
    fun setup() {
        @Suppress("DEPRECATION")
        context = RuntimeEnvironment.application
        // Clear any existing instance
        SettingsRepository.resetForTesting()
        settings = SettingsRepository.getInstance(context)
    }

    @After
    fun tearDown() {
        SettingsRepository.resetForTesting()
    }

    @Test
    fun `defaults are correct`() {
        assertThat(settings.appTheme).isEqualTo("light")
        assertThat(settings.markdownTheme).isEqualTo("DEFAULT")
        assertThat(settings.showErrorsInPdf).isTrue()
        assertThat(settings.defaultFolder).isEmpty()
        assertThat(settings.notificationsEnabled).isTrue()
        assertThat(settings.spellCheckEnabled).isTrue()
    }

    @Test
    fun `appTheme persists across instances`() {
        settings.appTheme = "dark"
        SettingsRepository.resetForTesting()
        val newInstance = SettingsRepository.getInstance(context)

        assertThat(newInstance.appTheme).isEqualTo("dark")
    }

    @Test
    fun `markdownTheme persists across instances`() {
        settings.markdownTheme = "DARK"
        SettingsRepository.resetForTesting()
        val newInstance = SettingsRepository.getInstance(context)

        assertThat(newInstance.markdownTheme).isEqualTo("DARK")
    }

    @Test
    fun `showErrorsInPdf persists across instances`() {
        settings.showErrorsInPdf = false
        SettingsRepository.resetForTesting()
        val newInstance = SettingsRepository.getInstance(context)

        assertThat(newInstance.showErrorsInPdf).isFalse()
    }

    @Test
    fun `defaultFolder persists across instances`() {
        settings.defaultFolder = "content://test/folder"
        SettingsRepository.resetForTesting()
        val newInstance = SettingsRepository.getInstance(context)

        assertThat(newInstance.defaultFolder).isEqualTo("content://test/folder")
    }

    @Test
    fun `notificationsEnabled persists across instances`() {
        settings.notificationsEnabled = false
        SettingsRepository.resetForTesting()
        val newInstance = SettingsRepository.getInstance(context)

        assertThat(newInstance.notificationsEnabled).isFalse()
    }

    @Test
    fun `spellCheckEnabled persists across instances`() {
        settings.spellCheckEnabled = false
        SettingsRepository.resetForTesting()
        val newInstance = SettingsRepository.getInstance(context)

        assertThat(newInstance.spellCheckEnabled).isFalse()
    }

    @Test
    fun `singleton returns same instance`() {
        val instance1 = SettingsRepository.getInstance(context)
        val instance2 = SettingsRepository.getInstance(context)

        assertThat(instance1).isSameInstanceAs(instance2)
    }

    @Test
    fun `all themes can be set`() {
        val themes = listOf("light", "dark", "pure_black")
        for (theme in themes) {
            settings.appTheme = theme
            assertThat(settings.appTheme).isEqualTo(theme)
        }
    }

    @Test
    fun `all markdown themes can be set`() {
        val themes = MdTheme.entries.map { it.name }
        for (theme in themes) {
            settings.markdownTheme = theme
            assertThat(settings.markdownTheme).isEqualTo(theme)
        }
    }
}
