package com.yusufteker.healthtrack.feature.food.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yusufteker.healthtrack.core.model.CustomFoodItem
import com.yusufteker.healthtrack.core.model.Micronutrient
import com.yusufteker.healthtrack.core.utils.rememberSpeechToTextLauncher
import com.yusufteker.healthtrack.core.utils.rememberExternalAppLauncher
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodHydrationAndSmartMealRow
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodPhotoScanBottomSheet
import com.yusufteker.healthtrack.feature.food.presentation.components.GroceryListBottomSheet
import com.yusufteker.healthtrack.feature.food.presentation.components.SmartMealMatcherBottomSheet
import com.yusufteker.healthtrack.core.utils.shiftDateIso
import com.yusufteker.healthtrack.feature.food.presentation.components.AddCustomFoodDialog
import com.yusufteker.healthtrack.feature.food.presentation.components.EditGoalsDialog
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodAiReviewBottomSheet
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodBentoRow
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodTableCard
import com.yusufteker.healthtrack.feature.food.presentation.components.GeneralMicronutrientsBottomSheet
import com.yusufteker.healthtrack.feature.food.presentation.components.InventoryMixStudioDialog
import com.yusufteker.healthtrack.feature.food.presentation.components.QuickFoodPresetsView
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalBottomBarVisibility
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.ui.components.CalorieProgressGauge
import com.yusufteker.healthtrack.ui.components.DaysStripSelector
import com.yusufteker.healthtrack.ui.components.ForkSpoonPlusIcon
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.HealthToastHost
import com.yusufteker.healthtrack.ui.components.ToastType
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import com.yusufteker.healthtrack.ui.components.onHorizontalSwipe
import io.github.aakira.napier.Napier

