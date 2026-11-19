package com.yusufteker.healthtrack.core.model

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import kotlinx.serialization.Serializable

@Serializable
enum class WeightChartPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY,
    ALL
}

/**
 * Selectable telemetry metric displayed by the Weight trajectory chart.
 */
@Serializable
enum class WeightChartMetric(
    val titleTr: String,
    val titleEn: String,
    val unit: String
) {
    WEIGHT("Kilo", "Weight", "kg"),
    BODY_FAT("Yağ %", "Body Fat", "%"),
    MUSCLE("Kas %", "Muscle", "%");

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)
}

@Serializable
data class WeightEntry(
    val id: String,
    val userId: Int,
    val date: String, // YYYY-MM-DD
    val weightKg: Double,
    val targetWeightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val muscleMassKg: Double? = null,
    val musclePercent: Double? = null,
    val note: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
