package com.yusufteker.healthtrack.core.model

import com.yusufteker.healthtrack.core.i18n.trEn
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutLog(
    val id: String,
    val userId: Int,
    val date: String, // YYYY-MM-DD
    val rawText: String,
    val caloriesBurned: Int = 0,
    val durationMinutes: Int = 0,
    val targetCalories: Int = 300,
    val targetDurationMinutes: Int = 45,
    val aiAnalysis: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
) {
    val isDailySteps: Boolean
        get() = id.endsWith("_steps") ||
            id.contains("daily_steps") ||
            rawText.contains("Adım • Günlük Hareket", ignoreCase = true) ||
            rawText.contains("Steps • Daily Activity", ignoreCase = true)
}

@Serializable
data class DailyStepTelemetry(
    val stepCount: Int = 0,
    val distanceKm: Double = 0.0,
    val durationMinutes: Int = 0,
    val caloriesBurned: Int = 0,
    val netCaloriesBurned: Int = 0,
    val sourceName: String = trEn("Akıllı Saat", "Smartwatch"),
    val stepTarget: Int = 10_000
) {
    val progress: Float
        get() = if (stepTarget > 0) (stepCount.toFloat() / stepTarget).coerceIn(0f, 1f) else 0f
}
