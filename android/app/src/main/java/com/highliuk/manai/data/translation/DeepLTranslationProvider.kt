package com.highliuk.manai.data.translation

import com.highliuk.manai.domain.model.TranslationResult
import com.highliuk.manai.domain.translation.TranslationProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DeepLResponse(
    val translations: List<DeepLTranslation>,
)

@Serializable
internal data class DeepLTranslation(
    @SerialName("text") val text: String,
)

class DeepLTranslationProvider(
    private val httpClient: HttpClient,
    private val apiKeyProvider: () -> String,
) : TranslationProvider {

    override val id: String = "deepl"

    private val baseUrl: String
        get() = if (apiKeyProvider().endsWith(":fx")) {
            "https://api-free.deepl.com"
        } else {
            "https://api.deepl.com"
        }

    override suspend fun translate(text: String, targetLang: String): TranslationResult {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            return TranslationResult.Error("DeepL API key is not configured")
        }
        return try {
            val httpResponse = httpClient.submitForm(
                url = "$baseUrl/v2/translate",
                formParameters = parameters {
                    append("text", text)
                    append("source_lang", "JA")
                    append("target_lang", targetLang)
                },
            ) {
                header(HttpHeaders.Authorization, "DeepL-Auth-Key $apiKey")
            }
            when {
                !httpResponse.status.isSuccess() -> TranslationResult.Error(
                    "DeepL error ${httpResponse.status.value} ${httpResponse.status.description}"
                )
                else -> {
                    val response: DeepLResponse = httpResponse.body()
                    val translated = response.translations.firstOrNull()?.text
                    if (translated != null) {
                        TranslationResult.Success(translated)
                    } else {
                        TranslationResult.Error("Empty translation response")
                    }
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            TranslationResult.Error(e.message ?: "Translation failed")
        }
    }
}
