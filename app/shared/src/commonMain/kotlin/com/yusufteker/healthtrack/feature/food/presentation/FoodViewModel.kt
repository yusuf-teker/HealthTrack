package com.yusufteker.healthtrack.feature.food.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.SyncEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodViewModel(
    private val database: HealthDatabase,
    private val apiClient: HealthApiClient,
    private val tokenStorage: TokenStorage,
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val _state = MutableStateFlow(FoodState())
    val state: StateFlow<FoodState> = _state.asStateFlow()

    private val _effects = Channel<FoodEffect>(Channel.BUFFERED)
    val effects: Flow<FoodEffect> = _effects.receiveAsFlow()

    private var foodObservationJob: Job? = null
    private var goalObservationJob: Job? = null

    init {
        viewModelScope.launch {
            tokenStorage.sessionFlow.collect { session ->
                if (session.isLoggedIn) {
                    observeDataForCurrentDate(session.userId, _state.value.selectedDate)
                    observeGoal(session.userId)
                }
            }
        }
    }

    fun onIntent(intent: FoodIntent) {
        when (intent) {
            is FoodIntent.OnDateChanged -> {
                _state.update { it.copy(selectedDate = intent.newDate) }
                val userId = tokenStorage.getUserId()
                if (userId > 0) {
                    observeDataForCurrentDate(userId, intent.newDate)
                }
            }
            is FoodIntent.OnRawTextChanged -> _state.update { it.copy(rawText = intent.text) }
            is FoodIntent.OnCalculateAiClicked -> calculateWithAi()
            is FoodIntent.OnSaveManualClicked -> saveManualLog()
            is FoodIntent.OnRemoveItem -> removeItem(intent.itemId)
            is FoodIntent.OnAddItem -> addItem(intent.name, intent.portion, intent.calories)
            is FoodIntent.OnOpenEditGoalsDialog -> _state.update { it.copy(isEditGoalsDialogOpen = true) }
            is FoodIntent.OnCloseEditGoalsDialog -> _state.update { it.copy(isEditGoalsDialogOpen = false) }
            is FoodIntent.OnSaveGoals -> saveGoals(intent.goal)
            is FoodIntent.OnDismissInfoMessage -> _state.update { it.copy(infoMessage = null) }
            is FoodIntent.OnDismissErrorMessage -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun observeDataForCurrentDate(userId: Int, date: String) {
        foodObservationJob?.cancel()
        foodObservationJob = viewModelScope.launch {
            database.observeFoods(userId, date).collect { logs ->
                val primaryLog = logs.firstOrNull()
                _state.update { current ->
                    val items = primaryLog?.items ?: emptyList()
                    val totalCals = items.sumOf { it.calories }
                    current.copy(
                        currentFoodLog = primaryLog,
                        rawText = primaryLog?.rawText ?: current.rawText,
                        items = items,
                        totalCalories = totalCals,
                        summary = primaryLog?.summary,
                        nutrientBars = calculateBars(items, current.nutritionGoal)
                    )
                }
            }
        }
    }

    private fun observeGoal(userId: Int) {
        goalObservationJob?.cancel()
        goalObservationJob = viewModelScope.launch {
            database.observeNutritionGoal(userId).collect { goal ->
                _state.update { current ->
                    current.copy(
                        nutritionGoal = goal,
                        nutrientBars = calculateBars(current.items, goal)
                    )
                }
            }
        }
    }

    private fun calculateWithAi() {
        val rawText = _state.value.rawText.trim()
        val date = _state.value.selectedDate
        val userId = tokenStorage.getUserId()

        if (rawText.isBlank()) {
            _state.update { it.copy(errorMessage = "Lütfen önce yediklerinizi serbest metin olarak yazınız.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCalculatingAi = true, errorMessage = null, infoMessage = null) }

            val result = apiClient.analyzeFood(rawText, date)
            result.onSuccess { aiResult ->
                val existingLog = _state.value.currentFoodLog
                val logId = existingLog?.id ?: Uuid.random().toString()
                val now = currentTime()

                val newLog = FoodLog(
                    id = logId,
                    userId = userId,
                    date = date,
                    rawText = rawText,
                    summary = aiResult.summary,
                    totalCalories = aiResult.totalCalories,
                    items = aiResult.items,
                    createdAt = existingLog?.createdAt ?: now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )

                database.upsertFood(newLog)
                _state.update {
                    it.copy(
                        isCalculatingAi = false,
                        infoMessage = "AI ile besin analizi tamamlandı ve kaydedildi."
                    )
                }
                // Background sync
                launch { syncEngine.sync() }
            }.onFailure { err ->
                val errorMsg = err.message ?: "Bilinmeyen hata"
                val displayMsg = when {
                    errorMsg.contains("404") ->
                        "Sunucuda AI servisi bulunamadı (404). Render üzerinde henüz yeni sürüm deploy edilmemiş olabilir."
                    errorMsg.contains("401") ->
                        "Oturum süresi doldu (401). Lütfen tekrar giriş yapınız."
                    errorMsg.contains("500") ->
                        "Sunucu hatası (500). Render sunucusunda GEMINI_API_KEY tanımlı olduğundan emin olunuz."
                    errorMsg.contains("Connect", ignoreCase = true) || errorMsg.contains("timeout", ignoreCase = true) ->
                        "Sunucuya bağlanılamadı. Lütfen internet bağlantınızı kontrol ediniz."
                    else ->
                        "AI hesaplama başarısız: $errorMsg"
                }

                _state.update {
                    it.copy(
                        isCalculatingAi = false,
                        errorMessage = displayMsg
                    )
                }
            }
        }
    }

    private fun saveManualLog() {
        val rawText = _state.value.rawText.trim()
        val date = _state.value.selectedDate
        val userId = tokenStorage.getUserId()
        val existingLog = _state.value.currentFoodLog
        val now = currentTime()

        val logId = existingLog?.id ?: Uuid.random().toString()
        val items = _state.value.items
        val totalCals = items.sumOf { it.calories }

        viewModelScope.launch {
            val updatedLog = FoodLog(
                id = logId,
                userId = userId,
                date = date,
                rawText = rawText,
                summary = existingLog?.summary ?: "Kullanıcı girişi",
                totalCalories = totalCals,
                items = items,
                createdAt = existingLog?.createdAt ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
            database.upsertFood(updatedLog)
            _effects.send(FoodEffect.ShowToast("Kayıt güncellendi."))
            launch { syncEngine.sync() }
        }
    }

    private fun addItem(name: String, portion: String, calories: Int) {
        val newItem = FoodItem(
            id = Uuid.random().toString(),
            name = name.trim(),
            formattedPortion = portion.trim(),
            calories = calories
        )
        val currentItems = _state.value.items + newItem
        updateItemsInDb(currentItems)
    }

    private fun removeItem(itemId: String) {
        val currentItems = _state.value.items.filter { it.id != itemId }
        updateItemsInDb(currentItems)
    }

    private fun updateItemsInDb(items: List<FoodItem>) {
        val existingLog = _state.value.currentFoodLog ?: return
        val now = currentTime()
        val totalCals = items.sumOf { it.calories }

        viewModelScope.launch {
            val updated = existingLog.copy(
                items = items,
                totalCalories = totalCals,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
            database.upsertFood(updated)
            launch { syncEngine.sync() }
        }
    }

    private fun saveGoals(goal: NutritionGoal) {
        val userId = tokenStorage.getUserId()
        viewModelScope.launch {
            val updated = goal.copy(
                userId = userId,
                updatedAt = currentTime(),
                syncStatus = SyncStatus.PENDING
            )
            database.upsertNutritionGoal(updated)
            _state.update { it.copy(isEditGoalsDialogOpen = false) }
            _effects.send(FoodEffect.ShowToast("Hedefler güncellendi."))
            launch { syncEngine.sync() }
        }
    }

    private fun calculateBars(items: List<FoodItem>, goal: NutritionGoal): List<NutrientProgress> {
        val totalCalories = items.sumOf { it.calories }.toDouble()
        val totalProtein = items.sumOf { it.proteinG }
        val totalCarbs = items.sumOf { it.carbsG }
        val totalFat = items.sumOf { it.fatG }
        val totalFiber = items.sumOf { it.fiberG }

        // Micronutrient sums
        fun sumMicronutrient(key: String): Double {
            return items.sumOf { item ->
                item.micronutrients
                    .filter { it.name.contains(key, ignoreCase = true) }
                    .sumOf { it.amount }
            }
        }

        val vitC = sumMicronutrient("C")
        val vitD = sumMicronutrient("D")
        val vitB12 = sumMicronutrient("B12")
        val iron = sumMicronutrient("Demir")
        val calcium = sumMicronutrient("Kalsiyum")
        val magnesium = sumMicronutrient("Magnezyum")

        return listOf(
            NutrientProgress("calories", "Kalori", totalCalories, goal.calories.toDouble(), "kcal", "#FF5722"),
            NutrientProgress("protein", "Protein", totalProtein, goal.proteinG, "g", "#8E24AA"),
            NutrientProgress("carbs", "Karbonhidrat", totalCarbs, goal.carbsG, "g", "#FFA000"),
            NutrientProgress("fat", "Yağ", totalFat, goal.fatG, "g", "#E53935"),
            NutrientProgress("fiber", "Lif", totalFiber, goal.fiberG, "g", "#43A047"),
            NutrientProgress("vit_c", "Vit C", vitC, goal.vitaminCMg, "mg", "#FB8C00"),
            NutrientProgress("vit_d", "Vit D", vitD, goal.vitaminDUg, "µg", "#00897B"),
            NutrientProgress("vit_b12", "Vit B12", vitB12, goal.vitaminB12Ug, "µg", "#3949AB"),
            NutrientProgress("iron", "Demir", iron, goal.ironMg, "mg", "#D84315"),
            NutrientProgress("calcium", "Kalsiyum", calcium, goal.calciumMg, "mg", "#00ACC1"),
            NutrientProgress("magnesium", "Magnezyum", magnesium, goal.magnesiumMg, "mg", "#7CB342")
        )
    }

    private fun currentTime(): Long = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds + 1720000000000L
}
