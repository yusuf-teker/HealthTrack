package com.yusufteker.healthtrack.feature.food.presentation

import com.yusufteker.healthtrack.core.model.CustomFoodIngredient
import com.yusufteker.healthtrack.core.model.CustomFoodItem
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.FoodItem
import com.yusufteker.healthtrack.core.model.FoodLog
import com.yusufteker.healthtrack.core.model.Micronutrient
import com.yusufteker.healthtrack.core.model.NutrientProgress
import com.yusufteker.healthtrack.core.model.NutrientSynergyTip
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.core.model.SmartMealSuggestion
import com.yusufteker.healthtrack.core.model.WaterEntry
import com.yusufteker.healthtrack.core.network.FoodAiResultDto
import com.yusufteker.healthtrack.core.utils.ExternalAppLauncher
import com.yusufteker.healthtrack.core.utils.getTodayIso

enum class SmartMealFilterType {
    FROM_USER_DB,
    QUICK,
    FEW_INGREDIENTS,
    EXCLUDE_RECENT_3_DAYS,
    HIGH_PROTEIN
}

data class FoodState(
    val selectedDate: String = getTodayIso(),
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
    val fitnessGoal: FitnessGoal = FitnessGoal.MAINTAIN,
    val selectedDateWeightKg: Double? = null,
    val isWeightExactForDate: Boolean = false,
    val weightRecordDate: String? = null,
    val effectiveProteinTarget: Double = 80.0,
    val nutrientBars: List<NutrientProgress> = emptyList(),
    val isEditGoalsDialogOpen: Boolean = false,
    val latestWeightKg: Double? = null,
    val weightDifferenceKg: Double? = null,
    val recentWeights: List<Double> = emptyList(),
    val pendingAiResult: FoodAiResultDto? = null,
    val isAiReviewSheetOpen: Boolean = false,
    val customFoods: List<CustomFoodItem> = emptyList(),
    val favoriteFoodIds: Set<String> = emptySet(),
    val frequentFoods: List<FoodItem> = emptyList(),
    val isAddCustomFoodDialogOpen: Boolean = false,
    val isAnalyzingCustomFoodAi: Boolean = false,
    val customFoodAiResult: FoodAiResultDto? = null,
    val customFoodAiPromptUsed: String = "",
    val customFoodAiError: String? = null,

    // ── 2.A: Su & Hidrasyon Telemetrisi ──
    val waterEntry: WaterEntry? = null,
    val dynamicWaterTargetMl: Int = 2500,

    // ── 1: Akıllı Öğün & "Ne Yesem?" Önerisi ──
    val isSmartMealSheetOpen: Boolean = false,
    val smartMealSuggestions: List<SmartMealSuggestion> = emptyList(),
    val latestAiRecipe: SmartMealSuggestion? = null,
    val isSmartMealFromUserDb: Boolean = false,
    val isSmartMealOnlyQuick: Boolean = false,
    val isSmartMealOnlyFewIngredients: Boolean = false,
    val isSmartMealExcludeRecent3Days: Boolean = true,
    val isSmartMealHighProtein: Boolean = false,
    val isGeneratingAiCustomRecipe: Boolean = false,

    // ── 2.B: Besin Sinerjisi & Emilim İpuçları ──
    val synergyTips: List<NutrientSynergyTip> = emptyList(),
    val dismissedTipIds: Set<String> = emptySet(),

    // ── 2.D: Fotoğraftan / Barkoddan / Besin Etiketinden Ekleme ──
    val isPhotoScanSheetOpen: Boolean = false,
    val isPhotoScanAnalyzing: Boolean = false,

    // ── Market Listesi & Planora Entegrasyonu ──
    val isGroceryListSheetOpen: Boolean = false,
    val groceryListTitle: String = "Market Alışveriş Listesi",
    val groceryItems: List<String> = emptyList(),
    val selectedGroceryItems: Set<String> = emptySet()
)

