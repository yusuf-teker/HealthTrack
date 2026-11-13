package com.yusufteker.healthtrack.core.model

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import kotlinx.serialization.Serializable

@Serializable
enum class FitnessGoal(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val subtitleTr: String,
    val subtitleEn: String,
    val proteinMultiplier: Double
) {
    LOSE_WEIGHT(
        id = "LOSE_WEIGHT",
        titleTr = "Kilo Verme",
        titleEn = "Fat Loss",
        subtitleTr = "Yağ yakımı & kas koruma (1.2x)",
        subtitleEn = "Fat loss & muscle retention (1.2x)",
        proteinMultiplier = 1.2
    ),
    MAINTAIN(
        id = "MAINTAIN",
        titleTr = "Sabit Kalma",
        titleEn = "Maintain",
        subtitleTr = "Kilo & form koruma (1.0x)",
        subtitleEn = "Weight & shape maintenance (1.0x)",
        proteinMultiplier = 1.0
    ),
    BUILD_MUSCLE(
        id = "BUILD_MUSCLE",
        titleTr = "Kaslanma",
        titleEn = "Build Muscle",
        subtitleTr = "Hipertrofi & kas kazanımı (1.4x)",
        subtitleEn = "Hypertrophy & muscle gain (1.4x)",
        proteinMultiplier = 1.4
    );

    val title: String
        get() = localizedTitle()

    val subtitle: String
        get() = localizedSubtitle()

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)

    fun localizedSubtitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(subtitleTr, subtitleEn, language)

    companion object {
        fun fromId(id: String?): FitnessGoal =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MAINTAIN
    }
}

/**
 * Calculates recommended daily protein in grams based on body weight and fitness goal.
 * Rule:
 * - Kilo Verme (Fat Loss / Deficit): 1.2 x body weight (prevents muscle breakdown in deficit)
 * - Sabit Kalma (Maintenance): 1.0 x body weight (at least body weight in grams)
 * - Kaslanma (Muscle Hypertrophy): 1.4 x body weight (optimal synthesis & growth)
 */
fun calculateProteinTarget(weightKg: Double, goal: FitnessGoal): Double {
    val safeWeight = if (weightKg > 0.0) weightKg else 70.0
    return kotlin.math.round(safeWeight * goal.proteinMultiplier * 10) / 10.0
}

@Serializable
data class NutritionGoal(
    val id: String,
    val userId: Int,
    val calories: Int = 2000,
    val proteinG: Double = 80.0,
    val carbsG: Double = 250.0,
    val fatG: Double = 65.0,
    val fiberG: Double = 30.0,
    val vitaminCMg: Double = 90.0,
    val vitaminDUg: Double = 15.0,
    val vitaminB12Ug: Double = 2.4,
    val ironMg: Double = 14.0,
    val calciumMg: Double = 1000.0,
    val magnesiumMg: Double = 350.0,
    val fitnessGoal: String = FitnessGoal.MAINTAIN.id,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
) {
    val parsedFitnessGoal: FitnessGoal
        get() = FitnessGoal.fromId(fitnessGoal)
}

@Serializable
data class NutrientProgress(
    val key: String,
    val label: String,
    val current: Double,
    val target: Double,
    val unit: String,
    val colorHex: String
) {
    val percentage: Float
        get() = if (target > 0) ((current / target) * 100).toFloat().coerceIn(0f, 100f) else 0f
}

@Serializable
data class WaterEntry(
    val date: String, // YYYY-MM-DD
    val userId: Int,
    val amountMl: Int,
    val targetMl: Int = 2500,
    val updatedAt: Long = 0L
)

@Serializable
data class SmartMealSuggestion(
    val id: String,
    val title: String,
    val subtitle: String,
    val tags: List<String> = emptyList(), // e.g. "Az Malzemeli", "Kolay (<10 dk)", "Lif Deposu", "Son 3 gündür yemediniz"
    val prepTimeMinutes: Int = 10,
    val ingredients: List<String> = emptyList(),
    val calories: Int,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val keyMicros: List<Micronutrient> = emptyList(),
    val instructions: String? = null
)

@Serializable
data class NutrientSynergyTip(
    val id: String,
    val title: String,
    val description: String,
    val iconKey: String = "synergy",
    val highlightNutrient: String
)
