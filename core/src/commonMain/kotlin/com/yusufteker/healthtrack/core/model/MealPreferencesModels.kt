package com.yusufteker.healthtrack.core.model

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn

/**
 * Bilingual ingredient preset used for allergy/dislike exclusions and favorite ingredient boosts.
 */
data class MealIngredientOption(
    val id: String,
    val labelTr: String,
    val labelEn: String,
    val emoji: String,
    val matchKeywords: List<String>
) {
    fun localizedLabel(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(labelTr, labelEn, language)
}

/**
 * Catalog of curated ingredient options plus KMP-safe Turkish diacritic folding and boundary-safe matching.
 */
object MealPreferencesCatalog {

    val allergyPresets: List<MealIngredientOption> = listOf(
        MealIngredientOption(
            id = "dairy",
            labelTr = "Süt & Laktoz",
            labelEn = "Dairy / Lactose",
            emoji = "🥛",
            matchKeywords = listOf(
                "süt", "yoğurt", "peynir", "lor", "kaşar", "kefir", "ayran", "krema", "tereyağ", "laktoz",
                "milk", "yogurt", "cheese", "cottage", "dairy", "whey", "butter", "cream"
            )
        ),
        MealIngredientOption(
            id = "egg",
            labelTr = "Yumurta",
            labelEn = "Egg",
            emoji = "🥚",
            matchKeywords = listOf("yumurta", "omlet", "menemen", "egg", "omelet")
        ),
        MealIngredientOption(
            id = "gluten",
            labelTr = "Gluten",
            labelEn = "Gluten",
            emoji = "🌾",
            matchKeywords = listOf(
                "gluten", "buğday", "ekmek", "lavaş", "dürüm", "makarna", "bulgur", "un", "simit",
                "bisküvi", "kraker", "yulaf", "wheat", "bread", "wrap", "pasta", "flour", "oat"
            )
        ),
        MealIngredientOption(
            id = "nuts",
            labelTr = "Kuruyemiş",
            labelEn = "Nuts",
            emoji = "🥜",
            matchKeywords = listOf(
                "ceviz", "badem", "fıstık", "fındık", "kaju", "antep", "kuruyemiş",
                "walnut", "almond", "peanut", "hazelnut", "cashew", "pistachio", "nut"
            )
        ),
        MealIngredientOption(
            id = "fish",
            labelTr = "Balık & Deniz",
            labelEn = "Fish & Seafood",
            emoji = "🐟",
            matchKeywords = listOf(
                "balık", "balığ", "ton", "somon", "hamsi", "levrek", "çipura", "karides", "midye", "deniz",
                "fish", "tuna", "salmon", "shrimp", "seafood"
            )
        ),
        MealIngredientOption(
            id = "chicken",
            labelTr = "Tavuk",
            labelEn = "Chicken",
            emoji = "🍗",
            matchKeywords = listOf("tavuk", "tavuğ", "piliç", "chicken", "poultry")
        ),
        MealIngredientOption(
            id = "red_meat",
            labelTr = "Kırmızı Et",
            labelEn = "Red Meat",
            emoji = "🥩",
            matchKeywords = listOf(
                "et", "dana", "kuzu", "kıyma", "köfte", "biftek", "antrikot", "sucuk", "pastırma",
                "beef", "lamb", "steak", "meatball", "meat", "pork"
            )
        ),
        MealIngredientOption(
            id = "legumes",
            labelTr = "Bakliyat",
            labelEn = "Legumes",
            emoji = "🫘",
            matchKeywords = listOf(
                "mercimek", "nohut", "fasulye", "bakla", "barbunya", "humus", "bezelye",
                "lentil", "chickpea", "bean", "hummus", "legume"
            )
        ),
        MealIngredientOption(
            id = "mushroom",
            labelTr = "Mantar",
            labelEn = "Mushroom",
            emoji = "🍄",
            matchKeywords = listOf("mantar", "mushroom")
        ),
        MealIngredientOption(
            id = "broccoli",
            labelTr = "Brokoli",
            labelEn = "Broccoli",
            emoji = "🥦",
            matchKeywords = listOf("brokoli", "broccoli")
        ),
        MealIngredientOption(
            id = "avocado",
            labelTr = "Avokado",
            labelEn = "Avocado",
            emoji = "🥑",
            matchKeywords = listOf("avokado", "avocado")
        ),
        MealIngredientOption(
            id = "soy",
            labelTr = "Soya",
            labelEn = "Soy",
            emoji = "🌱",
            matchKeywords = listOf("soya", "tofu", "edamame", "soy")
        )
    )

    val favoritePresets: List<MealIngredientOption> = listOf(
        MealIngredientOption(
            id = "chicken",
            labelTr = "Tavuk",
            labelEn = "Chicken",
            emoji = "🍗",
            matchKeywords = listOf("tavuk", "tavuğ", "piliç", "chicken")
        ),
        MealIngredientOption(
            id = "salad",
            labelTr = "Salata",
            labelEn = "Salad",
            emoji = "🥗",
            matchKeywords = listOf(
                "salata", "roka", "marul", "yeşillik", "akdeniz", "çoban", "maydanoz", "salatalık",
                "salad", "arugula", "lettuce", "greens", "cucumber"
            )
        ),
        MealIngredientOption(
            id = "egg",
            labelTr = "Yumurta",
            labelEn = "Egg",
            emoji = "🥚",
            matchKeywords = listOf("yumurta", "omlet", "menemen", "egg", "omelet")
        ),
        MealIngredientOption(
            id = "yogurt",
            labelTr = "Yoğurt",
            labelEn = "Yogurt",
            emoji = "🥣",
            matchKeywords = listOf("yoğurt", "yogurt")
        ),
        MealIngredientOption(
            id = "oats",
            labelTr = "Yulaf",
            labelEn = "Oats",
            emoji = "🌾",
            matchKeywords = listOf("yulaf", "oat")
        ),
        MealIngredientOption(
            id = "tuna",
            labelTr = "Ton Balığı",
            labelEn = "Tuna",
            emoji = "🐟",
            matchKeywords = listOf("ton balık", "ton balığ", "tuna")
        ),
        MealIngredientOption(
            id = "salmon",
            labelTr = "Somon",
            labelEn = "Salmon",
            emoji = "🍣",
            matchKeywords = listOf("somon", "salmon")
        ),
        MealIngredientOption(
            id = "red_meat",
            labelTr = "Köfte / Et",
            labelEn = "Beef / Meat",
            emoji = "🥩",
            matchKeywords = listOf("köfte", "kıyma", "dana", "biftek", "et", "beef", "steak", "meatball")
        ),
        MealIngredientOption(
            id = "cheese",
            labelTr = "Peynir / Lor",
            labelEn = "Cheese",
            emoji = "🧀",
            matchKeywords = listOf("peynir", "lor", "kaşar", "beyaz peynir", "cheese", "cottage")
        ),
        MealIngredientOption(
            id = "avocado",
            labelTr = "Avokado",
            labelEn = "Avocado",
            emoji = "🥑",
            matchKeywords = listOf("avokado", "avocado")
        ),
        MealIngredientOption(
            id = "lentil",
            labelTr = "Mercimek",
            labelEn = "Lentils",
            emoji = "🫘",
            matchKeywords = listOf("mercimek", "lentil")
        ),
        MealIngredientOption(
            id = "broccoli",
            labelTr = "Brokoli",
            labelEn = "Broccoli",
            emoji = "🥦",
            matchKeywords = listOf("brokoli", "broccoli")
        ),
        MealIngredientOption(
            id = "mushroom",
            labelTr = "Mantar",
            labelEn = "Mushroom",
            emoji = "🍄",
            matchKeywords = listOf("mantar", "mushroom")
        ),
        MealIngredientOption(
            id = "banana",
            labelTr = "Muz",
            labelEn = "Banana",
            emoji = "🍌",
            matchKeywords = listOf("muz", "banana")
        )
    )

    val allPresets: List<MealIngredientOption> = (allergyPresets + favoritePresets).distinctBy { it.id }

    private val whitespaceRegex = Regex("\\s+")
    private val tokenSplitRegex = Regex("[^a-z0-9]+")

    /**
     * Folds Turkish characters and uppercase letters into plain ASCII lowercase in a KMP-safe way
     * without relying on JVM `Locale`.
     */
    fun foldTurkish(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                '\u0307' -> Unit // Strip combining dot above (e.g. from decomposed İ)
                'İ', 'I', 'ı', 'i' -> sb.append('i')
                'Ş', 'ş' -> sb.append('s')
                'Ğ', 'ğ' -> sb.append('g')
                'Ü', 'ü' -> sb.append('u')
                'Ö', 'ö' -> sb.append('o')
                'Ç', 'ç' -> sb.append('c')
                in 'A'..'Z' -> sb.append((ch.code + 32).toChar())
                else -> sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString().trim().replace(whitespaceRegex, " ")
    }

    private fun lowercaseTurkishPreserving(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                '\u0307' -> Unit
                'İ' -> sb.append('i')
                'I' -> sb.append('ı')
                'Ş' -> sb.append('ş')
                'Ğ' -> sb.append('ğ')
                'Ü' -> sb.append('ü')
                'Ö' -> sb.append('ö')
                'Ç' -> sb.append('ç')
                in 'A'..'Z' -> sb.append((ch.code + 32).toChar())
                else -> sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString().trim().replace(whitespaceRegex, " ")
    }

    fun normalizeKey(raw: String, preferPresetPool: List<MealIngredientOption> = allPresets): String {
        val collapsed = raw.trim().replace(whitespaceRegex, " ")
        if (collapsed.isBlank()) return ""
        val folded = foldTurkish(collapsed)

        val pools = if (preferPresetPool === allPresets) listOf(allPresets) else listOf(preferPresetPool, allPresets)
        for (pool in pools) {
            val matched = pool.firstOrNull { option ->
                foldTurkish(option.id) == folded ||
                    foldTurkish(option.labelTr) == folded ||
                    foldTurkish(option.labelEn) == folded ||
                    option.matchKeywords.any { !it.contains(' ') && foldTurkish(it) == folded }
            }
            if (matched != null) return matched.id
        }

        return lowercaseTurkishPreserving(collapsed)
    }

    fun localizedLabel(key: String, language: AppLanguage = LocalizationManager.currentLanguage): String {
        val preset = allPresets.firstOrNull { it.id == key }
        if (preset != null) return preset.localizedLabel(language)
        val trimmed = key.trim()
        return trimmed.replaceFirstChar { ch ->
            when (ch) {
                'i' -> if (language == AppLanguage.TR) 'İ' else 'I'
                'ı' -> 'I'
                'ş' -> 'Ş'
                'ğ' -> 'Ğ'
                'ü' -> 'Ü'
                'ö' -> 'Ö'
                'ç' -> 'Ç'
                else -> ch.uppercaseChar()
            }
        }
    }

    fun keywordsFor(key: String): List<String> {
        val preset = allPresets.firstOrNull { it.id == key }
        return if (preset != null) {
            preset.matchKeywords.map(::foldTurkish).filter { it.isNotBlank() }
        } else {
            listOf(foldTurkish(key)).filter { it.isNotBlank() }
        }
    }

    fun matchesText(text: String, key: String): Boolean {
        val foldedText = foldTurkish(text)
        if (foldedText.isBlank()) return false
        val tokens = foldedText.split(tokenSplitRegex).filter { it.isNotEmpty() }

        for (kw in keywordsFor(key)) {
            if (kw.isBlank()) continue
            val matched = when {
                kw.contains(' ') -> foldedText.contains(kw)
                // Prevent 2-char keywords like "et" from matching substrings like "omlet" or "keten"
                kw.length <= 2 -> tokens.any { it == kw }
                // 3-char roots like "sut", "muz", "lor", "egg", "soy", "nut" match prefix of whole words
                kw.length == 3 -> tokens.any { it == kw || it.startsWith(kw) }
                else -> tokens.any { it.startsWith(kw) } || foldedText.contains(kw)
            }
            if (matched) return true
        }
        return false
    }

    fun conflictingFavoritesForExcluded(excludedKey: String): Set<String> = when (excludedKey) {
        "dairy" -> setOf("dairy", "yogurt", "cheese")
        "fish" -> setOf("fish", "tuna", "salmon")
        "legumes" -> setOf("legumes", "lentil")
        else -> setOf(excludedKey)
    }

    fun conflictingExcludesForFavorite(favoriteKey: String): Set<String> = when (favoriteKey) {
        "yogurt", "cheese" -> setOf(favoriteKey, "dairy")
        "tuna", "salmon" -> setOf(favoriteKey, "fish")
        "lentil" -> setOf(favoriteKey, "legumes")
        else -> setOf(favoriteKey)
    }
}
