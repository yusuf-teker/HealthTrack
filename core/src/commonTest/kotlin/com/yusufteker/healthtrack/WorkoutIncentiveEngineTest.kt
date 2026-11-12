package com.yusufteker.healthtrack

import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
import com.yusufteker.healthtrack.core.sync.WorkoutIncentiveEngine
import com.yusufteker.healthtrack.core.sync.WeightReminderEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkoutIncentiveEngineTest {

    @Test
    fun detectFavoriteSport_correctlyIdentifiesElliptical() {
        val workouts = listOf(
            WorkoutLog(id = "1", userId = 1, date = "2026-11-10", rawText = "30 dk eliptik bisiklet"),
            WorkoutLog(id = "2", userId = 1, date = "2026-11-08", rawText = "40 dk eliptik bisiklet ter döktüm"),
            WorkoutLog(id = "3", userId = 1, date = "2026-11-06", rawText = "15 dk yürüyüş")
        )
        val favorite = WorkoutIncentiveEngine.detectFavoriteSport(workouts)
        assertEquals("eliptik", favorite)
    }

    @Test
    fun detectFavoriteSport_correctlyIdentifiesRowing() {
        val workouts = listOf(
            WorkoutLog(id = "1", userId = 1, date = "2026-11-10", rawText = "20 dk kürek antrenmanı"),
            WorkoutLog(id = "2", userId = 1, date = "2026-11-08", rawText = "Rowing seansı"),
            WorkoutLog(id = "3", userId = 1, date = "2026-11-06", rawText = "kurek cektim")
        )
        val favorite = WorkoutIncentiveEngine.detectFavoriteSport(workouts)
        assertEquals("kürek", favorite)
    }

    @Test
    fun detectFavoriteSport_ignoresDailySteps() {
        val workouts = listOf(
            WorkoutLog(id = "step_1", userId = 1, date = "2026-11-10", rawText = "10000 Adım • Günlük Hareket"),
            WorkoutLog(id = "w_1", userId = 1, date = "2026-11-08", rawText = "30 dk yürüyüş")
        )
        val favorite = WorkoutIncentiveEngine.detectFavoriteSport(workouts)
        assertEquals("yürüyüş", favorite)
    }

    @Test
    fun shouldTriggerWorkoutReminder_blocksIfWorkoutDoneToday() {
        val shouldTrigger = WorkoutIncentiveEngine.shouldTriggerWorkoutReminder(
            frequency = WorkoutReminderFrequency.SMART_CADENCE,
            todayIso = "2026-11-12",
            hasWorkoutToday = true,
            yesterdayCaloriesBurned = 200,
            lastNotificationDate = null,
            ignoredConsecutiveDays = 0
        )
        assertFalse(shouldTrigger, "Bugün spor yapılmışsa bildirim tetiklenmemeli")
    }

    @Test
    fun shouldTriggerWorkoutReminder_blocksIfYesterdayBurnedOver400Calories() {
        val shouldTrigger = WorkoutIncentiveEngine.shouldTriggerWorkoutReminder(
            frequency = WorkoutReminderFrequency.SMART_CADENCE,
            todayIso = "2026-11-12",
            hasWorkoutToday = false,
            yesterdayCaloriesBurned = 450, // 400+ kcal (Rest Day)
            lastNotificationDate = null,
            ignoredConsecutiveDays = 0
        )
        assertFalse(shouldTrigger, "Dün 400+ kcal yakıldıysa bugün dinlenme günü sayılmalı")
    }

    @Test
    fun shouldTriggerWorkoutReminder_smartCadence_rulesCycle() {
        val today = "2026-11-12"

        // Gün 0 (ilk gün): Bildirim gitmeli
        val day0 = WorkoutIncentiveEngine.shouldTriggerWorkoutReminder(
            frequency = WorkoutReminderFrequency.SMART_CADENCE,
            todayIso = today,
            hasWorkoutToday = false,
            yesterdayCaloriesBurned = 150,
            lastNotificationDate = null,
            ignoredConsecutiveDays = 0
        )
        assertTrue(day0, "1. gün bildirim gitmeli")

        // Gün 1 (2. gün ihmal): Kullanıcı darlanmaz, es geçilir
        val day1 = WorkoutIncentiveEngine.shouldTriggerWorkoutReminder(
            frequency = WorkoutReminderFrequency.SMART_CADENCE,
            todayIso = today,
            hasWorkoutToday = false,
            yesterdayCaloriesBurned = 0,
            lastNotificationDate = "2026-11-11",
            ignoredConsecutiveDays = 1
        )
        assertFalse(day1, "2. gün es geçilmeli, bildirim gitmemeli")

        // Gün 2 (3. gün): 3. gün tekrar bildirim gitmeli!
        val day2 = WorkoutIncentiveEngine.shouldTriggerWorkoutReminder(
            frequency = WorkoutReminderFrequency.SMART_CADENCE,
            todayIso = today,
            hasWorkoutToday = false,
            yesterdayCaloriesBurned = 0,
            lastNotificationDate = "2026-11-10",
            ignoredConsecutiveDays = 2
        )
        assertTrue(day2, "3. gün tekrar bildirim gitmeli")

        // Sonrasında: 1 gün geçmişse bildirim yok, 2 gün geçmişse bildirim var
        val dayLater1Day = WorkoutIncentiveEngine.shouldTriggerWorkoutReminder(
            frequency = WorkoutReminderFrequency.SMART_CADENCE,
            todayIso = "2026-11-14",
            hasWorkoutToday = false,
            yesterdayCaloriesBurned = 0,
            lastNotificationDate = "2026-11-13",
            ignoredConsecutiveDays = 3
        )
        assertFalse(dayLater1Day, "Sonrasında her gün değil 2 günde 1 atılmalı (1 gün geçtiyse atılmaz)")

        val dayLater2Days = WorkoutIncentiveEngine.shouldTriggerWorkoutReminder(
            frequency = WorkoutReminderFrequency.SMART_CADENCE,
            todayIso = "2026-11-15",
            hasWorkoutToday = false,
            yesterdayCaloriesBurned = 0,
            lastNotificationDate = "2026-11-13",
            ignoredConsecutiveDays = 4
        )
        assertTrue(dayLater2Days, "Sonrasında 2 gün geçtiğinde bildirim gitmeli")
    }

    @Test
    fun buildWorkoutReminderPayload_containsPersonalizedSport() {
        val payloadElliptical = WorkoutIncentiveEngine.buildWorkoutReminderPayload("eliptik", "Yusuf")
        assertTrue(payloadElliptical.title.contains("Eliptik"))
        assertTrue(payloadElliptical.body.contains("eliptik bisiklet"))

        val payloadRowing = WorkoutIncentiveEngine.buildWorkoutReminderPayload("kürek", "Yusuf")
        assertTrue(payloadRowing.title.contains("Kürek"))
        assertTrue(payloadRowing.body.contains("kürek antrenmanı"))
    }

    @Test
    fun weightReminderEngine_triggersEvery3DaysCorrectly() {
        val today = "2026-11-12"

        // Son tartım 2 gün önce -> false
        val trigger2Days = WeightReminderEngine.shouldTriggerWeightReminder(
            frequency = WeightReminderFrequency.EVERY_3_DAYS,
            todayIso = today,
            latestWeightDate = "2026-11-10",
            lastNotificationDate = null
        )
        assertFalse(trigger2Days, "3 gün dolmadan bildirim gitmemeli")

        // Son tartım 3 gün önce -> true
        val trigger3Days = WeightReminderEngine.shouldTriggerWeightReminder(
            frequency = WeightReminderFrequency.EVERY_3_DAYS,
            todayIso = today,
            latestWeightDate = "2026-11-09",
            lastNotificationDate = null
        )
        assertTrue(trigger3Days, "3 gün dolduğunda bildirim gitmeli")

        // Bugün zaten tartıldıysa -> false
        val triggerToday = WeightReminderEngine.shouldTriggerWeightReminder(
            frequency = WeightReminderFrequency.EVERY_3_DAYS,
            todayIso = today,
            latestWeightDate = today,
            lastNotificationDate = null
        )
        assertFalse(triggerToday, "Bugün zaten tartıldıysa bildirim gitmemeli")
    }
}

