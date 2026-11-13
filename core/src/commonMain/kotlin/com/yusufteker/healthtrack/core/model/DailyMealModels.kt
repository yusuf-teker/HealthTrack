package com.yusufteker.healthtrack.core.model

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import kotlinx.serialization.Serializable

@Serializable
enum class MealSlotType(val labelTr: String, val labelEn: String, val iconKey: String) {
    BREAKFAST("Kahvaltı", "Breakfast", "breakfast"),
    LUNCH("Öğle Yemeği", "Lunch", "lunch"),
    DINNER("Akşam Yemeği", "Dinner", "dinner"),
    SNACK("Ara Öğün", "Snack", "snack");

    val label: String
        get() = localizedLabel()

    fun localizedLabel(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(labelTr, labelEn, language)
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

