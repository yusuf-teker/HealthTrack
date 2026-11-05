package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Female
import androidx.compose.material.icons.rounded.Male
import androidx.compose.material.icons.rounded.MonitorWeight
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
import com.yusufteker.healthtrack.core.model.calculateProteinTarget
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme

/**
 * Luxury Bio-Telemetry Profile Setup Dialog.
 * Prompts user on first launch for Weight, Height, Age, and Gender
 * with stepper controls and clean modern card inputs.
 */
@Composable
fun InitialProfileDialog(
    initialWeight: Double = 72.0,
    initialHeight: Double = 175.0,
    initialAge: Int = 26,
    initialGender: String = "MALE",
    initialFitnessGoal: FitnessGoal = FitnessGoal.MAINTAIN,
    onSaveProfile: (weight: Double, height: Double, age: Int, gender: String, goal: FitnessGoal) -> Unit
) {
    var weightText by remember { mutableStateOf(if (initialWeight > 0) initialWeight.toString() else "72.0") }
    var heightText by remember { mutableStateOf(if (initialHeight > 0) initialHeight.toInt().toString() else "175") }
    var ageText by remember { mutableStateOf(if (initialAge > 0) initialAge.toString() else "26") }
    var selectedGender by remember { mutableStateOf(initialGender) }
    var selectedGoal by remember { mutableStateOf(initialFitnessGoal) }

    val isDark = LocalIsDarkTheme.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = { /* Modal: Mandatory on initial profile setup */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
                .imePadding()
                .clearFocusOnTap(),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(28.dp),
                elevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Brand Icon Pod
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
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
                        GradientText(
                            text = "H",
                            colors = HealthColors.GradientPrimary,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GradientText(
                            text = "Sağlık Profilinizi Oluşturalım",
                            colors = HealthColors.GradientPrimary,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kalori ve spor harcamanızı doğru hesaplamak için lütfen güncel vücut ölçülerinizi belirtin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // 1. Kilo (Weight) Card
                    ProfileMetricInputCard(
                        title = "Güncel Kilo",
                        valueText = weightText,
                        unit = "kg",
                        icon = Icons.Rounded.MonitorWeight,
                        accentColor = HealthColors.Primary,
                        onValueChange = { weightText = it },
                        onDecrement = {
                            val cur = weightText.toDoubleOrNull() ?: 70.0
                            val next = (cur - 0.5).coerceAtLeast(30.0)
                            weightText = (kotlin.math.round(next * 10) / 10.0).toString()
                        },
                        onIncrement = {
                            val cur = weightText.toDoubleOrNull() ?: 70.0
                            val next = (cur + 0.5).coerceAtMost(300.0)
                            weightText = (kotlin.math.round(next * 10) / 10.0).toString()
                        }
                    )

                    // 2. Boy (Height) Card
                    ProfileMetricInputCard(
                        title = "Boy Uzunluğu",
                        valueText = heightText,
                        unit = "cm",
                        icon = Icons.Default.Straighten,
                        accentColor = HealthColors.Secondary,
                        onValueChange = { heightText = it },
                        onDecrement = {
                            val cur = heightText.toIntOrNull() ?: 170
                            val next = (cur - 1).coerceAtLeast(100)
                            heightText = next.toString()
                        },
                        onIncrement = {
                            val cur = heightText.toIntOrNull() ?: 170
                            val next = (cur + 1).coerceAtMost(250)
                            heightText = next.toString()
                        }
                    )

                    // 3. Yaş (Age) Card
                    ProfileMetricInputCard(
                        title = "Yaş",
                        valueText = ageText,
                        unit = "yaş",
                        icon = Icons.Default.Person,
                        accentColor = HealthColors.Tertiary,
                        onValueChange = { ageText = it },
                        onDecrement = {
                            val cur = ageText.toIntOrNull() ?: 25
                            val next = (cur - 1).coerceAtLeast(12)
                            ageText = next.toString()
                        },
                        onIncrement = {
                            val cur = ageText.toIntOrNull() ?: 25
                            val next = (cur + 1).coerceAtMost(120)
                            ageText = next.toString()
                        }
                    )

                    // 4. Cinsiyet (Gender) Selector
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Biyolojik Cinsiyet",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GenderChoiceCard(
                                label = "Erkek",
                                icon = Icons.Rounded.Male,
                                isSelected = selectedGender == "MALE",
                                onClick = { selectedGender = "MALE" },
                                modifier = Modifier.weight(1f)
                            )
                            GenderChoiceCard(
                                label = "Kadın",
                                icon = Icons.Rounded.Female,
                                isSelected = selectedGender == "FEMALE",
                                onClick = { selectedGender = "FEMALE" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Fitness & Beslenme Hedefi
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Fitness & Beslenme Hedefi",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GoalChoiceCard(
                                label = "Kilo Verme",
                                multiplier = "1.2x kg",
                                isSelected = selectedGoal == FitnessGoal.LOSE_WEIGHT,
                                onClick = { selectedGoal = FitnessGoal.LOSE_WEIGHT },
                                modifier = Modifier.weight(1f)
                            )
                            GoalChoiceCard(
                                label = "Sabit Kalma",
                                multiplier = "1.0x kg",
                                isSelected = selectedGoal == FitnessGoal.MAINTAIN,
                                onClick = { selectedGoal = FitnessGoal.MAINTAIN },
                                modifier = Modifier.weight(1f)
                            )
                            GoalChoiceCard(
                                label = "Kaslanma",
                                multiplier = "1.4x kg",
                                isSelected = selectedGoal == FitnessGoal.BUILD_MUSCLE,
                                onClick = { selectedGoal = FitnessGoal.BUILD_MUSCLE },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val parsedWeight = weightText.toDoubleOrNull() ?: 70.0
                        val estimatedProtein = calculateProteinTarget(parsedWeight, selectedGoal).toInt()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HealthColors.Primary.copy(alpha = 0.10f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Günlük Protein Hedefiniz: $estimatedProtein g ($parsedWeight kg × ${selectedGoal.proteinMultiplier}x)",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = HealthColors.Primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    var isSubmitting by remember { mutableStateOf(false) }

                    // Save Profile CTA Button
                    Button(
                        onClick = {
                            if (isSubmitting) return@Button
                            val weight = weightText.toDoubleOrNull() ?: 70.0
                            val height = heightText.toDoubleOrNull() ?: 175.0
                            val age = ageText.toIntOrNull() ?: 25
                            if (weight > 20.0 && height > 50.0) {
                                isSubmitting = true
                                onSaveProfile(weight, height, age, selectedGender, selectedGoal)
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .bounceClick(),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HealthColors.Primary,
                            contentColor = HealthColors.OnPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Profilimi Kaydet & Başla",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalChoiceCard(
    label: String,
    multiplier: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val borderColor = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val bgColor = if (isSelected) HealthColors.Primary.copy(alpha = 0.12f) else if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.4f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.5f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .clickable(onClick = onClick)
            .bounceClick()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = multiplier,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileMetricInputCard(
    title: String,
    valueText: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onValueChange: (String) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.5f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = onValueChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.width(90.dp)
                    )
                }
            }

            // Stepper controls: [-]  unit  [+]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Azalt",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Artır",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun GenderChoiceCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val borderColor = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val bgColor = if (isSelected) HealthColors.Primary.copy(alpha = 0.12f) else if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.4f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.5f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .clickable(onClick = onClick)
            .bounceClick()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
