package com.yusufteker.healthtrack.feature.food.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yusufteker.healthtrack.feature.food.presentation.components.AddCustomFoodDialog
import com.yusufteker.healthtrack.feature.food.presentation.components.EditGoalsDialog
import io.github.aakira.napier.Napier
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodAiReviewBottomSheet
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodBentoRow
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodTableCard
import com.yusufteker.healthtrack.feature.food.presentation.components.GeneralMicronutrientsBottomSheet
import com.yusufteker.healthtrack.feature.food.presentation.components.NutrientBarsView
import com.yusufteker.healthtrack.feature.food.presentation.components.QuickFoodPresetsView
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.ui.components.CalorieProgressGauge
import com.yusufteker.healthtrack.ui.components.DaysStripSelector
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.GradientText
import com.yusufteker.healthtrack.ui.components.HealthToastHost
import com.yusufteker.healthtrack.ui.components.ToastType
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

@Composable
fun FoodScreen(
    state: FoodState,
    onIntent: (FoodIntent) -> Unit,
    onNavigateToWeight: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showManualAddSheet by remember { mutableStateOf(false) }
    var showGeneralMicrosSheet by remember { mutableStateOf(false) }

    LaunchedEffect(showManualAddSheet) {
        if (showManualAddSheet) Napier.d("SCREEN: ManualFoodEntryDialog açıldı")
    }
    LaunchedEffect(showGeneralMicrosSheet) {
        if (showGeneralMicrosSheet) Napier.d("SCREEN: GeneralMicronutrientsBottomSheet açıldı")
    }
    LaunchedEffect(state.isAiReviewSheetOpen) {
        if (state.isAiReviewSheetOpen) Napier.d("SCREEN: FoodAiReviewBottomSheet açıldı")
    }
    LaunchedEffect(state.isEditGoalsDialogOpen) {
        if (state.isEditGoalsDialogOpen) Napier.d("SCREEN: EditGoalsDialog açıldı")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Stitch Horizontal Days Strip with Week Badges ──
            DaysStripSelector(
                selectedDate = state.selectedDate,
                onDateSelected = { newDate ->
                    onIntent(FoodIntent.OnDateChanged(newDate))
                }
            )

            val proteinProgress = state.nutrientBars.firstOrNull { it.key == "protein" }
            val carbsProgress = state.nutrientBars.firstOrNull { it.key == "carbs" }
            val fatProgress = state.nutrientBars.firstOrNull { it.key == "fat" }

            // ── Stitch Concentric Calorie Telemetry Hero Gauge with Centered Ring ──
            CalorieProgressGauge(
                consumedCalories = state.totalCalories,
                targetCalories = state.nutritionGoal.calories,
                burnedCalories = 0,
                proteinCurrent = proteinProgress?.current ?: 0.0,
                proteinTarget = state.effectiveProteinTarget,
                carbsCurrent = carbsProgress?.current ?: 0.0,
                carbsTarget = state.nutritionGoal.carbsG,
                fatCurrent = fatProgress?.current ?: 0.0,
                fatTarget = state.nutritionGoal.fatG
            )

            // ── Bento Split Row: [ Micros (Top 3 Vital) ] + [ Weight Scale Sparkline ] ──
            FoodBentoRow(
                nutrientBars = state.nutrientBars,
                latestWeightKg = state.selectedDateWeightKg ?: state.latestWeightKg,
                weightDifferenceKg = state.weightDifferenceKg,
                recentWeights = state.recentWeights,
                selectedDate = state.selectedDate,
                isWeightExactForDate = state.isWeightExactForDate,
                weightRecordDate = state.weightRecordDate,
                onMicrosClick = { showGeneralMicrosSheet = true },
                onWeightClick = onNavigateToWeight
            )

        // ── AI Nutrition Telemetry Logger Card ──
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(HealthColors.Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "AI Besin Analizi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(HealthColors.Primary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Gemini AI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = HealthColors.Primary
                        )
                    }
                }

                // Frosted Text Input Well
                OutlinedTextField(
                    value = state.rawText,
                    onValueChange = { onIntent(FoodIntent.OnRawTextChanged(it)) },
                    placeholder = {
                        Text(
                            "Bugün ne yediniz? Örn: sabah 50ml süt, 2 haşlanmış yumurta, 1 dilim siyez ekmeği...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HealthColors.Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    ),
                    maxLines = 4
                )

                // Quick Suggestion Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickChip(label = "+ 2 Yumurta") {
                        val current = if (state.rawText.isNotBlank()) "${state.rawText}, 2 yumurta" else "2 yumurta"
                        onIntent(FoodIntent.OnRawTextChanged(current))
                    }
                    QuickChip(label = "+ Yulaf & Süt") {
                        val current = if (state.rawText.isNotBlank()) "${state.rawText}, 1 kase yulaf ve süt" else "1 kase yulaf ve süt"
                        onIntent(FoodIntent.OnRawTextChanged(current))
                    }
                    QuickChip(label = "+ Izgara Tavuk") {
                        val current = if (state.rawText.isNotBlank()) "${state.rawText}, 150g ızgara tavuk" else "150g ızgara tavuk"
                        onIntent(FoodIntent.OnRawTextChanged(current))
                    }
                    QuickChip(label = "+ Filtre Kahve") {
                        val current = if (state.rawText.isNotBlank()) "${state.rawText}, 1 fincan filtre kahve" else "1 fincan filtre kahve"
                        onIntent(FoodIntent.OnRawTextChanged(current))
                    }
                }

                // Calculate with AI Button (Luminous Primary CTA)
                Button(
                    onClick = { onIntent(FoodIntent.OnCalculateAiClicked) },
                    enabled = !state.isCalculatingAi && state.rawText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f),
                        disabledContentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    if (state.isCalculatingAi) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Gemini Hesaplanıyor...",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI ile Hesapla",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }

                // Manual Save Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onIntent(FoodIntent.OnSaveManualClicked) },
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Ham Metni Kaydet",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ── Daily Fuel Manifest Table ──
        FoodTableCard(
            items = state.items,
            totalCalories = state.totalCalories,
            summary = state.summary,
            onRemoveItem = { onIntent(FoodIntent.OnRemoveItem(it)) },
            onClearAll = { onIntent(FoodIntent.OnClearFoodLog) }
        )

        // Manual Item Add Button
        OutlinedButton(
            onClick = { showManualAddSheet = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .bounceClick(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = HealthColors.Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Yiyecek Ekle (Sık Tüketilenler & Arama)", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ── Floating Toast Notifications (Auto-dismissing) ──
    HealthToastHost(
        message = state.errorMessage,
        type = ToastType.ERROR,
        onDismiss = { onIntent(FoodIntent.OnDismissErrorMessage) }
    )
    HealthToastHost(
        message = state.infoMessage,
        type = ToastType.SUCCESS,
        onDismiss = { onIntent(FoodIntent.OnDismissInfoMessage) }
    )

    // ── AI Food Review Bottom Sheet ──
    if (state.isAiReviewSheetOpen && state.pendingAiResult != null) {
        FoodAiReviewBottomSheet(
            aiResult = state.pendingAiResult,
            onDismiss = { onIntent(FoodIntent.OnDismissAiReview) },
            onConfirm = { onIntent(FoodIntent.OnConfirmAiReview) }
        )
    }

    // ── Edit Goals Dialog ──
    if (state.isEditGoalsDialogOpen) {
        EditGoalsDialog(
            currentGoal = state.nutritionGoal,
            currentWeight = state.selectedDateWeightKg ?: state.latestWeightKg ?: 70.0,
            fitnessGoal = state.fitnessGoal,
            onDismiss = { onIntent(FoodIntent.OnCloseEditGoalsDialog) },
            onSave = { updatedGoal, updatedFitnessGoal ->
                onIntent(FoodIntent.OnSaveGoals(updatedGoal, updatedFitnessGoal))
            }
        )
    }

    // ── General Micronutrients Bottom Sheet ──
    if (showGeneralMicrosSheet) {
        GeneralMicronutrientsBottomSheet(
            nutrientBars = state.nutrientBars,
            onDismiss = { showGeneralMicrosSheet = false },
            onEditGoalsClick = { onIntent(FoodIntent.OnOpenEditGoalsDialog) }
        )
    }

    // ── Manual Add Item Bottom Sheet ──
    if (showManualAddSheet) {
        ManualAddBottomSheet(
            customFoods = state.customFoods,
            favoriteFoodIds = state.favoriteFoodIds,
            onToggleFavorite = { onIntent(FoodIntent.OnToggleFavoriteFood(it)) },
            onOpenAddCustomFood = { onIntent(FoodIntent.OnOpenAddCustomFoodDialog) },
            onDeleteCustomFood = { onIntent(FoodIntent.OnDeleteCustomFood(it)) },
            onDismiss = { showManualAddSheet = false },
            onAdd = { name, portion, cal, p, c, f, fib ->
                onIntent(FoodIntent.OnAddItem(name, portion, cal, p, c, f, fib))
                showManualAddSheet = false
            }
        )
    }

    // ── Add Custom / Brand Food Dialog ──
    if (state.isAddCustomFoodDialogOpen) {
        AddCustomFoodDialog(
            onDismiss = { onIntent(FoodIntent.OnCloseAddCustomFoodDialog) },
            onSave = { name, brand, portionUnit, cal, prot, carb, fat, fib, cat, makeFav ->
                onIntent(
                    FoodIntent.OnSaveCustomFood(
                        name = name,
                        brand = brand,
                        portionUnit = portionUnit,
                        calories = cal,
                        proteinG = prot,
                        carbsG = carb,
                        fatG = fat,
                        fiberG = fib,
                        categoryName = cat,
                        makeFavorite = makeFav
                    )
                )
            }
        )
    }
}
}

@Composable
private fun QuickChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .bounceClick()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualAddBottomSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, portion: String, calories: Int, protein: Double, carbs: Double, fat: Double, fiber: Double) -> Unit,
    customFoods: List<com.yusufteker.healthtrack.core.model.CustomFoodItem> = emptyList(),
    favoriteFoodIds: Set<String> = emptySet(),
    onToggleFavorite: ((String) -> Unit)? = null,
    onOpenAddCustomFood: (() -> Unit)? = null,
    onDeleteCustomFood: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val isDark = LocalIsDarkTheme.current
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var portion by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .clearFocusOnTap()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(HealthColors.Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = HealthColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Yiyecek Ekle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sık tüketilenlerden seçin veya elle girin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── 1. Sık Tüketilenler (Presets) Carousel Inside Bottom Sheet ──
            QuickFoodPresetsView(
                customFoods = customFoods,
                favoriteFoodIds = favoriteFoodIds,
                onToggleFavorite = onToggleFavorite,
                onOpenAddCustomFood = onOpenAddCustomFood,
                onDeleteCustomFood = onDeleteCustomFood,
                onAddFood = { pName, pPortion, pCal, pProt, pCarb, pFat, pFib ->
                    onAdd(pName, pPortion, pCal, pProt, pCarb, pFat, pFib)
                },
                onSelectFood = { preset, qty ->
                    name = preset.nameTr
                    val totalAmount = preset.unitStep * qty
                    portion = if (preset.unitStep == 1) "$qty ${preset.unitLabelTr}" else "$totalAmount ${preset.unitLabelTr}"
                    calories = (preset.baseCalories * qty).toString()
                    proteinText = if (preset.baseProteinG > 0) ((preset.baseProteinG * qty * 10).toInt() / 10.0).toString() else ""
                    carbsText = if (preset.baseCarbsG > 0) ((preset.baseCarbsG * qty * 10).toInt() / 10.0).toString() else ""
                    fatText = if (preset.baseFatG > 0) ((preset.baseFatG * qty * 10).toInt() / 10.0).toString() else ""
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "Özel Yiyecek Gir (Manuel)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 1. Food Name Input
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Yiyecek Adı *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Örn: Izgara Somon, Yulaf Kasesi...") },
                    leadingIcon = {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = HealthColors.Primary, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // 2. Portion Input & Quick Chips
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Porsiyon / Miktar",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = portion,
                    onValueChange = { portion = it },
                    placeholder = { Text("Örn: 1 Porsiyon / 150g / 1 Kase") },
                    leadingIcon = {
                        Icon(Icons.Default.Straighten, contentDescription = null, tint = HealthColors.Secondary, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Quick portion suggestion chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("1 Porsiyon", "100g", "1 Adet", "1 Kase", "1 Dilim", "200ml").forEach { chip ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (portion == chip) HealthColors.Primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable { portion = chip }
                                .bounceClick()
                        ) {
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (portion == chip) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 3. Calorie Input
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Kalori (kcal) *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    placeholder = { Text("Örn: 250") },
                    leadingIcon = {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = HealthColors.Tertiary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        Text("kcal", style = MaterialTheme.typography.labelMedium, color = HealthColors.Tertiary, modifier = Modifier.padding(end = 12.dp))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // 4. Optional Macros (Protein, Carbs, Fat)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Makro Değerleri (İsteğe Bağlı)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = it },
                        placeholder = { Text("0") },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it },
                        placeholder = { Text("0") },
                        label = { Text("Karb (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        placeholder = { Text("0") },
                        label = { Text("Yağ (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp).bounceClick(),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text("İptal")
                }

                Button(
                    onClick = {
                        val calInt = calories.toIntOrNull() ?: 0
                        if (name.isNotBlank()) {
                            val prot = proteinText.toDoubleOrNull() ?: 0.0
                            val carb = carbsText.toDoubleOrNull() ?: 0.0
                            val fat = fatText.toDoubleOrNull() ?: 0.0
                            onAdd(name, portion.ifBlank { "1 Porsiyon" }, calInt, prot, carb, fat, 0.0)
                        }
                    },
                    enabled = name.isNotBlank() && calories.isNotBlank(),
                    modifier = Modifier.weight(1.5f).height(50.dp).bounceClick(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HealthColors.Primary,
                        contentColor = HealthColors.OnPrimary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Öğünü Ekle", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
