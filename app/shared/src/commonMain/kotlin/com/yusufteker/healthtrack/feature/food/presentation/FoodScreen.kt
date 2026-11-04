package com.yusufteker.healthtrack.feature.food.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.feature.food.presentation.components.EditGoalsDialog
import com.yusufteker.healthtrack.feature.food.presentation.components.FoodTableCard
import com.yusufteker.healthtrack.feature.food.presentation.components.NutrientBarsView
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.ui.components.CalorieProgressGauge
import com.yusufteker.healthtrack.ui.components.DaysStripSelector
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick

@Composable
fun FoodScreen(
    state: FoodState,
    onIntent: (FoodIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showManualAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Stitch Horizontal Days Strip with Week Badges ──
        DaysStripSelector(
            selectedDate = state.selectedDate,
            onDateSelected = { newDate ->
                onIntent(FoodIntent.OnDateChanged(newDate))
            }
        )

        // ── Info & Error Banners ──
        state.errorMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onIntent(FoodIntent.OnDismissErrorMessage) }) {
                        Text("Tamam")
                    }
                }
            }
        }

        state.infoMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onIntent(FoodIntent.OnDismissInfoMessage) }) {
                        Text("Tamam")
                    }
                }
            }
        }

        // ── Stitch Concentric Calorie Telemetry Hero Gauge ──
        CalorieProgressGauge(
            consumedCalories = state.totalCalories,
            targetCalories = state.nutritionGoal.calories,
            burnedCalories = 0
        )

        // ── Vertical Nutrient Capsule Bars (Stitch Luxury Design) ──
        NutrientBarsView(
            bars = state.nutrientBars,
            onEditGoalsClick = { onIntent(FoodIntent.OnOpenEditGoalsDialog) }
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
                        containerColor = HealthColors.Primary,
                        contentColor = HealthColors.OnPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    if (state.isCalculatingAi) {
                        CircularProgressIndicator(
                            color = HealthColors.OnPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Gemini Hesaplanıyor...",
                            fontWeight = FontWeight.Bold,
                            color = HealthColors.OnPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI ile Hesapla",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
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
            onRemoveItem = { onIntent(FoodIntent.OnRemoveItem(it)) }
        )

        // Manual Item Add Button
        OutlinedButton(
            onClick = { showManualAddDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .bounceClick(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = HealthColors.Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Elle Yiyecek Satırı Ekle", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ── Edit Goals Dialog ──
    if (state.isEditGoalsDialogOpen) {
        EditGoalsDialog(
            currentGoal = state.nutritionGoal,
            onDismiss = { onIntent(FoodIntent.OnCloseEditGoalsDialog) },
            onSave = { onIntent(FoodIntent.OnSaveGoals(it)) }
        )
    }

    // ── Manual Add Item Dialog ──
    if (showManualAddDialog) {
        ManualAddItemDialog(
            onDismiss = { showManualAddDialog = false },
            onAdd = { name, portion, cal ->
                onIntent(FoodIntent.OnAddItem(name, portion, cal))
                showManualAddDialog = false
            }
        )
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

@Composable
private fun ManualAddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var portion by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yiyecek Ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Yiyecek Adı (Örn: Süt)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = portion,
                    onValueChange = { portion = it },
                    label = { Text("Porsiyon (Örn: 1 Bardak / 200 ml)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Kalori (kcal)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val calInt = calories.toIntOrNull() ?: 0
                    if (name.isNotBlank()) {
                        onAdd(name, portion.ifBlank { "1 Porsiyon" }, calInt)
                    }
                },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HealthColors.Primary, contentColor = HealthColors.OnPrimary)
            ) {
                Text("Ekle", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