@Composable
fun FoodScreen(
    state: FoodState,
    onIntent: (FoodIntent) -> Unit,
    onNavigateToWeight: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val speechLauncher = rememberSpeechToTextLauncher()
    val appLauncher = rememberExternalAppLauncher()
    var showManualAddSheet by remember { mutableStateOf(false) }
    var showGeneralMicrosSheet by remember { mutableStateOf(false) }
    var showInventoryStudio by remember { mutableStateOf(false) }
    var initialStudioItem by remember { mutableStateOf<CustomFoodItem?>(null) }

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

    val bottomBarVisibility = LocalBottomBarVisibility.current
    val isAnySheetOpen = state.isSmartMealSheetOpen ||
        state.isPhotoScanSheetOpen ||
        state.isGroceryListSheetOpen ||
        state.isAiReviewSheetOpen ||
        showManualAddSheet ||
        showGeneralMicrosSheet

    LaunchedEffect(isAnySheetOpen) {
        bottomBarVisibility.value = !isAnySheetOpen
    }

    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .onHorizontalSwipe(
                    onSwipeLeft = { onIntent(FoodIntent.OnDateChanged(shiftDateIso(state.selectedDate, 1))) },
                    onSwipeRight = { onIntent(FoodIntent.OnDateChanged(shiftDateIso(state.selectedDate, -1))) }
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Stitch Horizontal Days Strip with Week Badges ──
            DaysStripSelector(
                selectedDate = state.selectedDate, onDateSelected = { newDate ->
                    onIntent(FoodIntent.OnDateChanged(newDate))
                })

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

            // ── Hidrasyon (2.A) & "Ne Yesem?" Akıllı Öğün (1) & Besin Sinerjisi (2.B) ──
            FoodHydrationAndSmartMealRow(
                waterEntry = state.waterEntry,
                dynamicWaterTargetMl = state.dynamicWaterTargetMl,
                nutrientBars = state.nutrientBars,
                remainingCalories = (state.nutritionGoal.calories - state.totalCalories).coerceAtLeast(0),
                onAddWater = { onIntent(FoodIntent.OnAddWater(it)) },
                onResetWater = { onIntent(FoodIntent.OnResetWater) },
                onOpenSmartMeal = { onIntent(FoodIntent.OnOpenSmartMealSheet) },
                synergyTips = state.synergyTips,
                onDismissSynergyTip = { onIntent(FoodIntent.OnDismissSynergyTip(it)) }
            )

            // ── Unified AI & Quick Food Logger Card ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                modifier = Modifier.size(30.dp).clip(CircleShape)
                                    .background(HealthColors.Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = HealthColors.Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Öğün Ekle (AI)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = HealthColors.Secondary.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable {
                                        initialStudioItem = null
                                        showInventoryStudio = true
                                    }
                                    .bounceClick()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = HealthColors.Secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Envanterim",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HealthColors.Secondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = HealthColors.Primary.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { showManualAddSheet = true }
                                    .bounceClick()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestaurantMenu,
                                        contentDescription = null,
                                        tint = HealthColors.Primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Hazır / Manuel",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HealthColors.Primary
                                    )
                                }
                            }
                        }
                    }

                    // Compact Expandable Text Input Well
                    OutlinedTextField(
                        value = state.rawText,
                        onValueChange = { onIntent(FoodIntent.OnRawTextChanged(it)) },
                        placeholder = {
                            Text(
                                "Ne yediniz? Örn: 2 yumurta, 100ml süt...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                if (state.rawText.isNotBlank()) {
                                    IconButton(
                                        onClick = { onIntent(FoodIntent.OnRawTextChanged("")) },
                                        modifier = Modifier.size(28.dp).bounceClick()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Temizle",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.6f
                                            ),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onIntent(FoodIntent.OnOpenPhotoScanSheet) },
                                    modifier = Modifier.size(34.dp).clip(CircleShape)
                                        .background(HealthColors.Primary.copy(alpha = 0.15f))
                                        .bounceClick()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Fotoğraf / Etiket Tara",
                                        tint = HealthColors.Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (speechLauncher.isAvailable) {
                                    IconButton(
                                        onClick = {
                                            speechLauncher.launch("Ne yediniz? Sesinizle söyleyin...") { spokenText ->
                                                val current = state.rawText.trim()
                                                val updated =
                                                    if (current.isBlank()) spokenText else "$current, $spokenText"
                                                onIntent(FoodIntent.OnRawTextChanged(updated))
                                            }
                                        },
                                        modifier = Modifier.size(34.dp).clip(CircleShape)
                                            .background(HealthColors.Primary.copy(alpha = 0.15f))
                                            .bounceClick()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Sesle Söyle",
                                            tint = HealthColors.Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HealthColors.Primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.22f
                            ),
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.36f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f)
                        ),
                        minLines = 1,
                        maxLines = 3
                    )

                    // Quick Suggestion Chips (No duplicate mic chip)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickChip(label = "+ 2 Yumurta") {
                            val current =
                                if (state.rawText.isNotBlank()) "${state.rawText}, 2 yumurta" else "2 yumurta"
                            onIntent(FoodIntent.OnRawTextChanged(current))
                        }
                        QuickChip(label = "+ Yulaf & Süt") {
                            val current =
                                if (state.rawText.isNotBlank()) "${state.rawText}, 1 kase yulaf ve süt" else "1 kase yulaf ve süt"
                            onIntent(FoodIntent.OnRawTextChanged(current))
                        }
                        QuickChip(label = "+ Izgara Tavuk") {
                            val current =
                                if (state.rawText.isNotBlank()) "${state.rawText}, 150g ızgara tavuk" else "150g ızgara tavuk"
                            onIntent(FoodIntent.OnRawTextChanged(current))
                        }
                        QuickChip(label = "+ Filtre Kahve") {
                            val current =
                                if (state.rawText.isNotBlank()) "${state.rawText}, 1 fincan filtre kahve" else "1 fincan filtre kahve"
                            onIntent(FoodIntent.OnRawTextChanged(current))
                        }
                    }

                    // Action Row: Calculate with AI + Optional Raw Save
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onIntent(FoodIntent.OnCalculateAiClicked) },
                            enabled = !state.isCalculatingAi && state.rawText.isNotBlank(),
                            modifier = Modifier.weight(1f).height(44.dp).bounceClick(),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                disabledContentColor = Color.White
                            )
                        ) {
                            if (state.isCalculatingAi) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Hesaplanıyor...",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "AI ile Hesapla",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }

                        if (state.rawText.isNotBlank()) {
                            OutlinedButton(
                                onClick = { onIntent(FoodIntent.OnSaveManualClicked) },
                                modifier = Modifier.height(44.dp).bounceClick(),
                                shape = RoundedCornerShape(999.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Ham Kaydet",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
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
                onClearAll = { onIntent(FoodIntent.OnClearFoodLog) },
                onAddClick = { showManualAddSheet = true })

            Spacer(modifier = Modifier.height(76.dp))
        }

        // ── Floating Action Button: Clean '+' FAB ──
        FloatingActionButton(
            onClick = { showManualAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .size(56.dp)
                .bounceClick(),
            containerColor = HealthColors.Primary,
            contentColor = HealthColors.OnPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Ekle",
                tint = HealthColors.OnPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        // ── Floating Toast Notifications (Auto-dismissing) ──
        HealthToastHost(
            message = state.errorMessage,
            type = ToastType.ERROR,
            onDismiss = { onIntent(FoodIntent.OnDismissErrorMessage) })
        HealthToastHost(
            message = state.infoMessage,
            type = ToastType.SUCCESS,
            onDismiss = { onIntent(FoodIntent.OnDismissInfoMessage) })

        // ── AI Analysis Blocking Loading Dialog ──
        if (state.isCalculatingAi) {
            Dialog(
                onDismissRequest = { /* Block clicks outside & back button while analyzing */ },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(0.85f).wrapContentHeight(),
                        shape = RoundedCornerShape(24.dp),
                        elevation = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(68.dp).clip(CircleShape)
                                    .background(HealthColors.Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = HealthColors.Primary,
                                    modifier = Modifier.size(54.dp),
                                    strokeWidth = 3.5.dp
                                )
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = HealthColors.Primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Yapay Zeka Analiz Ediyor...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Girdiğiniz besinlerin kalori ve makro değerleri hesaplanıyor. Lütfen bekleyin...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── AI Food Review Bottom Sheet ──
        if (state.isAiReviewSheetOpen && state.pendingAiResult != null) {
            FoodAiReviewBottomSheet(
                aiResult = state.pendingAiResult,
                onDismiss = { onIntent(FoodIntent.OnDismissAiReview) },
                onConfirm = { onIntent(FoodIntent.OnConfirmAiReview) })
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
                })
        }

        // ── General Micronutrients Bottom Sheet ──
        if (showGeneralMicrosSheet) {
            GeneralMicronutrientsBottomSheet(
                nutrientBars = state.nutrientBars,
                onDismiss = { showGeneralMicrosSheet = false },
                onEditGoalsClick = { onIntent(FoodIntent.OnOpenEditGoalsDialog) })
        }

        // ── Manual Add Item Bottom Sheet ──
        if (showManualAddSheet) {
            ManualAddBottomSheet(
                customFoods = state.customFoods,
                favoriteFoodIds = state.favoriteFoodIds,
                frequentFoods = state.frequentFoods,
                onToggleFavorite = { onIntent(FoodIntent.OnToggleFavoriteFood(it)) },
                onOpenAddCustomFood = { onIntent(FoodIntent.OnOpenAddCustomFoodDialog) },
                onDeleteCustomFood = { onIntent(FoodIntent.OnDeleteCustomFood(it)) },
                onOpenInventoryStudio = { customItem ->
                    initialStudioItem = customItem
                    showManualAddSheet = false
                    showInventoryStudio = true
                },
                onDismiss = { showManualAddSheet = false },
                onAdd = { name, portion, cal, p, c, f, fib ->
                    onIntent(FoodIntent.OnAddItem(name, portion, cal, p, c, f, fib))
                    showManualAddSheet = false
                },
                onAddWithDetails = { name, portion, cal, p, c, f, fib, micros, iconKey ->
                    onIntent(
                        FoodIntent.OnAddItem(
                            name, portion, cal, p, c, f, fib, micros, iconKey
                        )
                    )
                    showManualAddSheet = false
                })
        }

        // ── Full-Screen Inventory & Mix Level Studio Dialog ──
        if (showInventoryStudio) {
            InventoryMixStudioDialog(
                customFoods = state.customFoods,
                initialSelectedItem = initialStudioItem,
                onDismiss = { showInventoryStudio = false },
                onOpenCreateCustomFood = {
                    showInventoryStudio = false
                    onIntent(FoodIntent.OnOpenAddCustomFoodDialog)
                },
                onDeleteCustomFood = { id ->
                    onIntent(FoodIntent.OnDeleteCustomFood(id))
                },
                onAddItemsToMeal = { items, summaryNote ->
                    onIntent(FoodIntent.OnAddMultipleItems(items, summaryNote))
                    showInventoryStudio = false
                }
            )
        }

        // ── Add Custom / Brand Food Dialog ──
        if (state.isAddCustomFoodDialogOpen) {
            AddCustomFoodDialog(
                existingCustomFoods = state.customFoods,
                isAnalyzingAi = state.isAnalyzingCustomFoodAi,
                aiResult = state.customFoodAiResult,
                aiPromptUsed = state.customFoodAiPromptUsed,
                aiError = state.customFoodAiError,
                onAnalyzeWithAi = { prompt, isMixMode ->
                    onIntent(FoodIntent.OnAnalyzeCustomFoodAi(prompt, isMixMode))
                },
                onConsumeAiResult = {
                    onIntent(FoodIntent.OnConsumeCustomFoodAiResult)
                },
                onDismissAiError = {
                    onIntent(FoodIntent.OnDismissCustomFoodAiError)
                },
                onDismiss = { onIntent(FoodIntent.OnCloseAddCustomFoodDialog) },
                onSave = { name, brand, portionUnit, cal, prot, carb, fat, fib, cat, makeFav, iconKey, micros, ingredients ->
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
                            makeFavorite = makeFav,
                            iconKey = iconKey,
                            micronutrients = micros,
                            ingredients = ingredients
                        )
                    )
                })
        }

        // ── "Ne Yesem?" Smart Meal Matcher Bottom Sheet (1) ──
        if (state.isSmartMealSheetOpen) {
            SmartMealMatcherBottomSheet(
                suggestions = state.smartMealSuggestions,
                nutrientBars = state.nutrientBars,
                remainingCalories = (state.nutritionGoal.calories - state.totalCalories).coerceAtLeast(0),
                latestAiRecipe = state.latestAiRecipe,
                isFromUserDb = state.isSmartMealFromUserDb,
                isOnlyQuick = state.isSmartMealOnlyQuick,
                isOnlyFewIngredients = state.isSmartMealOnlyFewIngredients,
                isExcludeRecent3Days = state.isSmartMealExcludeRecent3Days,
                isHighProtein = state.isSmartMealHighProtein,
                isGeneratingAi = state.isGeneratingAiCustomRecipe,
                onToggleFilter = { onIntent(FoodIntent.OnToggleSmartMealFilter(it)) },
                onAddMeal = { onIntent(FoodIntent.OnAddSuggestedMeal(it)) },
                onOpenGroceryForMeal = { title, ingredients ->
                    onIntent(FoodIntent.OnOpenGroceryListSheet(title, ingredients))
                },
                onRequestAiRecipe = { onIntent(FoodIntent.OnRequestCustomAiMealSuggestion) },
                onDismissAiRecipe = { onIntent(FoodIntent.OnDismissAiRecipe) },
                onDismiss = { onIntent(FoodIntent.OnCloseSmartMealSheet) }
            )
        }

        // ── Grocery / Market List -> Planora To-Do Bottom Sheet ──
        if (state.isGroceryListSheetOpen) {
            GroceryListBottomSheet(
                title = state.groceryListTitle,
                items = state.groceryItems,
                selectedItems = state.selectedGroceryItems,
                onToggleItem = { onIntent(FoodIntent.OnToggleGroceryItem(it)) },
                onSendToPlanora = { onIntent(FoodIntent.OnSendGroceryToPlanora(appLauncher)) },
                onDismiss = { onIntent(FoodIntent.OnCloseGroceryListSheet) }
            )
        }

        // ── Food Photo & Nutrition Label Scan Bottom Sheet (2.D) ──
        if (state.isPhotoScanSheetOpen) {
            FoodPhotoScanBottomSheet(
                isAnalyzing = state.isPhotoScanAnalyzing,
                onAnalyze = { onIntent(FoodIntent.OnAnalyzePhotoOrLabel(it)) },
                onDismiss = { onIntent(FoodIntent.OnClosePhotoScanSheet) }
            )
        }
    }
}

