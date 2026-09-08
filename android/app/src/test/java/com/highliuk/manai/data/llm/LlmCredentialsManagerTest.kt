package com.highliuk.manai.data.llm

import android.content.SharedPreferences
import com.highliuk.manai.domain.model.LlmVendor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmCredentialsManagerTest {

    private val prefs = FakeSharedPreferences()

    private fun createManager() = LlmCredentialsManager(prefs)

    @Test
    fun `getApiKey returns stored key for each vendor independently`() {
        prefs.putString("groq_api_key", "gsk_groq")
        prefs.putString("deepseek_api_key", "sk_deepseek")

        val manager = createManager()

        assertEquals("gsk_groq", manager.getApiKey(LlmVendor.GROQ))
        assertEquals("sk_deepseek", manager.getApiKey(LlmVendor.DEEPSEEK))
    }

    @Test
    fun `getApiKey returns null when vendor not configured`() {
        assertNull(createManager().getApiKey(LlmVendor.GROQ))
        assertNull(createManager().getApiKey(LlmVendor.DEEPSEEK))
    }

    @Test
    fun `saveApiKey stores key under the vendor specific storage key`() {
        val manager = createManager()

        manager.saveApiKey(LlmVendor.GROQ, "gsk_new")
        manager.saveApiKey(LlmVendor.DEEPSEEK, "sk_new")

        assertEquals("gsk_new", prefs.getString("groq_api_key", null))
        assertEquals("sk_new", prefs.getString("deepseek_api_key", null))
    }

    @Test
    fun `clearApiKey removes only the vendor key`() {
        prefs.putString("groq_api_key", "gsk_groq")
        prefs.putString("deepseek_api_key", "sk_deepseek")
        val manager = createManager()

        manager.clearApiKey(LlmVendor.GROQ)

        assertNull(prefs.getString("groq_api_key", null))
        assertEquals("sk_deepseek", prefs.getString("deepseek_api_key", null))
    }

    @Test
    fun `isConfigured reflects presence of a non-blank key per vendor`() {
        prefs.putString("groq_api_key", "gsk_groq")
        prefs.putString("deepseek_api_key", "  ")
        val manager = createManager()

        assertTrue(manager.isConfigured(LlmVendor.GROQ))
        assertFalse(manager.isConfigured(LlmVendor.DEEPSEEK))
    }

    @Test
    fun `legacy llm_api_key migrates to the groq key on first access`() {
        prefs.putString("llm_api_key", "gsk_legacy")
        val manager = createManager()

        assertEquals("gsk_legacy", manager.getApiKey(LlmVendor.GROQ))
        assertEquals("gsk_legacy", prefs.getString("groq_api_key", null))
        assertNull(prefs.getString("llm_api_key", null))
    }

    @Test
    fun `legacy key does not overwrite an existing groq key`() {
        prefs.putString("groq_api_key", "gsk_current")
        prefs.putString("llm_api_key", "gsk_legacy")
        val manager = createManager()

        assertEquals("gsk_current", manager.getApiKey(LlmVendor.GROQ))
    }

    @Test
    fun `legacy key never leaks into the deepseek slot`() {
        prefs.putString("llm_api_key", "gsk_legacy")
        val manager = createManager()

        assertNull(manager.getApiKey(LlmVendor.DEEPSEEK))
        assertEquals("gsk_legacy", manager.getApiKey(LlmVendor.GROQ))
    }
}

/** Minimal in-memory [SharedPreferences] storing only string values. */
private class FakeSharedPreferences : SharedPreferences {

    private val values = mutableMapOf<String, String>()

    fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getString(key: String, defValue: String?): String? = values[key] ?: defValue

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun getAll(): Map<String, *> = values.toMap()

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        defValues

    override fun getInt(key: String, defValue: Int): Int = defValue

    override fun getLong(key: String, defValue: Long): Long = defValue

    override fun getFloat(key: String, defValue: Float): Float = defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean = defValue

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    private inner class FakeEditor : SharedPreferences.Editor {

        private val puts = mutableMapOf<String, String>()
        private val removals = mutableSetOf<String>()

        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply {
            if (value == null) removals += key else puts[key] = value
        }

        override fun remove(key: String): SharedPreferences.Editor = apply { removals += key }

        override fun apply() {
            commit()
        }

        override fun commit(): Boolean {
            removals.forEach { values.remove(it) }
            values.putAll(puts)
            return true
        }

        override fun clear(): SharedPreferences.Editor = apply {
            removals += values.keys
        }

        override fun putStringSet(
            key: String,
            values: MutableSet<String>?,
        ): SharedPreferences.Editor = throw UnsupportedOperationException()

        override fun putInt(key: String, value: Int): SharedPreferences.Editor =
            throw UnsupportedOperationException()

        override fun putLong(key: String, value: Long): SharedPreferences.Editor =
            throw UnsupportedOperationException()

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor =
            throw UnsupportedOperationException()

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor =
            throw UnsupportedOperationException()
    }
}
