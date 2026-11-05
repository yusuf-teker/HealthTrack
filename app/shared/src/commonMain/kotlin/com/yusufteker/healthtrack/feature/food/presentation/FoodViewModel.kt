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
    private var weightObservationJob: Job? = null
    private var customFoodsObservationJob: Job? = null
    private var favoriteFoodsObservationJob: Job? = null

    private var cachedWeights: List<WeightEntry> = emptyList()

    private data class DayWeightInfo(
        val weightKg: Double?,
        val isExactForDate: Boolean,
        val recordDate: String?,
        val targetProtein: Double
    )

    private fun resolveDayWeightAndProtein(date: String): DayWeightInfo {
        val fitnessGoal = tokenStorage.getUserFitnessGoal()
        val exactEntry = cachedWeights.firstOrNull { it.date == date }
        if (exactEntry != null) {
            val targetProtein = calculateProteinTarget(exactEntry.weightKg, fitnessGoal)
            return DayWeightInfo(
                weightKg = exactEntry.weightKg,
                isExactForDate = true,
                recordDate = exactEntry.date,
                targetProtein = targetProtein
            )
        }

        val pastEntry = cachedWeights.filter { it.date <= date }.maxByOrNull { it.date }
        if (pastEntry != null) {
            val targetProtein = calculateProteinTarget(pastEntry.weightKg, fitnessGoal)
            return DayWeightInfo(
                weightKg = pastEntry.weightKg,
                isExactForDate = false,
                recordDate = pastEntry.date,
                targetProtein = targetProtein
            )
        }

        val anyEntry = cachedWeights.minByOrNull { it.date } ?: cachedWeights.maxByOrNull { it.date }
        if (anyEntry != null) {
            val targetProtein = calculateProteinTarget(anyEntry.weightKg, fitnessGoal)
            return DayWeightInfo(
                weightKg = anyEntry.weightKg,
                isExactForDate = false,
                recordDate = anyEntry.date,
                targetProtein = targetProtein
            )
        }

        val profileWeight = tokenStorage.getUserWeight().takeIf { it > 0.0 }
        val effectiveWeight = profileWeight ?: 70.0
        val targetProtein = calculateProteinTarget(effectiveWeight, fitnessGoal)
        return DayWeightInfo(
            weightKg = profileWeight,
            isExactForDate = false,
            recordDate = null,
            targetProtein = targetProtein
        )
    }

    init {
        viewModelScope.launch {
            tokenStorage.sessionFlow.collect { session ->
                if (session.isLoggedIn) {
                    cachedWeights = database.getWeights(session.userId)
                    observeDataForCurrentDate(session.userId, _state.value.selectedDate)
                    observeGoal(session.userId)
                    observeWeights(session.userId)
                    observeCustomFoods(session.userId)
                    observeFavoriteFoods(session.userId)
                }
            }
        }
    }

    fun onIntent(intent: FoodIntent) {
        when (intent) {
            is FoodIntent.OnDateChanged -> {
                val dayWeightInfo = resolveDayWeightAndProtein(intent.newDate)
                val fitnessGoal = tokenStorage.getUserFitnessGoal()
                _state.update { current ->
                    current.copy(
                        selectedDate = intent.newDate,
                        rawText = "",
                        fitnessGoal = fitnessGoal,
                        selectedDateWeightKg = dayWeightInfo.weightKg,
                        isWeightExactForDate = dayWeightInfo.isExactForDate,
                        weightRecordDate = dayWeightInfo.recordDate,
                        effectiveProteinTarget = dayWeightInfo.targetProtein
                    )
                }
                val userId = tokenStorage.getUserId()
                if (userId > 0) {
                    observeDataForCurrentDate(userId, intent.newDate)
                }
            }
            is FoodIntent.OnRawTextChanged -> _state.update { it.copy(rawText = intent.text) }
            is FoodIntent.OnCalculateAiClicked -> calculateWithAi()
            is FoodIntent.OnSaveManualClicked -> saveManualLog()
            is FoodIntent.OnRemoveItem -> removeItem(intent.itemId)
            is FoodIntent.OnClearFoodLog -> clearCurrentLog()
            is FoodIntent.OnAddItem -> addItem(
                intent.name,
                intent.portion,
                intent.calories,
                intent.proteinG,
                intent.carbsG,
                intent.fatG,
                intent.fiberG
            )
            is FoodIntent.OnOpenEditGoalsDialog -> _state.update { it.copy(isEditGoalsDialogOpen = true) }
            is FoodIntent.OnCloseEditGoalsDialog -> _state.update { it.copy(isEditGoalsDialogOpen = false) }
            is FoodIntent.OnSaveGoals -> saveGoals(intent.goal, intent.fitnessGoal)
            is FoodIntent.OnDismissInfoMessage -> _state.update { it.copy(infoMessage = null) }
            is FoodIntent.OnDismissErrorMessage -> _state.update { it.copy(errorMessage = null) }
            is FoodIntent.OnDismissAiReview -> _state.update { it.copy(isAiReviewSheetOpen = false, pendingAiResult = null) }
            is FoodIntent.OnConfirmAiReview -> confirmAiReview()
            is FoodIntent.OnToggleFavoriteFood -> toggleFavorite(intent.foodId)
            is FoodIntent.OnOpenAddCustomFoodDialog -> _state.update { it.copy(isAddCustomFoodDialogOpen = true) }
            is FoodIntent.OnCloseAddCustomFoodDialog -> _state.update { it.copy(isAddCustomFoodDialogOpen = false) }
            is FoodIntent.OnSaveCustomFood -> saveCustomFood(intent)
            is FoodIntent.OnDeleteCustomFood -> deleteCustomFood(intent.id)
        }
    }

    private fun observeCustomFoods(userId: Int) {
        customFoodsObservationJob?.cancel()
        customFoodsObservationJob = viewModelScope.launch {
            database.observeCustomFoods(userId).collect { list ->
                _state.update { it.copy(customFoods = list) }
            }
        }
    }

    private fun observeFavoriteFoods(userId: Int) {
        favoriteFoodsObservationJob?.cancel()
        favoriteFoodsObservationJob = viewModelScope.launch {
            database.observeFavoriteFoodIds(userId).collect { set ->
                _state.update { it.copy(favoriteFoodIds = set) }
            }
        }
    }

    private fun toggleFavorite(foodId: String) {
        val userId = tokenStorage.getUserId()
        viewModelScope.launch {
            val isNowFavorite = database.toggleFavoriteFood(userId, foodId)
            val msg = if (isNowFavorite) "Favorilere eklendi." else "Favorilerden çıkarıldı."
            _effects.send(FoodEffect.ShowToast(msg))
        }
    }

    private fun saveCustomFood(intent: FoodIntent.OnSaveCustomFood) {
        val userId = tokenStorage.getUserId()
        val now = currentTime()
        val customId = "custom_" + Uuid.random().toString().take(12)
        val newItem = CustomFoodItem(
            id = customId,
            userId = userId,
            name = intent.name.trim(),
            brand = intent.brand?.trim()?.ifBlank { null },
            portionUnit = intent.portionUnit.trim(),
            unitStep = 1,
            calories = intent.calories,
            proteinG = intent.proteinG,
            carbsG = intent.carbsG,
            fatG = intent.fatG,
            fiberG = intent.fiberG,
            categoryName = intent.categoryName,
            createdAt = now,
            updatedAt = now
        )

        viewModelScope.launch {
            database.upsertCustomFood(newItem)
            if (intent.makeFavorite) {
                database.toggleFavoriteFood(userId, customId)
            }
            _state.update { it.copy(isAddCustomFoodDialogOpen = false) }
            _effects.send(FoodEffect.ShowToast("${newItem.name} hazır ürünlerime eklendi."))
        }
    }

    private fun deleteCustomFood(id: String) {
        viewModelScope.launch {
            database.deleteCustomFood(id)
            _effects.send(FoodEffect.ShowToast("Özel ürün silindi."))
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
                    val dayWeightInfo = resolveDayWeightAndProtein(date)
                    val fitnessGoal = tokenStorage.getUserFitnessGoal()
                    current.copy(
                        currentFoodLog = primaryLog,
                        items = items,
                        totalCalories = totalCals,
                        summary = primaryLog?.summary,
                        fitnessGoal = fitnessGoal,
                        selectedDateWeightKg = dayWeightInfo.weightKg,
                        isWeightExactForDate = dayWeightInfo.isExactForDate,
                        weightRecordDate = dayWeightInfo.recordDate,
                        effectiveProteinTarget = dayWeightInfo.targetProtein,
                        nutrientBars = calculateBars(items, current.nutritionGoal, dayWeightInfo.targetProtein)
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
                    val dayWeightInfo = resolveDayWeightAndProtein(current.selectedDate)
                    val fitnessGoal = tokenStorage.getUserFitnessGoal()
                    current.copy(
                        nutritionGoal = goal,
                        fitnessGoal = fitnessGoal,
                        selectedDateWeightKg = dayWeightInfo.weightKg,
                        isWeightExactForDate = dayWeightInfo.isExactForDate,
                        weightRecordDate = dayWeightInfo.recordDate,
                        effectiveProteinTarget = dayWeightInfo.targetProtein,
                        nutrientBars = calculateBars(current.items, goal, dayWeightInfo.targetProtein)
                    )
                }
            }
        }
    }

    private fun observeWeights(userId: Int) {
        weightObservationJob?.cancel()
        weightObservationJob = viewModelScope.launch {
            database.observeWeights(userId).collect { entries ->
                cachedWeights = entries
                val sorted = entries.sortedBy { it.date }
                val profileWeight = tokenStorage.getUserWeight().takeIf { it > 0.0 }
                val latest = sorted.lastOrNull()?.weightKg ?: profileWeight
                val recentEntries = sorted.takeLast(7)
                val diff = if (recentEntries.size >= 2) {
                    val first = recentEntries.first().weightKg
                    latest?.minus(first)
                } else null

                val recentWeights = when {
                    recentEntries.isNotEmpty() -> recentEntries.map { it.weightKg }
                    latest != null -> listOf(latest, latest)
                    else -> emptyList()
                }

                _state.update { current ->
                    val dayWeightInfo = resolveDayWeightAndProtein(current.selectedDate)
                    val fitnessGoal = tokenStorage.getUserFitnessGoal()
                    current.copy(
                        latestWeightKg = latest,
                        weightDifferenceKg = diff,
                        recentWeights = recentWeights,
                        fitnessGoal = fitnessGoal,
                        selectedDateWeightKg = dayWeightInfo.weightKg,
                        isWeightExactForDate = dayWeightInfo.isExactForDate,
                        weightRecordDate = dayWeightInfo.recordDate,
                        effectiveProteinTarget = dayWeightInfo.targetProtein,
                        nutrientBars = calculateBars(current.items, current.nutritionGoal, dayWeightInfo.targetProtein)
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
                _state.update {
                    it.copy(
                        isCalculatingAi = false,
                        pendingAiResult = aiResult,
                        isAiReviewSheetOpen = true,
                        rawText = ""
                    )
                }
            }.onFailure { err ->
                val errorMsg = err.message ?: "Bilinmeyen hata"
                println("[FoodViewModel] analyzeFood failure: $errorMsg")
                val displayMsg = when {
                    errorMsg.contains("404") ->
                        "Sunucuda AI servisi bulunamadı (404). Render üzerinde henüz yeni sürüm deploy edilmemiş olabilir."
                    errorMsg.contains("401") ->
                        "Oturum süresi doldu (401). Lütfen tekrar giriş yapınız."
                    errorMsg.contains("GEMINI_API_KEY is not configured", ignoreCase = true) ->
                        "Render sunucusunda GEMINI_API_KEY tanımlı değil."
                    errorMsg.contains("başarısız (HTTP", ignoreCase = true) -> {
                        val serverErr = if (errorMsg.contains("\"error\":")) {
                            errorMsg.substringAfter("\"error\":").substringBefore("}").trim('"', ' ', '\\')
                        } else {
                            errorMsg
                        }
                        "AI analizi başarısız: $serverErr"
                    }
                    errorMsg.contains("Connect", ignoreCase = true) || errorMsg.contains("timeout", ignoreCase = true) ->
                        "Sunucuya bağlanılamadı veya zaman aşımına uğradı. Lütfen internet bağlantınızı kontrol ediniz."
                    else ->
                        "AI analizi başarısız: $errorMsg"
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

    private fun confirmAiReview() {
        val aiResult = _state.value.pendingAiResult ?: return
        val date = _state.value.selectedDate
        val userId = tokenStorage.getUserId()
        val existingLog = _state.value.currentFoodLog
        val now = currentTime()
        val logId = existingLog?.id ?: Uuid.random().toString()

        viewModelScope.launch {
            val combinedItems = ((existingLog?.items ?: emptyList()) + aiResult.items).map { item ->
                if (item.id.isBlank()) item.copy(id = Uuid.random().toString()) else item
            }
            val totalCals = combinedItems.sumOf { it.calories }
            val newLog = FoodLog(
                id = logId,
                userId = userId,
                date = date,
                rawText = combinedItems.joinToString(", ") { "${it.name} (${it.formattedPortion})" },
                summary = aiResult.summary,
                totalCalories = totalCals,
                items = combinedItems,
                createdAt = existingLog?.createdAt ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )

            database.upsertFood(newLog)
            _state.update {
                it.copy(
                    isAiReviewSheetOpen = false,
                    pendingAiResult = null,
                    rawText = "",
                    infoMessage = "AI ile besin analizi onaylandı ve kaydedildi."
                )
            }
            launch { syncEngine.sync() }
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
            _state.update { it.copy(rawText = "") }
            _effects.send(FoodEffect.ShowToast("Kayıt güncellendi."))
            launch { syncEngine.sync() }
        }
    }

    private fun addItem(
        name: String,
        portion: String,
        calories: Int,
        proteinG: Double = 0.0,
        carbsG: Double = 0.0,
        fatG: Double = 0.0,
        fiberG: Double = 0.0
    ) {
        val newItem = FoodItem(
            id = Uuid.random().toString(),
            name = name.trim(),
            formattedPortion = portion.trim(),
            calories = calories,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            fiberG = fiberG
        )
        val currentItems = _state.value.items + newItem
        updateItemsInDb(currentItems)
    }

    private fun removeItem(itemId: String) {
        val currentItems = _state.value.items.filter { it.id != itemId && it.name != itemId }
        if (currentItems.isEmpty()) {
            val existingLog = _state.value.currentFoodLog
            if (existingLog != null) {
                viewModelScope.launch {
                    database.deleteFood(existingLog.id)
                    _state.update {
                        it.copy(
                            currentFoodLog = null,
                            rawText = "",
                            summary = null,
                            items = emptyList(),
                            totalCalories = 0,
                            nutrientBars = calculateBars(emptyList(), it.nutritionGoal),
                            infoMessage = "Yiyecek silindi."
                        )
                    }
                    launch { syncEngine.sync() }
                }
                return
            } else {
                _state.update {
                    it.copy(
                        items = emptyList(),
                        totalCalories = 0,
                        rawText = "",
                        summary = null,
                        nutrientBars = calculateBars(emptyList(), it.nutritionGoal),
                        infoMessage = "Yiyecek silindi."
                    )
                }
                return
            }
        }
        updateItemsInDb(currentItems)
        _state.update { it.copy(infoMessage = "Yiyecek silindi.") }
    }

    private fun clearCurrentLog() {
        val existingLog = _state.value.currentFoodLog
        if (existingLog != null) {
            viewModelScope.launch {
                database.deleteFood(existingLog.id)
                _state.update {
                    it.copy(
                        currentFoodLog = null,
                        rawText = "",
                        summary = null,
                        items = emptyList(),
                        totalCalories = 0,
                        nutrientBars = calculateBars(emptyList(), it.nutritionGoal),
                        infoMessage = "Günün tüm yemek kayıtları silindi."
                    )
                }
                launch { syncEngine.sync() }
            }
        } else {
            _state.update {
                it.copy(
                    items = emptyList(),
                    totalCalories = 0,
                    rawText = "",
                    summary = null,
                    nutrientBars = calculateBars(emptyList(), it.nutritionGoal),
                    infoMessage = "Kayıtlar temizlendi."
                )
            }
        }
    }

    private fun updateItemsInDb(items: List<FoodItem>) {
        val now = currentTime()
        val totalCals = items.sumOf { it.calories }
        val userId = tokenStorage.getUserId()
        val date = _state.value.selectedDate

        val existingLog = _state.value.currentFoodLog
        val updated = if (existingLog != null) {
            existingLog.copy(
                items = items,
                totalCalories = totalCals,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        } else {
            FoodLog(
                id = Uuid.random().toString(),
                userId = userId,
                date = date,
                rawText = items.joinToString(", ") { "${it.name} (${it.formattedPortion})" },
                summary = "Günlük Öğün Kaydı",
                totalCalories = totalCals,
                items = items,
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        }

        viewModelScope.launch {
            database.upsertFood(updated)
            launch { syncEngine.sync() }
        }
    }

    private fun saveGoals(goal: NutritionGoal, fitnessGoal: FitnessGoal) {
        val userId = tokenStorage.getUserId()
        viewModelScope.launch {
            val now = currentTime()
            val updated = goal.copy(
                userId = userId,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
            tokenStorage.setUserFitnessGoal(fitnessGoal)
            database.upsertNutritionGoal(updated)

            val dayWeightInfo = resolveDayWeightAndProtein(_state.value.selectedDate)
            _state.update { current ->
                current.copy(
                    nutritionGoal = updated,
                    fitnessGoal = fitnessGoal,
                    selectedDateWeightKg = dayWeightInfo.weightKg,
                    isWeightExactForDate = dayWeightInfo.isExactForDate,
                    weightRecordDate = dayWeightInfo.recordDate,
                    effectiveProteinTarget = dayWeightInfo.targetProtein,
                    nutrientBars = calculateBars(current.items, updated, dayWeightInfo.targetProtein),
                    isEditGoalsDialogOpen = false,
                    infoMessage = "Besin hedefleri ve ${fitnessGoal.title} hedefiniz güncellendi."
                )
            }
            _effects.send(FoodEffect.ShowToast("${fitnessGoal.title} hedefine göre besin hedefleri güncellendi."))
            launch { syncEngine.sync() }
        }
    }

    private fun calculateBars(
        items: List<FoodItem>,
        goal: NutritionGoal,
        proteinTargetG: Double = _state.value.effectiveProteinTarget
    ): List<NutrientProgress> {
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
            NutrientProgress("protein", "Protein", totalProtein, proteinTargetG, "g", "#8E24AA"),
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

    private fun currentTime(): Long = com.yusufteker.healthtrack.core.utils.currentTimeMillis()
}
