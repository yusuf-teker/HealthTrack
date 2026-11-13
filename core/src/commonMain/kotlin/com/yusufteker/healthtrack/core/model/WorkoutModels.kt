package com.yusufteker.healthtrack.core.model

import com.yusufteker.healthtrack.core.i18n.trEn
import kotlinx.serialization.Serializable
import kotlin.math.round
import kotlin.math.roundToInt

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

/**
 * Shared telemetry helpers used by both WorkoutViewModel and GamificationManager
 * so daily calorie burn rings and goal-reached celebrations always match 1:1.
 */
object WorkoutTelemetryHelper {
    fun estimateSportSteps(rawText: String, durationMinutes: Int): Int {
        if (durationMinutes <= 0) return 0
        val lower = rawText.lowercase()
        val stepsPerMinute = when {
            lower.contains("koşu") || lower.contains("running") || lower.contains("jogging") -> 140
            lower.contains("tenis") || lower.contains("tennis") -> 85
            lower.contains("basket") || lower.contains("futbol") || lower.contains("soccer") -> 100
            lower.contains("yürüyüş") || lower.contains("walking") || lower.contains("trekking") -> 100
            lower.contains("hiit") || lower.contains("kardiyo") -> 90
            lower.contains("fitness") || lower.contains("ağırlık") || lower.contains("body") || lower.contains("bench") -> 25
            lower.contains("pilates") || lower.contains("yoga") -> 10
            lower.contains("bisiklet") || lower.contains("cycling") || lower.contains("yüzme") || lower.contains("swim") -> 0
            else -> 60
        }
        return stepsPerMinute * durationMinutes
    }

    fun parseDailyStepTelemetry(
        stepLog: WorkoutLog,
        explicitWorkouts: List<WorkoutLog> = emptyList()
    ): DailyStepTelemetry {
        val steps = Regex("""(\d+)\s*(?:Adım|Steps)""", RegexOption.IGNORE_CASE)
            .find(stepLog.rawText)?.groupValues?.get(1)?.toIntOrNull()
            ?: (stepLog.durationMinutes * 100).coerceAtLeast(0)
        val analysis = stepLog.aiAnalysis.orEmpty()
        val distanceKm = Regex("""([\d.,]+)\s*km""").find(analysis)?.groupValues?.get(1)
            ?.replace(',', '.')?.toDoubleOrNull()
            ?: ((steps * 0.75) / 1000.0)
        val defaultWatchName = trEn("Akıllı Saat", "Smart Watch")
        val source = if (analysis.contains("•")) {
            analysis.substringBefore("•").trim().removePrefix("⌚").trim()
        } else {
            defaultWatchName
        }
        val estimatedSportSteps = explicitWorkouts.sumOf { estimateSportSteps(it.rawText, it.durationMinutes) }
        val netSteps = (steps - estimatedSportSteps).coerceAtLeast(0)
        val netKcal = if (steps > 0) {
            val ratio = netSteps.toDouble() / steps
            (stepLog.caloriesBurned * ratio).roundToInt()
        } else 0

        return DailyStepTelemetry(
            stepCount = steps,
            distanceKm = round(distanceKm * 10.0) / 10.0,
            durationMinutes = stepLog.durationMinutes,
            caloriesBurned = stepLog.caloriesBurned,
            netCaloriesBurned = netKcal,
            sourceName = source.ifBlank { defaultWatchName },
            stepTarget = 10_000
        )
    }

    fun calculateEffectiveDailyBurn(
        explicitWorkouts: List<WorkoutLog>,
        stepLog: WorkoutLog?
    ): Int {
        val explicitBurned = explicitWorkouts.sumOf { it.caloriesBurned.coerceAtLeast(0) }
        val netStepBurned = stepLog?.let { parseDailyStepTelemetry(it, explicitWorkouts).netCaloriesBurned } ?: 0
        return explicitBurned + netStepBurned
    }
}
