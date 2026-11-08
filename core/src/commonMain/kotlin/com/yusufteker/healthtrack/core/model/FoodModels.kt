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
    val id: String = "",
    val name: String,
    val formattedPortion: String,
    val calories: Int,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val micronutrients: List<Micronutrient> = emptyList(),
    val iconKey: String? = null
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

@Serializable
data class CustomFoodIngredient(
    val id: String = "",
    val name: String,
    val unitLabel: String, // e.g. "Shot", "Ölçek", "ml", "g", "Adet", "Tatlı Kaşığı"
    val defaultAmount: Double = 1.0,
    val stepAmount: Double = 1.0,
    val presetLevels: List<Double> = emptyList(),
    val baseCalories: Int,
    val baseProteinG: Double = 0.0,
    val baseCarbsG: Double = 0.0,
    val baseFatG: Double = 0.0,
    val baseFiberG: Double = 0.0,
    val micronutrients: List<Micronutrient> = emptyList(),
    val iconKey: String? = null
)

@Serializable
data class CustomFoodItem(
    val id: String,
    val userId: Int = 0,
    val name: String,
    val brand: String? = null,
    val portionUnit: String, // e.g. "500ml (1 Kutu)", "100g", "1 Dilim", "1 Adet"
    val unitStep: Int = 1,
    val calories: Int,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val categoryName: String = "ALL",
    val iconKey: String? = null,
    val micronutrients: List<Micronutrient> = emptyList(),
    val ingredients: List<CustomFoodIngredient> = emptyList(),
    val baseAmount: Double = 1.0,
    val baseUnitLabel: String = "",
    val presetLevels: List<Double> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val isMix: Boolean get() = ingredients.isNotEmpty()
}
