package com.yusufteker.healthtrack.feature.food.presentation

import com.yusufteker.healthtrack.core.model.FoodItem
import com.yusufteker.healthtrack.core.model.FoodLog
import com.yusufteker.healthtrack.core.model.NutrientProgress
import com.yusufteker.healthtrack.core.model.NutritionGoal

data class FoodState(
    val selectedDate: String = "2026-11-04",
    val rawText: String = "",
    val currentFoodLog: FoodLog? = null,
    val items: List<FoodItem> = emptyList(),
    val totalCalories: Int = 0,
    val summary: String? = null,
    val isCalculatingAi: Boolean = false,
    val isSaving: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val nutritionGoal: NutritionGoal = NutritionGoal(id = "default", userId = 0),
    val nutrientBars: List<NutrientProgress> = emptyList(),
    val isEditGoalsDialogOpen: Boolean = false
)

sealed interface FoodIntent {
    data class OnDateChanged(val newDate: String) : FoodIntent
    data class OnRawTextChanged(val text: String) : FoodIntent
    data object OnCalculateAiClicked : FoodIntent
    data object OnSaveManualClicked : FoodIntent
    data class OnRemoveItem(val itemId: String) : FoodIntent
    data class OnAddItem(val name: String, val portion: String, val calories: Int) : FoodIntent
    data object OnOpenEditGoalsDialog : FoodIntent
    data object OnCloseEditGoalsDialog : FoodIntent
    data class OnSaveGoals(val goal: NutritionGoal) : FoodIntent
    data object OnDismissInfoMessage : FoodIntent
    data object OnDismissErrorMessage : FoodIntent
}

sealed interface FoodEffect {
    data class ShowToast(val message: String) : FoodEffect
}
