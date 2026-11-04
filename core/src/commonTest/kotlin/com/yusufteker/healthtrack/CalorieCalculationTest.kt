package com.yusufteker.healthtrack

import com.yusufteker.healthtrack.core.model.FoodItem
import com.yusufteker.healthtrack.core.model.FoodLog
import com.yusufteker.healthtrack.core.model.WorkoutLog
import kotlin.test.Test
import kotlin.test.assertEquals

class CalorieCalculationTest {

    @Test
    fun testFoodTotalCaloriesCalculation() {
        val items = listOf(
            FoodItem(id = "1", name = "Süt", formattedPortion = "50 ml", calories = 25, proteinG = 1.6, carbsG = 2.4, fatG = 0.8),
            FoodItem(id = "2", name = "Yumurta", formattedPortion = "2 Adet", calories = 140, proteinG = 12.0, carbsG = 1.0, fatG = 10.0),
            FoodItem(id = "3", name = "Ekmek", formattedPortion = "1 Dilim", calories = 70, proteinG = 2.5, carbsG = 14.0, fatG = 0.5)
        )

        val totalCalories = items.sumOf { it.calories }
        val totalProtein = items.sumOf { it.proteinG }
        val totalCarbs = items.sumOf { it.carbsG }
        val totalFat = items.sumOf { it.fatG }

        val foodLog = FoodLog(
            id = "test_food_1",
            userId = 1,
            date = "2026-11-04",
            rawText = "sabah 50ml süt, 2 yumurta, 1 dilim ekmek",
            totalCalories = totalCalories,
            items = items
        )

        assertEquals(235, foodLog.totalCalories)
        assertEquals(16.1, totalProtein, 0.01)
        assertEquals(17.4, totalCarbs, 0.01)
        assertEquals(11.3, totalFat, 0.01)
    }

    @Test
    fun testNetCalorieCalculation() {
        val consumedCalories = 1850
        val burnedWorkoutCalories = 450

        val netCalories = consumedCalories - burnedWorkoutCalories
        assertEquals(1400, netCalories)

        val dailyTarget = 2000
        val remainingToTarget = dailyTarget - netCalories
        assertEquals(600, remainingToTarget)
    }
}
