package com.yusufteker.healthtrack.core.network

import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.*
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FoodAiRequestDto(
    val text: String,
    val date: String,
    val imageBase64: String? = null,
    val imageMimeType: String? = "image/jpeg"
)

@Serializable
data class WorkoutAiRequestDto(
    val text: String,
    val userWeightKg: Double? = null,
    val date: String
)

@Serializable
data class FoodAiResultDto(
    val summary: String,
    val totalCalories: Int,
    val items: List<FoodItem>,
    val disclaimer: String = trEn(
        "Besin değerleri tahmini yaklaşık değerlerdir.",
        "Nutritional values are estimated approximations."
    )
)

@Serializable
data class WorkoutAiResultDto(
    val summary: String,
    val caloriesBurned: Int,
    val durationMinutes: Int,
    val disclaimer: String = trEn(
        "Yakılan kalori tahmini olarak hesaplanmıştır.",
        "Calories burned are calculated as an estimate."
    )
)

@Serializable
data class HealthAiQuotaDto(
    val isPremium: Boolean = false,
    val dailyRemaining: Int = 3,
    val dailyLimit: Int = 3
)

@Serializable
data class HealthAiErrorDto(
    val error: String = "",
    val code: String = "",
    val quota: HealthAiQuotaDto? = null
)

class AiQuotaExceededException(
    override val message: String,
    val quota: HealthAiQuotaDto? = null
) : RuntimeException(message)

