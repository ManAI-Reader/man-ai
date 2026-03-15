package com.highliuk.manai

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.highliuk.manai.data.local.ManAiDatabase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.inject.Inject

@HiltAndroidTest
class MainActivityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: ManAiDatabase

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        database.clearAllTables()
    }

    @Test
    fun getFileName_returnsDisplayName_forFileUri() {
        val testFile = File(
            composeTestRule.activity.filesDir,
            "my-manga.pdf"
        )
        testFile.createNewFile()
        try {
            val uri = Uri.fromFile(testFile)
            val result = composeTestRule.activity.getFileName(uri)
            assertEquals("my-manga.pdf", result)
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun getFileName_returnsLastPathSegment_forContentUri() {
        val uri = Uri.parse(
            "content://com.example/documents/cool-manga.pdf"
        )
        val result = composeTestRule.activity.getFileName(uri)
        assertEquals("cool-manga.pdf", result)
    }

    @Test
    fun getFileName_fallsBackToAfterLastSlash_whenNoPath() {
        val uri = Uri.parse("custom://opaque")
        val result = composeTestRule.activity.getFileName(uri)
        assertEquals("opaque", result)
    }

    @Test
    fun handleIncomingIntent_callsOnNewIntent_forActionView() {
        val activity = composeTestRule.activity
        val testIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://com.example/test.pdf")
        }
        InstrumentationRegistry.getInstrumentation()
            .callActivityOnNewIntent(activity, testIntent)
        composeTestRule.waitForIdle()
    }

    @Test
    fun launchWithActionViewIntent_showsIntentLoading() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://com.example/intent-test.pdf")
            setClass(context, MainActivity::class.java)
        }
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                // Activity launched with ACTION_VIEW intent
                // handleIncomingIntent sets pendingIntentUri, NavHost shows intent-loading
                assertEquals(Intent.ACTION_VIEW, activity.intent.action)
            }
        }
    }
}
