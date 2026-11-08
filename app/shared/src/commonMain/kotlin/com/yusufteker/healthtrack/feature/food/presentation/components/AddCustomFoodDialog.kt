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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yusufteker.healthtrack.core.model.CustomFoodIngredient
import com.yusufteker.healthtrack.core.model.CustomFoodItem
import com.yusufteker.healthtrack.core.model.Micronutrient
import com.yusufteker.healthtrack.core.network.FoodAiResultDto
import com.yusufteker.healthtrack.core.utils.rememberSpeechToTextLauncher
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import kotlin.math.roundToInt

private data class DynamicMicroItem(
    val id: String,
    var name: String = "",
    var amountText: String = "",
    var unit: String = "mg"
)

private data class DraftIngredientRow(
    val id: String,
    val name: String = "",
    val unitLabel: String = "Ölçek",
    val defaultAmountText: String = "1",
    val caloriesText: String = "",
    val proteinText: String = "",
    val carbsText: String = "",
    val fatText: String = "",
    val fiberG: Double = 0.0,
    val micronutrients: List<Micronutrient> = emptyList(),
    val iconKey: String? = null,
    val quantity: Int = 1,
    val unitBaseAmount: Double = 0.0,
    val unitBaseCalories: Int = 0,
    val unitBaseProtein: Double = 0.0,
    val unitBaseCarbs: Double = 0.0,
    val unitBaseFat: Double = 0.0,
    val unitBaseFiber: Double = 0.0,
    val unitBaseMicronutrients: List<Micronutrient> = emptyList()
)

private fun scaleDraftIngredient(row: DraftIngredientRow, newQuantity: Int): DraftIngredientRow {
    val safeOldQty = row.quantity.coerceAtLeast(1)
    val safeNewQty = newQuantity.coerceAtLeast(1)

    val currentAmt = row.defaultAmountText.replace(',', '.').toDoubleOrNull() ?: 1.0
    val baseAmt = if (row.unitBaseAmount > 0.0) row.unitBaseAmount else (currentAmt / safeOldQty).coerceAtLeast(0.1)

    val currentCal = row.caloriesText.toIntOrNull() ?: 0
    val baseCal = if (row.unitBaseCalories > 0) row.unitBaseCalories else (currentCal.toDouble() / safeOldQty).roundToInt()

    val currentProt = row.proteinText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val baseProt = if (row.unitBaseProtein > 0.0) row.unitBaseProtein else currentProt / safeOldQty

    val currentCarb = row.carbsText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val baseCarb = if (row.unitBaseCarbs > 0.0) row.unitBaseCarbs else currentCarb / safeOldQty

    val currentFat = row.fatText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val baseFat = if (row.unitBaseFat > 0.0) row.unitBaseFat else currentFat / safeOldQty

    val baseFib = if (row.unitBaseFiber > 0.0) row.unitBaseFiber else row.fiberG / safeOldQty
    val baseMicros = row.unitBaseMicronutrients.ifEmpty {
        row.micronutrients.map { it.copy(amount = it.amount / safeOldQty) }
    }

    return row.copy(
        quantity = safeNewQty,
        unitBaseAmount = baseAmt,
        unitBaseCalories = baseCal,
        unitBaseProtein = baseProt,
        unitBaseCarbs = baseCarb,
        unitBaseFat = baseFat,
        unitBaseFiber = baseFib,
        unitBaseMicronutrients = baseMicros,
        defaultAmountText = formatNumClean(baseAmt * safeNewQty),
        caloriesText = (baseCal * safeNewQty).toString(),
        proteinText = formatNumClean(baseProt * safeNewQty),
        carbsText = formatNumClean(baseCarb * safeNewQty),
        fatText = formatNumClean(baseFat * safeNewQty),
        fiberG = ((baseFib * safeNewQty * 10.0).roundToInt()) / 10.0,
        micronutrients = baseMicros.map { m ->
            m.copy(amount = ((m.amount * safeNewQty * 10.0).roundToInt()) / 10.0)
        }
    )
}

private fun formatNumClean(value: Double): String {
    val rounded = (value * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

private fun inferFoodIconKey(name: String): String? {
    val lower = name.lowercase()
    return when {
        lower.contains("kahve") || lower.contains("espresso") || lower.contains("latte") || lower.contains("americano") -> "coffee"
        lower.contains("protein") || lower.contains("whey") || lower.contains("toz") -> "protein_powder"
        lower.contains("kreatin") || lower.contains("creatine") || lower.contains("carnitine") || lower.contains("karnitin") || lower.contains("bcaa") -> "creatine"
        lower.contains("süt") || lower.contains("sut") || lower.contains("kefir") || lower.contains("ayran") -> "milk"
        lower.contains("yulaf") || lower.contains("granola") || lower.contains("müsli") -> "oats"
        lower.contains("yumurta") -> "egg"
        lower.contains("tavuk") || lower.contains("hindi") -> "chicken"
        lower.contains("et") || lower.contains("köfte") || lower.contains("biftek") -> "meat"
        lower.contains("muz") || lower.contains("elma") || lower.contains("meyve") -> "fruit"
        else -> null
    }
}

/**
 * Extracts (unitLabel, defaultAmountText) from AI formattedPortion or user prompt.
 * E.g. "150 ml" -> ("ml", "150"), "2 shot" -> ("Shot", "2"), "1 ölçek (35g)" -> ("Ölçek", "1").
 */
private fun parseIngredientPortion(
    portionText: String,
    itemName: String = "",
    fullPrompt: String = ""
): Pair<String, String> {
    val combined = portionText.trim()
    val lower = combined.lowercase()
    val promptLower = fullPrompt.lowercase()
    val firstWord = itemName.trim().lowercase().split(" ").firstOrNull { it.length >= 3 } ?: ""

    // 1. Check explicit Shot
    Regex("""(\d+(?:[.,]\d+)?)\s*shot""").find(lower)?.let { m ->
        return "Shot" to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0)
    }
    if (firstWord.isNotBlank()) {
        Regex("""(\d+(?:[.,]\d+)?)\s*shot\s+[^,.\n]*$firstWord""").find(promptLower)?.let { m ->
            return "Shot" to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0)
        }
    }

    // 2. Check explicit Ölçek / Scoop
    Regex("""(\d+(?:[.,]\d+)?)\s*(?:ölçek|olcek|scoop)""").find(lower)?.let { m ->
        return "Ölçek" to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0)
    }
    if (firstWord.isNotBlank()) {
        Regex("""(\d+(?:[.,]\d+)?)\s*(?:ölçek|olcek|scoop)\s+[^,.\n]*$firstWord""").find(promptLower)?.let { m ->
            return "Ölçek" to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0)
        }
    }

    // 3. Check ml
    Regex("""(\d+(?:[.,]\d+)?)\s*(?:ml|mililitre)""").find(lower)?.let { m ->
        return "ml" to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 100.0)
    }

    // 4. Check g / gr / gram
    Regex("""(\d+(?:[.,]\d+)?)\s*(?:g|gr|gram)\b""").find(lower)?.let { m ->
        return "g" to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 50.0)
    }

    // 5. Check Kaşık
    Regex("""(\d+(?:[.,]\d+)?)\s*(?:yemek kaşığı|tatlı kaşığı|çay kaşığı|kaşık|kasik)""").find(lower)?.let { m ->
        return "Kaşık" to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0)
    }

    // 6. Check Dilim
    Regex("""(\d+(?:[.,]\d+)?)\s*dilim""").find(lower)?.let { m ->
        return "Dilim" to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0)
    }

    // 7. Check Adet
    Regex("""(\d+(?:[.,]\d+)?)\s*adet""").find(lower)?.let { m ->
        return "Adet" to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0)
    }

    // Fallback: extract leading number if any
    val fallbackNum = Regex("""(\d+(?:[.,]\d+)?)""").find(lower)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull() ?: 1.0
    return "Adet" to formatNumClean(fallbackNum)
}

