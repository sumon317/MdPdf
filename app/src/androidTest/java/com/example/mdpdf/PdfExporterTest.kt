package com.example.mdpdf

import android.net.Uri
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PdfExporterTest {

    private lateinit var exporter: PdfExporter
    private var tempUri: Uri? = null

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        activityRule.scenario.onActivity { activity ->
            exporter = PdfExporter(activity)

            // Create a temporary file URI for the export output
            val tempFile = File(activity.cacheDir, "test_export_${System.nanoTime()}.pdf")
            tempFile.createNewFile()
            tempUri = Uri.fromFile(tempFile)
        }
    }

    @After
    fun tearDown() {
        tempUri?.let { uri ->
            try {
                val file = File(uri.path ?: "")
                file.delete()
            } catch (_: Exception) {
            }
        }
    }

    @Test
    fun `export with activity context does not immediately fail`() {
        val latch = CountDownLatch(1)
        var completionError: Throwable? = null

        // This test verifies that export() does not synchronously throw.
        // The full PDF generation path is async (WebView), so we cannot
        // fully test it in a unit-test style, but we can verify the
        // synchronous guard (requires Activity context) passes.
        exporter.export(
            htmlContent = "<html><body><h1>Test</h1></body></html>",
            uri = tempUri!!,
            onProgress = { _, _ -> },
            onResult = { error ->
                completionError = error
                latch.countDown()
            }
        )

        // The synchronous path must not have thrown an error immediately.
        // completionError will be null until the async pipeline completes or fails.
        assertNull("Should not have received an immediate error", completionError)
    }

    @Test
    fun `export with service context returns error`() {
        val latch = CountDownLatch(1)
        var completionError: Throwable? = null

        activityRule.scenario.onActivity { activity ->
            val nonActivityContext = activity.applicationContext
            val nonActivityExporter = PdfExporter(nonActivityContext)

            nonActivityExporter.export(
                htmlContent = "<html><body><h1>Test</h1></body></html>",
                uri = tempUri!!,
                onResult = { error ->
                    completionError = error
                    latch.countDown()
                }
            )
        }

        latch.await(5, TimeUnit.SECONDS)
        assertNotNull("Expected an error for service context", completionError)
        assertTrue(
            "Expected error message about Activity",
            completionError!!.message?.contains("Activity") == true
        )
    }
}
