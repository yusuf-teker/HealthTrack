package com.yusufteker.healthtrack

import com.russhwolf.settings.MapSettings
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.DailyMealPlanEngine
import com.yusufteker.healthtrack.core.utils.toLocaleDoubleOrNull
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Phase1CoreFixesTest {

    @Test
    fun testToLocaleDoubleOrNullHandlesDotAndComma() {
        assertEquals(74.5, "74.5".toLocaleDoubleOrNull())
        assertEquals(74.5, "74,5".toLocaleDoubleOrNull())
        assertEquals(82.25, "  82,25  ".toLocaleDoubleOrNull())
        assertNull("".toLocaleDoubleOrNull())
        assertNull("abc".toLocaleDoubleOrNull())
    }

    @Test
    fun testTokenStorageClearAndAccountSwitchWipesUserScopedData() {
        val storage = TokenStorage(MapSettings())

        storage.saveSession(
            userId = 1,
            name = "User One",
            username = "user1",
            accessToken = "tok1",
            refreshToken = "ref1"
        )
        assertTrue(storage.isLoggedIn())
        storage.setUserWeight(81.5)
        storage.setWhatsAppNumber("05551112233")
        storage.setCustomGeminiApiKey("AIza-secret-1")

        // Switching account directly without logout must clear previous user's scoped prefs
        storage.saveSession(
            userId = 2,
            name = "User Two",
            username = "user2",
            accessToken = "tok2",
            refreshToken = "ref2"
        )
        assertEquals(0.0, storage.getUserWeight())
        assertEquals("", storage.getWhatsAppNumber())
        assertNull(storage.getCustomGeminiApiKey())

        // Clearing session must log out and clear user-scoped prefs
        storage.setUserWeight(65.0)
        storage.setCustomGeminiApiKey("AIza-secret-2")
        storage.clearSession()

        assertFalse(storage.isLoggedIn())
        assertEquals(0, storage.getUserId())
        assertEquals(0.0, storage.getUserWeight())
        assertNull(storage.getCustomGeminiApiKey())
    }

    @Test
    fun testDailyMealPlanDoesNotClaimAiWithoutRealAiPlan() = runTest {
        val storage = TokenStorage(MapSettings())
        val apiClient = HealthApiClient(storage, baseUrl = "http://127.0.0.1:0")
        val engine = DailyMealPlanEngine(apiClient)

        val plan = engine.generateDailyPlan(
            userId = 1,
            dateIso = "2026-11-13",
            recentDaysFoodLogs = emptyList(),
            goal = NutritionGoal(id = "g1", userId = 1, calories = 2000),
            fitnessGoal = FitnessGoal.MAINTAIN,
            userWeightKg = 75.0
        )

        assertFalse(plan.isAiGenerated)
        assertEquals(4, plan.allSlots.size)
    }
}
