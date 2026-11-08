package com.yusufteker.healthtrack.core.database

import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * HealthTrack Offline-First Local Database.
 * Acts as the Single Source of Truth (SSOT) for the entire application.
 * Persists all entities to local device storage and emits reactive Flows to the UI.
 */
class HealthDatabase(private val settings: Settings = Settings()) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // In-memory state holding the persistent cache for instant UI rendering
    private val _foodsFlow = MutableStateFlow<List<FoodLog>>(loadList(KEY_FOODS))
    private val _goalsFlow = MutableStateFlow<Map<Int, NutritionGoal>>(loadMap(KEY_GOALS))
    private val _workoutsFlow = MutableStateFlow<List<WorkoutLog>>(loadList(KEY_WORKOUTS))
    private val _weightsFlow = MutableStateFlow<List<WeightEntry>>(loadList(KEY_WEIGHTS))
    private val _customFoodsFlow = MutableStateFlow<List<CustomFoodItem>>(loadList(KEY_CUSTOM_FOODS))
    private val _favoriteFoodsFlow = MutableStateFlow<Map<Int, List<String>>>(loadMap(KEY_FAVORITE_FOODS))

    // ─────────────────────────────────────────────────────────────
    // FOOD OPERATIONS
    // ─────────────────────────────────────────────────────────────

    fun observeFoods(userId: Int, date: String): Flow<List<FoodLog>> {
        return _foodsFlow.map { list ->
            list.filter { it.userId == userId && it.date == date && !it.isDeleted }
                .sortedByDescending { it.updatedAt }
        }
    }

    fun observeAllFoods(userId: Int): Flow<List<FoodLog>> {
        return _foodsFlow.map { list ->
            list.filter { it.userId == userId && !it.isDeleted }
                .sortedByDescending { it.updatedAt }
        }
    }

    suspend fun getFoodById(id: String): FoodLog? {
        return _foodsFlow.value.find { it.id == id && !it.isDeleted }
    }

    suspend fun upsertFood(food: FoodLog) {
        val current = _foodsFlow.value.toMutableList()
        val now = currentTimeMillis()
        val index = current.indexOfFirst {
            it.id == food.id || (it.userId == food.userId && it.date == food.date && !it.isDeleted)
        }
        if (index >= 0) {
            val existing = current[index]
            val finalId = existing.id
            current[index] = food.copy(
                id = finalId,
                createdAt = existing.createdAt,
                updatedAt = if (food.updatedAt > existing.updatedAt) food.updatedAt else now
            )
            // If any other active logs exist for the same (userId, date), mark them deleted for sync cleanup
            for (i in current.indices) {
                if (i != index && current[i].userId == food.userId && current[i].date == food.date && !current[i].isDeleted) {
                    current[i] = current[i].copy(
                        isDeleted = true,
                        rawText = "",
                        summary = null,
                        totalCalories = 0,
                        items = emptyList(),
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING
                    )
                }
            }
        } else {
            current.add(food)
        }
        _foodsFlow.value = current
        saveList(KEY_FOODS, current)
    }

    suspend fun deleteFood(id: String) {
        val current = _foodsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(
                isDeleted = true,
                rawText = "",
                summary = null,
                totalCalories = 0,
                items = emptyList(),
                syncStatus = SyncStatus.PENDING,
                updatedAt = currentTimeMillis()
            )
            _foodsFlow.value = current
            saveList(KEY_FOODS, current)
        }
    }

    fun getPendingFoods(userId: Int): List<FoodLog> {
        return _foodsFlow.value.filter { it.userId == userId && it.syncStatus == SyncStatus.PENDING }
    }

    fun getAllFoodsForSync(userId: Int): List<FoodLog> {
        return _foodsFlow.value.filter { it.userId == userId }
    }

    suspend fun markFoodsSynced(ids: List<String>) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        val current = _foodsFlow.value.toMutableList()
        var changed = false
        val iterator = current.iterator()
        val updatedList = mutableListOf<FoodLog>()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (idSet.contains(item.id)) {
                if (item.isDeleted) {
                    // Permanently purge synced deleted tombstones so they don't take local DB space
                    changed = true
                } else {
                    if (item.syncStatus == SyncStatus.PENDING) {
                        updatedList.add(item.copy(syncStatus = SyncStatus.SYNCED))
                        changed = true
                    } else {
                        updatedList.add(item)
                    }
                }
            } else {
                updatedList.add(item)
            }
        }
        if (changed) {
            _foodsFlow.value = updatedList
            saveList(KEY_FOODS, updatedList)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CUSTOM FOODS & FAVORITES OPERATIONS
    // ─────────────────────────────────────────────────────────────

    fun observeCustomFoods(userId: Int): Flow<List<CustomFoodItem>> {
        return _customFoodsFlow.map { list ->
            list.filter { it.userId == userId || it.userId == 0 }
                .sortedByDescending { it.updatedAt }
        }
    }

    suspend fun upsertCustomFood(item: CustomFoodItem) {
        val current = _customFoodsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(0, item)
        }
        _customFoodsFlow.value = current
        saveList(KEY_CUSTOM_FOODS, current)
    }

    suspend fun deleteCustomFood(id: String) {
        val current = _customFoodsFlow.value.filter { it.id != id }
        _customFoodsFlow.value = current
        saveList(KEY_CUSTOM_FOODS, current)
    }

    fun observeFavoriteFoodIds(userId: Int): Flow<Set<String>> {
        return _favoriteFoodsFlow.map { map ->
            (map[userId] ?: emptyList()).toSet()
        }
    }

    suspend fun toggleFavoriteFood(userId: Int, foodId: String): Boolean {
        val current = _favoriteFoodsFlow.value.toMutableMap()
        val userFavorites = (current[userId] ?: emptyList()).toMutableList()
        val isNowFavorite = if (userFavorites.contains(foodId)) {
            userFavorites.remove(foodId)
            false
        } else {
            userFavorites.add(foodId)
            true
        }
        current[userId] = userFavorites
        _favoriteFoodsFlow.value = current
        saveMap(KEY_FAVORITE_FOODS, current)
        return isNowFavorite
    }

    // ─────────────────────────────────────────────────────────────
    // NUTRITION GOAL OPERATIONS
    // ─────────────────────────────────────────────────────────────

    fun observeNutritionGoal(userId: Int): Flow<NutritionGoal> {
        return _goalsFlow.map { map ->
            map[userId] ?: NutritionGoal(
                id = "goal_$userId",
                userId = userId,
                updatedAt = currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )
        }
    }

    suspend fun upsertNutritionGoal(goal: NutritionGoal) {
        val current = _goalsFlow.value.toMutableMap()
        current[goal.userId] = goal
        _goalsFlow.value = current
        saveMap(KEY_GOALS, current)
    }

    fun getPendingNutritionGoal(userId: Int): NutritionGoal? {
        val goal = _goalsFlow.value[userId]
        return if (goal != null && goal.syncStatus == SyncStatus.PENDING) goal else null
    }

    fun getNutritionGoalForSync(userId: Int): NutritionGoal? {
        return _goalsFlow.value[userId]
    }

    suspend fun markNutritionGoalSynced(userId: Int) {
        val current = _goalsFlow.value.toMutableMap()
        val goal = current[userId]
        if (goal != null && goal.syncStatus == SyncStatus.PENDING) {
            current[userId] = goal.copy(syncStatus = SyncStatus.SYNCED)
            _goalsFlow.value = current
            saveMap(KEY_GOALS, current)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // WORKOUT OPERATIONS
    // ─────────────────────────────────────────────────────────────

    fun observeWorkouts(userId: Int, date: String): Flow<List<WorkoutLog>> {
        return _workoutsFlow.map { list ->
            list.filter { it.userId == userId && it.date == date && !it.isDeleted }
                .sortedByDescending { it.updatedAt }
        }
    }

    suspend fun upsertWorkout(workout: WorkoutLog) {
        val current = _workoutsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == workout.id }
        if (index >= 0) {
            current[index] = workout
        } else {
            current.add(workout)
        }
        _workoutsFlow.value = current
        saveList(KEY_WORKOUTS, current)
    }

    suspend fun deleteWorkout(id: String) {
        val current = _workoutsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(
                isDeleted = true,
                rawText = "",
                caloriesBurned = 0,
                durationMinutes = 0,
                aiAnalysis = null,
                syncStatus = SyncStatus.PENDING,
                updatedAt = currentTimeMillis()
            )
            _workoutsFlow.value = current
            saveList(KEY_WORKOUTS, current)
        }
    }

    fun getPendingWorkouts(userId: Int): List<WorkoutLog> {
        return _workoutsFlow.value.filter { it.userId == userId && it.syncStatus == SyncStatus.PENDING }
    }

    fun getAllWorkoutsForSync(userId: Int): List<WorkoutLog> {
        return _workoutsFlow.value.filter { it.userId == userId }
    }

    suspend fun markWorkoutsSynced(ids: List<String>) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        val current = _workoutsFlow.value.toMutableList()
        var changed = false
        val updatedList = mutableListOf<WorkoutLog>()
        for (item in current) {
            if (idSet.contains(item.id)) {
                if (item.isDeleted) {
                    changed = true
                } else {
                    if (item.syncStatus == SyncStatus.PENDING) {
                        updatedList.add(item.copy(syncStatus = SyncStatus.SYNCED))
                        changed = true
                    } else {
                        updatedList.add(item)
                    }
                }
            } else {
                updatedList.add(item)
            }
        }
        if (changed) {
            _workoutsFlow.value = updatedList
            saveList(KEY_WORKOUTS, updatedList)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // WEIGHT OPERATIONS
    // ─────────────────────────────────────────────────────────────

    fun getWeights(userId: Int): List<WeightEntry> = _weightsFlow.value.filter { it.userId == userId && !it.isDeleted }

    fun observeWeights(userId: Int): Flow<List<WeightEntry>> {
        return _weightsFlow.map { list ->
            list.filter { it.userId == userId && !it.isDeleted }
                .groupBy { it.date }
                .map { (_, entries) -> entries.maxByOrNull { it.updatedAt } ?: entries.first() }
                .sortedBy { it.date }
        }
    }

    suspend fun upsertWeight(weight: WeightEntry) {
        val current = _weightsFlow.value.toMutableList()
        val now = currentTimeMillis()
        val index = current.indexOfFirst {
            it.id == weight.id || (it.userId == weight.userId && it.date == weight.date && !it.isDeleted)
        }
        if (index >= 0) {
            val existing = current[index]
            current[index] = weight.copy(
                id = existing.id,
                createdAt = existing.createdAt,
                updatedAt = if (weight.updatedAt > existing.updatedAt) weight.updatedAt else now
            )
            // Mark any other duplicate active weights for the same (userId, date) as deleted
            for (i in current.indices) {
                if (i != index && current[i].userId == weight.userId && current[i].date == weight.date && !current[i].isDeleted) {
                    current[i] = current[i].copy(
                        isDeleted = true,
                        note = null,
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING
                    )
                }
            }
        } else {
            current.add(weight)
        }
        _weightsFlow.value = current
        saveList(KEY_WEIGHTS, current)
    }

    suspend fun deleteWeight(id: String) {
        val current = _weightsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(
                isDeleted = true,
                note = null,
                syncStatus = SyncStatus.PENDING,
                updatedAt = currentTimeMillis()
            )
            _weightsFlow.value = current
            saveList(KEY_WEIGHTS, current)
        }
    }

    fun getPendingWeights(userId: Int): List<WeightEntry> {
        return _weightsFlow.value.filter { it.userId == userId && it.syncStatus == SyncStatus.PENDING }
    }

    fun getAllWeightsForSync(userId: Int): List<WeightEntry> {
        return _weightsFlow.value.filter { it.userId == userId }
    }

    suspend fun markWeightsSynced(ids: List<String>) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        val current = _weightsFlow.value.toMutableList()
        var changed = false
        val updatedList = mutableListOf<WeightEntry>()
        for (item in current) {
            if (idSet.contains(item.id)) {
                if (item.isDeleted) {
                    changed = true
                } else {
                    if (item.syncStatus == SyncStatus.PENDING) {
                        updatedList.add(item.copy(syncStatus = SyncStatus.SYNCED))
                        changed = true
                    } else {
                        updatedList.add(item)
                    }
                }
            } else {
                updatedList.add(item)
            }
        }
        if (changed) {
            _weightsFlow.value = updatedList
            saveList(KEY_WEIGHTS, updatedList)
        }
    }

    /**
     * Cleans up any corrupted dates (e.g., negative years) and deduplicates multiple active records
     * for the same (userId, date) so only the most recently updated record remains active while
     * older duplicates are queued as deletion tombstones for the remote server.
     */
    suspend fun deduplicateAndSanitizeLocalData(userId: Int, isoDateRegex: Regex) {
        val now = currentTimeMillis()

        // 1. Foods: remove invalid dates, deduplicate same-day active logs
        val currentFoods = _foodsFlow.value.toMutableList()
        var foodsChanged = false
        val validFoods = currentFoods.filter {
            val valid = isoDateRegex.matches(it.date)
            if (!valid) foodsChanged = true
            valid
        }.toMutableList()

        val activeFoodsByDate = validFoods
            .filter { it.userId == userId && !it.isDeleted }
            .groupBy { it.date }

        for ((_, logs) in activeFoodsByDate) {
            if (logs.size > 1) {
                val winner = logs.maxByOrNull { it.updatedAt } ?: logs.first()
                for (i in validFoods.indices) {
                    val item = validFoods[i]
                    if (item.userId == userId && item.date == winner.date && !item.isDeleted && item.id != winner.id) {
                        validFoods[i] = item.copy(
                            isDeleted = true,
                            rawText = "",
                            summary = null,
                            totalCalories = 0,
                            items = emptyList(),
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING
                        )
                        foodsChanged = true
                    }
                }
            }
        }
        if (foodsChanged) {
            _foodsFlow.value = validFoods
            saveList(KEY_FOODS, validFoods)
        }

        // 2. Weights: remove invalid dates, deduplicate same-day active weights
        val currentWeights = _weightsFlow.value.toMutableList()
        var weightsChanged = false
        val validWeights = currentWeights.filter {
            val valid = isoDateRegex.matches(it.date)
            if (!valid) weightsChanged = true
            valid
        }.toMutableList()

        val activeWeightsByDate = validWeights
            .filter { it.userId == userId && !it.isDeleted }
            .groupBy { it.date }

        for ((_, entries) in activeWeightsByDate) {
            if (entries.size > 1) {
                val winner = entries.maxByOrNull { it.updatedAt } ?: entries.first()
                for (i in validWeights.indices) {
                    val item = validWeights[i]
                    if (item.userId == userId && item.date == winner.date && !item.isDeleted && item.id != winner.id) {
                        validWeights[i] = item.copy(
                            isDeleted = true,
                            note = null,
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING
                        )
                        weightsChanged = true
                    }
                }
            }
        }
        if (weightsChanged) {
            _weightsFlow.value = validWeights
            saveList(KEY_WEIGHTS, validWeights)
        }

        // 3. Workouts: remove invalid dates
        val currentWorkouts = _workoutsFlow.value.toMutableList()
        val validWorkouts = currentWorkouts.filter { isoDateRegex.matches(it.date) }
        if (validWorkouts.size != currentWorkouts.size) {
            _workoutsFlow.value = validWorkouts
            saveList(KEY_WORKOUTS, validWorkouts)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SYNC PULL MERGE ENGINE (LAST-WRITE-WINS & TOMBSTONE PURGE)
    // ─────────────────────────────────────────────────────────────

    suspend fun mergePullFoods(incoming: List<FoodLog>) {
        if (incoming.isEmpty()) return
        val now = currentTimeMillis()
        val current = _foodsFlow.value.toMutableList()
        incoming.forEach { remote ->
            val index = current.indexOfFirst { it.id == remote.id }
            if (remote.isDeleted) {
                if (index >= 0) {
                    val local = current[index]
                    if (remote.updatedAt >= local.updatedAt) {
                        current.removeAt(index)
                    }
                }
                return@forEach
            }

            if (index >= 0) {
                val local = current[index]
                if (local.syncStatus == SyncStatus.PENDING && local.updatedAt > remote.updatedAt) {
                    // Keep newer unsynced local modification
                } else if (remote.updatedAt >= local.updatedAt) {
                    current[index] = remote.copy(syncStatus = SyncStatus.SYNCED)
                }
            } else {
                // Check if an active log for the exact same day already exists with a different ID
                val sameDateIndex = current.indexOfFirst { it.userId == remote.userId && it.date == remote.date && !it.isDeleted }
                if (sameDateIndex >= 0) {
                    val local = current[sameDateIndex]
                    if (remote.updatedAt > local.updatedAt) {
                        // Remote is newer: mark old local ID as deleted so server cleans it up, and add remote
                        current[sameDateIndex] = local.copy(
                            isDeleted = true,
                            rawText = "",
                            summary = null,
                            totalCalories = 0,
                            items = emptyList(),
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING
                        )
                        current.add(remote.copy(syncStatus = SyncStatus.SYNCED))
                    } else {
                        // Local is newer: mark outdated remote ID as deleted so server cleans up its ghost
                        current.add(
                            remote.copy(
                                isDeleted = true,
                                rawText = "",
                                summary = null,
                                totalCalories = 0,
                                items = emptyList(),
                                updatedAt = now,
                                syncStatus = SyncStatus.PENDING
                            )
                        )
                    }
                } else {
                    current.add(remote.copy(syncStatus = SyncStatus.SYNCED))
                }
            }
        }
        _foodsFlow.value = current
        saveList(KEY_FOODS, current)
    }

    suspend fun mergePullNutritionGoal(remote: NutritionGoal) {
        val current = _goalsFlow.value.toMutableMap()
        val local = current[remote.userId]
        if (local == null || (remote.updatedAt >= local.updatedAt && !(local.syncStatus == SyncStatus.PENDING && local.updatedAt > remote.updatedAt))) {
            current[remote.userId] = remote.copy(syncStatus = SyncStatus.SYNCED)
            _goalsFlow.value = current
            saveMap(KEY_GOALS, current)
        }
    }

    suspend fun mergePullWorkouts(incoming: List<WorkoutLog>) {
        if (incoming.isEmpty()) return
        val current = _workoutsFlow.value.toMutableList()
        incoming.forEach { remote ->
            val index = current.indexOfFirst { it.id == remote.id }
            if (remote.isDeleted) {
                if (index >= 0) {
                    val local = current[index]
                    if (remote.updatedAt >= local.updatedAt) {
                        current.removeAt(index)
                    }
                }
                return@forEach
            }

            if (index >= 0) {
                val local = current[index]
                if (local.syncStatus == SyncStatus.PENDING && local.updatedAt > remote.updatedAt) {
                    // Keep newer local modification
                } else if (remote.updatedAt >= local.updatedAt) {
                    current[index] = remote.copy(syncStatus = SyncStatus.SYNCED)
                }
            } else {
                current.add(remote.copy(syncStatus = SyncStatus.SYNCED))
            }
        }
        _workoutsFlow.value = current
        saveList(KEY_WORKOUTS, current)
    }

    suspend fun mergePullWeights(incoming: List<WeightEntry>) {
        if (incoming.isEmpty()) return
        val now = currentTimeMillis()
        val current = _weightsFlow.value.toMutableList()
        incoming.forEach { remote ->
            val index = current.indexOfFirst { it.id == remote.id }
            if (remote.isDeleted) {
                if (index >= 0) {
                    val local = current[index]
                    if (remote.updatedAt >= local.updatedAt) {
                        current.removeAt(index)
                    }
                }
                return@forEach
            }

            if (index >= 0) {
                val local = current[index]
                if (local.syncStatus == SyncStatus.PENDING && local.updatedAt > remote.updatedAt) {
                    // Keep newer local modification
                } else if (remote.updatedAt >= local.updatedAt) {
                    current[index] = remote.copy(syncStatus = SyncStatus.SYNCED)
                }
            } else {
                // Check if an active weight entry for the exact same day already exists with a different ID
                val sameDateIndex = current.indexOfFirst { it.userId == remote.userId && it.date == remote.date && !it.isDeleted }
                if (sameDateIndex >= 0) {
                    val local = current[sameDateIndex]
                    if (remote.updatedAt > local.updatedAt) {
                        current[sameDateIndex] = local.copy(
                            isDeleted = true,
                            note = null,
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING
                        )
                        current.add(remote.copy(syncStatus = SyncStatus.SYNCED))
                    } else {
                        current.add(
                            remote.copy(
                                isDeleted = true,
                                note = null,
                                updatedAt = now,
                                syncStatus = SyncStatus.PENDING
                            )
                        )
                    }
                } else {
                    current.add(remote.copy(syncStatus = SyncStatus.SYNCED))
                }
            }
        }
        _weightsFlow.value = current
        saveList(KEY_WEIGHTS, current)
    }

    suspend fun clearAll() {
        _foodsFlow.value = emptyList()
        _goalsFlow.value = emptyMap()
        _workoutsFlow.value = emptyList()
        _weightsFlow.value = emptyList()
        _customFoodsFlow.value = emptyList()
        _favoriteFoodsFlow.value = emptyMap()
        settings.remove(KEY_FOODS)
        settings.remove(KEY_GOALS)
        settings.remove(KEY_WORKOUTS)
        settings.remove(KEY_WEIGHTS)
        settings.remove(KEY_CUSTOM_FOODS)
        settings.remove(KEY_FAVORITE_FOODS)
    }

    // ─────────────────────────────────────────────────────────────
    // PERSISTENCE HELPERS
    // ─────────────────────────────────────────────────────────────

    private inline fun <reified T> loadList(key: String): List<T> {
        val serialized = settings.getStringOrNull(key) ?: return emptyList()
        return try {
            json.decodeFromString(serialized)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private inline fun <reified T> saveList(key: String, list: List<T>) {
        try {
            settings.putString(key, json.encodeToString(list))
        } catch (_: Exception) {}
    }

    private inline fun <reified K, reified V> loadMap(key: String): Map<K, V> {
        val serialized = settings.getStringOrNull(key) ?: return emptyMap()
        return try {
            json.decodeFromString(serialized)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private inline fun <reified K, reified V> saveMap(key: String, map: Map<K, V>) {
        try {
            settings.putString(key, json.encodeToString(map))
        } catch (_: Exception) {}
    }

    private fun currentTimeMillis(): Long = com.yusufteker.healthtrack.core.utils.currentTimeMillis()

    companion object {
        private const val KEY_FOODS = "ht_db_foods"
        private const val KEY_GOALS = "ht_db_goals"
        private const val KEY_WORKOUTS = "ht_db_workouts"
        private const val KEY_WEIGHTS = "ht_db_weights"
        private const val KEY_CUSTOM_FOODS = "ht_db_custom_foods"
        private const val KEY_FAVORITE_FOODS = "ht_db_favorite_foods"
    }
}
