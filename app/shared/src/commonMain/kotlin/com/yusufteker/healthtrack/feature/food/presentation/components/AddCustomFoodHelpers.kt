package com.yusufteker.healthtrack.feature.food.presentation.components

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.Micronutrient
import kotlin.math.roundToInt

internal data class DynamicMicroItem(
    val id: String,
    var name: String = "",
    var amountText: String = "",
    var unit: String = "mg"
)

internal data class DraftIngredientRow(
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

internal fun scaleDraftIngredient(row: DraftIngredientRow, newQuantity: Int): DraftIngredientRow {
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

internal fun formatNumClean(value: Double): String {
    val rounded = (value * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

internal fun inferFoodIconKey(name: String): String? {
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
internal fun parseIngredientPortion(
    portionText: String,
    itemName: String = "",
    fullPrompt: String = "",
    language: AppLanguage = LocalizationManager.currentLanguage
): Pair<String, String> {
    val combined = portionText.trim()
    val lower = combined.lowercase()
    val promptLower = fullPrompt.lowercase()
    val firstWord = itemName.trim().lowercase().split(" ").firstOrNull { it.length >= 3 } ?: ""

    Regex("""(\d+(?:[.,]\d+)?)\s*shot""").find(lower)?.let { m ->
        return "Shot" to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0)
    }
    if (firstWord.isNotBlank()) {
        Regex("""(\d+(?:[.,]\d+)?)\s*shot\s+[^,.\n]*$firstWord""").find(promptLower)?.let { m ->
            return "Shot" to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0)
        }
    }

    Regex("""(\d+(?:[.,]\d+)?)\s*(?:ölçek|olcek|scoop)""").find(lower)?.let { m ->
        return trEn("Ölçek", "Scoop", language) to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0)
    }
    if (firstWord.isNotBlank()) {
        Regex("""(\d+(?:[.,]\d+)?)\s*(?:ölçek|olcek|scoop)\s+[^,.\n]*$firstWord""").find(promptLower)?.let { m ->
            return trEn("Ölçek", "Scoop", language) to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0)
        }
    }

    Regex("""(\d+(?:[.,]\d+)?)\s*(?:ml|mililitre)""").find(lower)?.let { m ->
        return "ml" to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 100.0)
    }

    Regex("""(\d+(?:[.,]\d+)?)\s*(?:g|gr|gram)\b""").find(lower)?.let { m ->
        return "g" to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 50.0)
    }

    Regex("""(\d+(?:[.,]\d+)?)\s*(?:yemek kaşığı|tatlı kaşığı|çay kaşığı|kaşık|kasik|tbsp|tsp|tablespoon|teaspoon|spoon)""").find(lower)?.let { m ->
        return trEn("Kaşık", "Spoon", language) to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0)
    }

    Regex("""(\d+(?:[.,]\d+)?)\s*(?:dilim|slice)""").find(lower)?.let { m ->
        return trEn("Dilim", "Slice", language) to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0)
    }

    Regex("""(\d+(?:[.,]\d+)?)\s*(?:adet|piece|pcs)""").find(lower)?.let { m ->
        return trEn("Adet", "Piece", language) to formatNumClean(m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0)
    }

    val fallbackNum = Regex("""(\d+(?:[.,]\d+)?)""").find(lower)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull() ?: 1.0
    return trEn("Adet", "Piece", language) to formatNumClean(fallbackNum)
}

/**
 * Formats single product portion nicely when user specifies both scoop/ölçek and gram/ml.
 */
internal fun buildSingleProductPortionUnit(
    aiPortion: String,
    rawPrompt: String,
    language: AppLanguage = LocalizationManager.currentLanguage
): String {
    val pLower = rawPrompt.lowercase()
    val scoopMatch = Regex("""(\d+(?:[.,]\d+)?)\s*(?:ölçek|olcek|scoop)""").find(pLower)
    val weightOrVolMatch = Regex("""(\d+(?:[.,]\d+)?)\s*(g|gr|gram|ml)\b""").find(pLower)
    val scoopWord = trEn("Ölçek", "Scoop", language)

    if (scoopMatch != null && weightOrVolMatch != null) {
        val scoopAmt = scoopMatch.groupValues[1]
        val wAmt = weightOrVolMatch.groupValues[1]
        val wUnit = if (weightOrVolMatch.groupValues[2].startsWith("m")) "ml" else "g"
        return "$scoopAmt $scoopWord ($wAmt$wUnit)"
    }
    if (scoopMatch != null && !aiPortion.lowercase().contains("ölçek") && !aiPortion.lowercase().contains("scoop")) {
        val scoopAmt = scoopMatch.groupValues[1]
        return if (aiPortion.isNotBlank()) "$scoopAmt $scoopWord ($aiPortion)" else "$scoopAmt $scoopWord"
    }
    return aiPortion.ifBlank { trEn("1 Porsiyon", "1 Serving", language) }
}

internal fun presetLevelsForUnit(unitLabel: String, defaultAmt: Double): List<Double> {
    val u = unitLabel.lowercase()
    return when {
        u.contains("shot") -> listOf(1.0, 2.0, 3.0, 4.0)
        u.contains("ölçek") || u.contains("olcek") || u.contains("scoop") -> listOf(0.5, 1.0, 1.5, 2.0)
        u.contains("ml") -> listOf(50.0, 100.0, 150.0, 200.0, 250.0, 300.0)
        u.contains("g") -> listOf(20.0, 30.0, 50.0, 100.0, 150.0, 200.0)
        u.contains("kaşık") || u.contains("kasik") || u.contains("spoon") -> listOf(0.5, 1.0, 2.0, 3.0)
        else -> listOf(0.5, 1.0, 1.5, 2.0, 3.0)
    }.let { list ->
        if (defaultAmt > 0 && list.none { kotlin.math.abs(it - defaultAmt) < 0.01 }) {
            (list + defaultAmt).sorted()
        } else list
    }
}

internal fun stepForUnit(unitLabel: String): Double {
    val u = unitLabel.lowercase()
    return when {
        u.contains("ml") -> 50.0
        u.contains("g") -> 10.0
        u.contains("shot") -> 1.0
        u.contains("adet") || u.contains("dilim") || u.contains("piece") || u.contains("slice") -> 1.0
        else -> 0.5
    }
}
