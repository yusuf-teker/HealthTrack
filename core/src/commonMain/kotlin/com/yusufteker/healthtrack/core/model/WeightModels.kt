package com.yusufteker.healthtrack.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class WeightChartPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY,
    ALL
}

@Serializable
data class WeightEntry(
    val id: String,
    val userId: Int,
    val date: String, // YYYY-MM-DD
    val weightKg: Double,
    val targetWeightKg: Double? = null,
    val note: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
