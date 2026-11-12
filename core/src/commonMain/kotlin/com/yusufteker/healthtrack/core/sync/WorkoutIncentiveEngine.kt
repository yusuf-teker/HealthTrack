package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.model.ScheduledReminderPayload
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency
import com.yusufteker.healthtrack.core.utils.calculateDaysBetween
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.core.utils.shiftDateIso

object WorkoutIncentiveEngine {

    const val WORKOUT_CHANNEL_ID = "ht_workout_incentive_channel"
    const val REST_DAY_CALORIE_THRESHOLD = 400 // Dün 400+ kcal yaktıysa dinlenme günü

    /**
     * Identifies the user's primary/favorite sport activity based on workout history.
     * Looks at rawText keywords and matches against common fitness categories.
     */
    fun detectFavoriteSport(workoutHistory: List<WorkoutLog>): String? {
        val nonStepWorkouts = workoutHistory.filter { !it.isDailySteps && !it.isDeleted && it.rawText.isNotBlank() }
        if (nonStepWorkouts.isEmpty()) return null

        val counts = mutableMapOf<String, Int>()

        for (w in nonStepWorkouts) {
            val text = w.rawText.lowercase()
            when {
                text.contains("eliptik") || text.contains("elliptical") ->
                    counts["eliptik"] = (counts["eliptik"] ?: 0) + 1

                text.contains("kürek") || text.contains("kurek") || text.contains("rowing") ->
                    counts["kürek"] = (counts["kürek"] ?: 0) + 1

                text.contains("yürüyüş") || text.contains("yuruyus") || text.contains("walking") || text.contains("yürü") ->
                    counts["yürüyüş"] = (counts["yürüyüş"] ?: 0) + 1

                text.contains("koşu") || text.contains("kosu") || text.contains("running") || text.contains("koş") ->
                    counts["koşu"] = (counts["koşu"] ?: 0) + 1

                text.contains("bisiklet") || text.contains("cycling") || text.contains("pedal") ->
                    counts["bisiklet"] = (counts["bisiklet"] ?: 0) + 1

                text.contains("ağırlık") || text.contains("agirlik") || text.contains("fitness") || text.contains("bodybuilding") || text.contains("gym") || text.contains("dumbell") ->
                    counts["ağırlık"] = (counts["ağırlık"] ?: 0) + 1

                text.contains("yüzme") || text.contains("yuzme") || text.contains("swimming") ->
                    counts["yüzme"] = (counts["yüzme"] ?: 0) + 1

                text.contains("pilates") || text.contains("yoga") || text.contains("esneme") ->
                    counts["pilates"] = (counts["pilates"] ?: 0) + 1
            }
        }

        return counts.maxByOrNull { it.value }?.key
    }

    /**
     * Determines whether an evening workout incentive notification should be sent today.
     *
     * Rules:
     * 1. Frequency == OFF -> false
     * 2. If workout is already logged today -> false
     * 3. If notification already sent today -> false
     * 4. If yesterday burned >= 400 kcal -> false (Rest Day / Dinlenme Günü)
     * 5. SMART_CADENCE schedule:
     *    - Day 1 without workout -> Send
     *    - Day 2 without workout (consecutive ignore) -> Skip (do not bother user)
     *    - Day 3 without workout -> Send
     *    - Afterwards -> Every 2 days (send only if >= 2 days since last notification)
     * 6. EVERY_2_DAYS -> Send if >= 2 days since last notification
     * 7. THREE_DAYS_WEEK -> Send on Mon, Wed, Fri if no workout
     * 8. EVERY_DAY -> Send every day if no workout today
     */
    fun shouldTriggerWorkoutReminder(
        frequency: WorkoutReminderFrequency,
        todayIso: String,
        hasWorkoutToday: Boolean,
        yesterdayCaloriesBurned: Int,
        lastNotificationDate: String?,
        ignoredConsecutiveDays: Int,
        dayOfWeek: Int = 1 // 1: Mon, ..., 7: Sun
    ): Boolean {
        if (frequency == WorkoutReminderFrequency.OFF) return false

        // 1. Zaten bugün spor yapıldıysa asla bildirim gönderme
        if (hasWorkoutToday) return false

        // 2. Bugün zaten bildirim gönderildiyse tekrar gönderme
        if (lastNotificationDate == todayIso) return false

        // 3. Dün 400+ kcal yakıldıysa bugün dinlenme günü say ve bildirim atma
        if (yesterdayCaloriesBurned >= REST_DAY_CALORIE_THRESHOLD) return false

        val daysSinceLastNotification = if (lastNotificationDate != null) {
            calculateDaysBetween(lastNotificationDate, todayIso).coerceAtLeast(0)
        } else {
            999
        }

        return when (frequency) {
            WorkoutReminderFrequency.SMART_CADENCE -> {
                when {
                    // İlk gün: Bildirim at
                    ignoredConsecutiveDays == 0 -> true
                    // 2. gün: İhmal edildiğinde kullanıcıyı darlama, es geç
                    ignoredConsecutiveDays == 1 -> false
                    // 3. gün: 3. gün tekrar at
                    ignoredConsecutiveDays == 2 -> true
                    // Sonrasında: 2 günde 1 at (en az 2 gün geçmiş olmalı)
                    else -> daysSinceLastNotification >= 2
                }
            }

            WorkoutReminderFrequency.EVERY_2_DAYS -> {
                daysSinceLastNotification >= 2
            }

            WorkoutReminderFrequency.THREE_DAYS_WEEK -> {
                // Pazartesi (1), Çarşamba (3), Cuma (5)
                dayOfWeek in listOf(1, 3, 5) && daysSinceLastNotification >= 1
            }

            WorkoutReminderFrequency.EVERY_DAY -> {
                true
            }

            WorkoutReminderFrequency.OFF -> false
        }
    }

