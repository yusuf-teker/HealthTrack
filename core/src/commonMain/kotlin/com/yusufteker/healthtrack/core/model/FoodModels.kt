package com.yusufteker.healthtrack.core.model

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import kotlinx.serialization.Serializable

@Serializable
enum class MealHealthTier {
    EXCELLENT, // 80..100
    BALANCED,  // 60..79
    FLEXIBLE   // 0..59
}

@Serializable
data class MealHealthEvaluation(
    val score: Int,
    val tier: MealHealthTier,
    val badgeTitleTr: String,
    val badgeTitleEn: String,
    val encouragingQuoteTr: String,
    val encouragingQuoteEn: String,
    val highlightTagsTr: List<String> = emptyList(),
    val highlightTagsEn: List<String> = emptyList()
) {
    val isHealthyCelebrationWorthy: Boolean
        get() = score >= 80

    fun localizedBadgeTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(badgeTitleTr, badgeTitleEn, language)

    fun localizedQuote(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(encouragingQuoteTr, encouragingQuoteEn, language)

    fun localizedHighlights(language: AppLanguage = LocalizationManager.currentLanguage): List<String> =
        if (language == AppLanguage.EN) highlightTagsEn else highlightTagsTr
}

object MealHealthEvaluator {

    private val positiveKeywordGroups: List<List<String>> = listOf(
        listOf("yumurta", "egg"),
        listOf("tavuk", "chicken", "hindi", "turkey"),
        listOf("balık", "balik", "fish", "somon", "salmon", "ton", "tuna"),
        listOf("yoğurt", "yogurt", "kefir", "süzme", "suzme", "lor", "süt", "sut", "milk"),
        listOf("yulaf", "oat"),
        listOf("salata", "salad", "brokoli", "broccoli", "ıspanak", "ispanak", "spinach", "sebze", "vegetable", "yeşillik", "yesillik"),
        listOf("avokado", "avocado", "zeytinyağ", "zeytinyag", "olive"),
        listOf("mercimek", "lentil", "nohut", "chickpea", "fasulye", "bean", "kinoa", "quinoa"),
        listOf("ceviz", "walnut", "badem", "almond", "chia"),
        listOf("elma", "apple", "muz", "banana", "çilek", "cilek", "berry")
    )

    private val processedKeywordGroups: List<List<String>> = listOf(
        listOf("çikolata", "cikolata", "chocolate", "nutella", "kakaolu fındık", "snickers", "twix", "mars", "brownie", "ekler", "profiterol", "sufle", "waffle", "pankek"),
        listOf("popcorn", "patlamış mısır", "patlamis misir", "mısır cipsi", "doritos", "ruffles", "lays", "cheetos", "pringles", "kraker", "cracker", "çubuk kraker"),
        listOf("kola", "cola", "gazoz", "soda", "fanta", "sprite", "ice tea", "meyve suyu", "enerji içeceği", "redbull", "monster", "milkshake", "frappe"),
        listOf("cips", "chips", "nachos"),
        listOf("kızartma", "kizartma", "fries", "fried", "nugget", "şinitzel", "sinitzel", "soğan halkası"),
        listOf("pasta", "cake", "donut", "baklava", "şerbet", "serbet", "künefe", "kunefe", "tulumba", "lokma", "kadayıf", "helva", "reçel", "recel"),
        listOf("şekerleme", "sekerleme", "candy", "jelibon", "lokum", "marshmallow", "bonibon", "lolipop"),
        listOf("gofret", "wafer", "bisküvi", "biskuvi", "cookie", "oreo", "biskrem", "halley", "kek", "muffin", "kurabiye"),
        listOf("dondurma", "ice cream", "magnum", "cornetto"),
        listOf("fast food", "hamburger", "burger", "pizza", "hot dog", "kumpir", "kokoreç", "kokorec"),
        listOf("poğaça", "pogaca", "açma", "acma", "börek", "borek", "kruvasan", "croissant", "milföy"),
        listOf("sosis", "salam", "sucuk", "pastırma")
    )

    fun evaluate(
        items: List<FoodItem>,
        totalCalories: Int,
        summary: String = "",
        explicitScore: Int? = null,
        explicitPraise: String? = null
    ): MealHealthEvaluation {
        if (items.isEmpty() && totalCalories <= 0 && explicitScore == null) {
            return MealHealthEvaluation(
                score = 0,
                tier = MealHealthTier.FLEXIBLE,
                badgeTitleTr = "Değerlendirme Bekleniyor",
                badgeTitleEn = "Pending Analysis",
                encouragingQuoteTr = "Öğün içeriğini eklediğinde yapay zeka sağlık puanını hesaplayacak.",
                encouragingQuoteEn = "Add meal items so AI can calculate your health score."
            )
        }

        val cals = (totalCalories.takeIf { it > 0 } ?: items.sumOf { it.calories }).coerceAtLeast(1)
        val totalProtein = items.sumOf { it.proteinG }
        val totalCarbs = items.sumOf { it.carbsG }
        val totalFiber = items.sumOf { it.fiberG }
        val totalFat = items.sumOf { it.fatG }
        val proteinCalRatio = (totalProtein * 4.0) / cals.toDouble()
        val carbCalRatio = (totalCarbs * 4.0) / cals.toDouble()
        val fatCalRatio = (totalFat * 9.0) / cals.toDouble()
        val microCount = items
            .flatMap { it.micronutrients }
            .map { it.name.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .size

        val combinedText = buildString {
            items.forEach {
                append(it.name.lowercase())
                append(' ')
            }
            append(summary.lowercase())
        }

        val positiveGroupHits = positiveKeywordGroups.count { group ->
            group.any { kw -> combinedText.contains(kw) }
        }
        val processedGroupHits = processedKeywordGroups.count { group ->
            group.any { kw -> combinedText.contains(kw) }
        }

        val computedScore = run {
            var score = 54

            score += when {
                proteinCalRatio >= 0.25 -> 18
                proteinCalRatio >= 0.18 -> 12
                proteinCalRatio >= 0.12 -> 6
                else -> 0
            }
            if (totalProtein >= 25.0) {
                score += 6
            }

            score += when {
                totalFiber >= 8.0 -> 12
                totalFiber >= 4.0 -> 7
                totalFiber >= 2.0 -> 3
                else -> 0
            }

            score -= when {
                fatCalRatio > 0.55 -> 14
                fatCalRatio > 0.45 -> 8
                else -> 0
            }

            if (carbCalRatio > 0.70 && totalFiber < 3.0) {
                score -= 12
            } else if (carbCalRatio > 0.60 && totalFiber < 2.0 && proteinCalRatio < 0.10) {
                score -= 8
            }

            if (proteinCalRatio < 0.08 && totalFiber < 2.0 && positiveGroupHits == 0 && cals > 120) {
                score -= 8
            }

            score += when {
                microCount >= 3 -> 6
                microCount >= 1 -> 2
                else -> 0
            }
            score += (positiveGroupHits * 6).coerceAtMost(18)
            score -= (processedGroupHits * 16).coerceAtMost(44)

            if (processedGroupHits > 0 && positiveGroupHits == 0) {
                score -= 8
                score = minOf(score, if (processedGroupHits >= 2) 32 else 42)
            }

            score.coerceIn(12, 98)
        }

        val finalScore = if (explicitScore != null && explicitScore in 1..100) {
            if (processedGroupHits > 0) minOf(explicitScore, computedScore) else explicitScore
        } else {
            computedScore
        }

        val tier = when {
            finalScore >= 80 -> MealHealthTier.EXCELLENT
            finalScore >= 60 -> MealHealthTier.BALANCED
            else -> MealHealthTier.FLEXIBLE
        }

        val badgeTitleTr = when {
            finalScore >= 80 -> "Sağlıklı Öğün 🌟"
            finalScore >= 60 -> "Dengeli Öğün 👍"
            finalScore < 45 -> "Düşük Besin Değeri ⚠️"
            else -> "Esnek Öğün 😋"
        }
        val badgeTitleEn = when {
            finalScore >= 80 -> "Healthy Meal 🌟"
            finalScore >= 60 -> "Balanced Meal 👍"
            finalScore < 45 -> "Low Nutritional Value ⚠️"
            else -> "Flexible Meal 😋"
        }

        val (defaultQuoteTr, defaultQuoteEn) = when {
            finalScore >= 90 && totalProtein >= 20.0 ->
                "Muhteşem bir seçim! Yüksek protein ve temiz içerikle vücudunu en iyi şekilde besledin." to
                    "Outstanding choice! You fueled your body with clean nutrients and strong protein."
            finalScore >= 90 ->
                "Süper besin kalitesi! Bu öğün enerjini ve sağlığını zirveye taşıyor." to
                    "Superb food quality! This meal fuels your energy and long-term vitality."
            finalScore >= 80 && totalProtein >= 15.0 && totalFiber >= 4.0 ->
                "Harika denge! Hem güçlü protein hem lif desteğiyle metabolizman sana teşekkür ediyor." to
                    "Great balance! Both solid protein and fiber keep your metabolism thriving."
            finalScore >= 80 && totalFiber >= 4.0 ->
                "Lif ve mikro besin zengini harika bir tabak! Sindirimin ve tokluğun uzun süre korunacak." to
                    "Fiber and micronutrient-rich plate! You'll stay full and energized for hours."
            finalScore >= 80 ->
                "Çok sağlıklı bir öğün! Hedeflerine bir adım daha yaklaştın, böyle devam et." to
                    "Very healthy meal! One step closer to your goals — keep this momentum going."
            finalScore >= 60 ->
                "Güzel bir denge! Yanına biraz yeşillik veya ekstra protein eklersen puanı 80+ yapabilirsin." to
                    "Nice balance! Adding some greens or extra protein can easily push this above 80."
            finalScore < 45 ->
                "Şeker veya işlenmiş gıda oranı yüksek. Bir sonraki öğünde protein, lif ve su ile dengele." to
                    "High in sugar or processed ingredients. Balance your next meal with protein, fiber, and water."
            else ->
                "Afiyet olsun! Keyif öğünleri de yaşamın parçası — bir sonraki öğünde lif ve proteinle dengeleyebilirsin." to
                    "Enjoy your meal! Treat meals are part of life — balance the next one with protein and fiber."
        }

        val cleanPraise = explicitPraise?.trim()?.takeIf { it.isNotBlank() }
        val quoteTr = cleanPraise ?: defaultQuoteTr
        val quoteEn = cleanPraise ?: defaultQuoteEn

        val highlightsTr = mutableListOf<String>()
        val highlightsEn = mutableListOf<String>()
        if (totalProtein >= 18.0 || proteinCalRatio >= 0.22) {
            highlightsTr.add("💪 Yüksek Protein")
            highlightsEn.add("💪 High Protein")
        }
        if (totalFiber >= 4.0) {
            highlightsTr.add("🌿 Lif Zengini")
            highlightsEn.add("🌿 Fiber Rich")
        }
        if (microCount >= 2) {
            highlightsTr.add("🧬 Mikro Besin Desteği")
            highlightsEn.add("🧬 Micronutrient Boost")
        }
        if (positiveGroupHits >= 2 && processedGroupHits == 0) {
            highlightsTr.add("✨ Temiz İçerik")
            highlightsEn.add("✨ Clean Ingredients")
        }
        if (highlightsTr.isEmpty() && tier == MealHealthTier.EXCELLENT) {
            highlightsTr.add("🥗 Dengeli Makro")
            highlightsEn.add("🥗 Balanced Macros")
        }

        return MealHealthEvaluation(
            score = finalScore,
            tier = tier,
            badgeTitleTr = badgeTitleTr,
            badgeTitleEn = badgeTitleEn,
            encouragingQuoteTr = quoteTr,
            encouragingQuoteEn = quoteEn,
            highlightTagsTr = highlightsTr.take(3),
            highlightTagsEn = highlightsEn.take(3)
        )
    }
}

@Serializable
data class Micronutrient(
    val name: String,
    val amount: Double,
    val unit: String
)

@Serializable
data class FoodItem(
    val id: String = "",
    val name: String,
    val formattedPortion: String,
    val calories: Int,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val micronutrients: List<Micronutrient> = emptyList(),
    val iconKey: String? = null
)

@Serializable
data class FoodLog(
    val id: String,
    val userId: Int,
    val date: String, // YYYY-MM-DD
    val rawText: String,
    val summary: String? = null,
    val totalCalories: Int = 0,
    val items: List<FoodItem> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Serializable
data class CustomFoodIngredient(
    val id: String = "",
    val name: String,
    val unitLabel: String, // e.g. "Shot", "Ölçek", "ml", "g", "Adet", "Tatlı Kaşığı"
    val defaultAmount: Double = 1.0,
    val stepAmount: Double = 1.0,
    val presetLevels: List<Double> = emptyList(),
    val baseCalories: Int,
    val baseProteinG: Double = 0.0,
    val baseCarbsG: Double = 0.0,
    val baseFatG: Double = 0.0,
    val baseFiberG: Double = 0.0,
    val micronutrients: List<Micronutrient> = emptyList(),
    val iconKey: String? = null
)

@Serializable
data class CustomFoodItem(
    val id: String,
    val userId: Int = 0,
    val name: String,
    val brand: String? = null,
    val portionUnit: String, // e.g. "500ml (1 Kutu)", "100g", "1 Dilim", "1 Adet"
    val unitStep: Int = 1,
    val calories: Int,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val categoryName: String = "ALL",
    val iconKey: String? = null,
    val micronutrients: List<Micronutrient> = emptyList(),
    val ingredients: List<CustomFoodIngredient> = emptyList(),
    val baseAmount: Double = 1.0,
    val baseUnitLabel: String = "",
    val presetLevels: List<Double> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val isMix: Boolean get() = ingredients.isNotEmpty()
}
