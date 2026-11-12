package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.model.ScheduledReminderPayload
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
import com.yusufteker.healthtrack.core.utils.calculateDaysBetween

object WeightReminderEngine {

    const val WEIGHT_CHANNEL_ID = "ht_weight_reminder_channel"

    /**
     * Evaluates whether a weight reminder notification should be triggered today.
     */
    fun shouldTriggerWeightReminder(
        frequency: WeightReminderFrequency,
        todayIso: String,
        latestWeightDate: String?,
        lastNotificationDate: String?
    ): Boolean {
        if (frequency == WeightReminderFrequency.OFF) return false

        // Bugün zaten kilo kaydedilmişse bildirim gönderme
        if (latestWeightDate == todayIso) return false

        // Bugün zaten bildirim atılmışsa tekrar gönderme
        if (lastNotificationDate == todayIso) return false

        // Son tartılmadan bu yana kaç gün geçti?
        val daysSinceLastWeight = if (latestWeightDate != null) {
            calculateDaysBetween(latestWeightDate, todayIso).coerceAtLeast(0)
        } else {
            999
        }

        // Son bildirimden bu yana kaç gün geçti?
        val daysSinceLastNotification = if (lastNotificationDate != null) {
            calculateDaysBetween(lastNotificationDate, todayIso).coerceAtLeast(0)
        } else {
            999
        }

        val requiredInterval = frequency.days
        if (requiredInterval <= 0) return false

        // Hem son tartımdan hem son bildirimden en az requiredInterval geçmiş olmalı (veya son tartımdan geçmiş ve bugün henüz uyarılmamış)
        return daysSinceLastWeight >= requiredInterval && daysSinceLastNotification >= requiredInterval
    }

    /**
     * Builds a personalized smart weight reminder payload.
     */
    fun buildWeightReminderPayload(
        latestWeightKg: Double?,
        daysSinceLastWeight: Int,
        userName: String = ""
    ): ScheduledReminderPayload {
        val greeting = if (userName.isNotBlank()) "Hey $userName! " else ""

        val (title, body) = when {
            latestWeightKg != null && daysSinceLastWeight >= 3 -> Pair(
                "⚖️ Tartılma Vaktin Geldi!",
                "${greeting}Son tartımının üzerinden $daysSinceLastWeight gün geçti (Son: $latestWeightKg kg). Gelişimini görmek için kilonu kaydetmeye ne dersin?"
            )
            latestWeightKg != null -> Pair(
                "⚖️ Kilonu Güncelle",
                "${greeting}Düzenli tartım doğru kalori ve besin hedeflerinin anahtarıdır. Son kilon: $latestWeightKg kg."
            )
            else -> Pair(
                "⚖️ İlk Kilonu Kaydet",
                "${greeting}Sağlık hedeflerine doğru ilerlerken kilonu kaydederek yolculuğunu başlat!"
            )
        }

        return ScheduledReminderPayload(
            title = title,
            body = body,
            targetTab = "WEIGHT",
            channelId = WEIGHT_CHANNEL_ID
        )
    }
}
