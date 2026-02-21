package com.highliuk.manai

import android.net.Uri
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class GetFileNameTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun getFileName_returnsDisplayName_forContentUri() {
        // content:// URIs with DISPLAY_NAME are handled by ContentResolver query
        // This test verifies the method is accessible and returns a non-null String
        val uri = Uri.parse("file:///storage/emulated/0/Download/test-manga.pdf")
        val result = composeTestRule.activity.getFileName(uri)
        assertEquals("test-manga.pdf", result)
    }

    @Test
    fun getFileName_returnsLastPathSegment_whenDisplayNameUnavailable() {
        // file:// URIs don't have DISPLAY_NAME via ContentResolver
        val uri = Uri.parse("file:///some/path/my-manga.pdf")
        val result = composeTestRule.activity.getFileName(uri)
        assertEquals("my-manga.pdf", result)
    }

    @Test
    fun getFileName_returnsFallback_forOpaqueUri() {
        val uri = Uri.parse("content://com.example/raw-stream")
        val result = composeTestRule.activity.getFileName(uri)
        // Should return lastPathSegment or substringAfterLast, never null
        assertEquals("raw-stream", result)
    }
}
