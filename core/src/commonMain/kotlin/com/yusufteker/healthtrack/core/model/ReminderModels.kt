package com.yusufteker.healthtrack.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class WeightReminderFrequency(
    val id: String,
    val days: Int,
    val title: String,
    val shortLabel: String
) {
    OFF("OFF", 0, "Kapalı", "Kapalı"),
    DAILY("DAILY", 1, "Her Gün", "1 Gün"),
    EVERY_2_DAYS("EVERY_2_DAYS", 2, "2 Günde 1", "2 Gün"),
    EVERY_3_DAYS("EVERY_3_DAYS", 3, "3 Günde 1 (Önerilen)", "3 Gün"),
    WEEKLY("WEEKLY", 7, "Haftada 1", "7 Gün");

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
    val title: String,
    val description: String
) {
    SMART_CADENCE(
        id = "SMART_CADENCE",
        title = "Akıllı Ritim (Önerilen)",
        description = "Dün 400+ kcal yaktıysan dinlendirir. İhmal edilirse 3. gün ve ardından 2 günde 1 hatırlatır."
    ),
    EVERY_2_DAYS(
        id = "EVERY_2_DAYS",
        title = "2 Günde 1",
        description = "İki günde bir akşam 18:00'de spor yapmadıysan uyarır."
    ),
    THREE_DAYS_WEEK(
        id = "THREE_DAYS_WEEK",
        title = "Haftada 3 Gün",
        description = "Pazartesi, Çarşamba ve Cuma günleri spora teşvik eder."
    ),
    EVERY_DAY(
        id = "EVERY_DAY",
        title = "Her Gün",
        description = "Her gün akşam spor yapılmadıysa hatırlatır."
    ),
    OFF(
        id = "OFF",
        title = "Kapalı",
        description = "Spora teşvik bildirimleri gönderilmez."
    );

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
