package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yusufteker.healthtrack.core.model.CustomFoodIngredient
import com.yusufteker.healthtrack.core.model.CustomFoodItem
import com.yusufteker.healthtrack.core.model.FoodItem
import com.yusufteker.healthtrack.core.model.Micronutrient
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import kotlin.math.roundToInt

/**
 * Converts a single (non-mix) CustomFoodItem into a virtual 1-ingredient list
 * with intelligent unit detection (ml, g, Ölçek, Shot, Adet, Porsiyon) so every single
 * inventory item also gets the interactive level controller widget!
 */
fun CustomFoodItem.toEffectiveIngredients(
    language: AppLanguage = LocalizationManager.currentLanguage
): List<CustomFoodIngredient> {
    if (ingredients.isNotEmpty()) {
        // Deduplicate & merge any duplicate ingredients with the same name
        // (e.g., if 4 Süt rows + 2 Espresso rows were saved earlier -> merges into 2 unique ingredients: Süt 4x, Espresso 2x)
        return ingredients
            .groupBy { it.name.trim().lowercase() }
            .map { (_, group) ->
                if (group.size == 1) {
                    group.first()
                } else {
                    val first = group.first()
                    val singleBaseAmt = first.defaultAmount.coerceAtLeast(0.5)
                    val totalDefaultAmt = group.sumOf { it.defaultAmount }.coerceAtLeast(singleBaseAmt)
                    val mergedLevels = (1..6).map { singleBaseAmt * it }.let { list ->
                        if (list.none { kotlin.math.abs(it - totalDefaultAmt) < 0.01 }) {
                            (list + totalDefaultAmt).sorted()
                        } else list
                    }
                    val mergedMicros = group
                        .flatMap { it.micronutrients }
                        .groupBy { "${it.name}|${it.unit}" }
                        .map { (_, mList) ->
                            val mFirst = mList.first()
                            mFirst.copy(amount = ((mList.sumOf { it.amount } * 10.0).roundToInt()) / 10.0)
                        }
                    first.copy(
                        defaultAmount = totalDefaultAmt,
                        stepAmount = singleBaseAmt,
                        presetLevels = mergedLevels,
                        baseCalories = group.sumOf { it.baseCalories },
                        baseProteinG = group.sumOf { it.baseProteinG },
                        baseCarbsG = group.sumOf { it.baseCarbsG },
                        baseFatG = group.sumOf { it.baseFatG },
                        baseFiberG = group.sumOf { it.baseFiberG },
                        micronutrients = mergedMicros
                    )
                }
            }
    }

    val rawUnit = portionUnit.trim()
    val lower = rawUnit.lowercase()

    val (detectedBaseAmount, detectedUnitLabel, detectedStep, detectedLevels) = when {
        presetLevels.isNotEmpty() && baseUnitLabel.isNotBlank() -> {
            Quad(baseAmount.takeIf { it > 0.0 } ?: 1.0, baseUnitLabel, 1.0, presetLevels)
        }
        lower.contains("ölçek") || lower.contains("olcek") || lower.contains("scoop") -> {
            val num = Regex("""(\d+(?:[.,]\d+)?)\s*(?:ölçek|olcek|scoop)""").find(lower)
                ?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull() ?: 1.0
            Quad(num, trEn("Ölçek", "Scoop", language), 0.5, listOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0))
        }
        lower.contains("shot") -> {
            val num = Regex("""(\d+(?:[.,]\d+)?)\s*shot""").find(lower)
                ?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull() ?: 1.0
            Quad(num, "Shot", 1.0, listOf(1.0, 2.0, 3.0, 4.0))
        }
        lower.contains("ml") -> {
            val num = Regex("""(\d+)""").find(lower)?.groupValues?.get(1)?.toDoubleOrNull() ?: 200.0
            Quad(num, "ml", 50.0, listOf(50.0, 100.0, 150.0, 200.0, 250.0, 350.0, 500.0))
        }
        Regex("""^\d+\s*g\b""").containsMatchIn(lower) || lower.endsWith("g") || lower.contains("gram") || lower.contains("gr") -> {
            val num = Regex("""(\d+)""").find(lower)?.groupValues?.get(1)?.toDoubleOrNull() ?: 100.0
            Quad(num, "g", 25.0, listOf(25.0, 50.0, 75.0, 100.0, 150.0, 200.0, 250.0))
        }
        lower.contains("kaşık") || lower.contains("kasik") || lower.contains("spoon") -> {
            val num = Regex("""(\d+(?:[.,]\d+)?)""").find(lower)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull() ?: 1.0
            Quad(num, trEn("Kaşık", "Spoon", language), 0.5, listOf(0.5, 1.0, 1.5, 2.0, 3.0))
        }
        lower.contains("adet") || lower.contains("piece") -> {
            Quad(1.0, trEn("Adet", "Piece", language), 0.5, listOf(0.5, 1.0, 1.5, 2.0, 3.0, 4.0))
        }
        lower.contains("dilim") || lower.contains("slice") -> {
            Quad(1.0, trEn("Dilim", "Slice", language), 0.5, listOf(0.5, 1.0, 1.5, 2.0, 3.0))
        }
        else -> {
            Quad(1.0, rawUnit.ifBlank { trEn("Porsiyon", "Serving", language) }, 0.5, listOf(0.5, 1.0, 1.5, 2.0, 3.0))
        }
    }

    return listOf(
        CustomFoodIngredient(
            id = id,
            name = if (!brand.isNullOrBlank()) "$brand $name" else name,
            unitLabel = detectedUnitLabel,
            defaultAmount = detectedBaseAmount,
            stepAmount = detectedStep,
            presetLevels = detectedLevels,
            baseCalories = calories,
            baseProteinG = proteinG,
            baseCarbsG = carbsG,
            baseFatG = fatG,
            baseFiberG = fiberG,
            micronutrients = micronutrients,
            iconKey = iconKey
        )
    )
}

