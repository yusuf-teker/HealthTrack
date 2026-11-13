package com.yusufteker.healthtrack.core.model

import kotlinx.serialization.Serializable

/**
 * Platform-agnostic telemetry snapshot rendered by both Android Home Screen Widgets
 * and iOS WidgetKit extensions, as well as the in-app Live Widget Preview.
 */
@Serializable
data class WidgetTelemetrySnapshot(
    val userId: Int = 0,
    val dateIso: String = "",
    val isLoggedIn: Boolean = false,
    val consumedCalories: Int = 0,
    val targetCalories: Int = 2000,
    val burnedCalories: Int = 0,
    val proteinG: Int = 0,
    val proteinTargetG: Int = 100,
    val carbsG: Int = 0,
    val carbsTargetG: Int = 250,
    val fatG: Int = 0,
    val fatTargetG: Int = 65,
    val waterMl: Int = 0,
    val waterTargetMl: Int = 2500,
    val steps: Int = 0,
    val stepTarget: Int = 10_000,
    val latestWeightKg: Double? = null,
    val targetWeightKg: Double? = null,
    val streakDays: Int = 0,
    val isActiveToday: Boolean = false,
    val themeAccentId: String = "ocean",
    val isDarkTheme: Boolean = true,
    val languageCode: String = "tr",
    val updatedAtMs: Long = 0L
) {
    val safeTargetCalories: Int
        get() = targetCalories.coerceAtLeast(1)

    val remainingCalories: Int
        get() = (safeTargetCalories - consumedCalories).coerceAtLeast(0)

    val overCalories: Int
        get() = (consumedCalories - safeTargetCalories).coerceAtLeast(0)

    val isOverCalories: Boolean
        get() = consumedCalories > safeTargetCalories

    val rawCalorieRatio: Float
        get() = consumedCalories.toFloat() / safeTargetCalories.toFloat()

    val calorieProgress: Float
        get() = rawCalorieRatio.coerceIn(0f, 1f)

    val waterProgress: Float
        get() = if (waterTargetMl > 0) (waterMl.toFloat() / waterTargetMl.toFloat()).coerceIn(0f, 1f) else 0f

    val isWaterGoalMet: Boolean
        get() = waterTargetMl > 0 && waterMl >= waterTargetMl

    val proteinProgress: Float
        get() = if (proteinTargetG > 0) (proteinG.toFloat() / proteinTargetG.toFloat()).coerceIn(0f, 1f) else 0f

    val carbsProgress: Float
        get() = if (carbsTargetG > 0) (carbsG.toFloat() / carbsTargetG.toFloat()).coerceIn(0f, 1f) else 0f

    val fatProgress: Float
        get() = if (fatTargetG > 0) (fatG.toFloat() / fatTargetG.toFloat()).coerceIn(0f, 1f) else 0f

    val hasMeaningfulData: Boolean
        get() = consumedCalories > 0 || burnedCalories > 0 || waterMl > 0 || steps > 0 || latestWeightKg != null

    companion object {
        fun sample(
            languageCode: String = "tr",
            isDarkTheme: Boolean = true,
            themeAccentId: String = "ocean"
        ): WidgetTelemetrySnapshot = WidgetTelemetrySnapshot(
            userId = 1,
            dateIso = "2026-11-13",
            isLoggedIn = true,
            consumedCalories = 1480,
            targetCalories = 2150,
            burnedCalories = 360,
            proteinG = 104,
            proteinTargetG = 135,
            carbsG = 148,
            carbsTargetG = 230,
            fatG = 46,
            fatTargetG = 68,
            waterMl = 1500,
            waterTargetMl = 2500,
            steps = 7420,
            stepTarget = 10_000,
            latestWeightKg = 74.2,
            targetWeightKg = 71.0,
            streakDays = 7,
            isActiveToday = true,
            themeAccentId = themeAccentId,
            isDarkTheme = isDarkTheme,
            languageCode = languageCode,
            updatedAtMs = 0L
        )
    }
}

