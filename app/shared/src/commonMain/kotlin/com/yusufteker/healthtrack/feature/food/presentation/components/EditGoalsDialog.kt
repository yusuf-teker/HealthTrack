package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

@Composable
fun EditGoalsDialog(
    currentGoal: NutritionGoal,
    currentWeight: Double = 70.0,
    fitnessGoal: FitnessGoal = FitnessGoal.MAINTAIN,
    onDismiss: () -> Unit,
    onSave: (goal: NutritionGoal, fitnessGoal: FitnessGoal) -> Unit
) {
    var selectedFitnessGoal by remember { mutableStateOf(fitnessGoal) }
    val initialProtein = if (currentGoal.proteinG == 80.0) {
        (currentWeight * fitnessGoal.proteinMultiplier).toInt().toString()
    } else {
        currentGoal.proteinG.toInt().toString()
    }
    var caloriesText by remember { mutableStateOf(currentGoal.calories.toString()) }
    var proteinText by remember { mutableStateOf(initialProtein) }
    var carbsText by remember { mutableStateOf(currentGoal.carbsG.toInt().toString()) }
    var fatText by remember { mutableStateOf(currentGoal.fatG.toInt().toString()) }
    var fiberText by remember { mutableStateOf(currentGoal.fiberG.toInt().toString()) }

    var vitCText by remember { mutableStateOf(currentGoal.vitaminCMg.toInt().toString()) }
    var vitDText by remember { mutableStateOf(currentGoal.vitaminDUg.toInt().toString()) }
    var vitB12Text by remember { mutableStateOf(currentGoal.vitaminB12Ug.toString()) }
    var ironText by remember { mutableStateOf(currentGoal.ironMg.toInt().toString()) }
    var calciumText by remember { mutableStateOf(currentGoal.calciumMg.toInt().toString()) }
    var magnesiumText by remember { mutableStateOf(currentGoal.magnesiumMg.toInt().toString()) }

    var showMicros by remember { mutableStateOf(false) }

    val isDark = LocalIsDarkTheme.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .imePadding()
                .clearFocusOnTap(),
            shape = RoundedCornerShape(28.dp),
            color = if (isDark) HealthColors.SurfaceContainer else Color.White,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Badge & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            HealthColors.Primary.copy(alpha = 0.20f),
                                            HealthColors.Secondary.copy(alpha = 0.15f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Besin Hedefleri",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Günlük kalori ve makro hedefleri",
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Fitness Hedefi & Otomatik Protein Hesaplayıcı (Senkronize)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fitness & Beslenme Hedefi",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = HealthColors.Primary
                    )
                    Text(
                        text = "Kilo: ${currentWeight.toInt()} kg",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoalPresetChip(
                        title = "Kilo Verme",
                        sub = "1.2x (${(currentWeight * 1.2).toInt()}g)",
                        isSelected = selectedFitnessGoal == FitnessGoal.LOSE_WEIGHT,
                        onClick = {
                            selectedFitnessGoal = FitnessGoal.LOSE_WEIGHT
                            caloriesText = "1750"
                            proteinText = (currentWeight * 1.2).toInt().toString()
                            carbsText = "160"
                            fatText = "50"
                            fiberText = "30"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GoalPresetChip(
                        title = "Sabit Kalma",
                        sub = "1.0x (${(currentWeight * 1.0).toInt()}g)",
                        isSelected = selectedFitnessGoal == FitnessGoal.MAINTAIN,
                        onClick = {
                            selectedFitnessGoal = FitnessGoal.MAINTAIN
                            caloriesText = "2000"
                            proteinText = (currentWeight * 1.0).toInt().toString()
                            carbsText = "220"
                            fatText = "65"
                            fiberText = "30"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GoalPresetChip(
                        title = "Kaslanma",
                        sub = "1.4x (${(currentWeight * 1.4).toInt()}g)",
                        isSelected = selectedFitnessGoal == FitnessGoal.BUILD_MUSCLE,
                        onClick = {
                            selectedFitnessGoal = FitnessGoal.BUILD_MUSCLE
                            caloriesText = "2400"
                            proteinText = (currentWeight * 1.4).toInt().toString()
                            carbsText = "280"
                            fatText = "70"
                            fiberText = "35"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Senkronize Hedef & Kilo Bilgi Kartı
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HealthColors.NutrientProtein.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, HealthColors.NutrientProtein.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Aktif Hedef: ${selectedFitnessGoal.title}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = HealthColors.NutrientProtein
                            )
                            Text(
                                text = "${currentWeight.toInt()} kg × ${selectedFitnessGoal.proteinMultiplier}x = ${(currentWeight * selectedFitnessGoal.proteinMultiplier).toInt()}g protein/gün",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${proteinText}g",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = HealthColors.NutrientProtein
                        )
                    }
                }

                // Primary Goal: Calories Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.6f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = HealthColors.Tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Günlük Hedef Kalori",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Steppers for Calories: -50 / +50
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SmallStepButton(text = "-50") {
                                    val cur = caloriesText.toIntOrNull() ?: 2000
                                    caloriesText = maxOf(800, cur - 50).toString()
                                }
                                SmallStepButton(text = "+50") {
                                    val cur = caloriesText.toIntOrNull() ?: 2000
                                    caloriesText = minOf(6000, cur + 50).toString()
                                }
                            }
                        }

                        OutlinedTextField(
                            value = caloriesText,
                            onValueChange = { caloriesText = it },
                            placeholder = { Text("2000") },
                            trailingIcon = {
                                Text(
                                    text = "kcal",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HealthColors.Tertiary,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HealthColors.Tertiary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                // Macro Grid (Protein, Carbs, Fat, Fiber)
                Text(
                    text = "Makro Besinler (Günlük Gram)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = HealthColors.Primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MacroInputCard(
                        title = "Protein",
                        value = proteinText,
                        onValueChange = { proteinText = it },
                        color = HealthColors.NutrientProtein,
                        step = 5,
                        modifier = Modifier.weight(1f)
                    )
                    MacroInputCard(
                        title = "Karbonhidrat",
                        value = carbsText,
                        onValueChange = { carbsText = it },
                        color = HealthColors.NutrientCarbs,
                        step = 10,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MacroInputCard(
                        title = "Yağ",
                        value = fatText,
                        onValueChange = { fatText = it },
                        color = HealthColors.NutrientLipids,
                        step = 5,
                        modifier = Modifier.weight(1f)
                    )
                    MacroInputCard(
                        title = "Lif",
                        value = fiberText,
                        onValueChange = { fiberText = it },
                        color = HealthColors.NutrientFiber,
                        step = 5,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Micro Nutrients Toggle
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.4f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMicros = !showMicros }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Biotech,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Mikro Besinler & Vitaminler",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = if (showMicros) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Collapsible Micros
                AnimatedVisibility(
                    visible = showMicros,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MicroInputField(
                                label = "C Vit (mg)",
                                value = vitCText,
                                onValueChange = { vitCText = it },
                                modifier = Modifier.weight(1f)
                            )
                            MicroInputField(
                                label = "D Vit (µg)",
                                value = vitDText,
                                onValueChange = { vitDText = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MicroInputField(
                                label = "B12 Vit (µg)",
                                value = vitB12Text,
                                onValueChange = { vitB12Text = it },
                                modifier = Modifier.weight(1f)
                            )
                            MicroInputField(
                                label = "Demir (mg)",
                                value = ironText,
                                onValueChange = { ironText = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MicroInputField(
                                label = "Kalsiyum (mg)",
                                value = calciumText,
                                onValueChange = { calciumText = it },
                                modifier = Modifier.weight(1f)
                            )
                            MicroInputField(
                                label = "Magnezyum (mg)",
                                value = magnesiumText,
                                onValueChange = { magnesiumText = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(0.8f)
                            .height(50.dp)
                            .bounceClick(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "İptal",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            val updated = currentGoal.copy(
                                calories = caloriesText.toIntOrNull() ?: currentGoal.calories,
                                proteinG = proteinText.toDoubleOrNull() ?: currentGoal.proteinG,
                                carbsG = carbsText.toDoubleOrNull() ?: currentGoal.carbsG,
                                fatG = fatText.toDoubleOrNull() ?: currentGoal.fatG,
                                fiberG = fiberText.toDoubleOrNull() ?: currentGoal.fiberG,
                                vitaminCMg = vitCText.toDoubleOrNull() ?: currentGoal.vitaminCMg,
                                vitaminDUg = vitDText.toDoubleOrNull() ?: currentGoal.vitaminDUg,
                                vitaminB12Ug = vitB12Text.toDoubleOrNull() ?: currentGoal.vitaminB12Ug,
                                ironMg = ironText.toDoubleOrNull() ?: currentGoal.ironMg,
                                calciumMg = calciumText.toDoubleOrNull() ?: currentGoal.calciumMg,
                                magnesiumMg = magnesiumText.toDoubleOrNull() ?: currentGoal.magnesiumMg
                            )
                            onSave(updated, selectedFitnessGoal)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp)
                            .bounceClick()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(HealthColors.GradientPrimary)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hedefleri Kaydet",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalPresetChip(
    title: String,
    sub: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val borderColor = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val bgColor = if (isSelected) HealthColors.Primary.copy(alpha = 0.15f) else if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.5f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.5f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .clickable(onClick = onClick)
            .bounceClick()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SmallStepButton(
    text: String,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight)
            .clickable(onClick = onClick)
            .bounceClick()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MacroInputCard(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    color: Color,
    step: Int,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.45f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Stepper + / -
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f))
                            .clickable {
                                val cur = value.toIntOrNull() ?: 50
                                onValueChange(maxOf(0, cur - step).toString())
                            }
                            .bounceClick(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "-", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color)
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f))
                            .clickable {
                                val cur = value.toIntOrNull() ?: 50
                                onValueChange((cur + step).toString())
                            }
                            .bounceClick(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color)
                    }
                }
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                trailingIcon = {
                    Text(
                        text = "g",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = color,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            )
        }
    }
}

@Composable
private fun MicroInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    )
}
