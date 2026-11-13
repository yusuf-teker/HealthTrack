package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
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
        userName: String = "",
        language: AppLanguage = LocalizationManager.currentLanguage
    ): ScheduledReminderPayload {
        val greeting = if (userName.isNotBlank()) "Hey $userName! " else ""

        val (title, body) = when (favoriteSport) {
            "eliptik" -> Pair(
                trEn("🚴‍♂️ Eliptik Bisiklet Seni Bekliyor!", "🚴‍♂️ Your Elliptical Session Awaits!", language),
                trEn(
                    "${greeting}Günü eliptik bisiklet üzerinde hareketle kapatmaya ne dersin? 25-30 dakikalık tempolu bir seans tüm günün yorgunluğunu alır.",
                    "${greeting}How about finishing the day on the elliptical? A brisk 25-30 minute session melts away the day's fatigue.",
                    language
                )
            )

            "kürek" -> Pair(
                trEn("🚣‍♂️ Kürek Seansı Zamanı!", "🚣‍♂️ Time for a Rowing Session!", language),
                trEn(
                    "${greeting}Günü kürek başında geride bırakalım mı? 20 dakikalık bir kürek antrenmanı zihnini de açar.",
                    "${greeting}Ready to wrap up the day on the rower? Even a 20-minute rowing workout clears your mind.",
                    language
                )
            )

            "yürüyüş" -> Pair(
                trEn("🚶‍♂️ Akşam Yürüyüşü Vakti!", "🚶‍♂️ Time for an Evening Walk!", language),
                trEn(
                    "${greeting}Temiz bir akşam havası alıp 30 dakika tempolu yürümeye ne dersin? Adım hedefine çok yaklaştın.",
                    "${greeting}How about getting some fresh evening air with a 30-minute brisk walk? You're close to your step goal!",
                    language
                )
            )

            "koşu" -> Pair(
                trEn("🏃‍♂️ Koşu Ayakkabılarını Hazırla!", "🏃‍♂️ Lace Up Your Running Shoes!", language),
                trEn(
                    "${greeting}Günün stresini tempolu bir koşuyla geride bırak. 20-25 dakikalık bir koşu enerjini tazeler.",
                    "${greeting}Leave the day's stress behind with a brisk run. A 20-25 minute run will recharge your energy.",
                    language
                )
            )

            "bisiklet" -> Pair(
                trEn("🚲 Pedal Çevirme Zamanı!", "🚲 Time to Hit the Pedals!", language),
                trEn(
                    "${greeting}Akşam serinliğinde keyifli bir bisiklet turu ile kalorini yak ve gününü zinde tamamla.",
                    "${greeting}Burn calories with a refreshing evening bike ride and finish your day feeling energized.",
                    language
                )
            )

            "ağırlık" -> Pair(
                trEn("🏋️ Kaslar Hareketi Özledi!", "🏋️ Your Muscles Miss Moving!", language),
                trEn(
                    "${greeting}Günü kısa ve etkili bir güç antrenmanıyla taçlandır. Güçlü bir kapanış yapmaya hazır mısın?",
                    "${greeting}Cap off the day with a short, effective strength session. Ready for a strong finish?",
                    language
                )
            )

            "pilates" -> Pair(
                trEn("🧘‍♀️ Esneme & Pilates Vakti", "🧘‍♀️ Stretch & Pilates Time", language),
                trEn(
                    "${greeting}Vücuduna hak ettiği esnekliği ver. 20 dakikalık hafif bir pilates/esneme seansı seni bekliyor.",
                    "${greeting}Give your body the mobility it deserves. A gentle 20-minute pilates or stretching session awaits.",
                    language
                )
            )

            "yüzme" -> Pair(
                trEn("🏊‍♂️ Su Seni Çağırıyor!", "🏊‍♂️ The Pool Is Calling!", language),
                trEn(
                    "${greeting}Harika bir yüzme seansıyla tüm kaslarını çalıştırmaya ve rahatlamaya ne dersin?",
                    "${greeting}How about engaging your full body and unwinding with a refreshing swim session?",
                    language
                )
            )

            else -> Pair(
                trEn("🔥 Bugün Kendine 20 Dakika Ayır!", "🔥 Take 20 Minutes for Yourself Today!", language),
                trEn(
                    "${greeting}Akşam oldu, vücudunu harekete geçirme zamanı! Hafif bir yürüyüş veya antrenmanla günü zinde bitir.",
                    "${greeting}Evening is here — time to get moving! Finish the day strong with a light walk or workout.",
                    language
                )
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
