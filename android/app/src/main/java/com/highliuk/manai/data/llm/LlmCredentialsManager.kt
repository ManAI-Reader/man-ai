package com.highliuk.manai.data.llm

import android.content.SharedPreferences
import com.highliuk.manai.domain.model.LlmVendor

/**
 * Stores one API key per [LlmVendor] in encrypted preferences.
 *
 * The pre-multi-vendor releases stored a single key under `llm_api_key`,
 * which was always a Groq key: on first access it is migrated to the
 * Groq slot and the legacy entry is removed.
 */
class LlmCredentialsManager(
    private val encryptedPrefs: SharedPreferences,
) {
    fun getApiKey(vendor: LlmVendor): String? {
        migrateLegacyKeyIfNeeded()
        return encryptedPrefs.getString(vendor.storageKey, null)
    }

    fun saveApiKey(vendor: LlmVendor, key: String) {
        migrateLegacyKeyIfNeeded()
        encryptedPrefs.edit().putString(vendor.storageKey, key).apply()
    }

    fun clearApiKey(vendor: LlmVendor) {
        migrateLegacyKeyIfNeeded()
        encryptedPrefs.edit().remove(vendor.storageKey).apply()
    }

    fun isConfigured(vendor: LlmVendor): Boolean = !getApiKey(vendor).isNullOrBlank()

    private fun migrateLegacyKeyIfNeeded() {
        val legacy = encryptedPrefs.getString(KEY_LEGACY_API_KEY, null) ?: return
        val editor = encryptedPrefs.edit()
        if (encryptedPrefs.getString(KEY_GROQ_API_KEY, null) == null) {
            editor.putString(KEY_GROQ_API_KEY, legacy)
        }
        editor.remove(KEY_LEGACY_API_KEY).apply()
    }

    private val LlmVendor.storageKey: String
        get() = when (this) {
            LlmVendor.GROQ -> KEY_GROQ_API_KEY
            LlmVendor.DEEPSEEK -> KEY_DEEPSEEK_API_KEY
        }

    private companion object {
        const val KEY_LEGACY_API_KEY = "llm_api_key"
        const val KEY_GROQ_API_KEY = "groq_api_key"
        const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
    }
}
