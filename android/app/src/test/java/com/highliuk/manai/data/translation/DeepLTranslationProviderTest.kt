package com.highliuk.manai.data.translation

import com.highliuk.manai.domain.model.TranslationResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLTranslationProviderTest {

    private fun createMockClient(
        responseBody: String,
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        assertUrl: ((String) -> Unit)? = null,
        captureRequest: ((io.ktor.client.request.HttpRequestData) -> Unit)? = null,
    ): HttpClient {
        val engine = MockEngine { request ->
            assertUrl?.invoke(request.url.toString())
            captureRequest?.invoke(request)
            respond(
                content = responseBody,
                status = statusCode,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Test
    fun `id is deepl`() {
        val provider = DeepLTranslationProvider(
            createMockClient("{}"),
            apiKeyProvider = { "test-key" },
        )
        assertEquals("deepl", provider.id)
    }

    @Test
    fun `translate returns Success on valid response`() = runTest {
        val json = """{"translations":[{"text":"Hello"}]}"""
        val provider = DeepLTranslationProvider(
            createMockClient(json),
            apiKeyProvider = { "test-key" },
        )

        val result = provider.translate("こんにちは", "EN")

        assertEquals(TranslationResult.Success("Hello"), result)
    }

    @Test
    fun `translate returns Error on HTTP error`() = runTest {
        val provider = DeepLTranslationProvider(
            createMockClient("""{"message":"Forbidden"}""", HttpStatusCode.Forbidden),
            apiKeyProvider = { "bad-key" },
        )

        val result = provider.translate("テスト", "EN")

        assertTrue(result is TranslationResult.Error)
        val message = (result as TranslationResult.Error).message
        assertTrue(
            "Error message should mention HTTP status, got: $message",
            message.contains("403") || message.contains("Forbidden"),
        )
    }

    @Test
    fun `free key uses api-free endpoint`() = runTest {
        val json = """{"translations":[{"text":"Hi"}]}"""
        var capturedUrl = ""
        val provider = DeepLTranslationProvider(
            createMockClient(json, assertUrl = { url -> capturedUrl = url }),
            apiKeyProvider = { "free-key:fx" },
        )

        provider.translate("テスト", "EN")

        assertTrue(capturedUrl.contains("api-free.deepl.com"))
    }

    @Test
    fun `pro key uses api endpoint`() = runTest {
        val json = """{"translations":[{"text":"Hi"}]}"""
        var capturedUrl = ""
        val provider = DeepLTranslationProvider(
            createMockClient(json, assertUrl = { url -> capturedUrl = url }),
            apiKeyProvider = { "pro-key" },
        )

        provider.translate("テスト", "EN")

        assertTrue(capturedUrl.contains("api.deepl.com"))
    }

    @Test
    fun `translate returns Error on empty translations array`() = runTest {
        val json = """{"translations":[]}"""
        val provider = DeepLTranslationProvider(
            createMockClient(json),
            apiKeyProvider = { "test-key" },
        )

        val result = provider.translate("テスト", "EN")

        assertTrue(result is TranslationResult.Error)
    }

    @Test
    fun `translate returns Error when api key is blank`() = runTest {
        val provider = DeepLTranslationProvider(
            createMockClient("{}"),
            apiKeyProvider = { "" },
        )

        val result = provider.translate("テスト", "EN")

        assertTrue(result is TranslationResult.Error)
    }

    @Test
    fun `translate sends Authorization header with DeepL-Auth-Key scheme`() = runTest {
        val json = """{"translations":[{"text":"Hi"}]}"""
        var capturedAuth: String? = null
        val provider = DeepLTranslationProvider(
            createMockClient(json, captureRequest = { req ->
                capturedAuth = req.headers["Authorization"]
            }),
            apiKeyProvider = { "my-secret-key:fx" },
        )

        provider.translate("テスト", "EN")

        assertEquals("DeepL-Auth-Key my-secret-key:fx", capturedAuth)
    }

    @Test
    fun `translate does not send auth_key in form body`() = runTest {
        val json = """{"translations":[{"text":"Hi"}]}"""
        var capturedBody = ""
        val provider = DeepLTranslationProvider(
            createMockClient(json, captureRequest = { req ->
                capturedBody = (req.body as? io.ktor.http.content.OutgoingContent.ByteArrayContent)
                    ?.bytes()?.toString(Charsets.UTF_8)
                    ?: (req.body as? io.ktor.http.content.OutgoingContent.WriteChannelContent)?.toString()
                    ?: req.body.toString()
            }),
            apiKeyProvider = { "leaky-key:fx" },
        )

        provider.translate("テスト", "EN")

        assertTrue(
            "Form body must not contain auth_key, got: $capturedBody",
            !capturedBody.contains("auth_key") && !capturedBody.contains("leaky-key"),
        )
    }
}
