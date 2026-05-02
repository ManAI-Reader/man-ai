package com.highliuk.manai.data.translation

import android.content.SharedPreferences
import javax.inject.Inject

class DeepLCredentialsManager @Inject constructor(
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
        const val KEY_API_KEY = "deepl_api_key"
    }
}
