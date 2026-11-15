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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.NutrientProgress
import com.yusufteker.healthtrack.core.model.SmartMealSuggestion
import com.yusufteker.healthtrack.feature.food.presentation.SmartMealFilterType
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartMealMatcherBottomSheet(
    suggestions: List<SmartMealSuggestion>,
    nutrientBars: List<NutrientProgress>,
    remainingCalories: Int,
    latestAiRecipe: SmartMealSuggestion? = null,
    isFromUserDb: Boolean = false,
    isOnlyQuick: Boolean,
    isOnlyFewIngredients: Boolean,
    isExcludeRecent3Days: Boolean,
    isHighProtein: Boolean,
    isGeneratingAi: Boolean,
    onToggleFilter: (SmartMealFilterType) -> Unit,
    onAddMeal: (SmartMealSuggestion) -> Unit,
    onOpenGroceryForMeal: (title: String, ingredients: List<String>) -> Unit,
    onRequestAiRecipe: () -> Unit,
    onDismissAiRecipe: () -> Unit = {},
    onOpenDailyMealPlan: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .clearFocusOnTap()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Top Header ──
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HealthColors.Primary.copy(alpha = 0.16f)),
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
                            text = str("Ne Yesem? 🥗", "What Should I Eat? 🥗"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = str(
                                "Eksik makro & mikrolarınıza göre akıllı öneriler",
                                "Smart picks for your missing macros & micros"
                            ),
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
                        contentDescription = str("Kapat", "Close"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val lowestMicros = nutrientBars
                .filter { it.key != "calories" }
                .sortedBy { it.percentage }
                .take(2)
                .map {
                    val label = localizedNutrientLabel(it)
                    val pct = it.percentage.toInt()
                    str("$label %$pct", "$label $pct%")
                }

            // ── Filter Chips Row (Pinned for quick filtering) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(
                    label = str("📦 Kendi Ürünlerim", "📦 My Inventory"),
                    selected = isFromUserDb,
                    onClick = { onToggleFilter(SmartMealFilterType.FROM_USER_DB) }
                )
                FilterPill(
                    label = str("⚡ Kolay (<10 dk)", "⚡ Quick (<10 min)"),
                    selected = isOnlyQuick,
                    onClick = { onToggleFilter(SmartMealFilterType.QUICK) }
                )
                FilterPill(
                    label = str("🥣 Az Malzemeli (≤4)", "🥣 Few Ingredients (≤4)"),
                    selected = isOnlyFewIngredients,
                    onClick = { onToggleFilter(SmartMealFilterType.FEW_INGREDIENTS) }
                )
                FilterPill(
                    label = str("🔄 Son 3 Gün Yemediklerim", "🔄 Not Eaten Last 3 Days"),
                    selected = isExcludeRecent3Days,
                    onClick = { onToggleFilter(SmartMealFilterType.EXCLUDE_RECENT_3_DAYS) }
                )
                FilterPill(
                    label = str("💪 Yüksek Protein", "💪 High Protein"),
                    selected = isHighProtein,
                    onClick = { onToggleFilter(SmartMealFilterType.HIGH_PROTEIN) }
                )
            }

            // ── Scrollable Content (Targets + AI Actions + Meal Cards) ──
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                // Compact Open Targets Pill
                item(key = "targets_banner") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HealthColors.Primary.copy(alpha = 0.09f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (lowestMicros.isNotEmpty()) {
                                    str(
                                        "Kalan: $remainingCalories kcal  •  Düşük: ${lowestMicros.joinToString(", ")}",
                                        "Remaining: $remainingCalories kcal  •  Low: ${lowestMicros.joinToString(", ")}"
                                    )
                                } else {
                                    str("Kalan Kalori: $remainingCalories kcal", "Remaining Calories: $remainingCalories kcal")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // AI Action Row: 4-Meal Menu + Custom AI Recipe
                item(key = "ai_actions") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onOpenDailyMealPlan != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = HealthColors.Primary.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenDailyMealPlan() }
                                    .bounceClick()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = str("Günün 4 Öğünlük AI Menüsü 🥗", "Today's 4-Meal AI Menu 🥗"),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = HealthColors.Primary
                                        )
                                        Text(
                                            text = str(
                                                "Tam günlük öğün planını görüntüle",
                                                "View full daily meal plan"
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = HealthColors.Primary
                                    ) {
                                        Text(
                                            text = str("Aç →", "Open →"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onRequestAiRecipe,
                            enabled = !isGeneratingAi,
                            modifier = Modifier.fillMaxWidth().height(44.dp).bounceClick(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HealthColors.Primary
                            )
                        ) {
                            if (isGeneratingAi) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = HealthColors.Primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    str("Gemini AI Tarif Hazırlıyor...", "Gemini AI is crafting a recipe..."),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = HealthColors.Primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    str("Yapay Zekadan Kişisel Tarif İste", "Ask AI for a Personal Recipe"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // AI Recipe Spotlight Hero Card
                if (latestAiRecipe != null) {
                    item(key = "ai_spotlight") {
                        AiRecipeSpotlightCard(
                            meal = latestAiRecipe,
                            onAddMeal = { onAddMeal(latestAiRecipe) },
                            onOpenGrocery = { onOpenGroceryForMeal(latestAiRecipe.title, latestAiRecipe.ingredients) },
                            onDismiss = onDismissAiRecipe
                        )
                    }
                }

                // Matching Meal List or Empty State
                if (suggestions.isEmpty() && latestAiRecipe == null) {
                    item(key = "empty_state") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = str("Seçili filtrelere uygun öğün bulunamadı", "No meals match the selected filters"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = str(
                                    "Filtreleri gevşetebilir veya AI ile yeni tarif üretebilirsiniz.",
                                    "You can relax the filters or generate a new recipe with AI."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(suggestions, key = { it.id }) { meal ->
                        SmartMealCard(
                            meal = meal,
                            onAddMeal = { onAddMeal(meal) },
                            onOpenGrocery = { onOpenGroceryForMeal(meal.title, meal.ingredients) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) HealthColors.Primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .bounceClick()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartMealCard(
    meal: SmartMealSuggestion,
    onAddMeal: () -> Unit,
    onOpenGrocery: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title + Prep Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = meal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = meal.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = HealthColors.Primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    val timeText = when {
                        meal.prepTimeMinutes <= 0 -> str("Aç Ye", "Ready to Eat")
                        meal.prepTimeMinutes == 1 -> str("1 dk", "1 min")
                        else -> str("${meal.prepTimeMinutes} dk", "${meal.prepTimeMinutes} min")
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Macro badge capsules (wrapping cleanly)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MacroMiniPill(str("Kalori", "Calories"), "${meal.calories} kcal", Color(0xFFFF5722))
                MacroMiniPill("Protein", "${meal.proteinG.toInt()}g", Color(0xFF8E24AA))
                MacroMiniPill(str("Lif", "Fiber"), "${meal.fiberG.toInt()}g", Color(0xFF43A047))
                if (meal.keyMicros.isNotEmpty()) {
                    val firstMicro = meal.keyMicros.first()
                    MacroMiniPill(firstMicro.name, "${firstMicro.amount.toInt()}${firstMicro.unit}", Color(0xFF00B4D8))
                }
            }

            // Concise tags (max 2)
            if (meal.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    meal.tags.take(2).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Ingredients toggle
            Text(
                text = if (expanded) {
                    str("▲ Malzemeleri Gizle (${meal.ingredients.size})", "▲ Hide Ingredients (${meal.ingredients.size})")
                } else {
                    str("▼ Malzemeleri Gör (${meal.ingredients.size})", "▼ View Ingredients (${meal.ingredients.size})")
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp)
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    meal.ingredients.forEach { ingredient ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(HealthColors.Primary)
                            )
                            Text(
                                text = ingredient,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    meal.instructions?.let { instr ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = instr,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Actions row: [ Yedim Olarak Ekle ] + [ Planora'ya Market Listesi Gönder ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddMeal,
                    modifier = Modifier.weight(1f).height(38.dp).bounceClick(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HealthColors.Primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = str("Yedim Olarak Ekle", "Log as Eaten"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onOpenGrocery,
                    modifier = Modifier.height(38.dp).bounceClick(),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = HealthColors.Primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Planora To-Do",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = HealthColors.Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroMiniPill(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun AiRecipeSpotlightCard(
    meal: SmartMealSuggestion,
    onAddMeal: () -> Unit,
    onOpenGrocery: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = HealthColors.Primary.copy(alpha = 0.08f),
        border = BorderStroke(1.5.dp, HealthColors.Primary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Glow badge + Close icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = HealthColors.Primary.copy(alpha = 0.20f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = HealthColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = str("YAPAY ZEKA ÖZEL TARİFİ", "AI CUSTOM RECIPE"),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Black,
                            color = HealthColors.Primary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(26.dp).bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = str("Gizle", "Hide"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Recipe Title
            Text(
                text = meal.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Instructions / Description
            meal.instructions?.let { instr ->
                Text(
                    text = instr,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Macro pills: Kcal, Protein, Karb, Yağ, Lif
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MacroMiniPill(str("Kalori", "Calories"), "${meal.calories} kcal", Color(0xFFFF5722))
                MacroMiniPill("Protein", "${meal.proteinG.toInt()}g", Color(0xFF8E24AA))
                MacroMiniPill(str("Lif", "Fiber"), "${meal.fiberG.toInt()}g", Color(0xFF43A047))
                if (meal.carbsG > 0) {
                    MacroMiniPill(str("Karb", "Carbs"), "${meal.carbsG.toInt()}g", Color(0xFFFB923C))
                }
            }

            // Ingredients toggle
            if (meal.ingredients.isNotEmpty()) {
                Text(
                    text = if (expanded) {
                        str("▲ Malzemeleri Gizle (${meal.ingredients.size})", "▲ Hide Ingredients (${meal.ingredients.size})")
                    } else {
                        str("▼ Malzemeleri Gör (${meal.ingredients.size})", "▼ View Ingredients (${meal.ingredients.size})")
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = HealthColors.Primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 2.dp)
                )

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        meal.ingredients.forEach { ing ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(HealthColors.Primary)
                                )
                                Text(
                                    text = ing,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddMeal,
                    modifier = Modifier.weight(1f).height(40.dp).bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HealthColors.Primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = str("Bu Tarifi Günlüğe Ekle", "Add Recipe to Log"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onOpenGrocery,
                    modifier = Modifier.height(40.dp).bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = HealthColors.Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = HealthColors.Primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Planora To-Do",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

