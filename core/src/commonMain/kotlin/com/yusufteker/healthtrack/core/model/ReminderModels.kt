package com.yusufteker.healthtrack.core.model

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import kotlinx.serialization.Serializable

@Serializable
enum class WeightReminderFrequency(
    val id: String,
    val days: Int,
    val titleTr: String,
    val titleEn: String,
    val shortLabelTr: String,
    val shortLabelEn: String
) {
    OFF("OFF", 0, "Kapalı", "Off", "Kapalı", "Off"),
    DAILY("DAILY", 1, "Her Gün", "Daily", "1 Gün", "1 Day"),
    EVERY_2_DAYS("EVERY_2_DAYS", 2, "2 Günde 1", "Every 2 Days", "2 Gün", "2 Days"),
    EVERY_3_DAYS("EVERY_3_DAYS", 3, "3 Günde 1 (Önerilen)", "Every 3 Days (Recommended)", "3 Gün", "3 Days"),
    WEEKLY("WEEKLY", 7, "Haftada 1", "Weekly", "7 Gün", "7 Days");

    val title: String
        get() = localizedTitle()

    val shortLabel: String
        get() = localizedShortLabel()

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)

    fun localizedShortLabel(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(shortLabelTr, shortLabelEn, language)

    companion object {
        fun fromId(id: String?): WeightReminderFrequency {
            if (id == null) return EVERY_3_DAYS
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: EVERY_3_DAYS
        }
    }
}

/**
 * Frequency settings for evening workout incentive notifications (Default: 18:00).
 */
@Serializable
enum class WorkoutReminderFrequency(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val descriptionTr: String,
    val descriptionEn: String
) {
    SMART_CADENCE(
        id = "SMART_CADENCE",
        titleTr = "Akıllı Ritim (Önerilen)",
        titleEn = "Smart Cadence (Recommended)",
        descriptionTr = "Dün 400+ kcal yaktıysan dinlendirir. İhmal edilirse 3. gün ve ardından 2 günde 1 hatırlatır.",
        descriptionEn = "Rests you if you burned 400+ kcal yesterday. If skipped, reminds on day 3 and then every 2 days."
    ),
    EVERY_2_DAYS(
        id = "EVERY_2_DAYS",
        titleTr = "2 Günde 1",
        titleEn = "Every 2 Days",
        descriptionTr = "İki günde bir akşam 18:00'de spor yapmadıysan uyarır.",
        descriptionEn = "Reminds every 2 days at 18:00 if no workout is logged."
    ),
    THREE_DAYS_WEEK(
        id = "THREE_DAYS_WEEK",
        titleTr = "Haftada 3 Gün",
        titleEn = "3 Days a Week",
        descriptionTr = "Pazartesi, Çarşamba ve Cuma günleri spora teşvik eder.",
        descriptionEn = "Encourages workouts on Monday, Wednesday, and Friday."
    ),
    EVERY_DAY(
        id = "EVERY_DAY",
        titleTr = "Her Gün",
        titleEn = "Every Day",
        descriptionTr = "Her gün akşam spor yapılmadıysa hatırlatır.",
        descriptionEn = "Reminds every evening if no workout has been logged."
    ),
    OFF(
        id = "OFF",
        titleTr = "Kapalı",
        titleEn = "Off",
        descriptionTr = "Spora teşvik bildirimleri gönderilmez.",
        descriptionEn = "Workout reminder notifications are disabled."
    );

    val title: String
        get() = localizedTitle()

    val description: String
        get() = localizedDescription()

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)

    fun localizedDescription(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(descriptionTr, descriptionEn, language)

    companion object {
        fun fromId(id: String?): WorkoutReminderFrequency {
            if (id == null) return SMART_CADENCE
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SMART_CADENCE
        }
    }
}

enum class HealthReminderType {
    WEIGHT,
    WORKOUT,
    FOOD
}

/**
 * Data payload for a scheduled incentive notification to be displayed to the user.
 */
data class ScheduledReminderPayload(
    val title: String,
    val body: String,
    val targetTab: String, // "WORKOUT" or "WEIGHT"
    val channelId: String
)
