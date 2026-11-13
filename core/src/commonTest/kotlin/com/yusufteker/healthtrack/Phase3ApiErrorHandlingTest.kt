package com.yusufteker.healthtrack

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.network.AiQuotaExceededException
import com.yusufteker.healthtrack.core.network.HealthAiQuotaDto
import com.yusufteker.healthtrack.core.network.HttpApiException
import com.yusufteker.healthtrack.core.network.extractServerErrorMessage
import com.yusufteker.healthtrack.core.network.formatActionableApiError
import com.yusufteker.healthtrack.core.network.safeApiCall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Phase3ApiErrorHandlingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testExtractServerErrorMessageParsesJsonAndFiltersHtml() {
        assertEquals(
            "GEMINI_API_KEY is not configured",
            extractServerErrorMessage("""{"error":"GEMINI_API_KEY is not configured"}""", json)
        )
        assertEquals(
            "Token expired",
            extractServerErrorMessage("""{"message":"Token expired"}""", json)
        )
        assertEquals(
            "Plain backend error",
            extractServerErrorMessage("  Plain backend error  ", json)
        )
        assertNull(extractServerErrorMessage("<!DOCTYPE html><html><body>502 Bad Gateway</body></html>", json))
        assertNull(extractServerErrorMessage("<html><body>Error</body></html>", json))
        assertNull(extractServerErrorMessage("   ", json))
    }

    @Test
    fun testFormatActionableApiError404ExplainsBackendWakeupOrDeploy() {
        val err = HttpApiException(statusCode = 404, serverMessage = null, message = "HTTP 404")

        val trMsg = formatActionableApiError(
            error = err,
            operationTr = "AI besin analizi",
            operationEn = "AI food analysis",
            language = AppLanguage.TR
        )
        assertTrue(trMsg.contains("404"))
        assertTrue(trMsg.contains("uyanıyor") || trMsg.contains("yükleniyor"))

        val enMsg = formatActionableApiError(
            error = err,
            operationTr = "AI besin analizi",
            operationEn = "AI food analysis",
            language = AppLanguage.EN
        )
        assertTrue(enMsg.contains("404"))
        assertTrue(enMsg.contains("waking up") || enMsg.contains("deploying"))
    }

    @Test
    fun testFormatActionableApiError401DistinguishesSessionAndCredentialAuth() {
        val err = HttpApiException(statusCode = 401, serverMessage = "Unauthorized", message = "HTTP 401")

        val sessionMsg = formatActionableApiError(
            error = err,
            operationTr = "Senkronizasyon",
            operationEn = "Sync",
            isCredentialAuth = false,
            language = AppLanguage.TR
        )
        assertTrue(sessionMsg.contains("Oturum süreniz doldu"))

        val loginMsg = formatActionableApiError(
            error = err,
            operationTr = "Giriş",
            operationEn = "Sign in",
            isCredentialAuth = true,
            language = AppLanguage.EN
        )
        assertTrue(loginMsg.contains("Invalid username/email or password"))
    }

    @Test
    fun testFormatActionableApiError500AndGeminiKeyAnd429() {
        val missingKeyErr = HttpApiException(
            statusCode = 500,
            serverMessage = "GEMINI_API_KEY is not configured",
            message = "Server error"
        )
        val missingKeyMsg = formatActionableApiError(
            error = missingKeyErr,
            operationTr = "AI analizi",
            operationEn = "AI analysis",
            language = AppLanguage.TR
        )
        assertTrue(missingKeyMsg.contains("GEMINI_API_KEY"))
        assertTrue(missingKeyMsg.contains("Özel API Key"))

        val generic500 = HttpApiException(
            statusCode = 500,
            serverMessage = "Database connection pool exhausted",
            message = "HTTP 500"
        )
        val server500Msg = formatActionableApiError(
            error = generic500,
            operationTr = "Profil kaydı",
            operationEn = "Saving profile",
            language = AppLanguage.EN
        )
        assertTrue(server500Msg.contains("500"))
        assertTrue(server500Msg.contains("Database connection pool exhausted"))

        val quotaErr = AiQuotaExceededException(
            message = "Günlük ücretsiz AI hakkınız doldu.",
            quota = HealthAiQuotaDto(isPremium = false, dailyRemaining = 0, dailyLimit = 5)
        )
        assertEquals(
            "Günlük ücretsiz AI hakkınız doldu.",
            formatActionableApiError(
                error = quotaErr,
                operationTr = "Fotoğraf analizi",
                operationEn = "Photo analysis",
                language = AppLanguage.TR
            )
        )

        val rateLimitErr = HttpApiException(statusCode = 429, serverMessage = null, message = "HTTP 429")
        val rateLimitMsg = formatActionableApiError(
            error = rateLimitErr,
            operationTr = "AI analizi",
            operationEn = "AI analysis",
            language = AppLanguage.EN
        )
        assertTrue(rateLimitMsg.contains("429"))
    }

    @Test
    fun testFormatActionableApiErrorNetworkTimeoutAndCancellationSafety() = runTest {
        val timeoutErr = RuntimeException("ConnectTimeoutException: Request timeout has expired")
        val msg = formatActionableApiError(
            error = timeoutErr,
            operationTr = "Satın alma doğrulaması",
            operationEn = "Purchase verification",
            language = AppLanguage.TR
        )
        assertTrue(msg.contains("bağlanılamadı") || msg.contains("zaman aşımı"))

        assertFailsWith<CancellationException> {
            safeApiCall<Unit> {
                throw CancellationException("coroutine cancelled")
            }
        }
    }
}
