package com.highliuk.manai.data.translation

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLCredentialsManagerTest {

    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val prefs = mockk<SharedPreferences> {
        every { edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
    }

    private fun createManager() = DeepLCredentialsManager(prefs)

    @Test
    fun `getApiKey returns stored key`() {
        every { prefs.getString("deepl_api_key", null) } returns "test-key:fx"

        assertEquals("test-key:fx", createManager().getApiKey())
    }

    @Test
    fun `getApiKey returns null when not configured`() {
        every { prefs.getString("deepl_api_key", null) } returns null

        assertEquals(null, createManager().getApiKey())
    }

    @Test
    fun `saveApiKey stores key`() {
        createManager().saveApiKey("new-key")

        verify { editor.putString("deepl_api_key", "new-key") }
        verify { editor.apply() }
    }

    @Test
    fun `clearApiKey removes key`() {
        createManager().clearApiKey()

        verify { editor.remove("deepl_api_key") }
        verify { editor.apply() }
    }

    @Test
    fun `isConfigured returns true when key exists`() {
        every { prefs.getString("deepl_api_key", null) } returns "key"

        assertTrue(createManager().isConfigured())
    }

    @Test
    fun `isConfigured returns false when key is null`() {
        every { prefs.getString("deepl_api_key", null) } returns null

        assertFalse(createManager().isConfigured())
    }

    @Test
    fun `isConfigured returns false when key is blank`() {
        every { prefs.getString("deepl_api_key", null) } returns "  "

        assertFalse(createManager().isConfigured())
    }
}
