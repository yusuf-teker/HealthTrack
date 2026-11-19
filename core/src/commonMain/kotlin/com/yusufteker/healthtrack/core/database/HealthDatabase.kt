package com.yusufteker.healthtrack.core.database

import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class FavoriteFoodListPayload(
    val userId: Int,
    val foodIds: List<String>
)

/**
 * HealthTrack Offline-First Local Database.
 * Acts as the Single Source of Truth (SSOT) for the entire application.
 *
 * Backed by normalized SQLite tables via [SqliteHealthRecordStore] on Android/iOS/JVM
 * (with one-time automatic migration from legacy [Settings] JSON blobs) and [Settings]
 * fallback on Web (JS/WasmJS).
 */
class HealthDatabase(
    private val settings: Settings = Settings(),
    private val recordStore: HealthRecordStore = createDefaultHealthRecordStore(settings)
) {

    private val dbMutex = Mutex()
    private val useRowStore: Boolean = recordStore !is SettingsHealthRecordStore

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _foodsFlow = MutableStateFlow<List<FoodLog>>(emptyList())
    private val _goalsFlow = MutableStateFlow<Map<Int, NutritionGoal>>(emptyMap())
    private val _workoutsFlow = MutableStateFlow<List<WorkoutLog>>(emptyList())
    private val _weightsFlow = MutableStateFlow<List<WeightEntry>>(emptyList())
    private val _customFoodsFlow = MutableStateFlow<List<CustomFoodItem>>(emptyList())
    private val _favoriteFoodsFlow = MutableStateFlow<Map<Int, List<String>>>(emptyMap())
    private val _reportsFlow = MutableStateFlow<List<HealthAiReport>>(emptyList())
    private val _waterFlow = MutableStateFlow<List<WaterEntry>>(emptyList())
    private val _dailyMealPlansFlow = MutableStateFlow<List<DailyMealPlan>>(emptyList())

    init {
        if (useRowStore) {
            migrateLegacySettingsIfNeeded()
            _foodsFlow.value = loadBucketList(BUCKET_FOODS)
            _goalsFlow.value = loadBucketList<NutritionGoal>(BUCKET_GOALS).associateBy { it.userId }
            _workoutsFlow.value = loadBucketList(BUCKET_WORKOUTS)
            _weightsFlow.value = loadBucketList(BUCKET_WEIGHTS)
            _customFoodsFlow.value = loadBucketList(BUCKET_CUSTOM_FOODS)
            _favoriteFoodsFlow.value = loadBucketList<FavoriteFoodListPayload>(BUCKET_FAVORITE_FOODS)
                .associate { it.userId to it.foodIds }
            _reportsFlow.value = loadBucketList(BUCKET_AI_REPORTS)
            _waterFlow.value = loadBucketList(BUCKET_WATER)
            _dailyMealPlansFlow.value = loadBucketList(BUCKET_DAILY_MEAL_PLANS)
        } else {
            _foodsFlow.value = loadList(KEY_FOODS)
            _goalsFlow.value = loadMap(KEY_GOALS)
            _workoutsFlow.value = loadList(KEY_WORKOUTS)
            _weightsFlow.value = loadList(KEY_WEIGHTS)
            _customFoodsFlow.value = loadList(KEY_CUSTOM_FOODS)
            _favoriteFoodsFlow.value = loadMap(KEY_FAVORITE_FOODS)
            _reportsFlow.value = loadList(KEY_AI_REPORTS)
            _waterFlow.value = loadList(KEY_WATER)
            _dailyMealPlansFlow.value = loadList(KEY_DAILY_MEAL_PLANS)
        }
    }

    /**
     * One-time atomic migration from legacy [Settings] JSON blobs into SQLite [HealthRecordStore].
     * Ensures users updating from older versions keep all historical data intact.
     */
    private fun migrateLegacySettingsIfNeeded() {
        if (settings.getBoolean(KEY_SQLITE_MIGRATED_V1, false)) return
        try {
            val rows = mutableListOf<HealthRecordRow>()

            loadList<FoodLog>(KEY_FOODS).forEach { rows.add(it.toRow()) }
            loadMap<Int, NutritionGoal>(KEY_GOALS).values.forEach { rows.add(it.toRow()) }
            loadList<WorkoutLog>(KEY_WORKOUTS).forEach { rows.add(it.toRow()) }
            loadList<WeightEntry>(KEY_WEIGHTS).forEach { rows.add(it.toRow()) }
            loadList<CustomFoodItem>(KEY_CUSTOM_FOODS).forEach { rows.add(it.toRow()) }
            loadMap<Int, List<String>>(KEY_FAVORITE_FOODS).forEach { (uid, ids) ->
                rows.add(favoriteRow(uid, ids))
            }
            loadList<HealthAiReport>(KEY_AI_REPORTS).forEach { rows.add(it.toRow()) }
            loadList<WaterEntry>(KEY_WATER).forEach { rows.add(it.toRow()) }
            loadList<DailyMealPlan>(KEY_DAILY_MEAL_PLANS).forEach { rows.add(it.toRow()) }

            if (rows.isNotEmpty() && recordStore.isEmpty()) {
                recordStore.upsertRecords(rows)
                Napier.i("Migrated ${rows.size} legacy records into SQLite.", tag = "HEALTH_DB")
            }
            settings.putBoolean(KEY_SQLITE_MIGRATED_V1, true)
        } catch (e: Exception) {
            Napier.e("Legacy Settings -> SQLite migration failed: ${e.message}", e, tag = "HEALTH_DB")
        }
    }

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

    fun getFoods(userId: Int, date: String): List<FoodLog> {
        return _foodsFlow.value.filter { it.userId == userId && it.date == date && !it.isDeleted }
    }

    fun getAllFoods(userId: Int): List<FoodLog> {
        return _foodsFlow.value.filter { it.userId == userId && !it.isDeleted }
    }

    suspend fun getFoodById(id: String): FoodLog? {
        return _foodsFlow.value.find { it.id == id && !it.isDeleted }
    }

    suspend fun upsertFood(food: FoodLog) = dbMutex.withLock {
        val current = _foodsFlow.value.toMutableList()
        val now = currentTimeMillis()
        val changedRows = mutableListOf<HealthRecordRow>()
        val index = current.indexOfFirst {
            it.id == food.id || (it.userId == food.userId && it.date == food.date && !it.isDeleted)
        }
        if (index >= 0) {
            val existing = current[index]
            val updated = food.copy(
                id = existing.id,
                createdAt = existing.createdAt,
                updatedAt = if (food.updatedAt > existing.updatedAt) food.updatedAt else now
            )
            current[index] = updated
            changedRows.add(updated.toRow())
            for (i in current.indices) {
                if (i != index && current[i].userId == food.userId && current[i].date == food.date && !current[i].isDeleted) {
                    val tombstone = current[i].copy(
                        isDeleted = true,
                        rawText = "",
                        summary = null,
                        totalCalories = 0,
                        items = emptyList(),
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING
                    )
                    current[i] = tombstone
                    changedRows.add(tombstone.toRow())
                }
            }
        } else {
            current.add(food)
            changedRows.add(food.toRow())
        }
        _foodsFlow.value = current
        persistChanges(KEY_FOODS, current, changedRows)
    }

    suspend fun deleteFood(id: String) = dbMutex.withLock {
        val current = _foodsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val tombstone = current[index].copy(
                isDeleted = true,
                rawText = "",
                summary = null,
                totalCalories = 0,
                items = emptyList(),
                syncStatus = SyncStatus.PENDING,
                updatedAt = currentTimeMillis()
            )
            current[index] = tombstone
            _foodsFlow.value = current
            persistChanges(KEY_FOODS, current, listOf(tombstone.toRow()))
        }
    }

    fun getPendingFoods(userId: Int): List<FoodLog> {
        return _foodsFlow.value.filter { it.userId == userId && it.syncStatus == SyncStatus.PENDING }
    }

    fun getAllFoodsForSync(userId: Int): List<FoodLog> {
        return _foodsFlow.value.filter { it.userId == userId }
    }

    suspend fun markFoodsSynced(ids: List<String>) = dbMutex.withLock {
        if (ids.isEmpty()) return@withLock
        val idSet = ids.toSet()
        val current = _foodsFlow.value
        var changed = false
        val updatedList = mutableListOf<FoodLog>()
        val rowsToUpsert = mutableListOf<HealthRecordRow>()
        val idsToDelete = mutableListOf<String>()

        for (item in current) {
            if (idSet.contains(item.id)) {
                if (item.isDeleted) {
                    idsToDelete.add(item.id)
                    changed = true
                } else if (item.syncStatus == SyncStatus.PENDING) {
                    val synced = item.copy(syncStatus = SyncStatus.SYNCED)
                    updatedList.add(synced)
                    rowsToUpsert.add(synced.toRow())
                    changed = true
                } else {
                    updatedList.add(item)
                }
            } else {
                updatedList.add(item)
            }
        }
        if (changed) {
            _foodsFlow.value = updatedList
            persistChanges(KEY_FOODS, updatedList, rowsToUpsert, BUCKET_FOODS, idsToDelete)
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

    suspend fun upsertCustomFood(item: CustomFoodItem) = dbMutex.withLock {
        val current = _customFoodsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(0, item)
        }
        _customFoodsFlow.value = current
        persistChanges(KEY_CUSTOM_FOODS, current, listOf(item.toRow()))
    }

    suspend fun deleteCustomFood(id: String) = dbMutex.withLock {
        val current = _customFoodsFlow.value.filter { it.id != id }
        _customFoodsFlow.value = current
        persistChanges(KEY_CUSTOM_FOODS, current, emptyList(), BUCKET_CUSTOM_FOODS, listOf(id))
    }

    fun observeFavoriteFoodIds(userId: Int): Flow<Set<String>> {
        return _favoriteFoodsFlow.map { map ->
            (map[userId] ?: emptyList()).toSet()
        }
    }

    suspend fun toggleFavoriteFood(userId: Int, foodId: String): Boolean = dbMutex.withLock {
        val current = _favoriteFoodsFlow.value.toMutableMap()
        val userFavorites = (current[userId] ?: emptyList()).toMutableList()
        val isNowFavorite = if (userFavorites.contains(foodId)) {
            userFavorites.remove(foodId)
            false
        } else {
            userFavorites.add(0, foodId)
            true
        }
        current[userId] = userFavorites
        _favoriteFoodsFlow.value = current
        if (useRowStore) {
            recordStore.upsertRecord(favoriteRow(userId, userFavorites))
        } else {
            saveMap(KEY_FAVORITE_FOODS, current)
        }
        return@withLock isNowFavorite
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

    suspend fun upsertNutritionGoal(goal: NutritionGoal) = dbMutex.withLock {
        val current = _goalsFlow.value.toMutableMap()
        current[goal.userId] = goal
        _goalsFlow.value = current
        if (useRowStore) {
            recordStore.upsertRecord(goal.toRow())
        } else {
            saveMap(KEY_GOALS, current)
        }
    }

    fun getPendingNutritionGoal(userId: Int): NutritionGoal? {
        val goal = _goalsFlow.value[userId]
        return if (goal != null && goal.syncStatus == SyncStatus.PENDING) goal else null
    }

    fun getNutritionGoalForSync(userId: Int): NutritionGoal? {
        return _goalsFlow.value[userId]
    }

    suspend fun markNutritionGoalSynced(userId: Int) = dbMutex.withLock {
        val current = _goalsFlow.value.toMutableMap()
        val goal = current[userId]
        if (goal != null && goal.syncStatus == SyncStatus.PENDING) {
            val synced = goal.copy(syncStatus = SyncStatus.SYNCED)
            current[userId] = synced
            _goalsFlow.value = current
            if (useRowStore) {
                recordStore.upsertRecord(synced.toRow())
            } else {
                saveMap(KEY_GOALS, current)
            }
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

    fun observeAllWorkouts(userId: Int): Flow<List<WorkoutLog>> {
        return _workoutsFlow.map { list ->
            list.filter { it.userId == userId && !it.isDeleted }
                .sortedByDescending { it.updatedAt }
        }
    }

    fun getWorkouts(userId: Int, date: String): List<WorkoutLog> {
        return _workoutsFlow.value.filter { it.userId == userId && it.date == date && !it.isDeleted }
    }

    fun getAllWorkouts(userId: Int): List<WorkoutLog> {
        return _workoutsFlow.value.filter { it.userId == userId && !it.isDeleted }
    }

    suspend fun upsertWorkout(workout: WorkoutLog) = dbMutex.withLock {
        val current = _workoutsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == workout.id }
        if (index >= 0) {
            current[index] = workout
        } else {
            current.add(workout)
        }
        _workoutsFlow.value = current
        persistChanges(KEY_WORKOUTS, current, listOf(workout.toRow()))
    }

    suspend fun deleteWorkout(id: String) = dbMutex.withLock {
        val current = _workoutsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val tombstone = current[index].copy(
                isDeleted = true,
                rawText = "",
                caloriesBurned = 0,
                durationMinutes = 0,
                aiAnalysis = null,
                syncStatus = SyncStatus.PENDING,
                updatedAt = currentTimeMillis()
            )
            current[index] = tombstone
            _workoutsFlow.value = current
            persistChanges(KEY_WORKOUTS, current, listOf(tombstone.toRow()))
        }
    }

    fun getPendingWorkouts(userId: Int): List<WorkoutLog> {
        return _workoutsFlow.value.filter { it.userId == userId && it.syncStatus == SyncStatus.PENDING }
    }

    fun getAllWorkoutsForSync(userId: Int): List<WorkoutLog> {
        return _workoutsFlow.value.filter { it.userId == userId }
    }

    suspend fun markWorkoutsSynced(ids: List<String>) = dbMutex.withLock {
        if (ids.isEmpty()) return@withLock
        val idSet = ids.toSet()
        val current = _workoutsFlow.value
        var changed = false
        val updatedList = mutableListOf<WorkoutLog>()
        val rowsToUpsert = mutableListOf<HealthRecordRow>()
        val idsToDelete = mutableListOf<String>()

        for (item in current) {
            if (idSet.contains(item.id)) {
                if (item.isDeleted) {
                    idsToDelete.add(item.id)
                    changed = true
                } else if (item.syncStatus == SyncStatus.PENDING) {
                    val synced = item.copy(syncStatus = SyncStatus.SYNCED)
                    updatedList.add(synced)
                    rowsToUpsert.add(synced.toRow())
                    changed = true
                } else {
                    updatedList.add(item)
                }
            } else {
                updatedList.add(item)
            }
        }
        if (changed) {
            _workoutsFlow.value = updatedList
            persistChanges(KEY_WORKOUTS, updatedList, rowsToUpsert, BUCKET_WORKOUTS, idsToDelete)
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

    suspend fun upsertWeight(weight: WeightEntry) = dbMutex.withLock {
        val current = _weightsFlow.value.toMutableList()
        val now = currentTimeMillis()
        val changedRows = mutableListOf<HealthRecordRow>()
        val index = current.indexOfFirst {
            it.id == weight.id || (it.userId == weight.userId && it.date == weight.date && !it.isDeleted)
        }
        if (index >= 0) {
            val existing = current[index]
            val updated = weight.copy(
                id = existing.id,
                createdAt = existing.createdAt,
                updatedAt = if (weight.updatedAt > existing.updatedAt) weight.updatedAt else now
            )
            current[index] = updated
            changedRows.add(updated.toRow())
            for (i in current.indices) {
                if (i != index && current[i].userId == weight.userId && current[i].date == weight.date && !current[i].isDeleted) {
                    val tombstone = current[i].copy(
                        isDeleted = true,
                        bodyFatPercent = null,
                        muscleMassKg = null,
                        musclePercent = null,
                        note = null,
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING
                    )
                    current[i] = tombstone
                    changedRows.add(tombstone.toRow())
                }
            }
        } else {
            current.add(weight)
            changedRows.add(weight.toRow())
        }
        _weightsFlow.value = current
        persistChanges(KEY_WEIGHTS, current, changedRows)
    }

    suspend fun deleteWeight(id: String) = dbMutex.withLock {
        val current = _weightsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val tombstone = current[index].copy(
                isDeleted = true,
                bodyFatPercent = null,
                muscleMassKg = null,
                musclePercent = null,
                note = null,
                syncStatus = SyncStatus.PENDING,
                updatedAt = currentTimeMillis()
            )
            current[index] = tombstone
            _weightsFlow.value = current
            persistChanges(KEY_WEIGHTS, current, listOf(tombstone.toRow()))
        }
    }

    fun getPendingWeights(userId: Int): List<WeightEntry> {
        return _weightsFlow.value.filter { it.userId == userId && it.syncStatus == SyncStatus.PENDING }
    }

    fun getAllWeightsForSync(userId: Int): List<WeightEntry> {
        return _weightsFlow.value.filter { it.userId == userId }
    }

    suspend fun markWeightsSynced(ids: List<String>) = dbMutex.withLock {
        if (ids.isEmpty()) return@withLock
        val idSet = ids.toSet()
        val current = _weightsFlow.value
        var changed = false
        val updatedList = mutableListOf<WeightEntry>()
        val rowsToUpsert = mutableListOf<HealthRecordRow>()
        val idsToDelete = mutableListOf<String>()

        for (item in current) {
            if (idSet.contains(item.id)) {
                if (item.isDeleted) {
                    idsToDelete.add(item.id)
                    changed = true
                } else if (item.syncStatus == SyncStatus.PENDING) {
                    val synced = item.copy(syncStatus = SyncStatus.SYNCED)
                    updatedList.add(synced)
                    rowsToUpsert.add(synced.toRow())
                    changed = true
                } else {
                    updatedList.add(item)
                }
            } else {
                updatedList.add(item)
            }
        }
        if (changed) {
            _weightsFlow.value = updatedList
            persistChanges(KEY_WEIGHTS, updatedList, rowsToUpsert, BUCKET_WEIGHTS, idsToDelete)
        }
    }

    /**
     * Cleans up any corrupted dates (e.g., negative years) and deduplicates multiple active records
     * for the same (userId, date) so only the most recently updated record remains active while
     * older duplicates are queued as deletion tombstones for the remote server.
     */
    suspend fun deduplicateAndSanitizeLocalData(userId: Int, isoDateRegex: Regex) = dbMutex.withLock {
        val now = currentTimeMillis()

        // 1. Foods: remove invalid dates, deduplicate same-day active logs
        val currentFoods = _foodsFlow.value
        var foodsChanged = false
        val removedFoodIds = mutableListOf<String>()
        val validFoods = currentFoods.filter {
            val valid = isoDateRegex.matches(it.date)
            if (!valid) {
                foodsChanged = true
                removedFoodIds.add(it.id)
            }
            valid
        }.toMutableList()

        val upsertedFoodRows = mutableListOf<HealthRecordRow>()
        val activeFoodsByDate = validFoods
            .filter { it.userId == userId && !it.isDeleted }
            .groupBy { it.date }

        for ((_, logs) in activeFoodsByDate) {
            if (logs.size > 1) {
                val winner = logs.maxByOrNull { it.updatedAt } ?: logs.first()
                for (i in validFoods.indices) {
                    val item = validFoods[i]
                    if (item.userId == userId && item.date == winner.date && !item.isDeleted && item.id != winner.id) {
                        val tombstone = item.copy(
                            isDeleted = true,
                            rawText = "",
                            summary = null,
                            totalCalories = 0,
                            items = emptyList(),
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING
                        )
                        validFoods[i] = tombstone
                        upsertedFoodRows.add(tombstone.toRow())
                        foodsChanged = true
                    }
                }
            }
        }
        if (foodsChanged) {
            _foodsFlow.value = validFoods
            persistChanges(KEY_FOODS, validFoods, upsertedFoodRows, BUCKET_FOODS, removedFoodIds)
        }

        // 2. Weights: remove invalid dates, deduplicate same-day active weights
        val currentWeights = _weightsFlow.value
        var weightsChanged = false
        val removedWeightIds = mutableListOf<String>()
        val validWeights = currentWeights.filter {
            val valid = isoDateRegex.matches(it.date)
            if (!valid) {
                weightsChanged = true
                removedWeightIds.add(it.id)
            }
            valid
        }.toMutableList()

        val upsertedWeightRows = mutableListOf<HealthRecordRow>()
        val activeWeightsByDate = validWeights
            .filter { it.userId == userId && !it.isDeleted }
            .groupBy { it.date }

        for ((_, entries) in activeWeightsByDate) {
            if (entries.size > 1) {
                val winner = entries.maxByOrNull { it.updatedAt } ?: entries.first()
                for (i in validWeights.indices) {
                    val item = validWeights[i]
                    if (item.userId == userId && item.date == winner.date && !item.isDeleted && item.id != winner.id) {
                        val tombstone = item.copy(
                            isDeleted = true,
                            bodyFatPercent = null,
                            muscleMassKg = null,
                            musclePercent = null,
                            note = null,
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING
                        )
                        validWeights[i] = tombstone
                        upsertedWeightRows.add(tombstone.toRow())
                        weightsChanged = true
                    }
                }
            }
        }
        if (weightsChanged) {
            _weightsFlow.value = validWeights
            persistChanges(KEY_WEIGHTS, validWeights, upsertedWeightRows, BUCKET_WEIGHTS, removedWeightIds)
        }

        // 3. Workouts: remove invalid dates and migrate legacy >36-char device workout IDs to VARCHAR(36)-safe IDs
        val currentWorkouts = _workoutsFlow.value
        var workoutsChanged = false
        val removedWorkoutIds = mutableListOf<String>()
        val normalizedWorkouts = mutableMapOf<String, WorkoutLog>()
        for (workout in currentWorkouts) {
            if (!isoDateRegex.matches(workout.date)) {
                workoutsChanged = true
                removedWorkoutIds.add(workout.id)
                continue
            }
            val safeId = normalizeWorkoutId(workout.id)
            if (safeId != workout.id) {
                workoutsChanged = true
                removedWorkoutIds.add(workout.id)
            }
            val candidate = if (safeId != workout.id) workout.copy(id = safeId) else workout
            val existing = normalizedWorkouts[safeId]
            if (existing == null || candidate.updatedAt >= existing.updatedAt) {
                if (existing != null) workoutsChanged = true
                normalizedWorkouts[safeId] = candidate
            } else {
                workoutsChanged = true
            }
        }
        if (workoutsChanged) {
            val cleanedList = normalizedWorkouts.values.toList()
            _workoutsFlow.value = cleanedList
            persistChanges(
                KEY_WORKOUTS,
                cleanedList,
                cleanedList.map { it.toRow() },
                BUCKET_WORKOUTS,
                removedWorkoutIds
            )
        }
    }

    private fun normalizeWorkoutId(id: String): String {
        if (id.length <= 36 && !id.startsWith("workout_dev_")) return id
        return id
            .replace("workout_dev_", "wdev_")
            .replace("_daily_steps", "_steps")
            .take(36)
    }

    // ─────────────────────────────────────────────────────────────
    // SYNC PULL MERGE ENGINE (LAST-WRITE-WINS & TOMBSTONE PURGE)
    // ─────────────────────────────────────────────────────────────

    suspend fun mergePullFoods(incoming: List<FoodLog>) = dbMutex.withLock {
        if (incoming.isEmpty()) return@withLock
        val now = currentTimeMillis()
        val current = _foodsFlow.value.toMutableList()
        val upsertedRows = mutableListOf<HealthRecordRow>()
        val deletedIds = mutableListOf<String>()

        incoming.forEach { remote ->
            val index = current.indexOfFirst { it.id == remote.id }
            if (remote.isDeleted) {
                if (index >= 0) {
                    val local = current[index]
                    if (remote.updatedAt >= local.updatedAt) {
                        current.removeAt(index)
                        deletedIds.add(remote.id)
                    }
                }
                return@forEach
            }

            if (index >= 0) {
                val local = current[index]
                if (local.syncStatus == SyncStatus.PENDING && local.updatedAt > remote.updatedAt) {
                    // Keep newer unsynced local modification
                } else if (remote.updatedAt >= local.updatedAt) {
                    val synced = remote.copy(syncStatus = SyncStatus.SYNCED)
                    current[index] = synced
                    upsertedRows.add(synced.toRow())
                }
            } else {
                val sameDateIndex = current.indexOfFirst { it.userId == remote.userId && it.date == remote.date && !it.isDeleted }
                if (sameDateIndex >= 0) {
                    val local = current[sameDateIndex]
                    if (remote.updatedAt > local.updatedAt) {
                        val tombstone = local.copy(
                            isDeleted = true,
                            rawText = "",
                            summary = null,
                            totalCalories = 0,
                            items = emptyList(),
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING
                        )
                        val synced = remote.copy(syncStatus = SyncStatus.SYNCED)
                        current[sameDateIndex] = tombstone
                        current.add(synced)
                        upsertedRows.add(tombstone.toRow())
                        upsertedRows.add(synced.toRow())
                    } else {
                        val tombstone = remote.copy(
                            isDeleted = true,
                            rawText = "",
                            summary = null,
                            totalCalories = 0,
                            items = emptyList(),
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING
                        )
                        current.add(tombstone)
                        upsertedRows.add(tombstone.toRow())
                    }
                } else {
                    val synced = remote.copy(syncStatus = SyncStatus.SYNCED)
                    current.add(synced)
                    upsertedRows.add(synced.toRow())
                }
            }
        }
        _foodsFlow.value = current
        persistChanges(KEY_FOODS, current, upsertedRows, BUCKET_FOODS, deletedIds)
    }

    suspend fun mergePullNutritionGoal(remote: NutritionGoal) = dbMutex.withLock {
        val current = _goalsFlow.value.toMutableMap()
        val local = current[remote.userId]
        if (local == null || (remote.updatedAt >= local.updatedAt && !(local.syncStatus == SyncStatus.PENDING && local.updatedAt > remote.updatedAt))) {
            val synced = remote.copy(syncStatus = SyncStatus.SYNCED)
            current[remote.userId] = synced
            _goalsFlow.value = current
            if (useRowStore) {
                recordStore.upsertRecord(synced.toRow())
            } else {
                saveMap(KEY_GOALS, current)
            }
        }
    }

    suspend fun mergePullWorkouts(incoming: List<WorkoutLog>) = dbMutex.withLock {
        if (incoming.isEmpty()) return@withLock
        val current = _workoutsFlow.value.toMutableList()
        val upsertedRows = mutableListOf<HealthRecordRow>()
        val deletedIds = mutableListOf<String>()

        incoming.forEach { remote ->
            val index = current.indexOfFirst { it.id == remote.id }
            if (remote.isDeleted) {
                if (index >= 0) {
                    val local = current[index]
                    if (remote.updatedAt >= local.updatedAt) {
                        current.removeAt(index)
                        deletedIds.add(remote.id)
                    }
                }
                return@forEach
            }

            if (index >= 0) {
                val local = current[index]
                if (local.syncStatus == SyncStatus.PENDING && local.updatedAt > remote.updatedAt) {
                    // Keep newer local modification
                } else if (remote.updatedAt >= local.updatedAt) {
                    val synced = remote.copy(syncStatus = SyncStatus.SYNCED)
                    current[index] = synced
                    upsertedRows.add(synced.toRow())
                }
            } else {
                val synced = remote.copy(syncStatus = SyncStatus.SYNCED)
                current.add(synced)
                upsertedRows.add(synced.toRow())
            }
        }
        _workoutsFlow.value = current
        persistChanges(KEY_WORKOUTS, current, upsertedRows, BUCKET_WORKOUTS, deletedIds)
    }

    suspend fun mergePullWeights(incoming: List<WeightEntry>) = dbMutex.withLock {
        if (incoming.isEmpty()) return@withLock
        val now = currentTimeMillis()
        val current = _weightsFlow.value.toMutableList()
        val upsertedRows = mutableListOf<HealthRecordRow>()
        val deletedIds = mutableListOf<String>()

        incoming.forEach { remote ->
            val index = current.indexOfFirst { it.id == remote.id }
            if (remote.isDeleted) {
                if (index >= 0) {
                    val local = current[index]
                    if (remote.updatedAt >= local.updatedAt) {
                        current.removeAt(index)
                        deletedIds.add(remote.id)
                    }
                }
                return@forEach
            }

            if (index >= 0) {
                val local = current[index]
                if (local.syncStatus == SyncStatus.PENDING && local.updatedAt > remote.updatedAt) {
                    // Keep newer local modification
                } else if (remote.updatedAt >= local.updatedAt) {
                    val synced = remote.copy(syncStatus = SyncStatus.SYNCED)
                    current[index] = synced
                    upsertedRows.add(synced.toRow())
                }
            } else {
                val sameDateIndex = current.indexOfFirst { it.userId == remote.userId && it.date == remote.date && !it.isDeleted }
                if (sameDateIndex >= 0) {
                    val local = current[sameDateIndex]
                    if (remote.updatedAt > local.updatedAt) {
                        val tombstone = local.copy(
                            isDeleted = true,
                            bodyFatPercent = null,
                            muscleMassKg = null,
                            musclePercent = null,
                            note = null,
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING
                        )
                        val synced = remote.copy(syncStatus = SyncStatus.SYNCED)
                        current[sameDateIndex] = tombstone
                        current.add(synced)
                        upsertedRows.add(tombstone.toRow())
                        upsertedRows.add(synced.toRow())
                    } else {
                        val tombstone = remote.copy(
                            isDeleted = true,
                            bodyFatPercent = null,
                            muscleMassKg = null,
                            musclePercent = null,
                            note = null,
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING
                        )
                        current.add(tombstone)
                        upsertedRows.add(tombstone.toRow())
                    }
                } else {
                    val synced = remote.copy(syncStatus = SyncStatus.SYNCED)
                    current.add(synced)
                    upsertedRows.add(synced.toRow())
                }
            }
        }
        _weightsFlow.value = current
        persistChanges(KEY_WEIGHTS, current, upsertedRows, BUCKET_WEIGHTS, deletedIds)
    }

    // ─────────────────────────────────────────────────────────────
    // AI REPORTS OPERATIONS (DAILY & WEEKLY ANALYTICS)
    // ─────────────────────────────────────────────────────────────

    fun observeAiReports(userId: Int): Flow<List<HealthAiReport>> {
        return _reportsFlow.map { list ->
            list.filter { it.userId == userId }
                .sortedByDescending { it.createdAt }
        }
    }

    fun getAiReports(userId: Int): List<HealthAiReport> {
        return _reportsFlow.value.filter { it.userId == userId }
            .sortedByDescending { it.createdAt }
    }

    suspend fun upsertAiReport(report: HealthAiReport) = dbMutex.withLock {
        val current = _reportsFlow.value.toMutableList()
        val index = current.indexOfFirst {
            it.id == report.id || (
                it.userId == report.userId &&
                it.periodType == report.periodType &&
                it.startDate == report.startDate &&
                it.endDate == report.endDate
            )
        }
        if (index >= 0) {
            current[index] = report
        } else {
            current.add(0, report)
        }
        _reportsFlow.value = current
        persistChanges(KEY_AI_REPORTS, current, listOf(report.toRow()))
    }

    suspend fun markReportSentToWhatsApp(reportId: String) = dbMutex.withLock {
        val current = _reportsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == reportId }
        if (index >= 0) {
            val updated = current[index].copy(isSentToWhatsApp = true)
            current[index] = updated
            _reportsFlow.value = current
            persistChanges(KEY_AI_REPORTS, current, listOf(updated.toRow()))
        }
    }

    suspend fun deleteAiReport(reportId: String) = dbMutex.withLock {
        val current = _reportsFlow.value.filter { it.id != reportId }
        _reportsFlow.value = current
        persistChanges(KEY_AI_REPORTS, current, emptyList(), BUCKET_AI_REPORTS, listOf(reportId))
    }

    // ─────────────────────────────────────────────────────────────
    // DAILY AI MEAL PLAN OPERATIONS
    // ─────────────────────────────────────────────────────────────

    fun observeDailyMealPlan(userId: Int, dateIso: String): Flow<DailyMealPlan?> {
        return _dailyMealPlansFlow.map { list ->
            list.firstOrNull { it.userId == userId && it.dateIso == dateIso }
        }
    }

    fun getDailyMealPlan(userId: Int, dateIso: String): DailyMealPlan? {
        return _dailyMealPlansFlow.value.firstOrNull { it.userId == userId && it.dateIso == dateIso }
    }

    suspend fun upsertDailyMealPlan(plan: DailyMealPlan) = dbMutex.withLock {
        val current = _dailyMealPlansFlow.value.toMutableList()
        val index = current.indexOfFirst { it.userId == plan.userId && it.dateIso == plan.dateIso }
        if (index >= 0) {
            current[index] = plan
        } else {
            current.add(0, plan)
        }
        _dailyMealPlansFlow.value = current
        persistChanges(KEY_DAILY_MEAL_PLANS, current, listOf(plan.toRow()))
    }

    suspend fun clearAll() = dbMutex.withLock {
        _foodsFlow.value = emptyList()
        _goalsFlow.value = emptyMap()
        _workoutsFlow.value = emptyList()
        _weightsFlow.value = emptyList()
        _customFoodsFlow.value = emptyList()
        _favoriteFoodsFlow.value = emptyMap()
        _reportsFlow.value = emptyList()
        _waterFlow.value = emptyList()
        _dailyMealPlansFlow.value = emptyList()
        recordStore.clearAll()
        SettingsHealthRecordStore.LEGACY_KEYS.forEach { settings.remove(it) }
    }

    // ─────────────────────────────────────────────────────────────
    // WATER HYDRATION OPERATIONS & SYNC
    // ─────────────────────────────────────────────────────────────

    fun observeWater(userId: Int, date: String): Flow<WaterEntry?> {
        return _waterFlow.map { list ->
            list.find { it.userId == userId && it.date == date }
        }
    }

    fun observeAllWater(userId: Int): Flow<List<WaterEntry>> {
        return _waterFlow.map { list ->
            list.filter { it.userId == userId }
        }
    }

    suspend fun getWaterForDate(userId: Int, date: String): WaterEntry? {
        return _waterFlow.value.find { it.userId == userId && it.date == date }
    }

    fun getWaterSync(userId: Int, date: String): WaterEntry? {
        return _waterFlow.value.find { it.userId == userId && it.date == date }
    }

    fun getAllWaterSync(userId: Int): List<WaterEntry> {
        return _waterFlow.value.filter { it.userId == userId }
    }

    fun getPendingWaters(userId: Int): List<WaterEntry> {
        return _waterFlow.value.filter { it.userId == userId && it.syncStatus == SyncStatus.PENDING }
    }

    fun getAllWatersForSync(userId: Int): List<WaterEntry> {
        return _waterFlow.value.filter { it.userId == userId }
    }

    suspend fun addWater(userId: Int, date: String, deltaMl: Int, defaultTargetMl: Int = 2500): WaterEntry = dbMutex.withLock {
        val current = _waterFlow.value.toMutableList()
        val now = currentTimeMillis()
        val index = current.indexOfFirst { it.userId == userId && it.date == date }
        val updatedEntry = if (index >= 0) {
            val existing = current[index]
            val newAmount = (existing.amountMl + deltaMl).coerceAtLeast(0)
            existing.copy(
                amountMl = newAmount,
                targetMl = defaultTargetMl,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        } else {
            WaterEntry(
                date = date,
                userId = userId,
                amountMl = deltaMl.coerceAtLeast(0),
                targetMl = defaultTargetMl,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        }
        if (index >= 0) {
            current[index] = updatedEntry
        } else {
            current.add(updatedEntry)
        }
        _waterFlow.value = current
        persistChanges(KEY_WATER, current, listOf(updatedEntry.toRow()))
        updatedEntry
    }

    suspend fun resetWater(userId: Int, date: String) = dbMutex.withLock {
        val current = _waterFlow.value.toMutableList()
        val index = current.indexOfFirst { it.userId == userId && it.date == date }
        if (index >= 0) {
            val updated = current[index].copy(
                amountMl = 0,
                updatedAt = currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )
            current[index] = updated
            _waterFlow.value = current
            persistChanges(KEY_WATER, current, listOf(updated.toRow()))
        }
    }

    suspend fun markWatersSynced(userId: Int, dates: List<String>) = dbMutex.withLock {
        if (dates.isEmpty()) return@withLock
        val dateSet = dates.toSet()
        val current = _waterFlow.value.toMutableList()
        val changedRows = mutableListOf<HealthRecordRow>()
        var changed = false
        for (i in current.indices) {
            val item = current[i]
            if (item.userId == userId && dateSet.contains(item.date) && item.syncStatus == SyncStatus.PENDING) {
                val synced = item.copy(syncStatus = SyncStatus.SYNCED)
                current[i] = synced
                changedRows.add(synced.toRow())
                changed = true
            }
        }
        if (changed) {
            _waterFlow.value = current
            persistChanges(KEY_WATER, current, changedRows)
        }
    }

    suspend fun mergePullWaters(incoming: List<WaterEntry>) = dbMutex.withLock {
        if (incoming.isEmpty()) return@withLock
        val current = _waterFlow.value.toMutableList()
        val upsertedRows = mutableListOf<HealthRecordRow>()
        var changed = false

        incoming.forEach { remote ->
            val index = current.indexOfFirst { it.userId == remote.userId && it.date == remote.date }
            if (index >= 0) {
                val local = current[index]
                if (local.syncStatus == SyncStatus.PENDING && local.updatedAt > remote.updatedAt) {
                    // Keep newer unsynced local modification
                } else if (remote.updatedAt >= local.updatedAt) {
                    val synced = remote.copy(syncStatus = SyncStatus.SYNCED)
                    current[index] = synced
                    upsertedRows.add(synced.toRow())
                    changed = true
                }
            } else {
                val synced = remote.copy(syncStatus = SyncStatus.SYNCED)
                current.add(synced)
                upsertedRows.add(synced.toRow())
                changed = true
            }
        }
        if (changed) {
            _waterFlow.value = current
            persistChanges(KEY_WATER, current, upsertedRows)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PERSISTENCE & ROW MAPPING HELPERS
    // ─────────────────────────────────────────────────────────────

    private inline fun <reified T> persistChanges(
        legacyKey: String,
        fullList: List<T>,
        rowsToUpsert: List<HealthRecordRow>,
        deleteBucket: String = "",
        idsToDelete: List<String> = emptyList()
    ) {
        if (useRowStore) {
            try {
                if (rowsToUpsert.isNotEmpty()) {
                    recordStore.upsertRecords(rowsToUpsert)
                }
                if (idsToDelete.isNotEmpty() && deleteBucket.isNotEmpty()) {
                    recordStore.deleteRecords(deleteBucket, idsToDelete)
                }
            } catch (e: Exception) {
                Napier.e("SQLite persistChanges failed ($legacyKey): ${e.message}", e, tag = "HEALTH_DB")
            }
        } else {
            saveList(legacyKey, fullList)
        }
    }

    private inline fun <reified T> loadBucketList(bucket: String): List<T> {
        return recordStore.loadBucket(bucket).mapNotNull { payload ->
            try {
                json.decodeFromString<T>(payload)
            } catch (e: Exception) {
                Napier.e("Failed to decode row from bucket=$bucket: ${e.message}", e, tag = "HEALTH_DB")
                null
            }
        }
    }

    private fun FoodLog.toRow() = HealthRecordRow(
        bucket = BUCKET_FOODS,
        recordId = id,
        userId = userId,
        date = date,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        syncStatus = syncStatus.name,
        payloadJson = json.encodeToString(this)
    )

    private fun NutritionGoal.toRow() = HealthRecordRow(
        bucket = BUCKET_GOALS,
        recordId = userId.toString(),
        userId = userId,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        syncStatus = syncStatus.name,
        payloadJson = json.encodeToString(this)
    )

    private fun WorkoutLog.toRow() = HealthRecordRow(
        bucket = BUCKET_WORKOUTS,
        recordId = id,
        userId = userId,
        date = date,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        syncStatus = syncStatus.name,
        payloadJson = json.encodeToString(this)
    )

    private fun WeightEntry.toRow() = HealthRecordRow(
        bucket = BUCKET_WEIGHTS,
        recordId = id,
        userId = userId,
        date = date,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        syncStatus = syncStatus.name,
        payloadJson = json.encodeToString(this)
    )

    private fun CustomFoodItem.toRow() = HealthRecordRow(
        bucket = BUCKET_CUSTOM_FOODS,
        recordId = id,
        userId = userId,
        updatedAt = updatedAt,
        payloadJson = json.encodeToString(this)
    )

    private fun favoriteRow(userId: Int, ids: List<String>) = HealthRecordRow(
        bucket = BUCKET_FAVORITE_FOODS,
        recordId = userId.toString(),
        userId = userId,
        payloadJson = json.encodeToString(FavoriteFoodListPayload(userId = userId, foodIds = ids))
    )

    private fun HealthAiReport.toRow() = HealthRecordRow(
        bucket = BUCKET_AI_REPORTS,
        recordId = id,
        userId = userId,
        date = endDate,
        updatedAt = createdAt,
        payloadJson = json.encodeToString(this)
    )

    private fun WaterEntry.toRow() = HealthRecordRow(
        bucket = BUCKET_WATER,
        recordId = "${userId}_$date",
        userId = userId,
        date = date,
        updatedAt = updatedAt,
        syncStatus = syncStatus.name,
        payloadJson = json.encodeToString(this)
    )

    private fun DailyMealPlan.toRow() = HealthRecordRow(
        bucket = BUCKET_DAILY_MEAL_PLANS,
        recordId = "${userId}_$dateIso",
        userId = userId,
        date = dateIso,
        updatedAt = createdAt,
        payloadJson = json.encodeToString(this)
    )

    private inline fun <reified T> loadList(key: String): List<T> {
        val serialized = settings.getStringOrNull(key) ?: return emptyList()
        return try {
            json.decodeFromString(serialized)
        } catch (e: Exception) {
            Napier.e("Failed to decode legacy list for key=$key: ${e.message}", e, tag = "HEALTH_DB")
            emptyList()
        }
    }

    private inline fun <reified T> saveList(key: String, list: List<T>) {
        try {
            settings.putString(key, json.encodeToString(list))
        } catch (e: Exception) {
            Napier.e("Failed to save list for key=$key: ${e.message}", e, tag = "HEALTH_DB")
        }
    }

    private inline fun <reified K, reified V> loadMap(key: String): Map<K, V> {
        val serialized = settings.getStringOrNull(key) ?: return emptyMap()
        return try {
            json.decodeFromString(serialized)
        } catch (e: Exception) {
            Napier.e("Failed to decode legacy map for key=$key: ${e.message}", e, tag = "HEALTH_DB")
            emptyMap()
        }
    }

    private inline fun <reified K, reified V> saveMap(key: String, map: Map<K, V>) {
        try {
            settings.putString(key, json.encodeToString(map))
        } catch (e: Exception) {
            Napier.e("Failed to save map for key=$key: ${e.message}", e, tag = "HEALTH_DB")
        }
    }

    private fun currentTimeMillis(): Long = com.yusufteker.healthtrack.core.utils.currentTimeMillis()

    companion object {
        private const val KEY_SQLITE_MIGRATED_V1 = "ht_sqlite_migrated_v1"

        private const val BUCKET_FOODS = "foods"
        private const val BUCKET_GOALS = "goals"
        private const val BUCKET_WORKOUTS = "workouts"
        private const val BUCKET_WEIGHTS = "weights"
        private const val BUCKET_CUSTOM_FOODS = "custom_foods"
        private const val BUCKET_FAVORITE_FOODS = "favorite_foods"
        private const val BUCKET_AI_REPORTS = "ai_reports"
        private const val BUCKET_WATER = "water"
        private const val BUCKET_DAILY_MEAL_PLANS = "daily_meal_plans"

        private const val KEY_FOODS = "ht_db_foods"
        private const val KEY_GOALS = "ht_db_goals"
        private const val KEY_WORKOUTS = "ht_db_workouts"
        private const val KEY_WEIGHTS = "ht_db_weights"
        private const val KEY_CUSTOM_FOODS = "ht_db_custom_foods"
        private const val KEY_FAVORITE_FOODS = "ht_db_favorite_foods"
        private const val KEY_AI_REPORTS = "ht_db_ai_reports"
        private const val KEY_WATER = "ht_db_water"
        private const val KEY_DAILY_MEAL_PLANS = "ht_db_daily_meal_plans"
    }
}
