package com.yusufteker.healthtrack.feature.food.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yusufteker.healthtrack.core.model.CustomFoodIngredient
import com.yusufteker.healthtrack.core.model.CustomFoodItem
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.FoodItem
import com.yusufteker.healthtrack.core.model.Micronutrient
import com.yusufteker.healthtrack.core.model.NutrientProgress
import com.yusufteker.healthtrack.core.model.NutrientSynergyTip
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.core.model.WaterEntry
import com.yusufteker.healthtrack.core.network.FoodAiResultDto
import com.yusufteker.healthtrack.feature.food.presentation.components.AddCustomFoodDialog
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodAiReviewBottomSheet
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodBentoRow
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodHydrationAndSmartMealRow
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodIconSelector
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodPhotoScanBottomSheet
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodTableCard
import com.yusufteker.healthtrack.feature.food.presentation.components.InventoryMixStudioDialog
import com.yusufteker.healthtrack.feature.food.presentation.components.NutrientBarsView
import com.yusufteker.healthtrack.feature.food.presentation.components.QuickFoodPresetsView
import com.yusufteker.healthtrack.theme.HealthTrackTheme

private val sampleFoodItems = listOf(
    FoodItem(
        id = "1",
        name = "Izgara Tavuk Göğsü",
        formattedPortion = "200g",
        calories = 330,
        proteinG = 62.0,
        carbsG = 0.0,
        fatG = 7.2,
        fiberG = 0.0
    ),
    FoodItem(
        id = "2",
        name = "Yulaf Ezmesi & Muz",
        formattedPortion = "1 Kase (80g)",
        calories = 380,
        proteinG = 12.5,
        carbsG = 68.0,
        fatG = 6.0,
        fiberG = 9.4
    ),
    FoodItem(
        id = "3",
        name = "Zeytinyağlı Akdeniz Salata",
        formattedPortion = "1 Porsiyon",
        calories = 190,
        proteinG = 4.2,
        carbsG = 11.0,
        fatG = 14.5,
        fiberG = 4.8
    )
)

private val sampleNutrientBars = listOf(
    NutrientProgress(key = "protein", label = "Protein", current = 78.7, target = 140.0, unit = "g", colorHex = "#0df2a6"),
    NutrientProgress(key = "carbs", label = "Karbonhidrat", current = 79.0, target = 220.0, unit = "g", colorHex = "#06b6d4"),
    NutrientProgress(key = "fat", label = "Yağ", current = 27.7, target = 65.0, unit = "g", colorHex = "#f59e0b"),
    NutrientProgress(key = "fiber", label = "Lif", current = 14.2, target = 30.0, unit = "g", colorHex = "#10b981"),
    NutrientProgress(key = "magnesium", label = "Magnezyum", current = 280.0, target = 400.0, unit = "mg", colorHex = "#8b5cf6"),
    NutrientProgress(key = "iron", label = "Demir", current = 11.5, target = 18.0, unit = "mg", colorHex = "#ec4899")
)

private val populatedFoodState = FoodState(
    selectedDate = "2026-11-06",
    rawText = "",
    items = sampleFoodItems,
    totalCalories = 900,
    summary = "Günün ilk 3 öğünü kaydedildi. Protein alımı hedefin %56'sına ulaştı.",
    nutritionGoal = NutritionGoal(
        id = "goal_1",
        userId = 1,
        calories = 2100,
        proteinG = 140.0,
        carbsG = 220.0,
        fatG = 65.0,
        fiberG = 30.0
    ),
    fitnessGoal = FitnessGoal.BUILD_MUSCLE,
    selectedDateWeightKg = 73.4,
    isWeightExactForDate = true,
    effectiveProteinTarget = 140.0,
    nutrientBars = sampleNutrientBars,
    latestWeightKg = 73.4,
    weightDifferenceKg = -0.3,
    recentWeights = listOf(74.0, 73.8, 73.7, 73.5, 73.4),
    frequentFoods = sampleFoodItems,
    waterEntry = WaterEntry(date = "2026-11-06", userId = 1, amountMl = 1750, targetMl = 2500),
    dynamicWaterTargetMl = 2500,
    synergyTips = listOf(
        NutrientSynergyTip(
            id = "tip_sample",
            title = "🌱 Demir Emilimini 3 Kat Artırın",
            description = "Öğününüzdeki bitkisel demiri en iyi şekilde emmek için limon ekleyin.",
            highlightNutrient = "Demir + C Vitamini"
        )
    )
)

