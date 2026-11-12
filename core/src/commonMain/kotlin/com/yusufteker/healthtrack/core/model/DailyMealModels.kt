package com.yusufteker.healthtrack.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class MealSlotType(val label: String, val iconKey: String) {
    BREAKFAST("Kahvaltı", "breakfast"),
    LUNCH("Öğle Yemeği", "lunch"),
    DINNER("Akşam Yemeği", "dinner"),
    SNACK("Ara Öğün", "snack")
}

@Serializable
data class MealSlotSuggestion(
    val slot: MealSlotType,
    val title: String,
    val subtitle: String,
    val prepTimeMinutes: Int = 10,
    val ingredients: List<String> = emptyList(),
    val instructions: String = "",
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val keyBenefits: String = "",
    val targetDeficiencies: List<String> = emptyList(),
    val itemsToAdd: List<FoodItem> = emptyList()
) {
    val slotTitle: String get() = slot.label
}

@Serializable
data class DailyMealPlan(
    val id: String,
    val userId: Int,
    val dateIso: String,
    val headline: String,
    val deficiencySummary: String,
    val detectedDeficiencies: List<String>,
    val breakfast: MealSlotSuggestion,
    val lunch: MealSlotSuggestion,
    val dinner: MealSlotSuggestion,
    val snack: MealSlotSuggestion,
    val totalCalories: Int,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val totalFiberG: Double,
    val isAiGenerated: Boolean = true,
    val createdAt: Long = 0L
) {
    val allSlots: List<MealSlotSuggestion>
        get() = listOf(breakfast, lunch, dinner, snack)
}

