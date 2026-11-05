package com.yusufteker.healthtrack.core.network

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
    val date: String
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
    val disclaimer: String = "Besin değerleri tahmini yaklaşık değerlerdir."
)

@Serializable
data class WorkoutAiResultDto(
    val summary: String,
    val caloriesBurned: Int,
    val durationMinutes: Int,
    val disclaimer: String = "Yakılan kalori tahmini olarak hesaplanmıştır."
)

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
data class AppointmentSyncPayload(
    val id: String,
    val dateTime: Long,
    val doctorName: String,
    val hospitalName: String,
    val department: String? = null,
    val notes: String? = null,
    val reminderMinutes: Int = 60,
    val planoraTaskId: String? = null,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class PushRequestPayload(
    val foods: List<FoodSyncPayload> = emptyList(),
    val nutritionGoals: NutritionGoalSyncPayload? = null,
    val workouts: List<WorkoutSyncPayload> = emptyList(),
    val weights: List<WeightSyncPayload> = emptyList(),
    val appointments: List<AppointmentSyncPayload> = emptyList()
)

@Serializable
data class PushResponsePayload(
    val syncedFoodIds: List<String> = emptyList(),
    val syncedNutritionGoalId: String? = null,
    val syncedWorkoutIds: List<String> = emptyList(),
    val syncedWeightIds: List<String> = emptyList(),
    val syncedAppointmentIds: List<String> = emptyList(),
    val serverTimestamp: Long = 0L
)

@Serializable
data class PullResponsePayload(
    val foods: List<FoodSyncPayload> = emptyList(),
    val nutritionGoals: NutritionGoalSyncPayload? = null,
    val workouts: List<WorkoutSyncPayload> = emptyList(),
    val weights: List<WeightSyncPayload> = emptyList(),
    val appointments: List<AppointmentSyncPayload> = emptyList(),
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
                header.equals(HttpHeaders.Cookie, ignoreCase = true)
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
            val path = context.url.buildString()
            val isAuthEndpoint = path.contains("auth/login") || path.contains("auth/register") || path.contains("auth/refresh")
            if (!isAuthEndpoint) {
                val token = tokenStorage.getAccessToken()
                if (!token.isNullOrBlank()) {
                    context.headers.remove(HttpHeaders.Authorization)
                    context.headers.append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
    }

    suspend fun login(req: LoginRequest): Result<AuthResponse> = runCatching {
        val response = httpClient.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (response.status.isSuccess()) {
            response.body<AuthResponse>()
        } else {
            throw RuntimeException("Giriş başarısız: HTTP ${response.status.value}")
        }
    }

    suspend fun analyzeFood(text: String, date: String): Result<FoodAiResultDto> = runCatching {
        val response = httpClient.post("/health/ai/food") {
            contentType(ContentType.Application.Json)
            setBody(FoodAiRequestDto(text, date))
        }
        if (response.status.isSuccess()) {
            response.body<FoodAiResultDto>()
        } else {
            val errBody = runCatching { response.bodyAsText() }.getOrNull()?.take(200)
            throw RuntimeException("Yemek AI analizi başarısız (HTTP ${response.status.value}): ${errBody ?: ""}")
        }
    }

    suspend fun analyzeWorkout(text: String, userWeightKg: Double?, date: String): Result<WorkoutAiResultDto> = runCatching {
        val response = httpClient.post("/health/ai/workout") {
            contentType(ContentType.Application.Json)
            setBody(WorkoutAiRequestDto(text, userWeightKg, date))
        }
        if (response.status.isSuccess()) {
            response.body<WorkoutAiResultDto>()
        } else {
            val errBody = runCatching { response.bodyAsText() }.getOrNull()?.take(200)
            throw RuntimeException("Spor AI analizi başarısız (HTTP ${response.status.value}): ${errBody ?: ""}")
        }
    }

    suspend fun pushSync(req: PushRequestPayload): Result<PushResponsePayload> = runCatching {
        val response = httpClient.post("/health/sync/push") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (response.status.isSuccess()) {
            response.body<PushResponsePayload>()
        } else {
            throw RuntimeException("Senkronizasyon (push) başarısız: HTTP ${response.status.value}")
        }
    }

    suspend fun pullSync(since: Long): Result<PullResponsePayload> = runCatching {
        val response = httpClient.get("/health/sync/pull") {
            parameter("since", since)
        }
        if (response.status.isSuccess()) {
            response.body<PullResponsePayload>()
        } else {
            throw RuntimeException("Senkronizasyon (pull) başarısız: HTTP ${response.status.value}")
        }
    }
}
