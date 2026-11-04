package com.yusufteker.healthtrack.core.model

import kotlinx.serialization.Serializable

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
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

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
