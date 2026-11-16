package com.yusufteker.healthtrack

import com.yusufteker.healthtrack.core.model.MealHealthEvaluator
import com.yusufteker.healthtrack.core.network.FoodAiResultDto
import com.yusufteker.healthtrack.core.network.evaluateMealHealth
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FoodAiResultTest {
    @Test
    fun responseWithoutHealthScoreUsesLocalEvaluation() {
        val result = Json.decodeFromString<FoodAiResultDto>(
            """{"summary":"Salata","totalCalories":150,"items":[]}"""
        )

        assertNull(result.healthScore)
        assertEquals(
            MealHealthEvaluator.evaluate(result.items, result.totalCalories, result.summary),
            result.evaluateMealHealth()
        )
    }

    @Test
    fun validResponseHealthScoreIsUsedAndInvalidScoresFallBackToLocalEvaluation() {
        val result = Json.decodeFromString<FoodAiResultDto>(
            """{"summary":"Salata","totalCalories":150,"items":[],"healthScore":92}"""
        )

        assertEquals(92, result.evaluateMealHealth().score)
        val fallback = result.copy(healthScore = null).evaluateMealHealth().score
        assertEquals(fallback, result.copy(healthScore = 120).evaluateMealHealth().score)
        assertEquals(fallback, result.copy(healthScore = -10).evaluateMealHealth().score)
    }
}
