package com.yusufteker.healthtrack.feature.food.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.SmartMealEngine
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.utils.ExternalAppLauncher
import com.yusufteker.healthtrack.core.utils.shiftDateIso
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

    private val _state = MutableStateFlow(FoodState(fitnessGoal = tokenStorage.getUserFitnessGoal()))
    val state: StateFlow<FoodState> = _state.asStateFlow()

    private val _effects = Channel<FoodEffect>(Channel.BUFFERED)
    val effects: Flow<FoodEffect> = _effects.receiveAsFlow()

    private var foodObservationJob: Job? = null
    private var goalObservationJob: Job? = null
    private var weightObservationJob: Job? = null
    private var customFoodsObservationJob: Job? = null
    private var favoriteFoodsObservationJob: Job? = null
    private var allFoodsObservationJob: Job? = null
    private var waterObservationJob: Job? = null
    private var workoutObservationJob: Job? = null

    private var cachedWeights: List<WeightEntry> = emptyList()
    private var cachedRecent3DaysFoodNames: Set<String> = emptySet()

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
                    observeAllFoods(session.userId)
                    observeWaterAndWorkouts(session.userId, _state.value.selectedDate)
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
                        effectiveProteinTarget = dayWeightInfo.targetProtein,
                        currentFoodLog = null,
                        items = emptyList(),
                        totalCalories = 0,
                        summary = null,
                        nutrientBars = calculateBars(emptyList(), current.nutritionGoal, dayWeightInfo.targetProtein)
                    )
                }
                val userId = tokenStorage.getUserId()
                if (userId > 0) {
                    observeDataForCurrentDate(userId, intent.newDate)
                    observeWaterAndWorkouts(userId, intent.newDate)
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
                intent.fiberG,
                intent.micronutrients,
                intent.iconKey
            )
            is FoodIntent.OnAddMultipleItems -> addMultipleItems(intent.items, intent.summaryNote)
            is FoodIntent.OnOpenEditGoalsDialog -> {
                val currentFitnessGoal = if (_state.value.nutritionGoal.fitnessGoal.isNotBlank() && _state.value.nutritionGoal.fitnessGoal != FitnessGoal.MAINTAIN.id) {
                    _state.value.nutritionGoal.parsedFitnessGoal
                } else {
                    tokenStorage.getUserFitnessGoal()
                }
                _state.update {
                    it.copy(
                        isEditGoalsDialogOpen = true,
                        fitnessGoal = currentFitnessGoal
                    )
                }
            }
            is FoodIntent.OnCloseEditGoalsDialog -> _state.update { it.copy(isEditGoalsDialogOpen = false) }
            is FoodIntent.OnSaveGoals -> saveGoals(intent.goal, intent.fitnessGoal)
            is FoodIntent.OnDismissInfoMessage -> _state.update { it.copy(infoMessage = null) }
            is FoodIntent.OnDismissErrorMessage -> _state.update { it.copy(errorMessage = null) }
            is FoodIntent.OnDismissAiReview -> _state.update { it.copy(isAiReviewSheetOpen = false, pendingAiResult = null) }
            is FoodIntent.OnConfirmAiReview -> confirmAiReview()
            is FoodIntent.OnToggleFavoriteFood -> toggleFavorite(intent.foodId)
            is FoodIntent.OnOpenAddCustomFoodDialog -> _state.update {
                it.copy(
                    isAddCustomFoodDialogOpen = true,
                    customFoodAiResult = null,
                    customFoodAiError = null,
                    isAnalyzingCustomFoodAi = false
                )
            }
            is FoodIntent.OnCloseAddCustomFoodDialog -> _state.update {
                it.copy(
                    isAddCustomFoodDialogOpen = false,
                    customFoodAiResult = null,
                    customFoodAiError = null,
                    isAnalyzingCustomFoodAi = false
                )
            }
            is FoodIntent.OnSaveCustomFood -> saveCustomFood(intent)
            is FoodIntent.OnDeleteCustomFood -> deleteCustomFood(intent.id)
            is FoodIntent.OnAnalyzeCustomFoodAi -> analyzeCustomFoodWithAi(intent.prompt, intent.isMixMode)
            is FoodIntent.OnConsumeCustomFoodAiResult -> _state.update { it.copy(customFoodAiResult = null) }
            is FoodIntent.OnDismissCustomFoodAiError -> _state.update { it.copy(customFoodAiError = null) }

            // ── 2.A: Su & Hidrasyon ──
            is FoodIntent.OnAddWater -> addWater(intent.deltaMl)
            is FoodIntent.OnResetWater -> resetWater()

            // ── 1: Akıllı Öğün & "Ne Yesem?" ──
            is FoodIntent.OnOpenSmartMealSheet -> {
                _state.update { it.copy(isSmartMealSheetOpen = true) }
                updateSmartMealsAndSynergy()
            }
            is FoodIntent.OnCloseSmartMealSheet -> _state.update { it.copy(isSmartMealSheetOpen = false) }
            is FoodIntent.OnToggleSmartMealFilter -> {
                _state.update { current ->
                    when (intent.filterType) {
                        SmartMealFilterType.FROM_USER_DB -> current.copy(isSmartMealFromUserDb = !current.isSmartMealFromUserDb)
                        SmartMealFilterType.QUICK -> current.copy(isSmartMealOnlyQuick = !current.isSmartMealOnlyQuick)
                        SmartMealFilterType.FEW_INGREDIENTS -> current.copy(isSmartMealOnlyFewIngredients = !current.isSmartMealOnlyFewIngredients)
                        SmartMealFilterType.EXCLUDE_RECENT_3_DAYS -> current.copy(isSmartMealExcludeRecent3Days = !current.isSmartMealExcludeRecent3Days)
                        SmartMealFilterType.HIGH_PROTEIN -> current.copy(isSmartMealHighProtein = !current.isSmartMealHighProtein)
                    }
                }
                updateSmartMealsAndSynergy()
            }
            is FoodIntent.OnAddSuggestedMeal -> addSuggestedMeal(intent.meal)
            is FoodIntent.OnRequestCustomAiMealSuggestion -> requestCustomAiMealSuggestion()
            is FoodIntent.OnDismissAiRecipe -> {
                cachedAiRecipe = null
                _state.update { it.copy(latestAiRecipe = null) }
            }

            // ── 2.B: Besin Sinerjisi & Emilim ──
            is FoodIntent.OnDismissSynergyTip -> {
                _state.update { current ->
                    val updatedDismissed = current.dismissedTipIds + intent.tipId
                    current.copy(
                        dismissedTipIds = updatedDismissed,
                        synergyTips = current.synergyTips.filterNot { it.id == intent.tipId }
                    )
                }
            }

            // ── 2.D: Fotoğraf / Etiket Tarama ──
            is FoodIntent.OnOpenPhotoScanSheet -> _state.update { it.copy(isPhotoScanSheetOpen = true) }
            is FoodIntent.OnClosePhotoScanSheet -> _state.update { it.copy(isPhotoScanSheetOpen = false) }
            is FoodIntent.OnAnalyzePhotoOrLabel -> analyzePhotoOrLabel(intent.textOrPrompt)

            // ── Market Listesi & Planora ──
            is FoodIntent.OnOpenGroceryListSheet -> {
                _state.update {
                    it.copy(
                        isGroceryListSheetOpen = true,
                        groceryListTitle = intent.title,
                        groceryItems = intent.items,
                        selectedGroceryItems = intent.items.toSet()
                    )
                }
            }
            is FoodIntent.OnCloseGroceryListSheet -> _state.update { it.copy(isGroceryListSheetOpen = false) }
            is FoodIntent.OnToggleGroceryItem -> {
                _state.update { current ->
                    val updated = current.selectedGroceryItems.toMutableSet()
                    if (updated.contains(intent.item)) updated.remove(intent.item) else updated.add(intent.item)
                    current.copy(selectedGroceryItems = updated)
                }
            }
            is FoodIntent.OnSendGroceryToPlanora -> sendGroceryToPlanora(intent.appLauncher)
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

    private fun observeAllFoods(userId: Int) {
        allFoodsObservationJob?.cancel()
        allFoodsObservationJob = viewModelScope.launch {
            database.observeAllFoods(userId).collect { allLogs ->
                val frequent = allLogs
                    .flatMap { it.items }
                    .groupBy { it.name.trim().lowercase() }
                    .values
                    .sortedByDescending { it.size }
                    .map { it.first() }
                    .take(25)

                val cutoffDate = shiftDateIso(_state.value.selectedDate, -3)
                cachedRecent3DaysFoodNames = allLogs
                    .filter { it.date in cutoffDate..<_state.value.selectedDate }
                    .flatMap { it.items }
                    .map { it.name.trim().lowercase() }
                    .toSet()

                _state.update { it.copy(frequentFoods = frequent) }
                updateSmartMealsAndSynergy()
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
            iconKey = intent.iconKey,
            micronutrients = intent.micronutrients,
            ingredients = intent.ingredients,
            baseAmount = intent.baseAmount,
            baseUnitLabel = intent.baseUnitLabel,
            presetLevels = intent.presetLevels,
            createdAt = now,
            updatedAt = now
        )

        viewModelScope.launch {
            database.upsertCustomFood(newItem)
            if (intent.makeFavorite) {
                database.toggleFavoriteFood(userId, customId)
            }
            _state.update { it.copy(isAddCustomFoodDialogOpen = false) }
            val label = if (newItem.isMix) "${newItem.name} karışımı envanterinize eklendi." else "${newItem.name} envanterinize eklendi."
            _effects.send(FoodEffect.ShowToast(label))
        }
    }

    private fun deleteCustomFood(id: String) {
        viewModelScope.launch {
            database.deleteCustomFood(id)
            _effects.send(FoodEffect.ShowToast("Özel ürün silindi."))
        }
    }

    private fun analyzeCustomFoodWithAi(prompt: String, isMixMode: Boolean) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) {
            _state.update { it.copy(customFoodAiError = "Lütfen analiz edilecek ürün veya karışım içeriğini yazınız.") }
            return
        }

        val date = _state.value.selectedDate
        val structuredText = if (isMixMode) {
            "$cleanPrompt (Bu bir karışım/öğün tarifidir. Lütfen içindeki her bileşeni ayrı bir kalem olarak ayır. Her kalemin formattedPortion alanına kullanıcının belirttiği miktarı ve birimini net yaz: örn '150 ml', '2 shot', '1 ölçek', '35 g', '1 adet'. Kalori, protein, karbonhidrat, yağ, lif ve varsa vitamin/mineral/kafein/l-karnitin gibi mikro besinlerini doldur.)"
        } else {
            "$cleanPrompt (Bu tek bir üründür. Lütfen tek bir kalem olarak analiz et, formattedPortion alanına kullanıcının belirttiği birimi ve gramaj/ml bilgisini net yaz: örn '1 Ölçek (35g)' veya '1 Ölçek (30ml)'. Kalori, protein, karbonhidrat, yağ, lif ve içindeki tüm vitamin, mineral, aminoasit, kafein, l-karnitin, kreatin gibi mikro besin/içerik değerlerini micronutrients listesine eksiksiz ekle.)"
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isAnalyzingCustomFoodAi = true,
                    customFoodAiError = null,
                    customFoodAiResult = null
                )
            }

            val result = apiClient.analyzeFood(structuredText, date)
            result.onSuccess { aiResult ->
                _state.update {
                    it.copy(
                        isAnalyzingCustomFoodAi = false,
                        customFoodAiResult = aiResult,
                        customFoodAiPromptUsed = cleanPrompt,
                        customFoodAiError = null
                    )
                }
            }.onFailure { err ->
                val errorMsg = err.message ?: "Bilinmeyen hata"
                val displayMsg = when {
                    errorMsg.contains("404") ->
                        "Sunucuda AI servisi bulunamadı (404). Render uyanıyor olabilir."
                    errorMsg.contains("401") ->
                        "Oturum süresi doldu (401). Lütfen tekrar giriş yapınız."
                    errorMsg.contains("Connect", ignoreCase = true) || errorMsg.contains("timeout", ignoreCase = true) ->
                        "Sunucuya bağlanılamadı veya zaman aşımına uğradı."
                    else ->
                        "AI analizi başarısız: $errorMsg"
                }
                _state.update {
                    it.copy(
                        isAnalyzingCustomFoodAi = false,
                        customFoodAiError = displayMsg
                    )
                }
            }
        }
    }

    private fun observeDataForCurrentDate(userId: Int, date: String) {
        foodObservationJob?.cancel()
        foodObservationJob = viewModelScope.launch {
            database.observeFoods(userId, date).collect { logs ->
                val primaryLog = logs.firstOrNull()
                _state.update { current ->
                    val rawItems = primaryLog?.items ?: emptyList()
                    val items = mergeDuplicateFoodItems(rawItems)
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
                updateSmartMealsAndSynergy()
            }
        }
    }

    private fun observeGoal(userId: Int) {
        goalObservationJob?.cancel()
        goalObservationJob = viewModelScope.launch {
            database.observeNutritionGoal(userId).collect { goal ->
                _state.update { current ->
                    val dayWeightInfo = resolveDayWeightAndProtein(current.selectedDate)
                    val fitnessGoal = if (goal.fitnessGoal.isNotBlank() && goal.fitnessGoal != FitnessGoal.MAINTAIN.id) {
                        goal.parsedFitnessGoal
                    } else {
                        tokenStorage.getUserFitnessGoal()
                    }
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
        val existingLog = _state.value.currentFoodLog?.takeIf { it.date == date }
        val now = currentTime()
        val logId = existingLog?.id ?: "food_${userId}_${date}"

        viewModelScope.launch {
            val combinedItems = mergeDuplicateFoodItems(
                ((existingLog?.items ?: emptyList()) + aiResult.items).map { item ->
                    if (item.id.isBlank()) item.copy(id = Uuid.random().toString()) else item
                }
            )
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
        val existingLog = _state.value.currentFoodLog?.takeIf { it.date == date }
        val now = currentTime()

        val logId = existingLog?.id ?: "food_${userId}_${date}"
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
        fiberG: Double = 0.0,
        micronutrients: List<Micronutrient> = emptyList(),
        iconKey: String? = null
    ) {
        val newItem = FoodItem(
            id = Uuid.random().toString(),
            name = name.trim(),
            formattedPortion = portion.trim(),
            calories = calories,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            fiberG = fiberG,
            micronutrients = micronutrients,
            iconKey = iconKey
        )
        val currentItems = mergeDuplicateFoodItems(_state.value.items + newItem)
        updateItemsInDb(currentItems)
    }

    private fun addMultipleItems(itemsToAdd: List<FoodItem>, summaryNote: String? = null) {
        if (itemsToAdd.isEmpty()) return
        val normalized = itemsToAdd.map { item ->
            if (item.id.isBlank()) item.copy(id = Uuid.random().toString()) else item
        }
        val currentItems = mergeDuplicateFoodItems(_state.value.items + normalized)
        updateItemsInDb(currentItems)
        if (!summaryNote.isNullOrBlank()) {
            _state.update { it.copy(infoMessage = summaryNote) }
        }
    }

    /**
     * Merges duplicate FoodItems with the same name in a daily log into a single row with `xN` multiplier
     * (e.g., 4x Süt (50ml) -> 1 row "200ml (4x)"; 2x Espresso (30ml) -> 1 row "60ml (Shot x2)").
     */
    private fun mergeDuplicateFoodItems(items: List<FoodItem>): List<FoodItem> {
        if (items.size <= 1) return items
        return items
            .groupBy { it.name.trim().lowercase() }
            .map { (_, group) ->
                if (group.size == 1) {
                    group.first()
                } else {
                    val first = group.first()
                    val totalCount = group.sumOf { extractPortionMultiplier(it.formattedPortion) }
                    val mergedPortion = buildMergedPortionLabel(first.name, group.map { it.formattedPortion }, totalCount)
                    val mergedMicros = group
                        .flatMap { it.micronutrients }
                        .groupBy { "${it.name}|${it.unit}" }
                        .map { (_, mList) ->
                            val mFirst = mList.first()
                            mFirst.copy(amount = kotlin.math.round(mList.sumOf { it.amount } * 10.0) / 10.0)
                        }
                    first.copy(
                        formattedPortion = mergedPortion,
                        calories = group.sumOf { it.calories },
                        proteinG = kotlin.math.round(group.sumOf { it.proteinG } * 10.0) / 10.0,
                        carbsG = kotlin.math.round(group.sumOf { it.carbsG } * 10.0) / 10.0,
                        fatG = kotlin.math.round(group.sumOf { it.fatG } * 10.0) / 10.0,
                        fiberG = kotlin.math.round(group.sumOf { it.fiberG } * 10.0) / 10.0,
                        micronutrients = mergedMicros,
                        iconKey = group.firstNotNullOfOrNull { it.iconKey }
                    )
                }
            }
    }

    private fun extractPortionMultiplier(portion: String): Int {
        val p = portion.trim()
        // Match "(4x)", "(x4)", "x4", "4x"
        Regex("""\(\s*(?:x\s*(\d+)|(\d+)\s*x)\s*\)""", RegexOption.IGNORE_CASE).find(p)?.let { m ->
            val n = (m.groupValues[1].ifBlank { m.groupValues[2] }).toIntOrNull()
            if (n != null && n > 0) return n
        }
        Regex("""\b(?:shot\s*x\s*(\d+)|x\s*(\d+)|(\d+)\s*x)\b""", RegexOption.IGNORE_CASE).find(p)?.let { m ->
            val n = (m.groupValues[1].ifBlank { m.groupValues[2] }.ifBlank { m.groupValues[3] }).toIntOrNull()
            if (n != null && n > 0) return n
        }
        return 1
    }

    private fun buildMergedPortionLabel(itemName: String, portions: List<String>, totalCount: Int): String {
        val firstRaw = portions.firstOrNull()?.trim().orEmpty()
        if (firstRaw.lowercase().contains("shot") && !firstRaw.lowercase().contains("ml")) {
            return "Shot x$totalCount"
        }
        // Check if all portions have a leading ml or g amount (e.g. "50ml", "50 ml", "200ml (4x)")
        val mlOrG = portions.mapNotNull { p ->
            Regex("""^(\d+(?:[.,]\d+)?)\s*(ml|g|gr|gram)\b""", RegexOption.IGNORE_CASE).find(p.trim())?.let { m ->
                val amt = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
                val unit = if (m.groupValues[2].lowercase().startsWith("m")) "ml" else "g"
                amt to unit
            }
        }
        if (mlOrG.size == portions.size && mlOrG.map { it.second }.distinct().size == 1) {
            val totalAmt = kotlin.math.round(mlOrG.sumOf { it.first }).toInt()
            val unit = mlOrG.first().second
            return if (itemName.contains("espresso", ignoreCase = true)) {
                "$totalAmt$unit (Shot x$totalCount)"
            } else {
                "$totalAmt$unit (${totalCount}x)"
            }
        }
        val cleanBase = firstRaw
            .replace(Regex("""\(\s*(?:shot\s*)?(?:x\s*\d+|\d+\s*x)\s*\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\b(?:x\s*\d+|\d+\s*x)\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^1\s+"""), "")
            .trim()
        return if (cleanBase.isNotBlank()) "$cleanBase (${totalCount}x)" else "${totalCount}x"
    }

    private fun removeItem(itemId: String) {
        val currentItems = _state.value.items.filter { it.id != itemId && it.name != itemId }
        val date = _state.value.selectedDate
        if (currentItems.isEmpty()) {
            val existingLog = _state.value.currentFoodLog?.takeIf { it.date == date }
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
        val date = _state.value.selectedDate
        val existingLog = _state.value.currentFoodLog?.takeIf { it.date == date }
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

        val existingLog = _state.value.currentFoodLog?.takeIf { it.date == date }
        val updated = if (existingLog != null) {
            existingLog.copy(
                date = date,
                items = items,
                totalCalories = totalCals,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        } else {
            FoodLog(
                id = "food_${userId}_${date}",
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
                fitnessGoal = fitnessGoal.id,
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

    // ─────────────────────────────────────────────────────────────
    // 2.A: WATER HYDRATION & WORKOUT-RESPONSIVE TARGET
    // ─────────────────────────────────────────────────────────────

    private fun observeWaterAndWorkouts(userId: Int, date: String) {
        waterObservationJob?.cancel()
        waterObservationJob = viewModelScope.launch {
            database.observeWater(userId, date).collect { entry ->
                _state.update { current ->
                    current.copy(waterEntry = entry)
                }
                updateSmartMealsAndSynergy()
            }
        }

        workoutObservationJob?.cancel()
        workoutObservationJob = viewModelScope.launch {
            database.observeWorkouts(userId, date).collect { workouts ->
                val totalBurned = workouts.filter { !it.isDeleted }.sumOf { it.caloriesBurned }
                val extraWater = (totalBurned / 200) * 350
                val dynamicTarget = (2500 + extraWater).coerceIn(2000, 4500)
                _state.update { it.copy(dynamicWaterTargetMl = dynamicTarget) }
            }
        }
    }

    private fun addWater(deltaMl: Int) {
        val userId = tokenStorage.getUserId()
        if (userId <= 0) return
        val date = _state.value.selectedDate
        val target = _state.value.dynamicWaterTargetMl
        viewModelScope.launch {
            val updated = database.addWater(userId, date, deltaMl, target)
            _state.update { it.copy(waterEntry = updated) }
            updateSmartMealsAndSynergy()
            if (deltaMl > 0) {
                _effects.send(FoodEffect.ShowToast("+${deltaMl}ml su eklendi (${updated.amountMl}/${target}ml)"))
            }
        }
    }

    private fun resetWater() {
        val userId = tokenStorage.getUserId()
        if (userId <= 0) return
        val date = _state.value.selectedDate
        viewModelScope.launch {
            database.resetWater(userId, date)
            _state.update { it.copy(waterEntry = it.waterEntry?.copy(amountMl = 0)) }
            updateSmartMealsAndSynergy()
            _effects.send(FoodEffect.ShowToast("Günlük su kaydı sıfırlandı."))
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 1 & 2.B: SMART MEAL MATCHER & SYNERGY TIPS ENGINE
    // ─────────────────────────────────────────────────────────────

    private var cachedAiRecipe: SmartMealSuggestion? = null

    private fun updateSmartMealsAndSynergy() {
        val current = _state.value
        val remainingCals = (current.nutritionGoal.calories - current.totalCalories).coerceAtLeast(0)
        val missingProtein = (current.effectiveProteinTarget - (current.nutrientBars.firstOrNull { it.key == "protein" }?.current ?: 0.0)).coerceAtLeast(0.0)

        val suggestions = SmartMealEngine.matchSmartMeals(
            remainingCalories = remainingCals,
            missingProteinG = missingProtein,
            nutrientBars = current.nutrientBars,
            recent3DaysFoodNames = cachedRecent3DaysFoodNames,
            customFoods = current.customFoods,
            frequentFoods = current.frequentFoods,
            onlyQuick = current.isSmartMealOnlyQuick,
            onlyFewIngredients = current.isSmartMealOnlyFewIngredients,
            excludeRecent3Days = current.isSmartMealExcludeRecent3Days,
            highProteinOnly = current.isSmartMealHighProtein,
            fromUserDbOnly = current.isSmartMealFromUserDb
        )

        val combinedSuggestions = if (cachedAiRecipe != null) {
            listOf(cachedAiRecipe!!) + suggestions.filterNot { it.id == cachedAiRecipe!!.id }
        } else {
            suggestions
        }

        val tips = SmartMealEngine.detectSynergyTips(
            todayItems = current.items,
            nutrientBars = current.nutrientBars,
            waterCurrentMl = current.waterEntry?.amountMl ?: 0
        ).filterNot { current.dismissedTipIds.contains(it.id) }

        _state.update {
            it.copy(
                smartMealSuggestions = combinedSuggestions,
                latestAiRecipe = cachedAiRecipe,
                synergyTips = tips
            )
        }
    }

    private fun addSuggestedMeal(meal: SmartMealSuggestion) {
        val foodItem = FoodItem(
            id = Uuid.random().toString(),
            name = meal.title,
            formattedPortion = "1 porsiyon (${meal.ingredients.joinToString(", ")})",
            calories = meal.calories,
            proteinG = meal.proteinG,
            carbsG = meal.carbsG,
            fatG = meal.fatG,
            fiberG = meal.fiberG,
            micronutrients = meal.keyMicros,
            iconKey = "meal"
        )
        addItem(
            name = foodItem.name,
            portion = foodItem.formattedPortion,
            calories = foodItem.calories,
            proteinG = foodItem.proteinG,
            carbsG = foodItem.carbsG,
            fatG = foodItem.fatG,
            fiberG = foodItem.fiberG,
            micronutrients = foodItem.micronutrients,
            iconKey = foodItem.iconKey
        )
        _state.update { it.copy(isSmartMealSheetOpen = false) }
        viewModelScope.launch {
            _effects.send(FoodEffect.ShowToast("\"${meal.title}\" günlüğe eklendi."))
        }
    }

    private fun requestCustomAiMealSuggestion() {
        val current = _state.value
        val date = current.selectedDate
        val remainingCals = (current.nutritionGoal.calories - current.totalCalories).coerceAtLeast(0)
        val missingProtein = (current.effectiveProteinTarget - (current.nutrientBars.firstOrNull { it.key == "protein" }?.current ?: 0.0)).coerceAtLeast(0.0)

        val prompt = buildString {
            append("Bana kalan kalori ($remainingCals kcal) ve eksik protein (${missingProtein.toInt()}g) için ")
            if (current.isSmartMealOnlyQuick) append("10 dakikadan kısa sürede hazırlanan, ")
            if (current.isSmartMealOnlyFewIngredients) append("en fazla 4 malzemeli, ")
            if (current.isSmartMealExcludeRecent3Days && cachedRecent3DaysFoodNames.isNotEmpty()) {
                append("son 3 günde yediğim (${cachedRecent3DaysFoodNames.take(4).joinToString(", ")}) dışındaki malzemelerle ")
            }

            val userDbItems = (current.customFoods.map { "${it.name} (${it.portionUnit})" } +
                current.frequentFoods.map { "${it.name} (${it.formattedPortion})" }).distinct().take(10)

            if (userDbItems.isNotEmpty()) {
                if (current.isSmartMealFromUserDb) {
                    append("özellikle kullanıcının kendi kütüphanesinde/envanterinde kayıtlı şu ürünlerden yararlanarak: [${userDbItems.joinToString(", ")}], ")
                } else {
                    append("kullanıcının envanterindeki şu kayıtlı ürünleri de göz önünde bulundurarak: [${userDbItems.joinToString(", ")}], ")
                }
            }

            append("lif ve magnezyum zengini pratik bir sağlıklı öğün öner ve tam porsiyon besin değerlerini (kalori, protein, karbonhidrat, yağ, lif, vitamin/mineral) hesapla.")
        }

        viewModelScope.launch {
            _state.update { it.copy(isGeneratingAiCustomRecipe = true) }
            val result = apiClient.analyzeFood(prompt, date)
            result.onSuccess { aiResult ->
                val recipeTitle = run {
                    val firstLine = aiResult.summary.lineSequence().firstOrNull()?.trim() ?: ""
                    val candidate = when {
                        firstLine.contains(":") -> firstLine.substringBefore(":").trim()
                        firstLine.contains("\"") -> firstLine.substringAfter("\"").substringBefore("\"").trim()
                        firstLine.length in 4..35 && !firstLine.startsWith("Kalan", ignoreCase = true) -> firstLine
                        else -> null
                    }
                    if (!candidate.isNullOrBlank() && candidate.length <= 35) {
                        candidate
                    } else if (aiResult.items.size >= 2) {
                        val topTwo = aiResult.items.take(2).joinToString(" & ") { it.name.trim() }
                        "$topTwo Kasesi"
                    } else if (aiResult.items.isNotEmpty()) {
                        "${aiResult.items.first().name} Tabağı"
                    } else {
                        "Yapay Zeka Özel Öğünü"
                    }
                }

                val ingredientList = if (aiResult.items.isNotEmpty()) {
                    aiResult.items.map { "${it.name} (${it.formattedPortion})" }
                } else {
                    listOf(aiResult.summary.take(80))
                }

                val customSuggestion = SmartMealSuggestion(
                    id = "ai_${Uuid.random()}",
                    title = recipeTitle,
                    subtitle = "✨ Gemini AI Tarafından Size Özel Üretildi",
                    tags = listOf("AI Özel Tarif", "Kişiselleştirilmiş") +
                        if (aiResult.items.sumOf { it.proteinG } >= 20.0) listOf("Yüksek Protein") else emptyList(),
                    prepTimeMinutes = if (current.isSmartMealOnlyQuick) 8 else 15,
                    ingredients = ingredientList,
                    calories = aiResult.totalCalories,
                    proteinG = aiResult.items.sumOf { it.proteinG },
                    carbsG = aiResult.items.sumOf { it.carbsG },
                    fatG = aiResult.items.sumOf { it.fatG },
                    fiberG = aiResult.items.sumOf { it.fiberG },
                    keyMicros = aiResult.items.flatMap { it.micronutrients },
                    instructions = aiResult.summary
                )

                cachedAiRecipe = customSuggestion

                _state.update { st ->
                    val updatedSuggestions = listOf(customSuggestion) +
                        st.smartMealSuggestions.filterNot { it.id == customSuggestion.id }
                    st.copy(
                        isGeneratingAiCustomRecipe = false,
                        latestAiRecipe = customSuggestion,
                        smartMealSuggestions = updatedSuggestions,
                        infoMessage = "✨ \"$recipeTitle\" tarifi başarıyla hazırlandı!"
                    )
                }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isGeneratingAiCustomRecipe = false,
                        errorMessage = "AI önerisi oluşturulamadı: ${err.message}"
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2.D: PHOTO / LABEL SCANNER
    // ─────────────────────────────────────────────────────────────

    private fun analyzePhotoOrLabel(textOrPrompt: String) {
        val date = _state.value.selectedDate
        val structuredPrompt = "Besin Değerleri Etiketi veya Tabak Fotoğrafı Analizi: $textOrPrompt. " +
            "Lütfen ürünün veya tabağın porsiyonunu, kalori, protein, karbonhidrat, yağ, lif ve varsa etiket üzerindeki vitamin ve mineral değerlerini tam olarak çıkar."

        viewModelScope.launch {
            _state.update { it.copy(isPhotoScanAnalyzing = true, isPhotoScanSheetOpen = false, isCalculatingAi = true) }
            val result = apiClient.analyzeFood(structuredPrompt, date)
            result.onSuccess { aiResult ->
                _state.update {
                    it.copy(
                        isCalculatingAi = false,
                        isPhotoScanAnalyzing = false,
                        pendingAiResult = aiResult,
                        isAiReviewSheetOpen = true
                    )
                }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isCalculatingAi = false,
                        isPhotoScanAnalyzing = false,
                        errorMessage = "Fotoğraf/Etiket analizi başarısız: ${err.message}"
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GROCERY LIST -> PLANORA TO-DO INTEGRATION
    // ─────────────────────────────────────────────────────────────

    private fun sendGroceryToPlanora(appLauncher: ExternalAppLauncher) {
        val itemsToSend = _state.value.selectedGroceryItems.toList()
        if (itemsToSend.isEmpty()) {
            viewModelScope.launch {
                _effects.send(FoodEffect.ShowToast("Lütfen göndermek için en az bir malzeme seçin."))
            }
            return
        }
        val title = _state.value.groceryListTitle
        val success = appLauncher.sendToPlanoraTodo(title, itemsToSend)
        _state.update { it.copy(isGroceryListSheetOpen = false) }
        viewModelScope.launch {
            if (success) {
                _effects.send(FoodEffect.ShowToast("${itemsToSend.size} malzeme Planora to-do listesine aktarıldı!"))
            } else {
                _effects.send(FoodEffect.ShowToast("Planora açılamadı. Liste panoya kopyalandı."))
            }
        }
    }
}