@Preview
@Composable
fun FoodScreenDarkPreview() {
    HealthTrackTheme(darkTheme = true) {
        FoodScreen(
            state = populatedFoodState,
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun FoodScreenLightPreview() {
    HealthTrackTheme(darkTheme = false) {
        FoodScreen(
            state = populatedFoodState,
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun FoodScreenEmptyPreview() {
    HealthTrackTheme(darkTheme = true) {
        FoodScreen(
            state = FoodState(selectedDate = "2026-11-06"),
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun FoodTableCardPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            FoodTableCard(
                items = sampleFoodItems,
                totalCalories = 900,
                summary = "3 öğün başarıyla listelendi.",
                onRemoveItem = {},
                onClearAll = {}
            )
        }
    }
}

@Preview
@Composable
fun FoodBentoRowPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            FoodBentoRow(
                nutrientBars = sampleNutrientBars,
                latestWeightKg = 73.4,
                weightDifferenceKg = -0.3,
                recentWeights = listOf(74.0, 73.8, 73.7, 73.5, 73.4),
                selectedDate = "2026-11-06",
                isWeightExactForDate = true
            )
        }
    }
}

@Preview
@Composable
fun NutrientBarsViewPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            NutrientBarsView(
                bars = sampleNutrientBars,
                onEditGoalsClick = {}
            )
        }
    }
}

@Preview
@Composable
fun QuickFoodPresetsViewPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            QuickFoodPresetsView(
                onAddFood = { _, _, _, _, _, _, _ -> }
            )
        }
    }
}

@Preview
@Composable
fun FoodHydrationAndSmartMealRowPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            FoodHydrationAndSmartMealRow(
                waterEntry = WaterEntry(date = "2026-11-06", userId = 1, amountMl = 1500, targetMl = 2500),
                dynamicWaterTargetMl = 2500,
                nutrientBars = sampleNutrientBars,
                remainingCalories = 1200,
                onAddWater = {},
                onResetWater = {},
                onOpenSmartMeal = {},
                synergyTips = listOf(
                    NutrientSynergyTip(
                        id = "tip_sample",
                        title = "🌱 Demir Emilimini 3 Kat Artırın",
                        description = "Öğününüzdeki bitkisel demiri en iyi şekilde emmek için limon ekleyin.",
                        highlightNutrient = "Demir + C Vitamini"
                    )
                ),
                onDismissSynergyTip = {}
            )
        }
    }
}