/**
 * Formats single product portion nicely when user specifies both scoop/ölçek and gram/ml
 * (e.g., "1 ölçek protein tozu 35 gr" -> "1 Ölçek (35g)", "1 ölçek l carnitine 30ml" -> "1 Ölçek (30ml)").
 */
private fun buildSingleProductPortionUnit(aiPortion: String, rawPrompt: String): String {
    val pLower = rawPrompt.lowercase()
    val scoopMatch = Regex("""(\d+(?:[.,]\d+)?)\s*(?:ölçek|olcek|scoop)""").find(pLower)
    val weightOrVolMatch = Regex("""(\d+(?:[.,]\d+)?)\s*(g|gr|gram|ml)\b""").find(pLower)

    if (scoopMatch != null && weightOrVolMatch != null) {
        val scoopAmt = scoopMatch.groupValues[1]
        val wAmt = weightOrVolMatch.groupValues[1]
        val wUnit = if (weightOrVolMatch.groupValues[2].startsWith("m")) "ml" else "g"
        return "$scoopAmt Ölçek ($wAmt$wUnit)"
    }
    if (scoopMatch != null && !aiPortion.lowercase().contains("ölçek")) {
        val scoopAmt = scoopMatch.groupValues[1]
        return if (aiPortion.isNotBlank()) "$scoopAmt Ölçek ($aiPortion)" else "$scoopAmt Ölçek"
    }
    return aiPortion.ifBlank { "1 Porsiyon" }
}

private fun presetLevelsForUnit(unitLabel: String, defaultAmt: Double): List<Double> {
    val u = unitLabel.lowercase()
    return when {
        u.contains("shot") -> listOf(1.0, 2.0, 3.0, 4.0)
        u.contains("ölçek") || u.contains("scoop") -> listOf(0.5, 1.0, 1.5, 2.0)
        u.contains("ml") -> listOf(50.0, 100.0, 150.0, 200.0, 250.0, 300.0)
        u.contains("g") -> listOf(20.0, 30.0, 50.0, 100.0, 150.0, 200.0)
        u.contains("kaşık") -> listOf(0.5, 1.0, 2.0, 3.0)
        else -> listOf(0.5, 1.0, 1.5, 2.0, 3.0)
    }.let { list ->
        if (defaultAmt > 0 && list.none { kotlin.math.abs(it - defaultAmt) < 0.01 }) {
            (list + defaultAmt).sorted()
        } else list
    }
}

private fun stepForUnit(unitLabel: String): Double {
    val u = unitLabel.lowercase()
    return when {
        u.contains("ml") -> 50.0
        u.contains("g") -> 10.0
        u.contains("shot") -> 1.0
        u.contains("adet") || u.contains("dilim") -> 1.0
        else -> 0.5
    }
}

