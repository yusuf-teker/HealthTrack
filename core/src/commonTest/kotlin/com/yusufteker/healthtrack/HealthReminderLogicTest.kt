package com.yusufteker.healthtrack

import com.russhwolf.settings.MapSettings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.FoodLog
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.utils.HealthReminderLogic
import com.yusufteker.healthtrack.core.utils.calculateDaysBetween
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthReminderLogicTest {

    @Test
    fun testCalculateDaysBetween() {
        assertEquals(3, calculateDaysBetween("2026-11-09", "2026-11-12"))
        assertEquals(0, calculateDaysBetween("2026-11-12", "2026-11-12"))
        assertEquals(1, calculateDaysBetween("2026-11-11", "2026-11-12"))
        assertEquals(7, calculateDaysBetween("2026-11-05", "2026-11-12"))
    }

    @Test
    fun testWeightReminderDaily() = runTest {
        val settings = MapSettings()
        val database = HealthDatabase(settings)
        val tokenStorage = TokenStorage(settings)

        val userId = 10
        val today = "2026-11-12"

        tokenStorage.setWeightReminderFrequency(WeightReminderFrequency.DAILY)

        // No weights logged yet -> should send reminder
        assertTrue(HealthReminderLogic.shouldSendWeightReminder(userId, today, database, tokenStorage))

        // Log weight today -> should NOT send reminder
        database.upsertWeight(WeightEntry(id = "w1", userId = userId, date = today, weightKg = 75.0))
        assertFalse(HealthReminderLogic.shouldSendWeightReminder(userId, today, database, tokenStorage))
    }

    @Test
    fun testWeightReminderEvery3Days() = runTest {
        val settings = MapSettings()
        val database = HealthDatabase(settings)
        val tokenStorage = TokenStorage(settings)

        val userId = 10
        val today = "2026-11-12"

        tokenStorage.setWeightReminderFrequency(WeightReminderFrequency.EVERY_3_DAYS)

        // Latest weight logged 2 days ago ("2026-11-10") -> should NOT send reminder
        database.upsertWeight(WeightEntry(id = "w1", userId = userId, date = "2026-11-10", weightKg = 75.0))
        assertFalse(HealthReminderLogic.shouldSendWeightReminder(userId, today, database, tokenStorage))

        // If latest weight was 3 days ago ("2026-11-09") -> should send reminder
        database.deleteWeight("w1")
        database.upsertWeight(WeightEntry(id = "w2", userId = userId, date = "2026-11-09", weightKg = 75.0))
        assertTrue(HealthReminderLogic.shouldSendWeightReminder(userId, today, database, tokenStorage))

        // If user already notified today -> should NOT send reminder
        tokenStorage.setLastWeightReminderDate(today)
        assertFalse(HealthReminderLogic.shouldSendWeightReminder(userId, today, database, tokenStorage))
    }

    @Test
    fun testWeightReminderOff() = runTest {
        val settings = MapSettings()
        val database = HealthDatabase(settings)
        val tokenStorage = TokenStorage(settings)

        val userId = 10
        val today = "2026-11-12"

        tokenStorage.setWeightReminderFrequency(WeightReminderFrequency.OFF)
        assertFalse(HealthReminderLogic.shouldSendWeightReminder(userId, today, database, tokenStorage))
    }

    @Test
    fun testFoodReminder() = runTest {
        val settings = MapSettings()
        val database = HealthDatabase(settings)
        val tokenStorage = TokenStorage(settings)

        val userId = 10
        val today = "2026-11-12"

        tokenStorage.setFoodReminderEnabled(true)

        // No food logged yet today -> should send reminder
        assertTrue(HealthReminderLogic.shouldSendFoodReminder(userId, today, database, tokenStorage))

        // Food logged today -> should NOT send reminder
        database.upsertFood(FoodLog(id = "f1", userId = userId, date = today, rawText = "Öğle yemeği tavuk pilav", totalCalories = 500))
        assertFalse(HealthReminderLogic.shouldSendFoodReminder(userId, today, database, tokenStorage))

        // If food reminder is disabled -> should NOT send reminder
        tokenStorage.setFoodReminderEnabled(false)
        assertFalse(HealthReminderLogic.shouldSendFoodReminder(userId, today, database, tokenStorage))
    }
}
