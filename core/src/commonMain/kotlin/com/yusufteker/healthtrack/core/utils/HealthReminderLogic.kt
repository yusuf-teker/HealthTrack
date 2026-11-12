package com.yusufteker.healthtrack.core.utils

import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
import com.yusufteker.healthtrack.core.network.TokenStorage

/**
 * Pure, platform-agnostic business logic for checking reminder triggers and formatting
 * reminder notification texts for weight and meals.
 */
object HealthReminderLogic {

    /**
     * Determines whether a weight reminder notification should be triggered today.
     * Evaluates weighing frequency (e.g. daily, every 2 days, every 3 days, weekly)
     * and checks whether a weight record already exists for today or if a notification was
     * already sent today.
     */
    fun shouldSendWeightReminder(
        userId: Int,
        todayIso: String,
        database: HealthDatabase,
        tokenStorage: TokenStorage
    ): Boolean {
        if (userId <= 0) return false

        val freq = tokenStorage.getWeightReminderFrequency()
        if (freq == WeightReminderFrequency.OFF) return false

        // Do not notify twice on the same day
        if (tokenStorage.getLastWeightReminderDate() == todayIso) return false

        val weights = database.getWeights(userId)
        // If already logged weight today, no need to remind
        val hasLoggedToday = weights.any { it.date == todayIso }
        if (hasLoggedToday) return false

        if (freq == WeightReminderFrequency.DAILY) {
            return true
        }

        val latest = weights.maxByOrNull { it.date } ?: return true // Never logged, encourage user to weigh
        val daysDiff = calculateDaysBetween(latest.date, todayIso)
        return daysDiff >= freq.days
    }

    /**
     * Builds localized headline and body for the weight reminder notification.
     */
    fun buildWeightReminderContent(
        freq: WeightReminderFrequency,
        daysSinceLastWeight: Int?
    ): Pair<String, String> {
        val title = "Tartıldın mı? ⚖️"
        val body = when {
            daysSinceLastWeight != null && daysSinceLastWeight >= 2 ->
                "$daysSinceLastWeight gündür kilonu güncellemedin. Hedefine olan gelişimini görmek için tartıya çıkma zamanı!"
            freq == WeightReminderFrequency.DAILY ->
                "Günaydın! Sabah kilonu kaydetmeyi unutma. Güncel kilonu ekleyerek hedefine bir adım daha yaklaş."
            freq == WeightReminderFrequency.WEEKLY ->
                "Haftalık tartılma günün geldi! Güncel kilonu kaydederek haftalık değişimini gör."
            freq == WeightReminderFrequency.EVERY_3_DAYS ->
                "3 gündür tartılmadın. Kilonu kaydederek gelişimini takip etmeyi unutma!"
            freq == WeightReminderFrequency.EVERY_2_DAYS ->
                "2 gündür tartılmadın. Güncel kilonu kaydederek ilerlemeni güncel tut."
            else ->
                "Bugün tartılma günün! Kilonu kaydederek hedefine olan gelişimini takip etmeyi unutma."
        }
        return title to body
    }

    /**
     * Determines whether a food / meal reminder notification should be triggered today at 13:30.
     * Returns true if enabled and user has not logged any meal for today yet.
     */
    fun shouldSendFoodReminder(
        userId: Int,
        todayIso: String,
        database: HealthDatabase,
        tokenStorage: TokenStorage
    ): Boolean {
        if (userId <= 0) return false
        if (!tokenStorage.isFoodReminderEnabled()) return false

        // Do not notify twice on the same day
        if (tokenStorage.getLastFoodReminderDate() == todayIso) return false

        val foods = database.getFoods(userId, todayIso)
        return foods.isEmpty()
    }

    /**
     * Builds localized headline and body for the food reminder notification.
     */
    fun buildFoodReminderContent(): Pair<String, String> {
        val title = "Bugün Ne Yedin? 🥗"
        val body = "Saat 13:30 oldu ve henüz bir öğün kaydetmedin. Besin ve kalori hedeflerini tutturmak için yediklerini eklemeyi unutma!"
        return title to body
    }
}
