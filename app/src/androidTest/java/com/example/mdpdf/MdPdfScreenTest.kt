package com.example.mdpdf

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mdpdf.ui.MdPdfScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MdPdfScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var viewModel: MdPdfViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val savedStateHandle = SavedStateHandle()

        // Reset settings for clean test state
        SettingsRepository.resetForTesting()
        val settings = SettingsRepository.getInstance(app)
        settings.markdownTheme = MdTheme.DEFAULT.name
        settings.showErrorsInPdf = true

        viewModel = MdPdfViewModel(app, savedStateHandle)

        composeRule.setContent {
            MdPdfScreen(viewModel = viewModel)
        }
    }

    @Test
    fun `top bar renders title MdPdf`() {
        composeRule.onNodeWithText("MdPdf").assertExists()
    }

    @Test
    fun `view mode buttons are present`() {
        composeRule.onNodeWithText("Edit").assertExists()
        composeRule.onNodeWithText("Split").assertExists()
        composeRule.onNodeWithText("View").assertExists()
    }

    @Test
    fun `open button is present`() {
        composeRule.onNodeWithText("Open").assertExists()
    }

    @Test
    fun `export pdf button is present`() {
        composeRule.onNodeWithText("Export PDF").assertExists()
    }

    @Test
    fun `overflow menu icon is present`() {
        composeRule.onNodeWithTag("overflowMenuButton").assertExists()
    }
}