@Serializable
data class FoodSyncPayload(
    val id: String,
    val date: String,
    val rawText: String,
    val summary: String? = null,
    val totalCalories: Int = 0,
    val itemsJson: String = "[]",
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class NutritionGoalSyncPayload(
    val id: String,
    val calories: Int = 2000,
    val proteinG: Double = 80.0,
    val carbsG: Double = 250.0,
    val fatG: Double = 65.0,
    val fiberG: Double = 30.0,
    val vitaminCMg: Double = 90.0,
    val vitaminDUg: Double = 15.0,
    val vitaminB12Ug: Double = 2.4,
    val ironMg: Double = 14.0,
    val calciumMg: Double = 1000.0,
    val magnesiumMg: Double = 350.0,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class WorkoutSyncPayload(
    val id: String,
    val date: String,
    val rawText: String,
    val caloriesBurned: Int = 0,
    val durationMinutes: Int = 0,
    val targetCalories: Int = 300,
    val targetDurationMinutes: Int = 45,
    val aiAnalysis: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class WeightSyncPayload(
    val id: String,
    val date: String,
    val weightKg: Double,
    val targetWeightKg: Double? = null,
    val note: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class PushRequestPayload(
    val foods: List<FoodSyncPayload> = emptyList(),
    val nutritionGoals: NutritionGoalSyncPayload? = null,
    val workouts: List<WorkoutSyncPayload> = emptyList(),
    val weights: List<WeightSyncPayload> = emptyList()
)

@Serializable
data class PushResponsePayload(
    val syncedFoodIds: List<String> = emptyList(),
    val syncedNutritionGoalId: String? = null,
    val syncedWorkoutIds: List<String> = emptyList(),
    val syncedWeightIds: List<String> = emptyList(),
    val serverTimestamp: Long = 0L
)

@Serializable
data class PullResponsePayload(
    val foods: List<FoodSyncPayload> = emptyList(),
    val nutritionGoals: NutritionGoalSyncPayload? = null,
    val workouts: List<WorkoutSyncPayload> = emptyList(),
    val weights: List<WeightSyncPayload> = emptyList(),
    val serverTimestamp: Long = 0L
)

class HealthApiClient(
    private val tokenStorage: TokenStorage,
    private val baseUrl: String = "https://pulse-7b4z.onrender.com" // Planora Render backend
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(this@HealthApiClient.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 45_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 45_000
        }
        defaultRequest {
            url(baseUrl)
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Napier.d(message = message, tag = "HTTP_LOG")
                }
            }
            level = LogLevel.ALL
            sanitizeHeader { header ->
                header.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                header.equals(HttpHeaders.Cookie, ignoreCase = true) ||
                header.equals("X-Gemini-Api-Key", ignoreCase = true)
            }
        }
        install(Auth) {
            bearer {
                sendWithoutRequest { request ->
                    val path = request.url.encodedPath
                    !path.contains("auth/login") && !path.contains("auth/register") && !path.contains("auth/refresh")
                }

                loadTokens {
                    val accessToken = tokenStorage.getAccessToken()
                    val refreshToken = tokenStorage.getRefreshToken()
                    if (!accessToken.isNullOrBlank()) {
                        BearerTokens(accessToken, refreshToken ?: "")
                    } else {
                        null
                    }
                }

                refreshTokens {
                    val refreshToken = tokenStorage.getRefreshToken() ?: return@refreshTokens null
                    try {
                        val response = client.post("/auth/refresh") {
                            markAsRefreshTokenRequest()
                            contentType(ContentType.Application.Json)
                            setBody(RefreshTokenRequest(refreshToken))
                        }
                        if (response.status.isSuccess()) {
                            val authResponse = response.body<AuthResponse>()
                            tokenStorage.saveTokens(authResponse.accessToken, authResponse.refreshToken)
                            BearerTokens(authResponse.accessToken, authResponse.refreshToken)
                        } else {
                            if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                                Napier.w("Refresh token geçersiz (${response.status.value}), oturum kapatılıyor.", tag = "AUTH_LOG")
                                tokenStorage.clearSession()
                            }
                            null
                        }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        null
                    }
                }
            }
        }
    }

    init {
        httpClient.requestPipeline.intercept(HttpRequestPipeline.State) {
            val langCode = LocalizationManager.currentLanguage.code
            context.headers.remove(HttpHeaders.AcceptLanguage)
            context.headers.append(HttpHeaders.AcceptLanguage, langCode)
            val path = context.url.buildString()
            val isAuthEndpoint = path.contains("auth/login") || path.contains("auth/register") || path.contains("auth/refresh")
            if (!isAuthEndpoint) {
                val token = tokenStorage.getAccessToken()
                if (!token.isNullOrBlank()) {
                    context.headers.remove(HttpHeaders.Authorization)
                    context.headers.append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            if (path.contains("/health/ai/")) {
                val customGeminiKey = tokenStorage.getCustomGeminiApiKey()
                if (!customGeminiKey.isNullOrBlank()) {
                    context.headers.remove("X-Gemini-Api-Key")
                    context.headers.append("X-Gemini-Api-Key", customGeminiKey)
                }
            }
        }
    }

    suspend fun login(req: LoginRequest): Result<AuthResponse> = safeApiCall {
        val response = httpClient.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (response.status.isSuccess()) {
            response.body<AuthResponse>()
        } else {
            throw response.toHttpApiException("Giriş başarısız", "Sign in failed", json)
        }
    }

    suspend fun getAiQuota(): Result<HealthAiQuotaDto> = safeApiCall {
        val response = httpClient.get("/health/ai/quota")
        if (response.status.isSuccess()) {
            response.body<HealthAiQuotaDto>()
        } else {
            throw response.toHttpApiException("Kota bilgisi alınamadı", "Could not fetch quota info", json)
        }
    }

    suspend fun analyzeFood(
        text: String,
        date: String,
        imageBase64: String? = null,
        imageMimeType: String? = "image/jpeg"
    ): Result<FoodAiResultDto> = safeApiCall {
        val response = httpClient.post("/health/ai/food") {
            contentType(ContentType.Application.Json)
            setBody(FoodAiRequestDto(text, date, imageBase64, imageMimeType))
        }
        if (response.status.isSuccess()) {
            response.body<FoodAiResultDto>()
        } else if (response.status == HttpStatusCode.TooManyRequests) {
            val errDto = runCatching { response.body<HealthAiErrorDto>() }.getOrNull()
            throw AiQuotaExceededException(
                message = errDto?.error?.takeIf { it.isNotBlank() }
                    ?: trEn("Günlük ücretsiz AI hakkınız doldu.", "Daily free AI quota exceeded."),
                quota = errDto?.quota
            )
        } else {
            throw response.toHttpApiException("Yemek AI analizi başarısız", "Food AI analysis failed", json)
        }
    }

    suspend fun analyzeWorkout(text: String, userWeightKg: Double?, date: String): Result<WorkoutAiResultDto> = safeApiCall {
        val response = httpClient.post("/health/ai/workout") {
            contentType(ContentType.Application.Json)
            setBody(WorkoutAiRequestDto(text, userWeightKg, date))
        }
        if (response.status.isSuccess()) {
            response.body<WorkoutAiResultDto>()
        } else if (response.status == HttpStatusCode.TooManyRequests) {
            val errDto = runCatching { response.body<HealthAiErrorDto>() }.getOrNull()
            throw AiQuotaExceededException(
                message = errDto?.error?.takeIf { it.isNotBlank() }
                    ?: trEn("Günlük ücretsiz AI hakkınız doldu.", "Daily free AI quota exceeded."),
                quota = errDto?.quota
            )
        } else {
            throw response.toHttpApiException("Spor AI analizi başarısız", "Workout AI analysis failed", json)
        }
    }

    suspend fun pushSync(req: PushRequestPayload): Result<PushResponsePayload> = safeApiCall {
        val response = httpClient.post("/health/sync/push") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (response.status.isSuccess()) {
            response.body<PushResponsePayload>()
        } else {
            throw response.toHttpApiException("Senkronizasyon (push) başarısız", "Sync (push) failed", json)
        }
    }

    suspend fun pullSync(since: Long): Result<PullResponsePayload> = safeApiCall {
        val response = httpClient.get("/health/sync/pull") {
            parameter("since", since)
        }
        if (response.status.isSuccess()) {
            response.body<PullResponsePayload>()
        } else {
            throw response.toHttpApiException("Senkronizasyon (pull) başarısız", "Sync (pull) failed", json)
        }
    }

    suspend fun deleteAccount(): Result<Unit> = safeApiCall {
        val response = httpClient.delete("/users/me")
        if (!response.status.isSuccess()) {
            throw response.toHttpApiException("Hesap silme başarısız", "Account deletion failed", json)
        }
    }

    suspend fun fetchAppVersionConfig(): Result<AppVersionConfigDto> = safeApiCall {
        val response = httpClient.get("/api/app-config") {
            parameter("app", "healthtrack")
        }
        if (response.status.isSuccess()) {
            response.body<AppVersionConfigDto>()
        } else {
            throw response.toHttpApiException("Versiyon bilgisi alınamadı", "Could not fetch version info", json)
        }
    }

    suspend fun fetchSubscriptionPlans(): Result<List<SubscriptionPlan>> = safeApiCall {
        val response = httpClient.get("/api/subscriptions/plans") {
            parameter("app", "healthtrack")
        }
        if (response.status.isSuccess()) {
            response.body<List<SubscriptionPlan>>()
        } else {
            throw response.toHttpApiException("Abonelik planları alınamadı", "Could not fetch subscription plans", json)
        }
    }

    suspend fun fetchSubscriptionStatus(): Result<SubscriptionStatusResponse> = safeApiCall {
        val response = httpClient.get("/api/subscriptions/status")
        if (response.status.isSuccess()) {
            response.body<SubscriptionStatusResponse>()
        } else {
            throw response.toHttpApiException("Abonelik durumu alınamadı", "Could not fetch subscription status", json)
        }
    }

    suspend fun verifySubscriptionPurchase(req: VerifyPurchaseRequest): Result<VerifyPurchaseResponse> = safeApiCall {
        val response = httpClient.post("/api/subscriptions/verify") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (response.status.isSuccess() || response.status == HttpStatusCode.ServiceUnavailable) {
            response.body<VerifyPurchaseResponse>()
        } else {
            throw response.toHttpApiException("Satın alma doğrulama hatası", "Purchase verification error", json)
        }
    }

    suspend fun restoreSubscriptionPurchases(req: RestorePurchaseRequest): Result<SubscriptionStatusResponse> = safeApiCall {
        val response = httpClient.post("/api/subscriptions/restore") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (response.status.isSuccess()) {
            response.body<SubscriptionStatusResponse>()
        } else {
            throw response.toHttpApiException("Satın alma geri yükleme hatası", "Purchase restore error", json)
        }
    }

    suspend fun getLeaderboard(
        period: LeaderboardPeriod,
        scope: LeaderboardScope,
        category: LeaderboardCategory,
        dateIso: String
    ): Result<LeaderboardResponseDto> = safeApiCall {
        val response = httpClient.get("/health/leaderboard") {
            parameter("period", period.id)
            parameter("scope", scope.id)
            parameter("category", category.id)
            parameter("date", dateIso)
        }
        if (response.status.isSuccess()) {
            response.body<LeaderboardResponseDto>()
        } else {
            throw response.toHttpApiException("Lider tablosu alınamadı", "Could not fetch leaderboard", json)
        }
    }

    suspend fun searchUsers(query: String): Result<List<FriendUserDto>> = safeApiCall {
        val response = httpClient.get("/users/search") {
            parameter("q", query)
        }
        if (response.status.isSuccess()) {
            response.body<SearchUsersResponseDto>().users
        } else {
            throw response.toHttpApiException("Kullanıcı araması başarısız", "User search failed", json)
        }
    }

    suspend fun toggleFollowUser(userId: Int): Result<Unit> = safeApiCall {
        val response = httpClient.post("/users/$userId/follow")
        if (!response.status.isSuccess()) {
            throw response.toHttpApiException("Takip işlemi başarısız", "Follow action failed", json)
        }
    }

    suspend fun getFollowRequests(): Result<List<FollowRequestItemDto>> = safeApiCall {
        val response = httpClient.get("/users/follow-requests")
        if (response.status.isSuccess()) {
            response.body<List<FollowRequestItemDto>>()
        } else {
            throw response.toHttpApiException("Takip istekleri alınamadı", "Could not fetch follow requests", json)
        }
    }

    suspend fun acceptFollowRequest(requestId: Int): Result<Unit> = safeApiCall {
        val response = httpClient.post("/users/follow-requests/$requestId/accept")
        if (!response.status.isSuccess()) {
            throw response.toHttpApiException("İstek kabul edilemedi", "Could not accept request", json)
        }
    }

    suspend fun rejectFollowRequest(requestId: Int): Result<Unit> = safeApiCall {
        val response = httpClient.post("/users/follow-requests/$requestId/reject")
        if (!response.status.isSuccess()) {
            throw response.toHttpApiException("İstek reddedilemedi", "Could not reject request", json)
        }
    }
}