sealed interface FoodIntent {
    data class OnDateChanged(val newDate: String) : FoodIntent
    data class OnRawTextChanged(val text: String) : FoodIntent
    data object OnCalculateAiClicked : FoodIntent
    data object OnDismissAiReview : FoodIntent
    data object OnConfirmAiReview : FoodIntent
    data object OnSaveManualClicked : FoodIntent
    data class OnRemoveItem(val itemId: String) : FoodIntent
    data object OnClearFoodLog : FoodIntent
    data class OnAddItem(
        val name: String,
        val portion: String,
        val calories: Int,
        val proteinG: Double = 0.0,
        val carbsG: Double = 0.0,
        val fatG: Double = 0.0,
        val fiberG: Double = 0.0,
        val micronutrients: List<Micronutrient> = emptyList(),
        val iconKey: String? = null
    ) : FoodIntent
    data class OnAddMultipleItems(
        val items: List<FoodItem>,
        val summaryNote: String? = null
    ) : FoodIntent
    data object OnOpenEditGoalsDialog : FoodIntent
    data object OnCloseEditGoalsDialog : FoodIntent
    data class OnSaveGoals(val goal: NutritionGoal, val fitnessGoal: FitnessGoal = FitnessGoal.MAINTAIN) : FoodIntent
    data object OnDismissInfoMessage : FoodIntent
    data object OnDismissErrorMessage : FoodIntent
    data class OnToggleFavoriteFood(val foodId: String) : FoodIntent
    data object OnOpenAddCustomFoodDialog : FoodIntent
    data object OnCloseAddCustomFoodDialog : FoodIntent
    data class OnSaveCustomFood(
        val name: String,
        val brand: String?,
        val portionUnit: String,
        val calories: Int,
        val proteinG: Double = 0.0,
        val carbsG: Double = 0.0,
        val fatG: Double = 0.0,
        val fiberG: Double = 0.0,
        val categoryName: String = "ALL",
        val makeFavorite: Boolean = false,
        val iconKey: String? = null,
        val micronutrients: List<Micronutrient> = emptyList(),
        val ingredients: List<CustomFoodIngredient> = emptyList(),
        val baseAmount: Double = 1.0,
        val baseUnitLabel: String = "",
        val presetLevels: List<Double> = emptyList()
    ) : FoodIntent
    data class OnDeleteCustomFood(val id: String) : FoodIntent
    data class OnAnalyzeCustomFoodAi(val prompt: String, val isMixMode: Boolean) : FoodIntent
    data object OnConsumeCustomFoodAiResult : FoodIntent
    data object OnDismissCustomFoodAiError : FoodIntent

    // ── 2.A: Su & Hidrasyon Intent'leri ──
    data class OnAddWater(val deltaMl: Int) : FoodIntent
    data object OnResetWater : FoodIntent

    // ── 1: Akıllı Öğün Intent'leri ──
    data object OnOpenSmartMealSheet : FoodIntent
    data object OnCloseSmartMealSheet : FoodIntent
    data class OnToggleSmartMealFilter(val filterType: SmartMealFilterType) : FoodIntent
    data class OnAddSuggestedMeal(val meal: SmartMealSuggestion) : FoodIntent
    data object OnRequestCustomAiMealSuggestion : FoodIntent
    data object OnDismissAiRecipe : FoodIntent

    // ── 2.B: Besin Sinerjisi Intent'i ──
    data class OnDismissSynergyTip(val tipId: String) : FoodIntent

    // ── 2.D: Fotoğraf / Etiket Tarama Intent'leri ──
    data object OnOpenPhotoScanSheet : FoodIntent
    data object OnClosePhotoScanSheet : FoodIntent
    data class OnAnalyzePhotoOrLabel(val textOrPrompt: String) : FoodIntent

    // ── Market Listesi & Planora Intent'leri ──
    data class OnOpenGroceryListSheet(val title: String, val items: List<String>) : FoodIntent
    data object OnCloseGroceryListSheet : FoodIntent
    data class OnToggleGroceryItem(val item: String) : FoodIntent
    data class OnSendGroceryToPlanora(val appLauncher: ExternalAppLauncher) : FoodIntent
}

sealed interface FoodEffect {
    data class ShowToast(val message: String) : FoodEffect
}