private data class Quad(
    val baseAmount: Double,
    val unitLabel: String,
    val step: Double,
    val levels: List<Double>
)

internal fun formatAmountClean(value: Double): String {
    val rounded = (value * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

/**
 * Full-Screen Inventory ("Envanterim").
 * Only displays items added by the user to their inventory (`customFoods`),
 * lets them interactively adjust the level of each ingredient (e.g. 1/2 Shot coffee, 1/2 Scoop protein,
 * 50ml/100ml/200ml milk), see live macro telemetry, and add them either as separate items or a single item.
 */
@Composable
fun InventoryMixStudioDialog(
    customFoods: List<CustomFoodItem>,
    initialSelectedItem: CustomFoodItem? = null,
    onDismiss: () -> Unit,
    onOpenCreateCustomFood: () -> Unit,
    onDeleteCustomFood: ((String) -> Unit)? = null,
    onAddItemsToMeal: (items: List<FoodItem>, summaryNote: String) -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val lang = appLang()

    var selectedItem by remember(customFoods, initialSelectedItem) {
        mutableStateOf(initialSelectedItem)
    }

    val effectiveIngredients = remember(selectedItem, lang) {
        selectedItem?.toEffectiveIngredients(lang) ?: emptyList()
    }

    // Per-ingredient selected amount & enabled state
    val selectedAmounts = remember(selectedItem) {
        mutableStateMapOf<String, Double>().apply {
            effectiveIngredients.forEach { ing ->
                put(ing.id.ifBlank { ing.name }, ing.defaultAmount)
            }
        }
    }
    val enabledIngredients = remember(selectedItem) {
        mutableStateMapOf<String, Boolean>().apply {
            effectiveIngredients.forEach { ing ->
                put(ing.id.ifBlank { ing.name }, true)
            }
        }
    }

    // Add mode: default is false ("Tek satır olarak ekle")
    var addSeparately by remember(selectedItem) {
        mutableStateOf(false)
    }

    // Calculate live totals across all enabled ingredients
    val activeScaledIngredients = effectiveIngredients.mapNotNull { ing ->
        val key = ing.id.ifBlank { ing.name }
        val isEnabled = enabledIngredients[key] ?: true
        if (!isEnabled) return@mapNotNull null
        val currentAmt = (selectedAmounts[key] ?: ing.defaultAmount).coerceAtLeast(0.0)
        if (currentAmt <= 0.0) return@mapNotNull null
        val ratio = if (ing.defaultAmount > 0.0) currentAmt / ing.defaultAmount else 1.0
        val stepAmt = ing.stepAmount.takeIf { it > 0.0 } ?: ing.defaultAmount.coerceAtLeast(1.0)
        val mult = currentAmt / stepAmt
        val multInt = mult.roundToInt()
        val hasCleanMult = mult >= 1.95 && kotlin.math.abs(mult - multInt) < 0.08
        val compactPortion = when {
            ing.unitLabel.equals("Shot", ignoreCase = true) ->
                if (currentAmt >= 1.95) "Shot x${currentAmt.roundToInt()}" else "1 Shot"
            hasCleanMult && ing.name.contains("espresso", ignoreCase = true) ->
                "${formatAmountClean(currentAmt)}${ing.unitLabel} (Shot x$multInt)"
            hasCleanMult ->
                "${formatAmountClean(currentAmt)}${ing.unitLabel} (${multInt}x)"
            else ->
                "${formatAmountClean(currentAmt)} ${ing.unitLabel}"
        }
        ScaledIngredientPreview(
            ingredient = ing,
            amount = currentAmt,
            formattedPortion = compactPortion,
            calories = (ing.baseCalories * ratio).roundToInt(),
            proteinG = ((ing.baseProteinG * ratio * 10.0).roundToInt()) / 10.0,
            carbsG = ((ing.baseCarbsG * ratio * 10.0).roundToInt()) / 10.0,
            fatG = ((ing.baseFatG * ratio * 10.0).roundToInt()) / 10.0,
            fiberG = ((ing.baseFiberG * ratio * 10.0).roundToInt()) / 10.0,
            micronutrients = ing.micronutrients.map { m ->
                m.copy(amount = ((m.amount * ratio * 10.0).roundToInt()) / 10.0)
            }
        )
    }

    val totalCalories = activeScaledIngredients.sumOf { it.calories }
    val totalProtein = ((activeScaledIngredients.sumOf { it.proteinG } * 10.0).roundToInt()) / 10.0
    val totalCarbs = ((activeScaledIngredients.sumOf { it.carbsG } * 10.0).roundToInt()) / 10.0
    val totalFat = ((activeScaledIngredients.sumOf { it.fatG } * 10.0).roundToInt()) / 10.0
    val totalFiber = ((activeScaledIngredients.sumOf { it.fiberG } * 10.0).roundToInt()) / 10.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            val currentSelected = selectedItem

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                if (currentSelected == null) {
                    // ═══════════════════════════════════════════════════════
                    // VIEW A: 2-COLUMN INVENTORY LIST / GRID
                    // ═══════════════════════════════════════════════════════
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight
                                    )
                                    .bounceClick()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = str("Kapat", "Close"),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = str("Envanterim", "My Inventory"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = str("${customFoods.size} Ürün / Karışım", "${customFoods.size} Products / Mixes"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = HealthColors.Primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = HealthColors.Primary.copy(alpha = 0.16f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { onOpenCreateCustomFood() }
                                .bounceClick()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = HealthColors.Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = str("Yeni Ekle", "Add New"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = HealthColors.Primary
                                )
                            }
                        }
                    }

                    if (customFoods.isEmpty()) {
                        // Empty Inventory State
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                elevation = 6.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(HealthColors.Primary.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = HealthColors.Primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Text(
                                        text = str("Envanteriniz Henüz Boş", "Your Inventory Is Empty"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = str(
                                            "Kendi tükettiğiniz ürünleri veya çoklu içerikli öğünlerinizi (örn: kahve + protein tozu + süt) envanterinize ekleyerek her seferinde seviyelerini (1 shot, 2 ölçek, 100ml vb.) tek dokunuşla seçebilirsiniz.",
                                            "Add your custom products or multi-ingredient mixes (e.g. coffee + protein powder + milk) to pick serving levels (1 shot, 2 scoops, 100ml) with one tap."
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Button(
                                        onClick = onOpenCreateCustomFood,
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .bounceClick(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = HealthColors.Primary,
                                            contentColor = HealthColors.OnPrimary
                                        )
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            str("Envanterime Ürün / Öğün Ekle", "Add Product / Meal to Inventory"),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // 2-Column Grid List (2 items per row)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(customFoods, key = { it.id }) { item ->
                                InventoryGridItemCard(
                                    item = item,
                                    onSelect = { selectedItem = item },
                                    onDelete = onDeleteCustomFood
                                )
                            }
                        }
                    }
                } else {
                    // ═══════════════════════════════════════════════════════
                    // VIEW B: PRODUCT DETAIL & LEVEL STUDIO
                    // ═══════════════════════════════════════════════════════
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { selectedItem = null },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight
                                    )
                                    .bounceClick()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = str("Listeye Dön", "Back to List"),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentSelected.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = str("Seviye & Porsiyon Ayarı", "Level & Serving Adjustment"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = HealthColors.Primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight
                                )
                                .bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = str("Kapat", "Close"),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Scrollable Studio Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── 1. Unified Selected Product Card with Integrated Live Macros ──
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            backgroundColor = if (isDark) HealthColors.SurfaceContainer else Color.White,
                            elevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                HealthColors.Primary.copy(alpha = 0.12f),
                                                (if (isDark) HealthColors.Secondary else HealthColors.SecondaryVariant).copy(alpha = 0.08f)
                                            )
                                        )
                                    )
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Product Header & Calories
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        FoodIconBadge(
                                            iconKey = currentSelected.iconKey,
                                            size = 40.dp,
                                            tint = HealthColors.Primary
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = currentSelected.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (effectiveIngredients.size > 1) {
                                                        (if (isDark) HealthColors.Secondary else HealthColors.SecondaryVariant).copy(alpha = 0.18f)
                                                    } else {
                                                        HealthColors.Primary.copy(alpha = 0.16f)
                                                    }
                                                ) {
                                                    Text(
                                                        text = if (effectiveIngredients.size > 1) {
                                                            str("⚡ ${effectiveIngredients.size} İçerik", "⚡ ${effectiveIngredients.size} Items")
                                                        } else {
                                                            str("🏷️ Tekli", "🏷️ Single")
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (effectiveIngredients.size > 1) {
                                                            if (isDark) HealthColors.Secondary else HealthColors.SecondaryVariant
                                                        } else HealthColors.Primary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }

                                            val brand = currentSelected.brand
                                            if (!brand.isNullOrBlank()) {
                                                Text(
                                                    text = brand,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Text(
                                                text = if (activeScaledIngredients.isEmpty()) {
                                                    str("En az 1 içerik seçin", "Select at least 1 item")
                                                } else {
                                                    activeScaledIngredients.joinToString(" • ") {
                                                        "${it.ingredient.name.substringBefore(" ")}: ${it.formattedPortion}"
                                                    }
                                                },
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = HealthColors.Primary,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = HealthColors.Primary.copy(alpha = 0.18f),
                                        modifier = Modifier.defaultMinSize(minWidth = 58.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "$totalCalories",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Black,
                                                color = HealthColors.Primary
                                            )
                                            Text(
                                                text = "kcal",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = HealthColors.Primary
                                            )
                                        }
                                    }
                                }

                                // Integrated Macro Pills Row inside the same card
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StudioMacroPill(
                                        label = "Protein",
                                        value = "${totalProtein}g",
                                        color = HealthColors.NutrientProtein,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StudioMacroPill(
                                        label = str("Karb", "Carbs"),
                                        value = "${totalCarbs}g",
                                        color = HealthColors.NutrientCarbs,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StudioMacroPill(
                                        label = str("Yağ", "Fat"),
                                        value = "${totalFat}g",
                                        color = HealthColors.NutrientLipids,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StudioMacroPill(
                                        label = str("Lif", "Fiber"),
                                        value = "${totalFiber}g",
                                        color = HealthColors.Secondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // ── 2. Interactive Seviye Ayarı ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = str("Seviye Ayarı", "Level Adjustment"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = str("Seviye butonlarına dokunarak ölçeklendirin", "Tap level buttons to scale"),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        effectiveIngredients.forEach { ing ->
                            val key = ing.id.ifBlank { ing.name }
                            val isEnabled = enabledIngredients[key] ?: true
                            val currentAmount = selectedAmounts[key] ?: ing.defaultAmount
                            val ratio = if (ing.defaultAmount > 0.0) currentAmount / ing.defaultAmount else 1.0
                            val itemCal = (ing.baseCalories * ratio).roundToInt()
                            val itemProt = ((ing.baseProteinG * ratio * 10.0).roundToInt()) / 10.0
                            val itemCarb = ((ing.baseCarbsG * ratio * 10.0).roundToInt()) / 10.0
                            val itemFat = ((ing.baseFatG * ratio * 10.0).roundToInt()) / 10.0

                            IngredientLevelWidgetCard(
                                ingredient = ing,
                                isEnabled = isEnabled,
                                showToggle = effectiveIngredients.size > 1,
                                currentAmount = currentAmount,
                                scaledCalories = itemCal,
                                scaledProtein = itemProt,
                                scaledCarbs = itemCarb,
                                scaledFat = itemFat,
                                onToggleEnabled = { enabledIngredients[key] = !isEnabled },
                                onSelectAmount = { newAmt ->
                                    selectedAmounts[key] = newAmt.coerceAtLeast(ing.stepAmount)
                                    if (!isEnabled) enabledIngredients[key] = true
                                }
                            )
                        }

                        // ── 3. Logging Mode Selector (When > 1 ingredient) ──
                        if (effectiveIngredients.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.55f)
                                else HealthColors.SurfaceVariantLight.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = str("Günlük Tabloya Ekleme Biçimi", "Log Table Entry Mode"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ModeOptionChip(
                                            title = str("Tek Satır Olarak Ekle", "Add as Single Row"),
                                            subtitle = str(
                                                "${currentSelected.name} adıyla tek satır",
                                                "Single row named ${currentSelected.name}"
                                            ),
                                            selected = !addSeparately,
                                            onClick = { addSeparately = false },
                                            modifier = Modifier.weight(1f)
                                        )
                                        ModeOptionChip(
                                            title = str("İçerikleri Ayrı Ayrı Ekle", "Add Items Separately"),
                                            subtitle = str(
                                                "Her içerik tabloda ayrı satır olur",
                                                "Each item appears on its own row"
                                            ),
                                            selected = addSeparately,
                                            onClick = { addSeparately = true },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // ── 4. Sticky Bottom Action Bar ──
                    Surface(
                        color = if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (activeScaledIngredients.isEmpty()) return@Button
                                    if (addSeparately && activeScaledIngredients.size > 1) {
                                        val foodItems = activeScaledIngredients.map { scaled ->
                                            FoodItem(
                                                name = scaled.ingredient.name,
                                                formattedPortion = scaled.formattedPortion,
                                                calories = scaled.calories,
                                                proteinG = scaled.proteinG,
                                                carbsG = scaled.carbsG,
                                                fatG = scaled.fatG,
                                                fiberG = scaled.fiberG,
                                                micronutrients = scaled.micronutrients,
                                                iconKey = scaled.ingredient.iconKey ?: currentSelected.iconKey
                                            )
                                        }
                                        onAddItemsToMeal(
                                            foodItems,
                                            trEn(
                                                "${currentSelected.name} (${foodItems.size} içerik, $totalCalories kcal) öğüne eklendi.",
                                                "${currentSelected.name} (${foodItems.size} items, $totalCalories kcal) added to meal.",
                                                lang
                                            )
                                        )
                                    } else {
                                        val combinedPortion = if (activeScaledIngredients.size == 1) {
                                            activeScaledIngredients.first().formattedPortion
                                        } else {
                                            activeScaledIngredients.joinToString(" • ") { scaled ->
                                                val ing = scaled.ingredient
                                                val stepAmt = ing.stepAmount.takeIf { it > 0.0 } ?: ing.defaultAmount.coerceAtLeast(1.0)
                                                val mult = scaled.amount / stepAmt
                                                val multInt = mult.roundToInt()
                                                val hasCleanMult = mult >= 1.95 && kotlin.math.abs(mult - multInt) < 0.08
                                                val shortName = ing.name.substringBefore(" ")
                                                when {
                                                    ing.unitLabel.equals("Shot", ignoreCase = true) ->
                                                        "Shot x${scaled.amount.roundToInt().coerceAtLeast(1)}"
                                                    hasCleanMult && ing.name.contains("espresso", ignoreCase = true) ->
                                                        "Shot x$multInt (${formatAmountClean(scaled.amount)}${ing.unitLabel})"
                                                    hasCleanMult ->
                                                        "$shortName ${multInt}x (${formatAmountClean(scaled.amount)}${ing.unitLabel})"
                                                    else ->
                                                        "$shortName ${formatAmountClean(scaled.amount)}${ing.unitLabel}"
                                                }
                                            }
                                        }
                                        val mergedMicros = activeScaledIngredients
                                            .flatMap { it.micronutrients }
                                            .groupBy { "${it.name}|${it.unit}" }
                                            .map { (_, list) ->
                                                val first = list.first()
                                                first.copy(
                                                    amount = ((list.sumOf { it.amount } * 10.0).roundToInt()) / 10.0
                                                )
                                            }
                                        val singleItem = FoodItem(
                                            name = currentSelected.name,
                                            formattedPortion = combinedPortion,
                                            calories = totalCalories,
                                            proteinG = totalProtein,
                                            carbsG = totalCarbs,
                                            fatG = totalFat,
                                            fiberG = totalFiber,
                                            micronutrients = mergedMicros,
                                            iconKey = currentSelected.iconKey
                                        )
                                        onAddItemsToMeal(
                                            listOf(singleItem),
                                            trEn(
                                                "${currentSelected.name} ($totalCalories kcal) öğüne eklendi.",
                                                "${currentSelected.name} ($totalCalories kcal) added to meal.",
                                                lang
                                            )
                                        )
                                    }
                                    onDismiss()
                                },
                                enabled = activeScaledIngredients.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .bounceClick(),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HealthColors.Primary,
                                    contentColor = HealthColors.OnPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = str("Öğünüme Ekle ($totalCalories kcal)", "Add to My Meal ($totalCalories kcal)"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryGridItemCard(
    item: CustomFoodItem,
    onSelect: () -> Unit,
    onDelete: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val effectiveIngredients = item.toEffectiveIngredients()
    val isMix = effectiveIngredients.size > 1 || item.isMix

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect)
            .bounceClick(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = if (isDark) HealthColors.SurfaceContainer else Color.White,
        borderGradient = if (isDark) null else listOf(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), HealthColors.Primary.copy(alpha = 0.25f)),
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top tag & delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val mixTagColor = if (isMix) {
                    if (isDark) HealthColors.Secondary else HealthColors.SecondaryVariant
                } else HealthColors.Primary
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = mixTagColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = if (isMix) {
                            str("⚡ ${effectiveIngredients.size} İçerik", "⚡ ${effectiveIngredients.size} Items")
                        } else {
                            str("🏷️ Tekli", "🏷️ Single")
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = mixTagColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (onDelete != null) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = str("Sil", "Delete"),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onDelete(item.id) }
                    )
                }
            }

            // Food icon + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FoodIconBadge(
                    iconKey = item.iconKey,
                    size = 32.dp,
                    tint = HealthColors.Primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val itemBrand = item.brand
                    if (!itemBrand.isNullOrBlank()) {
                        Text(
                            text = itemBrand,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Calorie & Portion badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = HealthColors.Primary.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.calories} kcal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = HealthColors.Primary
                    )
                    Text(
                        text = if (isMix) {
                            effectiveIngredients.joinToString(" + ") { it.name.substringBefore(" ") }
                        } else {
                            item.portionUnit
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(start = 4.dp)
                    )
                }
            }

            // Macros mini-row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "P: ${item.proteinG.roundToInt()}g",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = HealthColors.NutrientProtein
                )
                Text(
                    text = str("K: ${item.carbsG.roundToInt()}g", "C: ${item.carbsG.roundToInt()}g"),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = HealthColors.NutrientCarbs
                )
                Text(
                    text = str("Y: ${item.fatG.roundToInt()}g", "F: ${item.fatG.roundToInt()}g"),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = HealthColors.NutrientLipids
                )
            }

            // Action hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = str("Ayarla & Ekle", "Adjust & Add"),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = HealthColors.Primary
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = HealthColors.Primary,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

private data class ScaledIngredientPreview(
    val ingredient: CustomFoodIngredient,
    val amount: Double,
    val formattedPortion: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val micronutrients: List<Micronutrient>
)

@Composable
private fun StudioMacroPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) HealthColors.SurfaceContainerLowest.copy(alpha = 0.75f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IngredientLevelWidgetCard(
    ingredient: CustomFoodIngredient,
    isEnabled: Boolean,
    showToggle: Boolean,
    currentAmount: Double,
    scaledCalories: Int,
    scaledProtein: Double,
    scaledCarbs: Double,
    scaledFat: Double,
    onToggleEnabled: () -> Unit,
    onSelectAmount: (Double) -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val levels = remember(ingredient) {
        if (ingredient.presetLevels.isNotEmpty()) {
            ingredient.presetLevels
        } else {
            listOf(
                ingredient.defaultAmount * 0.5,
                ingredient.defaultAmount,
                ingredient.defaultAmount * 1.5,
                ingredient.defaultAmount * 2.0
            ).distinct()
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = if (isDark) HealthColors.SurfaceContainer else Color.White,
        borderGradient = if (isDark) null else listOf(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), HealthColors.Primary.copy(alpha = 0.2f)),
        elevation = if (isEnabled) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Checkbox/Icon + Name + Live Scaled Macros + Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (showToggle) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    if (isEnabled) HealthColors.Primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                                .clickable { onToggleEnabled() }
                                .bounceClick(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isEnabled) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = str("Seçili", "Selected"),
                                    tint = HealthColors.OnPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    if (ingredient.iconKey != null) {
                        FoodIconBadge(
                            iconKey = ingredient.iconKey,
                            size = 30.dp,
                            tint = if (isEnabled) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ingredient.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isEnabled) {
                                str(
                                    "$scaledCalories kcal • ${scaledProtein}g P • ${scaledCarbs}g K • ${scaledFat}g Y",
                                    "$scaledCalories kcal • ${scaledProtein}g P • ${scaledCarbs}g C • ${scaledFat}g F"
                                )
                            } else {
                                str("Bu öğünde devre dışı (eklenmeyecek)", "Disabled for this meal (won't be added)")
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (isEnabled) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Fine-Tune Stepper: [-] 200 ml [+]
                if (isEnabled) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) HealthColors.SurfaceContainerLowest else HealthColors.SurfaceVariantLight)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val next = (currentAmount - ingredient.stepAmount).coerceAtLeast(ingredient.stepAmount)
                                onSelectAmount(next)
                            },
                            modifier = Modifier.size(26.dp).bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = str("Azalt", "Decrease"),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val stepAmt = ingredient.stepAmount.takeIf { it > 0.0 } ?: ingredient.defaultAmount.coerceAtLeast(1.0)
                        val curMult = currentAmount / stepAmt
                        val curMultInt = curMult.roundToInt()
                        val showCurMult = curMult >= 1.95 && kotlin.math.abs(curMult - curMultInt) < 0.08 && !ingredient.unitLabel.equals("Shot", ignoreCase = true)
                        val stepperText = if (showCurMult) {
                            "${formatAmountClean(currentAmount)}${ingredient.unitLabel} (${curMultInt}x)"
                        } else {
                            "${formatAmountClean(currentAmount)} ${ingredient.unitLabel}"
                        }
                        Text(
                            text = stepperText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = HealthColors.Primary,
                            maxLines = 1,
                            softWrap = false
                        )

                        IconButton(
                            onClick = {
                                onSelectAmount(currentAmount + ingredient.stepAmount)
                            },
                            modifier = Modifier.size(26.dp).bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = str("Artır", "Increase"),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Quick-Level Selector Chips Row (e.g. [1 Shot] [2 Shot] [3 Shot] or [50 ml] [100 ml] [200 ml])
            AnimatedVisibility(
                visible = isEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    levels.forEach { levelValue ->
                        val isSelectedLevel = kotlin.math.abs(currentAmount - levelValue) < 0.01
                        val stepAmt = ingredient.stepAmount.takeIf { it > 0.0 } ?: ingredient.defaultAmount.coerceAtLeast(1.0)
                        val lvlMult = levelValue / stepAmt
                        val lvlMultInt = lvlMult.roundToInt()
                        val showLvlMult = lvlMult >= 1.95 && kotlin.math.abs(lvlMult - lvlMultInt) < 0.08 && !ingredient.unitLabel.equals("Shot", ignoreCase = true)
                        val chipText = if (showLvlMult) {
                            "${formatAmountClean(levelValue)} ${ingredient.unitLabel} (${lvlMultInt}x)"
                        } else {
                            "${formatAmountClean(levelValue)} ${ingredient.unitLabel}"
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelectedLevel) {
                                HealthColors.Primary
                            } else {
                                if (isDark) HealthColors.SurfaceContainerLowest
                                else HealthColors.SurfaceVariantLight
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectAmount(levelValue) }
                                .bounceClick()
                        ) {
                            Text(
                                text = chipText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelectedLevel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelectedLevel) HealthColors.OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeOptionChip(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            HealthColors.Primary.copy(alpha = 0.18f)
        } else {
            if (isDark) HealthColors.SurfaceContainerLowest.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        },
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 1.5.dp else 0.5.dp,
                color = if (selected) HealthColors.Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .bounceClick()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) HealthColors.Primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