@Composable
private fun QuickChip(
    label: String, onClick: () -> Unit
) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp).bounceClick()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

private enum class AddFoodTab(
    val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    PRESETS("Hazır Besinler", Icons.Default.RestaurantMenu),
    MANUAL("Manuel Giriş", Icons.Default.Edit)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualAddBottomSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, portion: String, calories: Int, protein: Double, carbs: Double, fat: Double, fiber: Double) -> Unit,
    onAddWithDetails: ((name: String, portion: String, calories: Int, protein: Double, carbs: Double, fat: Double, fiber: Double, micronutrients: List<Micronutrient>, iconKey: String?) -> Unit)? = null,
    customFoods: List<com.yusufteker.healthtrack.core.model.CustomFoodItem> = emptyList(),
    favoriteFoodIds: Set<String> = emptySet(),
    frequentFoods: List<com.yusufteker.healthtrack.core.model.FoodItem> = emptyList(),
    onToggleFavorite: ((String) -> Unit)? = null,
    onOpenAddCustomFood: (() -> Unit)? = null,
    onDeleteCustomFood: ((String) -> Unit)? = null,
    onOpenInventoryStudio: ((com.yusufteker.healthtrack.core.model.CustomFoodItem?) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = LocalIsDarkTheme.current
    val scrollState = rememberScrollState()
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f)
    val calmFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = HealthColors.Primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f)
    )

    var activeTab by remember { mutableStateOf(AddFoodTab.PRESETS) }

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
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                )
            }
        },
        modifier = Modifier.navigationBarsPadding().imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .verticalScroll(scrollState)
                .clearFocusOnTap()
                .padding(horizontal = 18.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Segmented Tab Selector & Close Button on the Same Horizontal Line ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) HealthColors.SurfaceContainerLowest else HealthColors.SurfaceVariantLight)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AddFoodTab.entries.forEach { tab ->
                        val isSelected = activeTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (isSelected) {
                                        if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surface
                                    } else Color.Transparent
                                )
                                .clickable { activeTab = tab }
                                .padding(vertical = 8.dp, horizontal = 6.dp)
                                .bounceClick(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isDark) HealthColors.SurfaceContainerLowest else HealthColors.SurfaceVariantLight)
                        .bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── Tab Content ──
            if (activeTab == AddFoodTab.PRESETS) {
                QuickFoodPresetsView(
                    customFoods = customFoods,
                    favoriteFoodIds = favoriteFoodIds,
                    frequentFoods = frequentFoods,
                    onToggleFavorite = onToggleFavorite,
                    onOpenAddCustomFood = onOpenAddCustomFood,
                    onDeleteCustomFood = onDeleteCustomFood,
                    onOpenInventoryStudio = onOpenInventoryStudio,
                    onAddFood = { pName, pPortion, pCal, pProt, pCarb, pFat, pFib ->
                        onAdd(pName, pPortion, pCal, pProt, pCarb, pFat, pFib)
                    },
                    onAddFoodWithDetails = onAddWithDetails,
                    onSelectFood = { preset, qty ->
                        name = preset.nameTr
                        val totalAmount = preset.unitStep * qty
                        val cleanUnit = preset.unitLabelTr.replace(Regex("^1\\s+"), "")
                        portion =
                            if (preset.unitStep == 1) "$qty $cleanUnit" else "$totalAmount $cleanUnit"
                        calories = (preset.baseCalories * qty).toString()
                        proteinText =
                            if (preset.baseProteinG > 0) ((preset.baseProteinG * qty * 10).toInt() / 10.0).toString() else ""
                        carbsText =
                            if (preset.baseCarbsG > 0) ((preset.baseCarbsG * qty * 10).toInt() / 10.0).toString() else ""
                        fatText =
                            if (preset.baseFatG > 0) ((preset.baseFatG * qty * 10).toInt() / 10.0).toString() else ""
                        activeTab = AddFoodTab.MANUAL
                    })

                // ── "Envanterim" Card (Right above "Aradığınızı bulamadınız mı?") ──
                if (onOpenInventoryStudio != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = HealthColors.Secondary.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenInventoryStudio(null) }
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(HealthColors.Secondary.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = HealthColors.Secondary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Envanterim",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Kendi eklediğiniz ürünlerin ve öğünlerin seviyelerini ayarlayın",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (onOpenAddCustomFood != null) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = HealthColors.Secondary.copy(alpha = 0.18f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { onOpenAddCustomFood() }
                                        .bounceClick()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = HealthColors.Secondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Yeni",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = HealthColors.Secondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = HealthColors.Secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Quick Shortcut Card to Manual Entry
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        .clickable { activeTab = AddFoodTab.MANUAL }.bounceClick()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(17.dp)
                            )
                            Column {
                                Text(
                                    text = "Aradığınızı bulamadınız mı?",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Kendi besininizi gramaj ve kalorisiyle elle girin",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {

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
                        placeholder = {
                            Text(
                                "Örn: Izgara Somon, Yulaf Kasesi...",
                                color = placeholderColor
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = calmFieldColors,
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
                        placeholder = {
                            Text(
                                "Örn: 1 Porsiyon / 150g / 1 Kase",
                                color = placeholderColor
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Straighten,
                                contentDescription = null,
                                tint = HealthColors.Secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = calmFieldColors,
                        singleLine = true
                    )

                    // Quick portion suggestion chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "1 Porsiyon", "100g", "1 Adet", "1 Kase", "1 Dilim", "200ml"
                        ).forEach { chip ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (portion == chip) HealthColors.Primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.5f
                                ),
                                modifier = Modifier.clickable { portion = chip }.bounceClick()
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
                        placeholder = {
                            Text(
                                "Örn: 250",
                                color = placeholderColor
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = HealthColors.Tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Text(
                                "kcal",
                                style = MaterialTheme.typography.labelMedium,
                                color = HealthColors.Tertiary,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = calmFieldColors,
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
                            placeholder = {
                                Text("0", color = placeholderColor)
                            },
                            label = { Text("Protein (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = calmFieldColors,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = carbsText,
                            onValueChange = { carbsText = it },
                            placeholder = {
                                Text("0", color = placeholderColor)
                            },
                            label = { Text("Karb (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = calmFieldColors,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = fatText,
                            onValueChange = { fatText = it },
                            placeholder = {
                                Text("0", color = placeholderColor)
                            },
                            label = { Text("Yağ (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = calmFieldColors,
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                onAdd(
                                    name,
                                    portion.ifBlank { "1 Porsiyon" },
                                    calInt,
                                    prot,
                                    carb,
                                    fat,
                                    0.0
                                )
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
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Öğünü Ekle", fontWeight = FontWeight.Bold)
                    }
                }

                // Return to Presets Tab Link
                TextButton(
                    onClick = { activeTab = AddFoodTab.PRESETS },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = HealthColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hazır Besin Listesine Dön",
                        fontWeight = FontWeight.SemiBold,
                        color = HealthColors.Primary
                    )
                }
            }
        }
    }
}
