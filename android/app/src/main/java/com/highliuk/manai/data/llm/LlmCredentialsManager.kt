package com.highliuk.manai.data.llm

import android.content.SharedPreferences

class LlmCredentialsManager(
    private val encryptedPrefs: SharedPreferences,
) {
    fun getApiKey(): String? = encryptedPrefs.getString(KEY_API_KEY, null)

    fun saveApiKey(key: String) {
        encryptedPrefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun clearApiKey() {
        encryptedPrefs.edit().remove(KEY_API_KEY).apply()
    }

    fun isConfigured(): Boolean = !getApiKey().isNullOrBlank()

    private companion object {
        const val KEY_API_KEY = "llm_api_key"
    }
}