@Composable
fun AddCustomFoodDialog(
    existingCustomFoods: List<CustomFoodItem> = emptyList(),
    isAnalyzingAi: Boolean = false,
    aiResult: FoodAiResultDto? = null,
    aiPromptUsed: String = "",
    aiError: String? = null,
    onAnalyzeWithAi: ((prompt: String, isMixMode: Boolean) -> Unit)? = null,
    onConsumeAiResult: (() -> Unit)? = null,
    onDismissAiError: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        brand: String?,
        portionUnit: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        fiberG: Double,
        categoryName: String,
        makeFavorite: Boolean,
        iconKey: String?,
        micronutrients: List<Micronutrient>,
        ingredients: List<CustomFoodIngredient>
    ) -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val speechLauncher = rememberSpeechToTextLauncher()
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f)
    val calmFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = HealthColors.Primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)
    )

    var isMixMode by remember { mutableStateOf(false) }
    var aiPromptText by remember { mutableStateOf("") }
    var aiSuccessMessage by remember { mutableStateOf<String?>(null) }

    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf<String?>(null) }
    var portionUnit by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }
    var fiberText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FoodCategory.PROTEIN) }
    var makeFavorite by remember { mutableStateOf(true) }

    // Multi-ingredient state (starts empty, no pre-filled suggestions)
    val draftIngredients = remember {
        mutableStateListOf(
            DraftIngredientRow(
                id = "ing_1",
                name = "",
                unitLabel = "Ölçek",
                defaultAmountText = "1",
                caloriesText = ""
            ),
            DraftIngredientRow(
                id = "ing_2",
                name = "",
                unitLabel = "ml",
                defaultAmountText = "100",
                caloriesText = ""
            )
        )
    }

    // Micronutrient States (Optional)
    var isMicrosExpanded by remember { mutableStateOf(false) }
    var vitDText by remember { mutableStateOf("") }
    var vitCText by remember { mutableStateOf("") }
    var vitB12Text by remember { mutableStateOf("") }
    var ironText by remember { mutableStateOf("") }
    var calciumText by remember { mutableStateOf("") }
    var magnesiumText by remember { mutableStateOf("") }
    var zincText by remember { mutableStateOf("") }
    var omega3Text by remember { mutableStateOf("") }
    var potassiumText by remember { mutableStateOf("") }
    var sodiumText by remember { mutableStateOf("") }

    val dynamicMicros = remember { mutableStateListOf<DynamicMicroItem>() }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Helper to merge a list of Micronutrients into the dialog's standard & dynamic fields
    fun applyMicronutrientsToFields(micros: List<Micronutrient>, accumulate: Boolean = false) {
        if (micros.isEmpty()) return
        fun addOrSet(current: String, incoming: Double): String {
            val base = if (accumulate) (current.replace(',', '.').toDoubleOrNull() ?: 0.0) else 0.0
            val sum = base + incoming
            return if (sum > 0.0) formatNumClean(sum) else current
        }
        if (!accumulate) {
            dynamicMicros.clear()
        }
        micros.forEach { m ->
            val n = m.name.trim().lowercase()
            when {
                n.contains("d vit") || n == "vitamin d" -> vitDText = addOrSet(vitDText, m.amount)
                n.contains("c vit") || n == "vitamin c" -> vitCText = addOrSet(vitCText, m.amount)
                n.contains("b12") -> vitB12Text = addOrSet(vitB12Text, m.amount)
                n.contains("demir") || n.contains("iron") -> ironText = addOrSet(ironText, m.amount)
                n.contains("kalsiyum") || n.contains("calcium") -> calciumText = addOrSet(calciumText, m.amount)
                n.contains("magnezyum") || n.contains("magnesium") -> magnesiumText = addOrSet(magnesiumText, m.amount)
                n.contains("çinko") || n.contains("cinko") || n.contains("zinc") -> zincText = addOrSet(zincText, m.amount)
                n.contains("omega") -> omega3Text = addOrSet(omega3Text, m.amount)
                n.contains("potasyum") || n.contains("potassium") -> potassiumText = addOrSet(potassiumText, m.amount)
                n.contains("sodyum") || n.contains("sodium") -> sodiumText = addOrSet(sodiumText, m.amount)
                else -> {
                    val existingIdx = dynamicMicros.indexOfFirst { it.name.equals(m.name.trim(), ignoreCase = true) }
                    if (existingIdx >= 0) {
                        val cur = dynamicMicros[existingIdx]
                        val newAmt = (cur.amountText.replace(',', '.').toDoubleOrNull() ?: 0.0) + m.amount
                        dynamicMicros[existingIdx] = cur.copy(amountText = formatNumClean(newAmt))
                    } else if (m.amount > 0.0) {
                        dynamicMicros.add(
                            DynamicMicroItem(
                                id = "dyn_${currentTime()}_${dynamicMicros.size}",
                                name = m.name.trim(),
                                amountText = formatNumClean(m.amount),
                                unit = m.unit.trim().ifBlank { "mg" }
                            )
                        )
                    }
                }
            }
        }
        if (micros.any { it.amount > 0.0 }) {
            isMicrosExpanded = true
        }
    }

    // Consume AI analysis result and populate either Mix Mode or Single Product Mode
    LaunchedEffect(aiResult) {
        val result = aiResult ?: return@LaunchedEffect
        val items = result.items
        if (items.isNotEmpty()) {
            val promptReference = aiPromptUsed.ifBlank { aiPromptText }
            if (isMixMode) {
                // Convert each AI item into a DraftIngredientRow with smart unit & amount detection
                val aiRows = items.mapIndexed { idx, item ->
                    val (detectedUnit, detectedAmt) = parseIngredientPortion(
                        portionText = item.formattedPortion,
                        itemName = item.name,
                        fullPrompt = promptReference
                    )
                    val parsedBaseAmt = detectedAmt.replace(',', '.').toDoubleOrNull() ?: 1.0
                    DraftIngredientRow(
                        id = "ing_ai_${currentTime()}_$idx",
                        name = item.name,
                        unitLabel = detectedUnit,
                        defaultAmountText = detectedAmt,
                        caloriesText = item.calories.toString(),
                        proteinText = formatNumClean(item.proteinG),
                        carbsText = formatNumClean(item.carbsG),
                        fatText = formatNumClean(item.fatG),
                        fiberG = item.fiberG,
                        micronutrients = item.micronutrients,
                        iconKey = item.iconKey ?: inferFoodIconKey(item.name),
                        quantity = 1,
                        unitBaseAmount = parsedBaseAmt,
                        unitBaseCalories = item.calories,
                        unitBaseProtein = item.proteinG,
                        unitBaseCarbs = item.carbsG,
                        unitBaseFat = item.fatG,
                        unitBaseFiber = item.fiberG,
                        unitBaseMicronutrients = item.micronutrients
                    )
                }
                val nonBlankExisting = draftIngredients.filter { it.name.isNotBlank() || it.caloriesText.isNotBlank() }.toMutableList()
                aiRows.forEach { incoming ->
                    val matchIdx = nonBlankExisting.indexOfFirst {
                        it.name.trim().equals(incoming.name.trim(), ignoreCase = true)
                    }
                    if (matchIdx >= 0) {
                        val cur = nonBlankExisting[matchIdx]
                        nonBlankExisting[matchIdx] = scaleDraftIngredient(cur, cur.quantity + incoming.quantity)
                    } else {
                        nonBlankExisting.add(incoming)
                    }
                }
                draftIngredients.clear()
                draftIngredients.addAll(nonBlankExisting)

                if (name.isBlank()) {
                    name = if (items.size <= 3) {
                        items.joinToString(" + ") { it.name }
                    } else {
                        result.summary.take(35).ifBlank { "Özel Karışım" }
                    }
                }
                if (selectedIconKey == null) {
                    selectedIconKey = aiRows.firstNotNullOfOrNull { it.iconKey }
                }
                val combinedMicros = items.flatMap { it.micronutrients }
                applyMicronutrientsToFields(combinedMicros, accumulate = nonBlankExisting.isNotEmpty())
                aiSuccessMessage = "AI ${items.size} içeriği birim, miktar ve makrolarıyla otomatik doldurdu."
            } else {
                // Single Product Mode: combine items into one product
                val first = items.first()
                if (name.isBlank()) {
                    name = if (items.size == 1) first.name else items.joinToString(" & ") { it.name }
                }
                portionUnit = buildSingleProductPortionUnit(
                    aiPortion = first.formattedPortion,
                    rawPrompt = promptReference
                )
                caloriesText = items.sumOf { it.calories }.toString()
                proteinText = formatNumClean(items.sumOf { it.proteinG })
                carbsText = formatNumClean(items.sumOf { it.carbsG })
                fatText = formatNumClean(items.sumOf { it.fatG })
                val totalFib = items.sumOf { it.fiberG }
                if (totalFib > 0.0) {
                    fiberText = formatNumClean(totalFib)
                }
                if (selectedIconKey == null) {
                    selectedIconKey = first.iconKey ?: inferFoodIconKey(first.name)
                }
                val allMicros = items.flatMap { it.micronutrients }
                applyMicronutrientsToFields(allMicros, accumulate = false)
                aiSuccessMessage = "AI ürünün birim, kalori, makro ve mikro besin değerlerini doldurdu."
            }
            errorMessage = null
        }
        onConsumeAiResult?.invoke()
    }

    val commonPortionSuggestions = listOf(
        "100g",
        "500ml (1 Kutu)",
        "250ml (1 Bardak)",
        "1 Adet",
        "1 Porsiyon",
        "1 Dilim",
        "1 Ölçek (30g)",
        "1 Shot"
    )

    val unitSuggestions = listOf("Shot", "Ölçek", "ml", "g", "Adet", "Dilim", "Kaşık")

    val enteredStandardMicrosCount = listOf(
        vitDText, vitCText, vitB12Text, ironText, calciumText,
        magnesiumText, zincText, omega3Text, potassiumText, sodiumText
    ).count { (it.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0 }
    val enteredDynamicMicrosCount = dynamicMicros.count {
        it.name.isNotBlank() && (it.amountText.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0
    }
    val totalEnteredMicros = enteredStandardMicrosCount + enteredDynamicMicrosCount

    // Live computed totals for Mix Mode
    val mixTotalCalories = draftIngredients.sumOf { it.caloriesText.toIntOrNull() ?: 0 }
    val mixTotalProtein = draftIngredients.sumOf { it.proteinText.replace(',', '.').toDoubleOrNull() ?: 0.0 }
    val mixTotalCarbs = draftIngredients.sumOf { it.carbsText.replace(',', '.').toDoubleOrNull() ?: 0.0 }
    val mixTotalFat = draftIngredients.sumOf { it.fatText.replace(',', '.').toDoubleOrNull() ?: 0.0 }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .padding(14.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                elevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with Icon & Close
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
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(HealthColors.Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isMixMode) Icons.Default.Tune else Icons.Default.AddBusiness,
                                    contentDescription = null,
                                    tint = HealthColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Envanterime Ekle",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isMixMode) "İçerik seviyelerini seçerken ayrı ayrı değiştirebilirsiniz" else "Kendi ürünlerinizi veya çoklu içerikli öğünlerinizi kaydedin",
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

                    // ── Mode Switcher: Tekli Ürün vs Çoklu İçerik ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) HealthColors.SurfaceContainerLowest else HealthColors.SurfaceVariantLight)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (!isMixMode) {
                                        if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surface
                                    } else Color.Transparent
                                )
                                .clickable { isMixMode = false }
                                .padding(vertical = 8.dp)
                                .bounceClick(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = if (!isMixMode) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Tekli Ürün",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (!isMixMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isMixMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (isMixMode) {
                                        if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surface
                                    } else Color.Transparent
                                )
                                .clickable { isMixMode = true }
                                .padding(vertical = 8.dp)
                                .bounceClick(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = if (isMixMode) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Çoklu İçerik",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isMixMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isMixMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (errorMessage != null || aiError != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = errorMessage ?: aiError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (aiSuccessMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HealthColors.Primary.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                    text = aiSuccessMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HealthColors.Primary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { aiSuccessMessage = null },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Kapat",
                                        tint = HealthColors.Primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // ── ✨ AI Smart Fill Card (Works for BOTH Single Product & Multi-Ingredient Mix) ──
                    if (onAnalyzeWithAi != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.52f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.65f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = HealthColors.Primary.copy(alpha = 0.32f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = HealthColors.Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isMixMode) "AI ile Karışım & Birim Analizi" else "AI ile Ürün & Besin Değeri Doldur",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = HealthColors.Primary.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "Gemini AI",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = HealthColors.Primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = aiPromptText,
                                    onValueChange = {
                                        aiPromptText = it
                                        errorMessage = null
                                        onDismissAiError?.invoke()
                                    },
                                    placeholder = {
                                        Text(
                                            text = if (isMixMode) {
                                                "Örn: 150ml proteinli süt, 2 shot espresso, 1 ölçek protein tozu..."
                                            } else {
                                                "Örn: 1 ölçek protein tozu 35 gr, veya 1 ölçek L-Carnitine 30ml..."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = placeholderColor
                                        )
                                    },
                                    trailingIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(end = 4.dp)
                                        ) {
                                            if (aiPromptText.isNotBlank()) {
                                                IconButton(
                                                    onClick = { aiPromptText = "" },
                                                    modifier = Modifier.size(26.dp).bounceClick()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Temizle",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                            if (speechLauncher.isAvailable) {
                                                IconButton(
                                                    onClick = {
                                                        speechLauncher.launch("Ürün veya karışımı söyleyin...") { spoken ->
                                                            aiPromptText = if (aiPromptText.isBlank()) spoken else "$aiPromptText, $spoken"
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(HealthColors.Primary.copy(alpha = 0.15f))
                                                        .bounceClick()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Mic,
                                                        contentDescription = "Sesle Söyle",
                                                        tint = HealthColors.Primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = calmFieldColors,
                                    maxLines = 3
                                )

                                Button(
                                    onClick = {
                                        aiSuccessMessage = null
                                        onAnalyzeWithAi(aiPromptText, isMixMode)
                                    },
                                    enabled = !isAnalyzingAi && aiPromptText.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .bounceClick(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HealthColors.Primary,
                                        contentColor = HealthColors.OnPrimary
                                    )
                                ) {
                                    if (isAnalyzingAi) {
                                        CircularProgressIndicator(
                                            color = HealthColors.OnPrimary,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "AI Birim & Değerleri Analiz Ediyor...",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isMixMode) "AI ile İçerikleri & Birimleri Doldur" else "AI ile Besin Değerlerini Doldur",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Product / Mix Name
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isMixMode) "Karışım / Öğün Adı *" else "Ürün / Besin Adı *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                errorMessage = null
                            },
                            placeholder = {
                                Text(
                                    text = if (isMixMode) "Örn: Proteinli Sütlü Kahve, Sabah Yulaf Kasesi..." else "Örn: Kahveli Protein Süt, Kreatin...",
                                    color = placeholderColor
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Restaurant, contentDescription = null, tint = HealthColors.Primary, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = calmFieldColors,
                            singleLine = true
                        )
                    }

                    // Brand (Optional)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Marka / Etiket (İsteğe Bağlı)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            placeholder = {
                                Text(
                                    "Örn: Kendi Tarifim, Hardline, Pınar...",
                                    color = placeholderColor
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Sell, contentDescription = null, tint = HealthColors.Secondary, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = calmFieldColors,
                            singleLine = true
                        )
                    }

                    // ── Product Icon Selector (Optional) ──
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "İkon Seçimi (İsteğe Bağlı)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedIconKey != null) {
                                Text(
                                    text = "İkonu Kaldır",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HealthColors.Primary,
                                    modifier = Modifier
                                        .clickable { selectedIconKey = null }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        FoodIconSelector(
                            selectedKey = selectedIconKey,
                            onSelectKey = { selectedIconKey = it }
                        )
                    }

                    if (isMixMode) {
                        // ═══════════════════════════════════════════════════════════
                        // MIX MODE: Multi-Ingredient Builder + Existing Inventory Picker
                        // ═══════════════════════════════════════════════════════════
                        val singleInventoryFoods = remember(existingCustomFoods) {
                            existingCustomFoods.filter { !it.isMix }.ifEmpty { existingCustomFoods }
                        }

                        if (singleInventoryFoods.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Envanterimdeki Ürünlerden Ekle (${singleInventoryFoods.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Dokunarak Ekle",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HealthColors.Secondary
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    singleInventoryFoods.forEach { invFood ->
                                        val existingIdx = draftIngredients.indexOfFirst {
                                            it.name.trim().equals(invFood.name.trim(), ignoreCase = true)
                                        }
                                        val currentQty = if (existingIdx >= 0) draftIngredients[existingIdx].quantity else 0
                                        val addOrIncrementFromInventory = {
                                            if (existingIdx >= 0) {
                                                val existing = draftIngredients[existingIdx]
                                                draftIngredients[existingIdx] = scaleDraftIngredient(existing, existing.quantity + 1)
                                            } else {
                                                val (parsedUnit, parsedAmt) = parseIngredientPortion(
                                                    portionText = invFood.portionUnit,
                                                    itemName = invFood.name,
                                                    fullPrompt = invFood.portionUnit
                                                )
                                                val baseAmt = parsedAmt.replace(',', '.').toDoubleOrNull() ?: 1.0
                                                val newRow = DraftIngredientRow(
                                                    id = "ing_inv_${currentTime()}_${draftIngredients.size}",
                                                    name = invFood.name,
                                                    unitLabel = parsedUnit,
                                                    defaultAmountText = parsedAmt,
                                                    caloriesText = invFood.calories.toString(),
                                                    proteinText = formatNumClean(invFood.proteinG),
                                                    carbsText = formatNumClean(invFood.carbsG),
                                                    fatText = formatNumClean(invFood.fatG),
                                                    fiberG = invFood.fiberG,
                                                    micronutrients = invFood.micronutrients,
                                                    iconKey = invFood.iconKey ?: inferFoodIconKey(invFood.name),
                                                    quantity = 1,
                                                    unitBaseAmount = baseAmt,
                                                    unitBaseCalories = invFood.calories,
                                                    unitBaseProtein = invFood.proteinG,
                                                    unitBaseCarbs = invFood.carbsG,
                                                    unitBaseFat = invFood.fatG,
                                                    unitBaseFiber = invFood.fiberG,
                                                    unitBaseMicronutrients = invFood.micronutrients
                                                )
                                                val emptyIdx = draftIngredients.indexOfFirst {
                                                    it.name.isBlank() && it.caloriesText.isBlank()
                                                }
                                                if (emptyIdx >= 0) {
                                                    draftIngredients[emptyIdx] = newRow
                                                } else {
                                                    draftIngredients.add(newRow)
                                                }
                                            }
                                            if (invFood.micronutrients.isNotEmpty()) {
                                                applyMicronutrientsToFields(invFood.micronutrients, accumulate = true)
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (currentQty > 0) {
                                                HealthColors.Primary.copy(alpha = 0.16f)
                                            } else {
                                                if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.65f) else HealthColors.SurfaceVariantLight
                                            },
                                            modifier = Modifier
                                                .widthIn(min = 165.dp, max = 230.dp)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (currentQty > 0) {
                                                        HealthColors.Primary.copy(alpha = 0.45f)
                                                    } else {
                                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                                    },
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable { addOrIncrementFromInventory() }
                                                .bounceClick()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = invFood.name,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    if (currentQty > 0 && existingIdx >= 0) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(999.dp))
                                                                .background(HealthColors.Primary)
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Remove,
                                                                contentDescription = "Azalt",
                                                                tint = HealthColors.OnPrimary,
                                                                modifier = Modifier
                                                                    .size(14.dp)
                                                                    .clickable {
                                                                        val cur = draftIngredients[existingIdx]
                                                                        if (cur.quantity > 1) {
                                                                            draftIngredients[existingIdx] = scaleDraftIngredient(cur, cur.quantity - 1)
                                                                        } else if (draftIngredients.size > 1) {
                                                                            draftIngredients.removeAt(existingIdx)
                                                                        } else {
                                                                            draftIngredients[existingIdx] = DraftIngredientRow(id = cur.id)
                                                                        }
                                                                    }
                                                            )
                                                            Text(
                                                                text = "${currentQty}x",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = HealthColors.OnPrimary,
                                                                modifier = Modifier.padding(horizontal = 3.dp)
                                                            )
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = "Artır",
                                                                tint = HealthColors.OnPrimary,
                                                                modifier = Modifier
                                                                    .size(14.dp)
                                                                    .clickable { addOrIncrementFromInventory() }
                                                            )
                                                        }
                                                    } else {
                                                        Surface(
                                                            shape = RoundedCornerShape(999.dp),
                                                            color = HealthColors.Secondary.copy(alpha = 0.18f)
                                                        ) {
                                                            Text(
                                                                text = "+ Ekle",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                fontWeight = FontWeight.Bold,
                                                                color = HealthColors.Secondary,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                val displaySummary = if (currentQty > 1 && existingIdx >= 0) {
                                                    val r = draftIngredients[existingIdx]
                                                    "${r.defaultAmountText} ${r.unitLabel} (${currentQty}x) • ${r.caloriesText} kcal"
                                                } else {
                                                    "${invFood.portionUnit} • ${invFood.calories} kcal"
                                                }
                                                Text(
                                                    text = displaySummary,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${formatNumClean(invFood.proteinG * currentQty.coerceAtLeast(1))}g P • ${formatNumClean(invFood.carbsG * currentQty.coerceAtLeast(1))}g K • ${formatNumClean(invFood.fatG * currentQty.coerceAtLeast(1))}g Y" +
                                                        (if (invFood.micronutrients.isNotEmpty()) " • +${invFood.micronutrients.size} içerik" else ""),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = HealthColors.Primary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val uniqueIngredientCount = draftIngredients
                            .filter { it.name.isNotBlank() || it.caloriesText.isNotBlank() }
                            .distinctBy { it.name.trim().lowercase().ifBlank { it.id } }
                            .size
                            .coerceAtLeast(draftIngredients.size)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "İçerik Kalemleri ($uniqueIngredientCount)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = HealthColors.Primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Toplam: $mixTotalCalories kcal • ${((mixTotalProtein * 10).roundToInt() / 10.0)}g P",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HealthColors.Primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            draftIngredients.forEachIndexed { idx, ing ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.55f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "${idx + 1}. İçerik Kalemi",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = HealthColors.Primary
                                                )
                                                if (ing.quantity > 1) {
                                                    Surface(
                                                        shape = RoundedCornerShape(999.dp),
                                                        color = HealthColors.Primary.copy(alpha = 0.18f)
                                                    ) {
                                                        Text(
                                                            text = "${ing.defaultAmountText} ${ing.unitLabel} (${ing.quantity}x)",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            fontWeight = FontWeight.Bold,
                                                            color = HealthColors.Primary,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // [- / xN / +] Multiplier Stepper on every ingredient row
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(
                                                            if (isDark) HealthColors.SurfaceContainerLowest else MaterialTheme.colorScheme.surface
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = HealthColors.Primary.copy(alpha = 0.35f),
                                                            shape = RoundedCornerShape(999.dp)
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            if (ing.quantity > 1) {
                                                                draftIngredients[idx] = scaleDraftIngredient(ing, ing.quantity - 1)
                                                            }
                                                        },
                                                        enabled = ing.quantity > 1,
                                                        modifier = Modifier.size(22.dp).bounceClick()
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Remove,
                                                            contentDescription = "Azalt",
                                                            tint = if (ing.quantity > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = "${ing.quantity}x",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = HealthColors.Primary,
                                                        modifier = Modifier.padding(horizontal = 4.dp)
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            draftIngredients[idx] = scaleDraftIngredient(ing, ing.quantity + 1)
                                                        },
                                                        modifier = Modifier.size(22.dp).bounceClick()
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = "Artır",
                                                            tint = HealthColors.Primary,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }

                                                if (draftIngredients.size > 1) {
                                                    IconButton(
                                                        onClick = { draftIngredients.removeAt(idx) },
                                                        modifier = Modifier.size(24.dp).bounceClick()
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteOutline,
                                                            contentDescription = "İçeriği Sil",
                                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = ing.name,
                                            onValueChange = { draftIngredients[idx] = ing.copy(name = it) },
                                            placeholder = { Text("Örn: Kahve, Protein Tozu, Süt...", color = placeholderColor) },
                                            label = { Text("İçerik Adı") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = calmFieldColors,
                                            singleLine = true
                                        )

                                        // Unit Chips + Default Amount + Calories
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Birim:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            unitSuggestions.forEach { u ->
                                                val selected = ing.unitLabel.equals(u, ignoreCase = true)
                                                Surface(
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = if (selected) HealthColors.Primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                                    modifier = Modifier.clickable {
                                                        val defaultAmt = when (u) {
                                                            "ml" -> "100"
                                                            "g" -> "50"
                                                            else -> "1"
                                                        }
                                                        draftIngredients[idx] = ing.copy(unitLabel = u, defaultAmountText = defaultAmt)
                                                    }.bounceClick()
                                                ) {
                                                    Text(
                                                        text = u,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (selected) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = ing.defaultAmountText,
                                                onValueChange = { draftIngredients[idx] = ing.copy(defaultAmountText = it.replace(',', '.')) },
                                                placeholder = { Text("1", color = placeholderColor) },
                                                label = { Text("Varsayılan (${ing.unitLabel})") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = calmFieldColors,
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = ing.caloriesText,
                                                onValueChange = { draftIngredients[idx] = ing.copy(caloriesText = it.filter { ch -> ch.isDigit() }) },
                                                placeholder = { Text("0", color = placeholderColor) },
                                                label = { Text("Kalori (kcal)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = calmFieldColors,
                                                singleLine = true
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = ing.proteinText,
                                                onValueChange = { draftIngredients[idx] = ing.copy(proteinText = it.replace(',', '.')) },
                                                placeholder = { Text("0", color = placeholderColor) },
                                                label = { Text("Prot (g)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = calmFieldColors,
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = ing.carbsText,
                                                onValueChange = { draftIngredients[idx] = ing.copy(carbsText = it.replace(',', '.')) },
                                                placeholder = { Text("0", color = placeholderColor) },
                                                label = { Text("Karb (g)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = calmFieldColors,
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = ing.fatText,
                                                onValueChange = { draftIngredients[idx] = ing.copy(fatText = it.replace(',', '.')) },
                                                placeholder = { Text("0", color = placeholderColor) },
                                                label = { Text("Yağ (g)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = calmFieldColors,
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    draftIngredients.add(
                                        DraftIngredientRow(
                                            id = "ing_${currentTime()}_${draftIngredients.size}",
                                            name = "",
                                            unitLabel = "Adet",
                                            defaultAmountText = "1",
                                            caloriesText = ""
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().bounceClick(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Yeni İçerik Kalemi Ekle (Örn: Buz, Yulaf, Bal...)", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        // ═══════════════════════════════════════════════════════════
                        // SINGLE PRODUCT MODE
                        // ═══════════════════════════════════════════════════════════
                        // Portion Unit & Suggestions
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Porsiyon / Ambalaj Birimi *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = portionUnit,
                                onValueChange = {
                                    portionUnit = it
                                    errorMessage = null
                                },
                                placeholder = { Text("Örn: 500ml (1 Kutu), 100g, 1 Ölçek...", color = placeholderColor) },
                                leadingIcon = {
                                    Icon(Icons.Default.Straighten, contentDescription = null, tint = HealthColors.Secondary, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = calmFieldColors,
                                singleLine = true
                            )

                            // Quick chips for portion
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                commonPortionSuggestions.forEach { suggestion ->
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (portionUnit == suggestion) {
                                            HealthColors.Primary.copy(alpha = 0.2f)
                                        } else {
                                            if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight
                                        },
                                        modifier = Modifier
                                            .clickable { portionUnit = suggestion }
                                            .bounceClick()
                                    ) {
                                        Text(
                                            text = suggestion,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (portionUnit == suggestion) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Calorie (Required)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Porsiyon Kalorisi (kcal) *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = caloriesText,
                                onValueChange = {
                                    caloriesText = it.filter { ch -> ch.isDigit() }
                                    errorMessage = null
                                },
                                placeholder = { Text("Örn: 120", color = placeholderColor) },
                                leadingIcon = {
                                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = HealthColors.Tertiary, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    Text("kcal", style = MaterialTheme.typography.labelMedium, color = HealthColors.Tertiary, modifier = Modifier.padding(end = 12.dp))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = calmFieldColors,
                                singleLine = true
                            )
                        }

                        // Macros: Protein, Carbs, Fat
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Makro Değerleri (Porsiyon Başına)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = proteinText,
                                    onValueChange = { proteinText = it.replace(',', '.') },
                                    placeholder = { Text("0", color = placeholderColor) },
                                    label = { Text("Protein (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = calmFieldColors,
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = carbsText,
                                    onValueChange = { carbsText = it.replace(',', '.') },
                                    placeholder = { Text("0", color = placeholderColor) },
                                    label = { Text("Karb (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = calmFieldColors,
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = fatText,
                                    onValueChange = { fatText = it.replace(',', '.') },
                                    placeholder = { Text("0", color = placeholderColor) },
                                    label = { Text("Yağ (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = calmFieldColors,
                                    singleLine = true
                                )
                            }

                            // Fiber (Optional)
                            OutlinedTextField(
                                value = fiberText,
                                onValueChange = { fiberText = it.replace(',', '.') },
                                placeholder = { Text("0", color = placeholderColor) },
                                label = { Text("Lif (g, İsteğe Bağlı)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = calmFieldColors,
                                singleLine = true
                            )
                        }
                    }

                    // ── Micronutrients & Vitamins (Optional Expandable Section) ──
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.5f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (totalEnteredMicros > 0) HealthColors.Secondary.copy(alpha = 0.4f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isMicrosExpanded = !isMicrosExpanded }
                                    .padding(vertical = 4.dp),
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
                                            .background(HealthColors.Secondary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Biotech,
                                            contentDescription = null,
                                            tint = HealthColors.Secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Mikro Besin & Vitamin Değerleri",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (totalEnteredMicros > 0) "$totalEnteredMicros mikro besin değeri girildi" else "İsteğe bağlı - D Vit, Magnezyum, Çinko vb.",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = if (totalEnteredMicros > 0) HealthColors.Secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (totalEnteredMicros > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = HealthColors.Secondary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "$totalEnteredMicros",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = HealthColors.Secondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { isMicrosExpanded = !isMicrosExpanded },
                                        modifier = Modifier.size(28.dp).bounceClick()
                                    ) {
                                        Icon(
                                            imageVector = if (isMicrosExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isMicrosExpanded) "Gizle" else "Genişlet",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = isMicrosExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Porsiyon başına miktar giriniz. Boş bıraktığınız değerler sıfır kabul edilir.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Vitamin D & Vitamin C
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = vitDText,
                                            onValueChange = { vitDText = it.replace(',', '.') },
                                            placeholder = { Text("0", color = placeholderColor) },
                                            label = { Text("D Vitamini (µg)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = calmFieldColors,
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = vitCText,
                                            onValueChange = { vitCText = it.replace(',', '.') },
                                            placeholder = { Text("0", color = placeholderColor) },
                                            label = { Text("C Vitamini (mg)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = calmFieldColors,
                                            singleLine = true
                                        )
                                    }

                                    // Vitamin B12 & Iron
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = vitB12Text,
                                            onValueChange = { vitB12Text = it.replace(',', '.') },
                                            placeholder = { Text("0", color = placeholderColor) },
                                            label = { Text("B12 Vitamini (µg)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = calmFieldColors,
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = ironText,
                                            onValueChange = { ironText = it.replace(',', '.') },
                                            placeholder = { Text("0", color = placeholderColor) },
                                            label = { Text("Demir (mg)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = calmFieldColors,
                                            singleLine = true
                                        )
                                    }

                                    // Magnesium & Calcium
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = magnesiumText,
                                            onValueChange = { magnesiumText = it.replace(',', '.') },
                                            placeholder = { Text("0", color = placeholderColor) },
                                            label = { Text("Magnezyum (mg)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = calmFieldColors,
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = calciumText,
                                            onValueChange = { calciumText = it.replace(',', '.') },
                                            placeholder = { Text("0", color = placeholderColor) },
                                            label = { Text("Kalsiyum (mg)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = calmFieldColors,
                                            singleLine = true
                                        )
                                    }

                                    // Zinc & Omega 3
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = zincText,
                                            onValueChange = { zincText = it.replace(',', '.') },
                                            placeholder = { Text("0", color = placeholderColor) },
                                            label = { Text("Çinko (mg)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = calmFieldColors,
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = omega3Text,
                                            onValueChange = { omega3Text = it.replace(',', '.') },
                                            placeholder = { Text("0", color = placeholderColor) },
                                            label = { Text("Omega-3 (mg)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = calmFieldColors,
                                            singleLine = true
                                        )
                                    }

                                    // Potassium & Sodium
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = potassiumText,
                                            onValueChange = { potassiumText = it.replace(',', '.') },
                                            placeholder = { Text("0", color = placeholderColor) },
                                            label = { Text("Potasyum (mg)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = calmFieldColors,
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = sodiumText,
                                            onValueChange = { sodiumText = it.replace(',', '.') },
                                            placeholder = { Text("0", color = placeholderColor) },
                                            label = { Text("Sodyum (mg)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = calmFieldColors,
                                            singleLine = true
                                        )
                                    }

                                    // Dynamic extra nutrients list
                                    dynamicMicros.forEachIndexed { index, item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = item.name,
                                                onValueChange = { dynamicMicros[index] = item.copy(name = it) },
                                                placeholder = { Text("Örn: Kollajen", color = placeholderColor) },
                                                label = { Text("Besin Adı") },
                                                modifier = Modifier.weight(1.3f),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = calmFieldColors,
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = item.amountText,
                                                onValueChange = { dynamicMicros[index] = item.copy(amountText = it.replace(',', '.')) },
                                                placeholder = { Text("0", color = placeholderColor) },
                                                label = { Text("Miktar") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(0.9f),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = calmFieldColors,
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = item.unit,
                                                onValueChange = { dynamicMicros[index] = item.copy(unit = it) },
                                                placeholder = { Text("mg", color = placeholderColor) },
                                                label = { Text("Birim") },
                                                modifier = Modifier.weight(0.8f),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = calmFieldColors,
                                                singleLine = true
                                            )
                                            IconButton(
                                                onClick = { dynamicMicros.removeAt(index) },
                                                modifier = Modifier.size(28.dp).bounceClick()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Sil",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Add Dynamic Micro Button
                                    TextButton(
                                        onClick = {
                                            dynamicMicros.add(
                                                DynamicMicroItem(
                                                    id = "dyn_${currentTime()}_${dynamicMicros.size}",
                                                    name = "",
                                                    amountText = "",
                                                    unit = "mg"
                                                )
                                            )
                                        },
                                        modifier = Modifier.bounceClick()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Farklı Bir Mikro Besin / Vitamin Ekle",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = HealthColors.Secondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Category Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Kategori",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                FoodCategory.PROTEIN,
                                FoodCategory.BREAKFAST,
                                FoodCategory.CARBS,
                                FoodCategory.FRUITS_NUTS,
                                FoodCategory.HEALTHY_FATS
                            ).forEach { cat ->
                                val isSelected = selectedCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isSelected) {
                                        HealthColors.Primary.copy(alpha = 0.2f)
                                    } else {
                                        if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight
                                    },
                                    modifier = Modifier
                                        .clickable { selectedCategory = cat }
                                        .bounceClick()
                                ) {
                                    Text(
                                        text = cat.labelTr,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Favorite Toggle Checkbox / Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.4f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.6f))
                            .clickable { makeFavorite = !makeFavorite }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (makeFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (makeFavorite) HealthColors.Tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Favori Listeme de Ekle",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = makeFavorite,
                            onCheckedChange = { makeFavorite = it }
                        )
                    }

                    // Save Button
                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            if (trimmedName.isBlank()) {
                                errorMessage = "Lütfen ürün veya karışım adını giriniz."
                                return@Button
                            }

                            // Collect all micronutrients
                            val microsList = mutableListOf<Micronutrient>()
                            vitDText.toDoubleOrNull()?.takeIf { it > 0 }?.let { microsList.add(Micronutrient("D Vitamini", it, "µg")) }
                            vitCText.toDoubleOrNull()?.takeIf { it > 0 }?.let { microsList.add(Micronutrient("C Vitamini", it, "mg")) }
                            vitB12Text.toDoubleOrNull()?.takeIf { it > 0 }?.let { microsList.add(Micronutrient("B12 Vitamini", it, "µg")) }
                            ironText.toDoubleOrNull()?.takeIf { it > 0 }?.let { microsList.add(Micronutrient("Demir", it, "mg")) }
                            calciumText.toDoubleOrNull()?.takeIf { it > 0 }?.let { microsList.add(Micronutrient("Kalsiyum", it, "mg")) }
                            magnesiumText.toDoubleOrNull()?.takeIf { it > 0 }?.let { microsList.add(Micronutrient("Magnezyum", it, "mg")) }
                            zincText.toDoubleOrNull()?.takeIf { it > 0 }?.let { microsList.add(Micronutrient("Çinko", it, "mg")) }
                            omega3Text.toDoubleOrNull()?.takeIf { it > 0 }?.let { microsList.add(Micronutrient("Omega-3", it, "mg")) }
                            potassiumText.toDoubleOrNull()?.takeIf { it > 0 }?.let { microsList.add(Micronutrient("Potasyum", it, "mg")) }
                            sodiumText.toDoubleOrNull()?.takeIf { it > 0 }?.let { microsList.add(Micronutrient("Sodyum", it, "mg")) }

                            dynamicMicros.forEach { extra ->
                                val amt = extra.amountText.replace(',', '.').toDoubleOrNull()
                                if (extra.name.isNotBlank() && amt != null && amt > 0) {
                                    microsList.add(Micronutrient(extra.name.trim(), amt, extra.unit.trim().ifBlank { "mg" }))
                                }
                            }

                            if (isMixMode) {
                                val rawRows = draftIngredients.filter { it.name.trim().isNotBlank() }
                                val builtIngredients = rawRows
                                    .groupBy { it.name.trim().lowercase() }
                                    .map { (_, group) ->
                                        val first = group.first()
                                        val rName = first.name.trim()
                                        val uLabel = first.unitLabel.trim().ifBlank { "Porsiyon" }
                                        val totalDefAmt = group.sumOf {
                                            it.defaultAmountText.replace(',', '.').toDoubleOrNull() ?: 1.0
                                        }.coerceAtLeast(0.1)
                                        val totalQty = group.sumOf { it.quantity.coerceAtLeast(1) }
                                        val effectiveStep = when {
                                            first.unitBaseAmount > 0.0 -> first.unitBaseAmount
                                            totalQty > 1 -> (totalDefAmt / totalQty).coerceAtLeast(0.5)
                                            else -> stepForUnit(uLabel)
                                        }
                                        val effectivePresets = if (first.unitBaseAmount > 0.0 || totalQty > 1) {
                                            (1..6).map { effectiveStep * it }.let { list ->
                                                if (list.none { kotlin.math.abs(it - totalDefAmt) < 0.01 }) {
                                                    (list + totalDefAmt).sorted()
                                                } else list
                                            }
                                        } else {
                                            presetLevelsForUnit(uLabel, totalDefAmt)
                                        }
                                        val mergedRowMicros = group
                                            .flatMap { it.micronutrients }
                                            .groupBy { "${it.name}|${it.unit}" }
                                            .map { (_, mList) ->
                                                val mFirst = mList.first()
                                                mFirst.copy(amount = ((mList.sumOf { it.amount } * 10.0).roundToInt()) / 10.0)
                                            }

                                        CustomFoodIngredient(
                                            id = first.id,
                                            name = rName,
                                            unitLabel = uLabel,
                                            defaultAmount = totalDefAmt,
                                            stepAmount = effectiveStep,
                                            presetLevels = effectivePresets,
                                            baseCalories = group.sumOf { it.caloriesText.toIntOrNull() ?: 0 },
                                            baseProteinG = group.sumOf { it.proteinText.replace(',', '.').toDoubleOrNull() ?: 0.0 },
                                            baseCarbsG = group.sumOf { it.carbsText.replace(',', '.').toDoubleOrNull() ?: 0.0 },
                                            baseFatG = group.sumOf { it.fatText.replace(',', '.').toDoubleOrNull() ?: 0.0 },
                                            baseFiberG = group.sumOf { it.fiberG },
                                            micronutrients = mergedRowMicros,
                                            iconKey = first.iconKey
                                        )
                                    }
                                if (builtIngredients.isEmpty()) {
                                    errorMessage = "Lütfen karışıma en az 1 içerik kalemi ekleyiniz."
                                    return@Button
                                }
                                val compactMixSummary = builtIngredients.joinToString(" • ") { ing ->
                                    val mult = if (ing.stepAmount > 0.0) (ing.defaultAmount / ing.stepAmount) else 1.0
                                    val multInt = mult.roundToInt()
                                    val hasCleanMult = mult >= 1.95 && kotlin.math.abs(mult - multInt) < 0.08
                                    val shortName = ing.name.substringBefore(" ")
                                    when {
                                        ing.unitLabel.equals("Shot", ignoreCase = true) ->
                                            "Shot x${ing.defaultAmount.roundToInt().coerceAtLeast(1)}"
                                        hasCleanMult ->
                                            "$shortName ${multInt}x (${formatNumClean(ing.defaultAmount)}${ing.unitLabel})"
                                        else ->
                                            "$shortName ${formatNumClean(ing.defaultAmount)}${ing.unitLabel}"
                                    }
                                }
                                onSave(
                                    trimmedName,
                                    brand.trim().ifBlank { "Özel Karışım" },
                                    portionUnit.trim().ifBlank { compactMixSummary },
                                    builtIngredients.sumOf { it.baseCalories },
                                    builtIngredients.sumOf { it.baseProteinG },
                                    builtIngredients.sumOf { it.baseCarbsG },
                                    builtIngredients.sumOf { it.baseFatG },
                                    builtIngredients.sumOf { it.baseFiberG },
                                    selectedCategory.name,
                                    makeFavorite,
                                    selectedIconKey ?: "coffee",
                                    microsList,
                                    builtIngredients
                                )
                            } else {
                                val trimmedPortion = portionUnit.trim()
                                val cal = caloriesText.toIntOrNull()
                                if (trimmedPortion.isBlank()) {
                                    errorMessage = "Lütfen porsiyon veya ambalaj birimini giriniz (örn: 500ml, 100g, 1 Ölçek)."
                                    return@Button
                                }
                                if (cal == null || cal < 0) {
                                    errorMessage = "Lütfen geçerli bir kalori değeri giriniz (sıfır kalori için 0)."
                                    return@Button
                                }
                                val prot = proteinText.toDoubleOrNull() ?: 0.0
                                val carb = carbsText.toDoubleOrNull() ?: 0.0
                                val fat = fatText.toDoubleOrNull() ?: 0.0
                                val fib = fiberText.toDoubleOrNull() ?: 0.0

                                onSave(
                                    trimmedName,
                                    brand.trim().ifBlank { null },
                                    trimmedPortion,
                                    cal,
                                    prot,
                                    carb,
                                    fat,
                                    fib,
                                    selectedCategory.name,
                                    makeFavorite,
                                    selectedIconKey,
                                    microsList,
                                    emptyList()
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .bounceClick(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HealthColors.Primary,
                            contentColor = HealthColors.OnPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isMixMode) "Karışımı Envantere Kaydet" else "Ürünü Envantere Kaydet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun currentTime(): Long = com.yusufteker.healthtrack.core.utils.currentTimeMillis()