@Preview
@Composable
fun AddCustomFoodDialogPreviewDark() {
    HealthTrackTheme(darkTheme = true) {
        AddCustomFoodDialog(
            onDismiss = {},
            onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
    }
}

@Preview
@Composable
fun AddCustomFoodDialogPreviewLight() {
    HealthTrackTheme(darkTheme = false) {
        AddCustomFoodDialog(
            onDismiss = {},
            onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
    }
}

private val previewCustomFoods = listOf(
    CustomFoodItem(
        id = "cf_1",
        name = "Proteinli Yulaf Lapası",
        brand = "FitKahvaltı",
        portionUnit = "1 Kase",
        calories = 360,
        proteinG = 28.0,
        carbsG = 45.0,
        fatG = 6.0,
        ingredients = listOf(
            CustomFoodIngredient(
                id = "ing_1",
                name = "Yulaf Ezmesi",
                unitLabel = "g",
                defaultAmount = 50.0,
                stepAmount = 10.0,
                presetLevels = listOf(30.0, 50.0, 70.0, 100.0),
                baseCalories = 185,
                baseProteinG = 6.5,
                baseCarbsG = 33.0,
                baseFatG = 3.5
            ),
            CustomFoodIngredient(
                id = "ing_2",
                name = "Yarım Yağlı Süt",
                unitLabel = "ml",
                defaultAmount = 150.0,
                stepAmount = 50.0,
                presetLevels = listOf(100.0, 150.0, 200.0, 250.0),
                baseCalories = 75,
                baseProteinG = 5.0,
                baseCarbsG = 7.2,
                baseFatG = 2.9
            ),
            CustomFoodIngredient(
                id = "ing_3",
                name = "Whey Protein Tozu",
                unitLabel = "Ölçek",
                defaultAmount = 1.0,
                stepAmount = 0.5,
                presetLevels = listOf(0.5, 1.0, 1.5, 2.0),
                baseCalories = 100,
                baseProteinG = 20.0,
                baseCarbsG = 1.5,
                baseFatG = 1.0
            )
        )
    ),
    CustomFoodItem(
        id = "cf_2",
        name = "Filtre Kahve",
        brand = "Starbucks",
        portionUnit = "250 ml",
        calories = 4,
        proteinG = 0.3,
        carbsG = 0.0,
        fatG = 0.1
    ),
    CustomFoodItem(
        id = "cf_3",
        name = "Tavuklu Kinoa Salatası",
        brand = "Ev Yapımı",
        portionUnit = "1 Porsiyon",
        calories = 420,
        proteinG = 42.0,
        carbsG = 35.0,
        fatG = 11.0
    ),
    CustomFoodItem(
        id = "cf_4",
        name = "Fıstık Ezmesi",
        brand = "Züber",
        portionUnit = "30 g",
        calories = 175,
        proteinG = 8.0,
        carbsG = 5.0,
        fatG = 14.0
    )
)

@Preview
@Composable
fun InventoryMixStudioDialogPreviewDark() {
    HealthTrackTheme(darkTheme = true) {
        InventoryMixStudioDialog(
            customFoods = previewCustomFoods,
            initialSelectedItem = null,
            onDismiss = {},
            onOpenCreateCustomFood = {},
            onAddItemsToMeal = { _, _ -> }
        )
    }
}

@Preview
@Composable
fun InventoryMixStudioDialogPreviewLight() {
    HealthTrackTheme(darkTheme = false) {
        InventoryMixStudioDialog(
            customFoods = previewCustomFoods,
            initialSelectedItem = null,
            onDismiss = {},
            onOpenCreateCustomFood = {},
            onAddItemsToMeal = { _, _ -> }
        )
    }
}

@Preview
@Composable
fun InventoryMixStudioDialogDetailPreview() {
    HealthTrackTheme(darkTheme = true) {
        InventoryMixStudioDialog(
            customFoods = previewCustomFoods,
            initialSelectedItem = previewCustomFoods.first(),
            onDismiss = {},
            onOpenCreateCustomFood = {},
            onAddItemsToMeal = { _, _ -> }
        )
    }
}

@Preview
@Composable
fun FoodIconSelectorPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            FoodIconSelector(
                selectedKey = "creatine",
                onSelectKey = {}
            )
        }
    }
}

@Preview
@Composable
fun FoodPhotoScanBottomSheetPreviewDark() {
    HealthTrackTheme(darkTheme = true) {
        FoodPhotoScanBottomSheet(
            isAnalyzing = false,
            initialTarget = FoodScanTarget.INVENTORY,
            onAnalyze = { _, _, _ -> },
            onDismiss = {}
        )
    }
}

@Preview
@Composable
fun FoodPhotoScanBottomSheetPreviewLight() {
    HealthTrackTheme(darkTheme = false) {
        FoodPhotoScanBottomSheet(
            isAnalyzing = false,
            initialTarget = FoodScanTarget.DAILY_MEAL,
            onAnalyze = { _, _, _ -> },
            onDismiss = {}
        )
    }
}

@Preview
@Composable
fun FoodAiReviewBottomSheetPreviewDark() {
    HealthTrackTheme(darkTheme = true) {
        FoodAiReviewBottomSheet(
            aiResult = FoodAiResultDto(
                summary = "1 kutu Pınar Protein Süt ve 1 paket Fellas Bar tespit edildi.",
                totalCalories = 450,
                items = sampleFoodItems
            ),
            target = FoodScanTarget.INVENTORY,
            onDismiss = {},
            onConfirm = {},
            onSaveToInventory = {}
        )
    }
}

@Preview
@Composable
fun FoodAiReviewBottomSheetPreviewLight() {
    HealthTrackTheme(darkTheme = false) {
        FoodAiReviewBottomSheet(
            aiResult = FoodAiResultDto(
                summary = "1 kutu Pınar Protein Süt ve 1 paket Fellas Bar tespit edildi.",
                totalCalories = 450,
                items = sampleFoodItems
            ),
            target = FoodScanTarget.DAILY_MEAL,
            onDismiss = {},
            onConfirm = {},
            onSaveToInventory = {}
        )
    }
}
