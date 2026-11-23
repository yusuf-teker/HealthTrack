package com.yusufteker.healthtrack

import com.russhwolf.settings.MapSettings
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.MealPreferencesCatalog
import com.yusufteker.healthtrack.core.model.NutrientProgress
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.DailyMealPlanEngine
import com.yusufteker.healthtrack.core.sync.SmartMealEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmartMealPreferencesTest {

    @Test
    fun testTurkishFoldingAndBoundarySafeMatching() {
        // Turkish diacritics & case folding
        assertTrue(MealPreferencesCatalog.matchesText("150g Süzme Yoğurt", "yogurt"))
        assertTrue(MealPreferencesCatalog.matchesText("150g Süzme Yoğurt", "dairy"))
        assertTrue(MealPreferencesCatalog.matchesText("150g Greek Yogurt", "dairy"))
        assertTrue(MealPreferencesCatalog.matchesText("1 Avuç Taze Ispanak", "ispanak"))
        assertTrue(MealPreferencesCatalog.matchesText("160g Tavuğu Izgara", "chicken"))
        assertTrue(MealPreferencesCatalog.matchesText("50g Lor Peyniri", "dairy"))

        // Short-word boundary safety: "et" must NOT match "Omlet" or "Keten"
        assertFalse(MealPreferencesCatalog.matchesText("Ispanaklı Omlet", "et"))
        assertFalse(MealPreferencesCatalog.matchesText("Keten Tohumlu Süzme Yoğurt", "et"))
        assertFalse(MealPreferencesCatalog.matchesText("Ispanaklı Lorlu Omlet", "red_meat"))
        assertFalse(MealPreferencesCatalog.matchesText("Keten Tohumlu Kase", "red_meat"))

        // Whole word "et" still matches
        assertTrue(MealPreferencesCatalog.matchesText("150g Izgara Dana Et", "red_meat"))
        assertTrue(MealPreferencesCatalog.matchesText("150g Dana Et", "et"))

        // Key normalization
        assertEquals("chicken", MealPreferencesCatalog.normalizeKey("Tavuk", MealPreferencesCatalog.favoritePresets))
        assertEquals("chicken", MealPreferencesCatalog.normalizeKey("Chicken", MealPreferencesCatalog.favoritePresets))
        assertEquals("dairy", MealPreferencesCatalog.normalizeKey("Süt & Laktoz", MealPreferencesCatalog.allergyPresets))
        assertEquals("karabuğday", MealPreferencesCatalog.normalizeKey("  Karabuğday  "))
    }

    @Test
    fun testSmartMealEngineExcludesAllergiesAndDislikes() {
        val dairyExcluded = SmartMealEngine.matchSmartMeals(
            remainingCalories = 500,
            missingProteinG = 30.0,
            nutrientBars = emptyList(),
            recent3DaysFoodNames = emptySet(),
            excludedIngredients = setOf("dairy"),
            language = AppLanguage.TR
        )
        assertTrue(dairyExcluded.isNotEmpty())
        assertTrue(
            dairyExcluded.none { meal ->
                val text = (listOf(meal.title, meal.subtitle, meal.instructions.orEmpty()) + meal.ingredients).joinToString(" ")
                MealPreferencesCatalog.matchesText(text, "dairy")
            },
            "No dairy meal should remain when dairy is excluded"
        )

        val nutsExcluded = SmartMealEngine.matchSmartMeals(
            remainingCalories = 500,
            missingProteinG = 10.0,
            nutrientBars = emptyList(),
            recent3DaysFoodNames = emptySet(),
            excludedIngredients = setOf("nuts"),
            language = AppLanguage.TR
        )
        assertTrue(nutsExcluded.isNotEmpty())
        assertTrue(
            nutsExcluded.none { meal ->
                meal.id == "meal_banana_peanut_butter" ||
                    meal.id == "meal_green_apple_walnut" ||
                    meal.id == "meal_tuna_arugula_bowl" ||
                    meal.id == "meal_berry_cottage_parfait" ||
                    meal.id == "meal_boiled_eggs_walnuts_bellpepper"
            }
        )

        val customMushroomExcluded = SmartMealEngine.matchSmartMeals(
            remainingCalories = 500,
            missingProteinG = 20.0,
            nutrientBars = emptyList(),
            recent3DaysFoodNames = emptySet(),
            excludedIngredients = setOf("mantar"),
            language = AppLanguage.TR
        )
        assertTrue(customMushroomExcluded.none { it.id == "meal_spinach_mushroom_omelet" })

        val fishExcluded = SmartMealEngine.matchSmartMeals(
            remainingCalories = 500,
            missingProteinG = 25.0,
            nutrientBars = emptyList(),
            recent3DaysFoodNames = emptySet(),
            excludedIngredients = setOf("fish"),
            language = AppLanguage.TR
        )
        assertTrue(fishExcluded.none { it.id == "meal_tuna_arugula_bowl" || it.id == "meal_salmon_green_salad" })
    }

    @Test
    fun testSmartMealEngineBoostsFavoriteIngredientsInTurkishAndEnglish() {
        val fiberDeficitBars = listOf(
            NutrientProgress(key = "fiber", label = "Lif", current = 2.0, target = 30.0, unit = "g", colorHex = "#43A047")
        )

        val trMeals = SmartMealEngine.matchSmartMeals(
            remainingCalories = 500,
            missingProteinG = 30.0,
            nutrientBars = fiberDeficitBars,
            recent3DaysFoodNames = emptySet(),
            favoriteIngredients = setOf("chicken", "salad"),
            language = AppLanguage.TR
        )
        assertTrue(trMeals.isNotEmpty())
        assertEquals("meal_grilled_chicken_salad", trMeals.first().id)
        assertTrue(trMeals.first().matchedFavorites.containsAll(listOf("Tavuk", "Salata")))

        // Meals matching at least one favorite should outrank non-favorite high-fiber meals
        val firstNonFavoriteIdx = trMeals.indexOfFirst { it.matchedFavorites.isEmpty() }
        val lastFavoriteIdx = trMeals.indexOfLast { it.matchedFavorites.isNotEmpty() }
        assertTrue(firstNonFavoriteIdx > lastFavoriteIdx, "All favorite-matched meals should appear before non-favorites")

        val enMeals = SmartMealEngine.matchSmartMeals(
            remainingCalories = 500,
            missingProteinG = 30.0,
            nutrientBars = fiberDeficitBars,
            recent3DaysFoodNames = emptySet(),
            favoriteIngredients = setOf("chicken", "salad"),
            language = AppLanguage.EN
        )
        assertEquals("meal_grilled_chicken_salad", enMeals.first().id)
        assertTrue(enMeals.first().matchedFavorites.containsAll(listOf("Chicken", "Salad")))
    }

    @Test
    fun testDailyMealPlanEngineRespectsExclusionsAndFavorites() = runTest {
        val settings = MapSettings()
        val apiClient = HealthApiClient(TokenStorage(settings), baseUrl = "http://localhost")
        val engine = DailyMealPlanEngine(apiClient)
        val goal = NutritionGoal(id = "g1", userId = 1, calories = 2000, proteinG = 110.0, fiberG = 30.0)

        val allergenFreePlan = engine.generateDailyPlan(
            userId = 1,
            dateIso = "2026-11-23",
            recentDaysFoodLogs = emptyList(),
            goal = goal,
            fitnessGoal = FitnessGoal.MAINTAIN,
            userWeightKg = 75.0,
            excludedIngredients = setOf("dairy", "egg"),
            favoriteIngredients = emptySet(),
            language = AppLanguage.TR
        )
        allergenFreePlan.allSlots.forEach { slot ->
            val slotText = (listOf(slot.title, slot.subtitle, slot.instructions) + slot.ingredients).joinToString(" ")
            assertFalse(MealPreferencesCatalog.matchesText(slotText, "dairy"), "Slot ${slot.title} should not contain dairy")
            assertFalse(MealPreferencesCatalog.matchesText(slotText, "egg"), "Slot ${slot.title} should not contain egg")
        }

        val favoritePlan = engine.generateDailyPlan(
            userId = 1,
            dateIso = "2026-11-23",
            recentDaysFoodLogs = emptyList(),
            goal = goal,
            fitnessGoal = FitnessGoal.BUILD_MUSCLE,
            userWeightKg = 75.0,
            excludedIngredients = emptySet(),
            favoriteIngredients = setOf("chicken", "salad"),
            language = AppLanguage.TR
        )
        val lunchText = (listOf(favoritePlan.lunch.title) + favoritePlan.lunch.ingredients).joinToString(" ")
        assertTrue(MealPreferencesCatalog.matchesText(lunchText, "chicken"))
        assertTrue(MealPreferencesCatalog.matchesText(lunchText, "salad"))
        val dinnerText = (listOf(favoritePlan.dinner.title) + favoritePlan.dinner.ingredients).joinToString(" ")
        assertTrue(MealPreferencesCatalog.matchesText(dinnerText, "chicken"))
    }

    @Test
    fun testTokenStoragePersistenceAndConflictResolution() {
        val settings = MapSettings()
        val storage = TokenStorage(settings)
        storage.saveSession(
            userId = 7,
            name = "Test",
            username = "tester",
            accessToken = "tok",
            refreshToken = "ref"
        )

        storage.setExcludedFoodIngredients(setOf("dairy", "nuts"))
        storage.setFavoriteFoodIngredients(setOf("chicken", "salad"))

        val reloaded = TokenStorage(settings)
        assertEquals(setOf("dairy", "nuts"), reloaded.getExcludedFoodIngredients())
        assertEquals(setOf("chicken", "salad"), reloaded.getFavoriteFoodIngredients())

        assertTrue(MealPreferencesCatalog.conflictingFavoritesForExcluded("dairy").containsAll(listOf("yogurt", "cheese")))
        assertTrue(MealPreferencesCatalog.conflictingExcludesForFavorite("yogurt").contains("dairy"))
    }
}
