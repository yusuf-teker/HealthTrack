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
    private val _appointmentsFlow = MutableStateFlow<List<AppointmentEntry>>(loadList(KEY_APPOINTMENTS))
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

    suspend fun getFoodById(id: String): FoodLog? {
        return _foodsFlow.value.find { it.id == id && !it.isDeleted }
    }

    suspend fun upsertFood(food: FoodLog) {
        val current = _foodsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == food.id }
        if (index >= 0) {
            current[index] = food
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

    suspend fun markFoodsSynced(ids: List<String>) {
        if (ids.isEmpty()) return
        val current = _foodsFlow.value.toMutableList()
        var changed = false
        for (i in current.indices) {
            if (ids.contains(current[i].id) && current[i].syncStatus == SyncStatus.PENDING) {
                current[i] = current[i].copy(syncStatus = SyncStatus.SYNCED)
                changed = true
            }
        }
        if (changed) {
            _foodsFlow.value = current
            saveList(KEY_FOODS, current)
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

    suspend fun markWorkoutsSynced(ids: List<String>) {
        if (ids.isEmpty()) return
        val current = _workoutsFlow.value.toMutableList()
        var changed = false
        for (i in current.indices) {
            if (ids.contains(current[i].id) && current[i].syncStatus == SyncStatus.PENDING) {
                current[i] = current[i].copy(syncStatus = SyncStatus.SYNCED)
                changed = true
            }
        }
        if (changed) {
            _workoutsFlow.value = current
            saveList(KEY_WORKOUTS, current)
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
        val index = current.indexOfFirst {
            it.id == weight.id || (it.userId == weight.userId && it.date == weight.date && !it.isDeleted)
        }
        if (index >= 0) {
            val existing = current[index]
            current[index] = weight.copy(
                id = existing.id,
                createdAt = existing.createdAt,
                updatedAt = if (weight.updatedAt > existing.updatedAt) weight.updatedAt else currentTimeMillis()
            )
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

    suspend fun markWeightsSynced(ids: List<String>) {
        if (ids.isEmpty()) return
        val current = _weightsFlow.value.toMutableList()
        var changed = false
        for (i in current.indices) {
            if (ids.contains(current[i].id) && current[i].syncStatus == SyncStatus.PENDING) {
                current[i] = current[i].copy(syncStatus = SyncStatus.SYNCED)
                changed = true
            }
        }
        if (changed) {
            _weightsFlow.value = current
            saveList(KEY_WEIGHTS, current)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // APPOINTMENT OPERATIONS
    // ─────────────────────────────────────────────────────────────

    fun observeAppointments(userId: Int): Flow<List<AppointmentEntry>> {
        return _appointmentsFlow.map { list ->
            list.filter { it.userId == userId && !it.isDeleted }
                .sortedBy { it.dateTime }
        }
    }

    suspend fun upsertAppointment(appointment: AppointmentEntry) {
        val current = _appointmentsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == appointment.id }
        if (index >= 0) {
            current[index] = appointment
        } else {
            current.add(appointment)
        }
        _appointmentsFlow.value = current
        saveList(KEY_APPOINTMENTS, current)
    }

    suspend fun deleteAppointment(id: String) {
        val current = _appointmentsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(
                isDeleted = true,
                syncStatus = SyncStatus.PENDING,
                updatedAt = currentTimeMillis()
            )
            _appointmentsFlow.value = current
            saveList(KEY_APPOINTMENTS, current)
        }
    }

    fun getPendingAppointments(userId: Int): List<AppointmentEntry> {
        return _appointmentsFlow.value.filter { it.userId == userId && it.syncStatus == SyncStatus.PENDING }
    }

    suspend fun markAppointmentsSynced(ids: List<String>) {
        if (ids.isEmpty()) return
        val current = _appointmentsFlow.value.toMutableList()
        var changed = false
        for (i in current.indices) {
            if (ids.contains(current[i].id) && current[i].syncStatus == SyncStatus.PENDING) {
                current[i] = current[i].copy(syncStatus = SyncStatus.SYNCED)
                changed = true
            }
        }
        if (changed) {
            _appointmentsFlow.value = current
            saveList(KEY_APPOINTMENTS, current)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SYNC PULL MERGE ENGINE (LAST-WRITE-WINS)
    // ─────────────────────────────────────────────────────────────

    suspend fun mergePullFoods(incoming: List<FoodLog>) {
        if (incoming.isEmpty()) return
        val current = _foodsFlow.value.toMutableList()
        incoming.forEach { remote ->
            val index = current.indexOfFirst { it.id == remote.id }
            if (index >= 0) {
                val local = current[index]
                if (remote.updatedAt >= local.updatedAt) {
                    current[index] = remote.copy(syncStatus = SyncStatus.SYNCED)
                }
            } else {
                current.add(remote.copy(syncStatus = SyncStatus.SYNCED))
            }
        }
        _foodsFlow.value = current
        saveList(KEY_FOODS, current)
    }

    suspend fun mergePullNutritionGoal(remote: NutritionGoal) {
        val current = _goalsFlow.value.toMutableMap()
        val local = current[remote.userId]
        if (local == null || remote.updatedAt >= local.updatedAt) {
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
            if (index >= 0) {
                val local = current[index]
                if (remote.updatedAt >= local.updatedAt) {
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
        val current = _weightsFlow.value.toMutableList()
        incoming.forEach { remote ->
            val index = current.indexOfFirst { it.id == remote.id }
            if (index >= 0) {
                val local = current[index]
                if (remote.updatedAt >= local.updatedAt) {
                    current[index] = remote.copy(syncStatus = SyncStatus.SYNCED)
                }
            } else {
                current.add(remote.copy(syncStatus = SyncStatus.SYNCED))
            }
        }
        _weightsFlow.value = current
        saveList(KEY_WEIGHTS, current)
    }

    suspend fun mergePullAppointments(incoming: List<AppointmentEntry>) {
        if (incoming.isEmpty()) return
        val current = _appointmentsFlow.value.toMutableList()
        incoming.forEach { remote ->
            val index = current.indexOfFirst { it.id == remote.id }
            if (index >= 0) {
                val local = current[index]
                if (remote.updatedAt >= local.updatedAt) {
                    current[index] = remote.copy(syncStatus = SyncStatus.SYNCED)
                }
            } else {
                current.add(remote.copy(syncStatus = SyncStatus.SYNCED))
            }
        }
        _appointmentsFlow.value = current
        saveList(KEY_APPOINTMENTS, current)
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
        private const val KEY_APPOINTMENTS = "ht_db_appointments"
        private const val KEY_CUSTOM_FOODS = "ht_db_custom_foods"
        private const val KEY_FAVORITE_FOODS = "ht_db_favorite_foods"
    }
}
