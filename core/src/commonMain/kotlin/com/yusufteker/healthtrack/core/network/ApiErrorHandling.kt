package com.yusufteker.healthtrack.core.network

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Typed HTTP failure preserving status code and extracted server error message.
 */
class HttpApiException(
    val statusCode: Int,
    val serverMessage: String? = null,
    override val message: String
) : RuntimeException(message)

/**
 * Wraps suspend network operations while preserving coroutine cancellation semantics.
 */
internal suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        Result.failure(t)
    }
}

internal suspend fun HttpResponse.toHttpApiException(
    operationTr: String,
    operationEn: String,
    json: Json
): HttpApiException {
    val rawBody = runCatching { bodyAsText() }.getOrNull()?.trim()
    val extractedServerMsg = extractServerErrorMessage(rawBody, json)
    val baseMsg = trEn(
        "$operationTr (HTTP ${status.value})",
        "$operationEn (HTTP ${status.value})"
    )
    val fullMsg = if (!extractedServerMsg.isNullOrBlank()) {
        "$baseMsg: $extractedServerMsg"
    } else {
        baseMsg
    }
    return HttpApiException(
        statusCode = status.value,
        serverMessage = extractedServerMsg,
        message = fullMsg
    )
}

internal fun extractServerErrorMessage(rawBody: String?, json: Json): String? {
    if (rawBody.isNullOrBlank()) return null
    val parsed = runCatching {
        val obj = json.parseToJsonElement(rawBody).jsonObject
        obj["error"]?.jsonPrimitive?.content
            ?: obj["message"]?.jsonPrimitive?.content
    }.getOrNull()?.takeIf { it.isNotBlank() }

    val cleaned = parsed ?: rawBody.takeIf {
        !it.startsWith("<!DOCTYPE", ignoreCase = true) &&
            !it.startsWith("<html", ignoreCase = true)
    }

    return cleaned?.trim()?.take(220)?.takeIf { it.isNotBlank() }
}

/**
 * Formats HTTP and transport failures into actionable, localized user messages.
 */
fun formatActionableApiError(
    error: Throwable,
    operationTr: String,
    operationEn: String,
    isCredentialAuth: Boolean = false,
    language: AppLanguage = LocalizationManager.currentLanguage
): String {
    if (error is AiQuotaExceededException) {
        return error.message
    }

    val httpException = error as? HttpApiException
    val serverDetail = httpException?.serverMessage
    val rawMessage = error.message.orEmpty()

    val effectiveStatus = httpException?.statusCode ?: when {
        Regex("""\b401\b""").containsMatchIn(rawMessage) -> 401
        Regex("""\b403\b""").containsMatchIn(rawMessage) -> 403
        Regex("""\b404\b""").containsMatchIn(rawMessage) -> 404
        Regex("""\b429\b""").containsMatchIn(rawMessage) -> 429
        Regex("""\b500\b""").containsMatchIn(rawMessage) -> 500
        Regex("""\b50[234]\b""").containsMatchIn(rawMessage) -> 503
        else -> null
    }

    val detailText = serverDetail?.takeIf { it.isNotBlank() } ?: rawMessage

    return when {
        detailText.contains("GEMINI_API_KEY is not configured", ignoreCase = true) ->
            trEn(
                "Render sunucusunda GEMINI_API_KEY tanımlı değil. Profil > Özel API Key ile kendi anahtarınızı kullanabilirsiniz.",
                "GEMINI_API_KEY is not configured on the server. You can enter your own key in Profile > Custom API Key.",
                language
            )

        effectiveStatus == 404 ->
            trEn(
                "$operationTr başarısız (404): Sunucu servisi henüz uyanıyor veya yeni sürüm yükleniyor olabilir. 20-30 saniye sonra tekrar deneyin.",
                "$operationEn failed (404): The backend service may be waking up or deploying. Please try again in 20-30 seconds.",
                language
            )

        effectiveStatus == 401 ->
            if (isCredentialAuth) {
                trEn(
                    "Giriş başarısız (401): Kullanıcı adı/e-posta veya şifre hatalı.",
                    "Sign in failed (401): Invalid username/email or password.",
                    language
                )
            } else {
                trEn(
                    "$operationTr başarısız (401): Oturum süreniz doldu. Lütfen tekrar giriş yapınız.",
                    "$operationEn failed (401): Your session has expired. Please sign in again.",
                    language
                )
            }

        effectiveStatus == 403 ->
            trEn(
                "$operationTr reddedildi (403)${serverDetail?.let { ": $it" } ?: "."}",
                "$operationEn denied (403)${serverDetail?.let { ": $it" } ?: "."}",
                language
            )

        effectiveStatus == 429 ->
            serverDetail?.takeIf { it.isNotBlank() } ?: trEn(
                "$operationTr geçici olarak sınırlandı (429). Lütfen kısa süre sonra tekrar deneyin.",
                "$operationEn rate limit reached (429). Please try again shortly.",
                language
            )

        effectiveStatus != null && effectiveStatus >= 500 ->
            trEn(
                "$operationTr sunucu hatası ($effectiveStatus)${serverDetail?.let { ": $it" } ?: ". Sunucu ortam ayarları eksik olabilir veya geçici bir sorun yaşanıyor."}",
                "$operationEn server error ($effectiveStatus)${serverDetail?.let { ": $it" } ?: ". Missing server configuration or temporary backend error."}",
                language
            )

        rawMessage.contains("Connect", ignoreCase = true) ||
            rawMessage.contains("timeout", ignoreCase = true) ||
            rawMessage.contains("UnresolvedAddress", ignoreCase = true) ||
            rawMessage.contains("UnknownHost", ignoreCase = true) ||
            rawMessage.contains("Socket", ignoreCase = true) ||
            rawMessage.contains("Network", ignoreCase = true) ->
            trEn(
                "$operationTr sırasında sunucuya bağlanılamadı veya zaman aşımına uğradı. İnternet bağlantınızı kontrol ediniz.",
                "$operationEn failed due to connection or timeout. Please check your internet connection.",
                language
            )

        else ->
            trEn(
                "$operationTr başarısız: ${detailText.ifBlank { "Bilinmeyen hata" }}",
                "$operationEn failed: ${detailText.ifBlank { "Unknown error" }}",
                language
            )
    }
}

