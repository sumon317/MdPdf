package com.example.mdpdf

import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.example.mdpdf.ui.ViewMode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MdPdfViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: MdPdfViewModel
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var application: Application

    @Before
    fun setup() {
        application = RuntimeEnvironment.getApplication()

        // Reset singleton for clean test state
        SettingsRepository.resetForTesting()
        val settings = SettingsRepository.getInstance(application)
        settings.markdownTheme = MdTheme.DEFAULT.name
        settings.showErrorsInPdf = true
        settings.spellCheckEnabled = true
        settings.notificationsEnabled = true
        settings.appTheme = "light"

        savedStateHandle = SavedStateHandle()
        viewModel = MdPdfViewModel(application, savedStateHandle)
    }

    @After
    fun tearDown() {
        SettingsRepository.resetForTesting()
    }

    @Test
    fun `initial markdown text is default`() = runTest {
        assertThat(viewModel.markdownText.value)
            .isEqualTo(MdPdfViewModel.getDefaultMarkdownForTesting())
    }

    @Test
    fun `updateMarkdownText mutates flow`() = runTest {
        val newText = "# New\nContent"
        viewModel.updateMarkdownText(newText)
        assertThat(viewModel.markdownText.value).isEqualTo(newText)
    }

    @Test
    fun `updateMarkdownText persists to savedStateHandle`() = runTest {
        val newText = "persisted"
        viewModel.updateMarkdownText(newText)
        assertThat(savedStateHandle.get<String>(Constants.STATE_KEY_MARKDOWN_TEXT)).isEqualTo(newText)
    }

    @Test
    fun `updateCurrentFileName mutates flow`() = runTest {
        viewModel.updateCurrentFileName("test.md")
        assertThat(viewModel.currentFileName.value).isEqualTo("test.md")
    }

    @Test
    fun `updateCurrentFileName persists to savedStateHandle`() = runTest {
        viewModel.updateCurrentFileName("doc.md")
        assertThat(savedStateHandle.get<String>(Constants.STATE_KEY_CURRENT_FILE_NAME)).isEqualTo("doc.md")
    }

    @Test
    fun `updateCurrentFileUri mutates flow`() = runTest {
        val uri = Uri.parse("content://test/file.md")
        viewModel.updateCurrentFileUri(uri)
        assertThat(viewModel.currentFileUri.value).isEqualTo(uri.toString())
    }

    @Test
    fun `updateCurrentFileUri persists to savedStateHandle`() = runTest {
        val uri = Uri.parse("content://a/b.md")
        viewModel.updateCurrentFileUri(uri)
        assertThat(savedStateHandle.get<String>(Constants.STATE_KEY_CURRENT_FILE_URI)).isEqualTo(uri.toString())
    }

    @Test
    fun `updateViewMode mutates savedStateHandle`() = runTest {
        viewModel.updateViewMode(ViewMode.EDITOR)
        assertThat(savedStateHandle.get<String>(Constants.STATE_KEY_VIEW_MODE)).isEqualTo(ViewMode.EDITOR.name)
    }

    @Test
    fun `updateSelectedTheme persists to settings`() = runTest {
        viewModel.updateSelectedTheme(MdTheme.DARK)
        val settings = SettingsRepository.getInstance(application)
        assertThat(settings.markdownTheme).isEqualTo(MdTheme.DARK.name)
    }

    @Test
    fun `htmlContent exists as a StateFlow`() = runTest {
        val flow = viewModel.htmlContent

        @Suppress("UNUSED_VARIABLE")
        val collected: String = flow.value
        // StateFlow.value always returns a result (may be initial empty string)
        // The test verifies the property exists and doesn't throw
    }

    @Test
    fun `parser instance is available`() = runTest {
        assertThat(viewModel.parser).isNotNull()
    }

    @Test
    fun `loadMarkdownFromUri calls callback`() = runBlocking {
        var called = false
        viewModel.loadMarkdownFromUri(Uri.parse("content://nonexistent/file.md")) { _, _ ->
            called = true
        }
        // Poll the main looper until the IO coroutine posts its callback back on Main.
        val deadline = System.currentTimeMillis() + 2_000
        while (!called && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertThat(called).isTrue()
    }

    @Test
    fun `loadMarkdownFromUri reports failure`() = runBlocking {
        var success = true // default true; we expect false on failure
        viewModel.loadMarkdownFromUri(Uri.parse("content://nonexistent/file.md")) { s, _ ->
            success = s
        }
        // Poll the main looper until the IO coroutine posts its callback back on Main.
        val deadline = System.currentTimeMillis() + 2_000
        while (success && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertThat(success).isFalse()
    }
}
