package com.yusufteker.healthtrack.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class FitnessGoal(
    val id: String,
    val title: String,
    val subtitle: String,
    val proteinMultiplier: Double
) {
    LOSE_WEIGHT(
        id = "LOSE_WEIGHT",
        title = "Kilo Verme",
        subtitle = "Yağ yakımı & kas koruma (1.2x)",
        proteinMultiplier = 1.2
    ),
    MAINTAIN(
        id = "MAINTAIN",
        title = "Sabit Kalma",
        subtitle = "Kilo & form koruma (1.0x)",
        proteinMultiplier = 1.0
    ),
    BUILD_MUSCLE(
        id = "BUILD_MUSCLE",
        title = "Kaslanma",
        subtitle = "Hipertrofi & kas kazanımı (1.4x)",
        proteinMultiplier = 1.4
    );

    companion object {
        fun fromId(id: String?): FitnessGoal =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MAINTAIN
    }
}

/**
 * Calculates recommended daily protein in grams based on body weight and fitness goal.
 * Rule:
 * - Kilo Verme (Fat Loss / Deficit): 1.2 x body weight (prevents muscle breakdown in deficit)
 * - Sabit Kalma (Maintenance): 1.0 x body weight (at least body weight in grams)
 * - Kaslanma (Muscle Hypertrophy): 1.4 x body weight (optimal synthesis & growth)
 */
fun calculateProteinTarget(weightKg: Double, goal: FitnessGoal): Double {
    val safeWeight = if (weightKg > 0.0) weightKg else 70.0
    return kotlin.math.round(safeWeight * goal.proteinMultiplier * 10) / 10.0
}

@Serializable
data class NutritionGoal(
    val id: String,
    val userId: Int,
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
    val fitnessGoal: String = FitnessGoal.MAINTAIN.id,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
) {
    val parsedFitnessGoal: FitnessGoal
        get() = FitnessGoal.fromId(fitnessGoal)
}

@Serializable
data class NutrientProgress(
    val key: String,
    val label: String,
    val current: Double,
    val target: Double,
    val unit: String,
    val colorHex: String
) {
    val percentage: Float
        get() = if (target > 0) ((current / target) * 100).toFloat().coerceIn(0f, 100f) else 0f
}
