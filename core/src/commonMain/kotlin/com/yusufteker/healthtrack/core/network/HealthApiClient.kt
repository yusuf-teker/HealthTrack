package com.yusufteker.healthtrack.core.network

import com.yusufteker.healthtrack.core.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
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
    private val baseUrl: String = "http://10.0.2.2:8080" // Default for Android Emulator to localhost, or Render URL
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
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 15_000
        }
        defaultRequest {
            url(baseUrl)
            tokenStorage.getAccessToken()?.let { token ->
                if (token.isNotBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
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
            throw RuntimeException("Yemek AI analizi başarısız: HTTP ${response.status.value}")
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
            throw RuntimeException("Spor AI analizi başarısız: HTTP ${response.status.value}")
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