    /**
     * Builds a personalized, encouraging notification message based on the user's favorite sport.
     */
    fun buildWorkoutReminderPayload(
        favoriteSport: String?,
        userName: String = ""
    ): ScheduledReminderPayload {
        val greeting = if (userName.isNotBlank()) "Hey $userName! " else ""

        val (title, body) = when (favoriteSport) {
            "eliptik" -> Pair(
                "🚴‍♂️ Eliptik Bisiklet Seni Bekliyor!",
                "${greeting}Günü eliptik bisiklet üzerinde hareketle kapatmaya ne dersin? 25-30 dakikalık tempolu bir seans tüm günün yorgunluğunu alır."
            )

            "kürek" -> Pair(
                "🚣‍♂️ Kürek Seansı Zamanı!",
                "${greeting}Günü kürek başında geride bırakalım mı? 20 dakikalık bir kürek antrenmanı zihnini de açar."
            )

            "yürüyüş" -> Pair(
                "🚶‍♂️ Akşam Yürüyüşü Vakti!",
                "${greeting}Temiz bir akşam havası alıp 30 dakika tempolu yürümeye ne dersin? Adım hedefine çok yaklaştın."
            )

            "koşu" -> Pair(
                "🏃‍♂️ Koşu Ayakkabılarını Hazırla!",
                "${greeting}Günün stresini tempolu bir koşuyla geride bırak. 20-25 dakikalık bir koşu enerjini tazeler."
            )

            "bisiklet" -> Pair(
                "🚲 Pedal Çevirme Zamanı!",
                "${greeting}Akşam serinliğinde keyifli bir bisiklet turu ile kalorini yak ve gününü zinde tamamla."
            )

            "ağırlık" -> Pair(
                "🏋️ Kaslar Hareketi Özledi!",
                "${greeting}Günü kısa ve etkili bir güç antrenmanıyla taçlandır. Güçlü bir kapanış yapmaya hazır mısın?"
            )

            "pilates" -> Pair(
                "🧘‍♀️ Esneme & Pilates Vakti",
                "${greeting}Vücuduna hak ettiği esnekliği ver. 20 dakikalık hafif bir pilates/esneme seansı seni bekliyor."
            )

            "yüzme" -> Pair(
                "🏊‍♂️ Su Seni Çağırıyor!",
                "${greeting}Harika bir yüzme seansıyla tüm kaslarını çalıştırmaya ve rahatlamaya ne dersin?"
            )

            else -> Pair(
                "🔥 Bugün Kendine 20 Dakika Ayır!",
                "${greeting}Akşam oldu, vücudunu harekete geçirme zamanı! Hafif bir yürüyüş veya antrenmanla günü zinde bitir."
            )
        }

        return ScheduledReminderPayload(
            title = title,
            body = body,
            targetTab = "WORKOUT",
            channelId = WORKOUT_CHANNEL_ID
        )
    }
}
