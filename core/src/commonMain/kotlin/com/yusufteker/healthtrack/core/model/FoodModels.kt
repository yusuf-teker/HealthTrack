package com.yusufteker.healthtrack.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Micronutrient(
    val name: String,
    val amount: Double,
    val unit: String
)

@Serializable
data class FoodItem(
    val id: String,
    val name: String,
    val formattedPortion: String,
    val calories: Int,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val micronutrients: List<Micronutrient> = emptyList()
)

@Serializable
data class FoodLog(
    val id: String,
    val userId: Int,
    val date: String, // YYYY-MM-DD
    val rawText: String,
    val summary: String? = null,
    val totalCalories: Int = 0,
    val items: List<FoodItem> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
