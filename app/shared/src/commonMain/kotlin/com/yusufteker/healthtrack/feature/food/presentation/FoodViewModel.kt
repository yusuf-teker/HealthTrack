package com.yusufteker.healthtrack.feature.food.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.AiQuotaExceededException
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.network.formatActionableApiError
import com.yusufteker.healthtrack.core.sync.DailyMealPlanEngine
import com.yusufteker.healthtrack.core.sync.SmartMealEngine
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.utils.ExternalAppLauncher
import com.yusufteker.healthtrack.core.utils.getTodayIso
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
    private val syncEngine: SyncEngine,
    private val dailyMealPlanEngine: DailyMealPlanEngine
) : ViewModel() {

    private val _state = MutableStateFlow(
        FoodState(
            fitnessGoal = tokenStorage.getUserFitnessGoal(),
            isDailyMealSuggestionBannerVisible = tokenStorage.getDailyMealPlanDismissedDate() != getTodayIso()
        )
    )
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
    private var dailyMealPlanObservationJob: Job? = null

    private var cachedWeights: List<WeightEntry> = emptyList()
    private var cachedAllFoodLogs: List<FoodLog> = emptyList()
    private var cachedRecent3DaysFoodNames: Set<String> = emptySet()

    private fun recomputeRecent3DaysFoodNames(selectedDate: String) {
        val cutoffDate = shiftDateIso(selectedDate, -3)
        cachedRecent3DaysFoodNames = cachedAllFoodLogs
            .filter { it.date in cutoffDate..<selectedDate }
            .flatMap { it.items }
            .map { it.name.trim().lowercase() }
            .toSet()
    }

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
            LocalizationManager.languageFlow
                .drop(1)
                .collect {
                    _state.update { current ->
                        current.copy(
                            nutrientBars = calculateBars(
                                current.items,
                                current.nutritionGoal,
                                current.effectiveProteinTarget
                            )
                        )
                    }
                    cachedAiRecipe = null
                    updateSmartMealsAndSynergy()
                }
        }
        viewModelScope.launch {
            tokenStorage.sessionFlow
                .distinctUntilChangedBy { it.isLoggedIn to it.userId }
                .collect { session ->
                    if (session.isLoggedIn && session.userId > 0) {
                        cachedWeights = database.getWeights(session.userId)
                        observeDataForCurrentDate(session.userId, _state.value.selectedDate)
                        observeGoal(session.userId)
                        observeWeights(session.userId)
                        observeCustomFoods(session.userId)
                        observeFavoriteFoods(session.userId)
                        observeAllFoods(session.userId)
                        observeWaterAndWorkouts(session.userId, _state.value.selectedDate)
                        observeDailyMealPlan(session.userId, _state.value.selectedDate)
                        refreshAiQuota()
                    }
                }
        }
    }

    private fun refreshAiQuota() {
        val hasCustomKey = !tokenStorage.getCustomGeminiApiKey().isNullOrBlank()
        _state.update { it.copy(hasCustomGeminiKey = hasCustomKey) }
        if (hasCustomKey) return
        viewModelScope.launch {
            apiClient.getAiQuota().onSuccess { quota ->
                _state.update { it.copy(aiQuota = quota) }
            }
        }
    }

    fun onIntent(intent: FoodIntent) {
        when (intent) {
            is FoodIntent.OnDateChanged -> {
                val dayWeightInfo = resolveDayWeightAndProtein(intent.newDate)
                val fitnessGoal = tokenStorage.getUserFitnessGoal()
                val today = getTodayIso()
                val isToday = intent.newDate == today
                val isBannerVisible = isToday && tokenStorage.getDailyMealPlanDismissedDate() != today
                recomputeRecent3DaysFoodNames(intent.newDate)
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
                        burnedCalories = 0,
                        addedMealSlotTypes = emptySet(),
                        summary = null,
                        nutrientBars = calculateBars(emptyList(), current.nutritionGoal, dayWeightInfo.targetProtein),
                        isDailyMealSuggestionBannerVisible = isBannerVisible
                    )
                }
                val userId = tokenStorage.getUserId()
                if (userId > 0) {
                    observeDataForCurrentDate(userId, intent.newDate)
                    observeWaterAndWorkouts(userId, intent.newDate)
                    observeDailyMealPlan(userId, intent.newDate)
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
            is FoodIntent.OnClosePhotoScanSheet -> _state.update { it.copy(isPhotoScanSheetOpen = false, isPhotoScanAnalyzing = false) }
            is FoodIntent.OnSetPhotoScanTarget -> _state.update { it.copy(photoScanTarget = intent.target) }
            is FoodIntent.OnAnalyzePhotoOrLabel -> analyzePhotoOrLabel(intent.textOrPrompt, intent.imageBase64, intent.target)
            is FoodIntent.OnSaveAiItemsToInventory -> saveAiItemsToInventory(intent.items)
            is FoodIntent.OnSaveSingleAiItemToInventory -> saveSingleAiItemToInventory(intent.item)

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

            // ── Günün Öğün Önerisi (Daily AI Meal Plan) ──
            is FoodIntent.OnOpenDailyMealPlan -> {
                val today = getTodayIso()
                tokenStorage.setDailyMealPlanDismissedDate(today)
                _state.update { it.copy(isDailyMealSuggestionBannerVisible = false, isDailyMealPlanSheetOpen = true) }
                if (_state.value.dailyMealPlan == null || _state.value.dailyMealPlan?.dateIso != _state.value.selectedDate) {
                    generateDailyMealPlan(force = false)
                }
            }
            is FoodIntent.OnCloseDailyMealPlanSheet -> _state.update { it.copy(isDailyMealPlanSheetOpen = false) }
            is FoodIntent.OnDismissDailyMealPlanBanner -> {
                val today = getTodayIso()
                tokenStorage.setDailyMealPlanDismissedDate(today)
                _state.update { it.copy(isDailyMealSuggestionBannerVisible = false) }
            }
            is FoodIntent.OnRegenerateDailyMealPlan -> generateDailyMealPlan(force = true)
            is FoodIntent.OnAddSuggestedMealSlot -> addSuggestedMealSlot(intent.slot)
            is FoodIntent.OnAddEntireDailyMealPlan -> addEntireDailyMealPlan()
        }
    }

    private fun observeDailyMealPlan(userId: Int, date: String) {
        dailyMealPlanObservationJob?.cancel()
        dailyMealPlanObservationJob = viewModelScope.launch {
            database.observeDailyMealPlan(userId, date).collect { plan ->
                _state.update { it.copy(dailyMealPlan = plan) }
            }
        }
    }

    private fun generateDailyMealPlan(force: Boolean = false) {
        val userId = tokenStorage.getUserId()
        if (userId <= 0) return
        val currentDate = _state.value.selectedDate

        if (!force) {
            val existing = database.getDailyMealPlan(userId, currentDate)
            if (existing != null) {
                _state.update { it.copy(dailyMealPlan = existing, isGeneratingDailyMealPlan = false) }
                return
            }
        }

        _state.update { it.copy(isGeneratingDailyMealPlan = true) }

        viewModelScope.launch {
            try {
                val allPastLogs = database.getAllFoodsForSync(userId)
                val dayWeightInfo = resolveDayWeightAndProtein(currentDate)
                val effectiveWeight = dayWeightInfo.weightKg ?: 70.0
                val goal = _state.value.nutritionGoal
                val fitnessGoal = tokenStorage.getUserFitnessGoal()

                val lang = LocalizationManager.currentLanguage
                val plan = dailyMealPlanEngine.generateDailyPlan(
                    userId = userId,
                    dateIso = currentDate,
                    recentDaysFoodLogs = allPastLogs,
                    goal = goal,
                    fitnessGoal = fitnessGoal,
                    userWeightKg = effectiveWeight,
                    language = lang
                )

                database.upsertDailyMealPlan(plan)
                _state.update { it.copy(dailyMealPlan = plan, isGeneratingDailyMealPlan = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isGeneratingDailyMealPlan = false,
                        errorMessage = trEn(
                            "Günün öğün önerisi oluşturulurken bir hata oluştu: ${e.message}",
                            "Error generating daily meal plan: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    private fun addSuggestedMealSlot(slot: MealSlotSuggestion) {
        if (slot.itemsToAdd.isEmpty()) return
        val slotLabel = slot.slot.localizedLabel(LocalizationManager.currentLanguage)
        addMultipleItems(
            slot.itemsToAdd,
            trEn("$slotLabel eklendi: ${slot.title}", "$slotLabel added: ${slot.title}")
        )
        _state.update { it.copy(addedMealSlotTypes = it.addedMealSlotTypes + slot.slot) }
        viewModelScope.launch {
            _effects.send(
                FoodEffect.ShowToast(
                    trEn(
                        "$slotLabel başarıyla günlüğe eklendi!",
                        "$slotLabel added to your log!"
                    )
                )
            )
        }
    }

    private fun addEntireDailyMealPlan() {
        val plan = _state.value.dailyMealPlan ?: return
        val allItems = plan.allSlots.map { it.itemsToAdd }.flatten()
        if (allItems.isEmpty()) return
        addMultipleItems(
            allItems,
            trEn("Günün 4 öğünlük planı günlüğe eklendi!", "Daily 4-meal plan added to your log!")
        )
        _state.update { it.copy(addedMealSlotTypes = MealSlotType.entries.toSet()) }
        viewModelScope.launch {
            _effects.send(
                FoodEffect.ShowToast(
                    trEn("Günün 4 öğünü günlüğe eklendi!", "All 4 daily meals added to your log!")
                )
            )
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

                cachedAllFoodLogs = allLogs
                recomputeRecent3DaysFoodNames(_state.value.selectedDate)

                _state.update { it.copy(frequentFoods = frequent) }
                updateSmartMealsAndSynergy()
            }
        }
    }

    private fun toggleFavorite(foodId: String) {
        val userId = tokenStorage.getUserId()
        viewModelScope.launch {
            val isNowFavorite = database.toggleFavoriteFood(userId, foodId)
            val msg = if (isNowFavorite) {
                trEn("Favorilere eklendi.", "Added to favorites.")
            } else {
                trEn("Favorilerden çıkarıldı.", "Removed from favorites.")
            }
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
            val label = if (newItem.isMix) {
                trEn("${newItem.name} karışımı envanterinize eklendi.", "${newItem.name} mix added to your inventory.")
            } else {
                trEn("${newItem.name} envanterinize eklendi.", "${newItem.name} added to your inventory.")
            }
            _effects.send(FoodEffect.ShowToast(label))
        }
    }

    private fun deleteCustomFood(id: String) {
        viewModelScope.launch {
            database.deleteCustomFood(id)
            _effects.send(FoodEffect.ShowToast(trEn("Özel ürün silindi.", "Custom item deleted.")))
        }
    }

    private fun analyzeCustomFoodWithAi(prompt: String, isMixMode: Boolean) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) {
            _state.update {
                it.copy(
                    customFoodAiError = trEn(
                        "Lütfen analiz edilecek ürün veya karışım içeriğini yazınız.",
                        "Please enter the product or mix details to analyze."
                    )
                )
            }
            return
        }

        val date = _state.value.selectedDate
        val lang = LocalizationManager.currentLanguage
        val structuredText = if (lang == AppLanguage.EN) {
            val ocrSanityRule = "IMPORTANT RULE: Scanned or entered text may have OCR decimal misses ('3.3g' -> '33g', '6.1g' -> '61g' or '610g', '0.5g' -> '50g'). Per 100g, the sum of macros can never exceed 100g and must align with total calories (4*P + 4*C + 9*F). Fix numbers that contradict calories as decimal shifts (e.g., 3.3, 6.1)."
            if (isMixMode) {
                "$cleanPrompt ($ocrSanityRule. This is a mix/meal recipe. Separate each ingredient as a distinct item. Set each item's formattedPortion clearly using the user's amount and unit, e.g., '150 ml', '2 shot', '1 scoop', '35 g', '1 pc'. Fill calories, protein, carbs, fat, fiber, and any vitamins/minerals/caffeine/l-carnitine micronutrients.)"
            } else {
                "$cleanPrompt ($ocrSanityRule. This is a single product. Analyze as a single item, set formattedPortion clearly with user's unit and weight/ml, e.g., '1 Scoop (35g)' or '1 Scoop (30ml)'. Include complete calories, protein, carbs, fat, fiber, and all vitamins, minerals, amino acids, caffeine, l-carnitine, creatine in the micronutrients list.)"
            }
        } else {
            val ocrSanityRule = "ÖNEMLİ KURAL: Taranan veya girilen metinde OCR kaynaklı nokta/virgül atlamaları olabilir ('3.3g' -> '33g', '6.1g' -> '61g' veya '610g', '0.5g' -> '50g'). 100g'da makroların toplamı 100g'ı asla geçemez ve toplam kalori (4*P + 4*K + 9*Y) ile uyumlu olmalıdır. Kaloriyle çelişen sayıları nokta kayması olarak düzeltip doğru ondalıklı değer yaz (örn: 3.3, 6.1)."
            if (isMixMode) {
                "$cleanPrompt ($ocrSanityRule. Bu bir karışım/öğün tarifidir. Lütfen içindeki her bileşeni ayrı bir kalem olarak ayır. Her kalemin formattedPortion alanına kullanıcının belirttiği miktarı ve birimini net yaz: örn '150 ml', '2 shot', '1 ölçek', '35 g', '1 adet'. Kalori, protein, karbonhidrat, yağ, lif ve varsa vitamin/mineral/kafein/l-karnitin gibi mikro besinlerini doldur.)"
            } else {
                "$cleanPrompt ($ocrSanityRule. Bu tek bir üründür. Lütfen tek bir kalem olarak analiz et, formattedPortion alanına kullanıcının belirttiği birimi ve gramaj/ml bilgisini net yaz: örn '1 Ölçek (35g)' veya '1 Ölçek (30ml)'. Kalori, protein, karbonhidrat, yağ, lif ve içindeki tüm vitamin, mineral, aminoasit, kafein, l-karnitin, kreatin gibi mikro besin/içerik değerlerini micronutrients listesine eksiksiz ekle.)"
            }
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
                refreshAiQuota()
            }.onFailure { err ->
                if (err is AiQuotaExceededException) {
                    _state.update {
                        it.copy(
                            isAnalyzingCustomFoodAi = false,
                            customFoodAiError = err.message,
                            aiQuota = err.quota ?: it.aiQuota
                        )
                    }
                    return@onFailure
                }
                val displayMsg = formatActionableApiError(
                    error = err,
                    operationTr = "Özel besin AI analizi",
                    operationEn = "Custom food AI analysis"
                )
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
            _state.update {
                it.copy(
                    errorMessage = trEn(
                        "Lütfen önce yediklerinizi serbest metin olarak yazınız.",
                        "Please describe what you ate first."
                    )
                )
            }
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
                refreshAiQuota()
            }.onFailure { err ->
                if (err is AiQuotaExceededException) {
                    _state.update {
                        it.copy(
                            isCalculatingAi = false,
                            errorMessage = err.message,
                            aiQuota = err.quota ?: it.aiQuota
                        )
                    }
                    return@onFailure
                }
                println("[FoodViewModel] analyzeFood failure: ${err.message ?: "Bilinmeyen hata"}")
                val displayMsg = formatActionableApiError(
                    error = err,
                    operationTr = "AI besin analizi",
                    operationEn = "AI food analysis"
                )

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
            try {
                val baseItems = existingLog?.items?.takeIf { it.isNotEmpty() } ?: _state.value.items
                val combinedItems = mergeDuplicateFoodItems(
                    (baseItems + aiResult.items).map { item ->
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

                val dayWeightInfo = resolveDayWeightAndProtein(date)
                _state.update {
                    it.copy(
                        currentFoodLog = newLog,
                        items = combinedItems,
                        totalCalories = totalCals,
                        summary = aiResult.summary,
                        nutrientBars = calculateBars(combinedItems, it.nutritionGoal, dayWeightInfo.targetProtein),
                        isAiReviewSheetOpen = false,
                        pendingAiResult = null,
                        rawText = "",
                        infoMessage = trEn(
                            "AI ile besin analizi onaylandı ve kaydedildi.",
                            "AI nutrition analysis approved and saved."
                        )
                    )
                }
                updateSmartMealsAndSynergy()
                launch { syncEngine.sync() }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isAiReviewSheetOpen = false,
                        pendingAiResult = null,
                        errorMessage = trEn(
                            "Besin kaydedilirken bir sorun oluştu: ${e.message}",
                            "Error saving food: ${e.message}"
                        )
                    )
                }
            }
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
                summary = existingLog?.summary ?: trEn("Kullanıcı girişi", "Manual entry"),
                totalCalories = totalCals,
                items = items,
                createdAt = existingLog?.createdAt ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
            database.upsertFood(updatedLog)
            _state.update { it.copy(rawText = "") }
            _effects.send(FoodEffect.ShowToast(trEn("Kayıt güncellendi.", "Log updated.")))
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
                        infoMessage = trEn("Günün tüm yemek kayıtları silindi.", "All food logs for the day were deleted.")
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
                    infoMessage = trEn("Kayıtlar temizlendi.", "Logs cleared.")
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
                summary = trEn("Günlük Öğün Kaydı", "Daily Meal Log"),
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
            val lang = LocalizationManager.currentLanguage
            val goalLabel = fitnessGoal.localizedTitle(lang)
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
                    infoMessage = trEn(
                        "Besin hedefleri ve $goalLabel hedefiniz güncellendi.",
                        "Nutrition targets and your $goalLabel goal have been updated."
                    )
                )
            }
            _effects.send(
                FoodEffect.ShowToast(
                    trEn(
                        "$goalLabel hedefine göre besin hedefleri güncellendi.",
                        "Nutrition targets updated for $goalLabel goal."
                    )
                )
            )
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

        // Micronutrient sums (match both Turkish and English names)
        fun sumMicronutrient(vararg keys: String): Double {
            return items.sumOf { item ->
                item.micronutrients
                    .filter { micro -> keys.any { key -> micro.name.contains(key, ignoreCase = true) } }
                    .sumOf { it.amount }
            }
        }

        val vitC = sumMicronutrient("C")
        val vitD = sumMicronutrient("D")
        val vitB12 = sumMicronutrient("B12")
        val iron = sumMicronutrient("Demir", "Iron")
        val calcium = sumMicronutrient("Kalsiyum", "Calcium")
        val magnesium = sumMicronutrient("Magnezyum", "Magnesium")

        val lang = LocalizationManager.currentLanguage
        return listOf(
            NutrientProgress("calories", trEn("Kalori", "Calories", lang), totalCalories, goal.calories.toDouble(), "kcal", "#FF5722"),
            NutrientProgress("protein", "Protein", totalProtein, proteinTargetG, "g", "#8E24AA"),
            NutrientProgress("carbs", trEn("Karbonhidrat", "Carbs", lang), totalCarbs, goal.carbsG, "g", "#FFA000"),
            NutrientProgress("fat", trEn("Yağ", "Fat", lang), totalFat, goal.fatG, "g", "#E53935"),
            NutrientProgress("fiber", trEn("Lif", "Fiber", lang), totalFiber, goal.fiberG, "g", "#43A047"),
            NutrientProgress("vit_c", "Vit C", vitC, goal.vitaminCMg, "mg", "#FB8C00"),
            NutrientProgress("vit_d", "Vit D", vitD, goal.vitaminDUg, "µg", "#00897B"),
            NutrientProgress("vit_b12", "Vit B12", vitB12, goal.vitaminB12Ug, "µg", "#3949AB"),
            NutrientProgress("iron", trEn("Demir", "Iron", lang), iron, goal.ironMg, "mg", "#D84315"),
            NutrientProgress("calcium", trEn("Kalsiyum", "Calcium", lang), calcium, goal.calciumMg, "mg", "#00ACC1"),
            NutrientProgress("magnesium", trEn("Magnezyum", "Magnesium", lang), magnesium, goal.magnesiumMg, "mg", "#7CB342")
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
                _state.update { it.copy(burnedCalories = totalBurned, dynamicWaterTargetMl = dynamicTarget) }
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
                _effects.send(
                    FoodEffect.ShowToast(
                        trEn(
                            "+${deltaMl}ml su eklendi (${updated.amountMl}/${target}ml)",
                            "+${deltaMl}ml water added (${updated.amountMl}/${target}ml)"
                        )
                    )
                )
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
            _effects.send(FoodEffect.ShowToast(trEn("Günlük su kaydı sıfırlandı.", "Daily water log reset.")))
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 1 & 2.B: SMART MEAL MATCHER & SYNERGY TIPS ENGINE
    // ─────────────────────────────────────────────────────────────

    private var cachedAiRecipe: SmartMealSuggestion? = null

    private fun updateSmartMealsAndSynergy() {
        val current = _state.value
        val lang = LocalizationManager.currentLanguage
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
            fromUserDbOnly = current.isSmartMealFromUserDb,
            language = lang
        )

        val combinedSuggestions = if (cachedAiRecipe != null) {
            listOf(cachedAiRecipe!!) + suggestions.filterNot { it.id == cachedAiRecipe!!.id }
        } else {
            suggestions
        }

        val tips = SmartMealEngine.detectSynergyTips(
            todayItems = current.items,
            nutrientBars = current.nutrientBars,
            waterCurrentMl = current.waterEntry?.amountMl ?: 0,
            language = lang
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
            formattedPortion = trEn(
                "1 porsiyon (${meal.ingredients.joinToString(", ")})",
                "1 serving (${meal.ingredients.joinToString(", ")})"
            ),
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
            _effects.send(
                FoodEffect.ShowToast(
                    trEn(
                        "\"${meal.title}\" günlüğe eklendi.",
                        "\"${meal.title}\" added to your log."
                    )
                )
            )
        }
    }

    private fun requestCustomAiMealSuggestion() {
        val current = _state.value
        val date = current.selectedDate
        val lang = LocalizationManager.currentLanguage
        val remainingCals = (current.nutritionGoal.calories - current.totalCalories).coerceAtLeast(0)
        val missingProtein = (current.effectiveProteinTarget - (current.nutrientBars.firstOrNull { it.key == "protein" }?.current ?: 0.0)).coerceAtLeast(0.0)

        val userDbItems = (current.customFoods.map { "${it.name} (${it.portionUnit})" } +
            current.frequentFoods.map { "${it.name} (${it.formattedPortion})" }).distinct().take(10)

        val prompt = buildString {
            if (lang == AppLanguage.EN) {
                append("For my remaining calories ($remainingCals kcal) and missing protein (${missingProtein.toInt()}g), ")
                if (current.isSmartMealOnlyQuick) append("taking under 10 minutes to prepare, ")
                if (current.isSmartMealOnlyFewIngredients) append("with at most 4 ingredients, ")
                if (current.isSmartMealExcludeRecent3Days && cachedRecent3DaysFoodNames.isNotEmpty()) {
                    append("excluding ingredients I ate in the last 3 days (${cachedRecent3DaysFoodNames.take(4).joinToString(", ")}), ")
                }
                if (userDbItems.isNotEmpty()) {
                    if (current.isSmartMealFromUserDb) {
                        append("especially using these items saved in the user's own pantry/inventory: [${userDbItems.joinToString(", ")}], ")
                    } else {
                        append("also considering these saved items from the user's inventory: [${userDbItems.joinToString(", ")}], ")
                    }
                }
                append("suggest a practical healthy meal rich in fiber and magnesium and calculate full serving nutrition values (calories, protein, carbs, fat, fiber, vitamins/minerals).")
            } else {
                append("Bana kalan kalori ($remainingCals kcal) ve eksik protein (${missingProtein.toInt()}g) için ")
                if (current.isSmartMealOnlyQuick) append("10 dakikadan kısa sürede hazırlanan, ")
                if (current.isSmartMealOnlyFewIngredients) append("en fazla 4 malzemeli, ")
                if (current.isSmartMealExcludeRecent3Days && cachedRecent3DaysFoodNames.isNotEmpty()) {
                    append("son 3 günde yediğim (${cachedRecent3DaysFoodNames.take(4).joinToString(", ")}) dışındaki malzemelerle ")
                }
                if (userDbItems.isNotEmpty()) {
                    if (current.isSmartMealFromUserDb) {
                        append("özellikle kullanıcının kendi kütüphanesinde/envanterinde kayıtlı şu ürünlerden yararlanarak: [${userDbItems.joinToString(", ")}], ")
                    } else {
                        append("kullanıcının envanterindeki şu kayıtlı ürünleri de göz önünde bulundurarak: [${userDbItems.joinToString(", ")}], ")
                    }
                }
                append("lif ve magnezyum zengini pratik bir sağlıklı öğün öner ve tam porsiyon besin değerlerini (kalori, protein, karbonhidrat, yağ, lif, vitamin/mineral) hesapla.")
            }
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
                        firstLine.length in 4..35 && !firstLine.startsWith("Kalan", ignoreCase = true) && !firstLine.startsWith("Remaining", ignoreCase = true) -> firstLine
                        else -> null
                    }
                    if (!candidate.isNullOrBlank() && candidate.length <= 35) {
                        candidate
                    } else if (aiResult.items.size >= 2) {
                        val topTwo = aiResult.items.take(2).joinToString(" & ") { it.name.trim() }
                        if (lang == AppLanguage.EN) "$topTwo Bowl" else "$topTwo Kasesi"
                    } else if (aiResult.items.isNotEmpty()) {
                        if (lang == AppLanguage.EN) "${aiResult.items.first().name} Plate" else "${aiResult.items.first().name} Tabağı"
                    } else {
                        trEn("Yapay Zeka Özel Öğünü", "AI Custom Meal", lang)
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
                    subtitle = trEn("✨ Gemini AI Tarafından Size Özel Üretildi", "✨ Custom Generated for You by Gemini AI", lang),
                    tags = listOf(
                        trEn("AI Özel Tarif", "AI Custom Recipe", lang),
                        trEn("Kişiselleştirilmiş", "Personalized", lang)
                    ) + if (aiResult.items.sumOf { it.proteinG } >= 20.0) listOf(trEn("Yüksek Protein", "High Protein", lang)) else emptyList(),
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
                        infoMessage = trEn(
                            "✨ \"$recipeTitle\" tarifi başarıyla hazırlandı!",
                            "✨ \"$recipeTitle\" recipe is ready!"
                        )
                    )
                }
                refreshAiQuota()
            }.onFailure { err ->
                val displayMsg = if (err is AiQuotaExceededException) {
                    err.message
                } else {
                    trEn(
                        "AI önerisi oluşturulamadı: ${err.message}",
                        "Could not generate AI suggestion: ${err.message}"
                    )
                }
                _state.update {
                    it.copy(
                        isGeneratingAiCustomRecipe = false,
                        errorMessage = displayMsg,
                        aiQuota = (err as? AiQuotaExceededException)?.quota ?: it.aiQuota
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2.D: PHOTO / LABEL SCANNER
    // ─────────────────────────────────────────────────────────────

    private fun analyzePhotoOrLabel(
        textOrPrompt: String,
        imageBase64: String? = null,
        target: FoodScanTarget = FoodScanTarget.DAILY_MEAL
    ) {
        val date = _state.value.selectedDate
        val lang = LocalizationManager.currentLanguage
        val structuredPrompt = if (lang == AppLanguage.EN) {
            val ocrDecimalFixInstruction =
                "ATTENTION - OCR DECIMAL & MATH SANITY CHECK:\n" +
                "1. On packaged food labels, OCR sometimes misses small decimal dots/commas ('3.3 g' -> '33' or '3 3'; '6.1 g' -> '61' or '610'; '0.5 g' -> '50'; '1.5 g' -> '15').\n" +
                "2. Physical Rule: Per 100g, protein + carbs + fat + fiber can NEVER exceed 100g.\n" +
                "3. Calorie Check: Total calories must roughly match (Protein*4 + Carbs*4 + Fat*9). Fix contradictory numbers back to their decimal value (e.g., 3.3, 6.1, 0.5)."

            when (target) {
                FoodScanTarget.INVENTORY -> {
                    if (!imageBase64.isNullOrBlank()) {
                        "Inventory analysis of product/nutrition label or grocery receipt in the image: $textOrPrompt.\n\n" +
                        "$ocrDecimalFixInstruction\n\n" +
                        "4. OUTPUT FORMAT: Extract each detected product with a clear name, standard serving/package unit (e.g., '100g' or '1 Pack (45g)'), calories, and complete macros/micronutrients for the user's inventory."
                    } else {
                        "Nutrition Label or Grocery Receipt Inventory Text Analysis: $textOrPrompt.\n\n" +
                        "$ocrDecimalFixInstruction\n\n" +
                        "4. OUTPUT FORMAT: Extract each detected product with a clear name, standard serving/package unit (e.g., '100g' or '1 Pack (45g)'), calories, and complete macros/micronutrients for the user's inventory."
                    }
                }
                FoodScanTarget.DAILY_MEAL -> {
                    if (!imageBase64.isNullOrBlank()) {
                        "Meal/plate or nutrition label analysis from the image: $textOrPrompt.\n\n" +
                        "$ocrDecimalFixInstruction\n\n" +
                        "4. OUTPUT FORMAT: Extract exact serving size, calories, protein, carbs, fat, fiber, and vitamin/mineral values."
                    } else {
                        "Nutrition Facts Label or Meal Text Analysis: $textOrPrompt.\n\n" +
                        "$ocrDecimalFixInstruction\n\n" +
                        "4. OUTPUT FORMAT: Extract exact serving size, calories, protein, carbs, fat, fiber, and any vitamins/minerals on the label."
                    }
                }
            }
        } else {
            val ocrDecimalFixInstruction =
                "DİKKAT - OCR NOKTA/VİRGÜL VE MATEMATİKSEL SAĞLAMA KONTROLÜ:\n" +
                "1. Paketli gıda tablolarında OCR bazen küçük nokta ve virgülleri kaçırarak boşluk veya sıfır gibi okuyabilir (örneğin: '3.3 g' veya '3,3 g' metinde '33' veya '3 3' olarak görünebilir; '6.1 g' metinde '61' veya '610' olarak görünebilir; '0.5 g' metinde '50' olarak görünebilir; '1.5 g' metinde '15' olarak görünebilir).\n" +
                "2. Fiziksel ve Biyolojik Gerçeklik Kuralı: 100 gramlık bir ürünün içindeki protein, karbonhidrat, yağ ve lif toplamı ASLA 100 gramı geçemez!\n" +
                "3. Kalori Sağlaması: Toplam kalori yaklaşık olarak (Protein*4 + Karbonhidrat*4 + Yağ*9) formülüyle tutarlı olmalıdır.\n" +
                "   - Örneğin: 45-60 kcal olan bir süt/yoğurt ürününde '33g protein' okunmuşsa o KESİNLİKLE '3.3g'dır.\n" +
                "   - '610g' veya '61g' karbonhidrat okunmuşsa o KESİNLİKLE '6.1g'dır.\n" +
                "   - '15g' yağ okunmuşsa o KESİNLİKLE '1.5g'dır.\n" +
                "Bu tür kalori veya porsiyonla çelişen sayıları kesinlikle doğru ondalıklı değere (örn: 3.3, 6.1, 0.5) dönüştür."

            when (target) {
                FoodScanTarget.INVENTORY -> {
                    if (!imageBase64.isNullOrBlank()) {
                        "Görseldeki ürün/besin etiketi veya market fişinin envanter analizi: $textOrPrompt.\n\n" +
                        "$ocrDecimalFixInstruction\n\n" +
                        "4. ÇIKTI FORMATI: Lütfen tespit edilen her bir ürünü kullanıcının kendi kütüphanesine/envanterine ekleyebileceği net ürün ismi, standart porsiyon/paket birimi (örn '100g' veya '1 Paket (45g)'), kalori ve makro/mikro besinleriyle tam olarak çıkar."
                    } else {
                        "Besin Değerleri Etiketi veya Market Fişi Envanter Metni Analizi: $textOrPrompt.\n\n" +
                        "$ocrDecimalFixInstruction\n\n" +
                        "4. ÇIKTI FORMATI: Lütfen tespit edilen her bir ürünü kullanıcının kendi kütüphanesine/envanterine ekleyebileceği net ürün ismi, standart porsiyon/paket birimi (örn '100g' veya '1 Paket (45g)'), kalori ve makro/mikro besinleriyle tam olarak çıkar."
                    }
                }
                FoodScanTarget.DAILY_MEAL -> {
                    if (!imageBase64.isNullOrBlank()) {
                        "Görseldeki yemek/tabak veya besin etiketi analizi: $textOrPrompt.\n\n" +
                        "$ocrDecimalFixInstruction\n\n" +
                        "4. ÇIKTI FORMATI: Lütfen yemeğin/ürünün porsiyonunu, kalori, protein, karbonhidrat, yağ, lif ve vitamin/mineral değerlerini tam olarak çıkar."
                    } else {
                        "Besin Değerleri Etiketi veya Tabak Metni Analizi: $textOrPrompt.\n\n" +
                        "$ocrDecimalFixInstruction\n\n" +
                        "4. ÇIKTI FORMATI: Lütfen ürünün veya tabağın porsiyonunu, kalori, protein, karbonhidrat, yağ, lif ve varsa etiket üzerindeki vitamin ve mineral değerlerini tam olarak çıkar."
                    }
                }
            }
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isPhotoScanAnalyzing = true,
                    isCalculatingAi = true,
                    photoScanTarget = target
                )
            }
            val result = apiClient.analyzeFood(structuredPrompt, date, imageBase64)
            result.onSuccess { aiResult ->
                _state.update {
                    it.copy(
                        isCalculatingAi = false,
                        isPhotoScanAnalyzing = false,
                        isPhotoScanSheetOpen = false,
                        pendingAiResult = aiResult,
                        isAiReviewSheetOpen = true
                    )
                }
                refreshAiQuota()
            }.onFailure { err ->
                val displayMsg = formatActionableApiError(
                    error = err,
                    operationTr = "Fotoğraf/etiket analizi",
                    operationEn = "Photo/label analysis"
                )
                _state.update {
                    it.copy(
                        isCalculatingAi = false,
                        isPhotoScanAnalyzing = false,
                        errorMessage = displayMsg,
                        aiQuota = (err as? AiQuotaExceededException)?.quota ?: it.aiQuota
                    )
                }
            }
        }
    }

    private fun saveAiItemsToInventory(items: List<FoodItem>) {
        val userId = tokenStorage.getUserId()
        val now = currentTime()
        viewModelScope.launch {
            try {
                items.forEach { item ->
                    val customId = "custom_" + Uuid.random().toString().take(12)
                    val customItem = CustomFoodItem(
                        id = customId,
                        userId = userId,
                        name = item.name.trim(),
                        brand = null,
                        portionUnit = item.formattedPortion.ifBlank { trEn("1 Porsiyon", "1 Serving") },
                        unitStep = 1,
                        calories = item.calories,
                        proteinG = item.proteinG,
                        carbsG = item.carbsG,
                        fatG = item.fatG,
                        fiberG = item.fiberG,
                        micronutrients = item.micronutrients,
                        createdAt = now,
                        updatedAt = now
                    )
                    database.upsertCustomFood(customItem)
                }
                _state.update {
                    it.copy(
                        isAiReviewSheetOpen = false,
                        pendingAiResult = null,
                        infoMessage = trEn(
                            "${items.size} ürün envanterinize kaydedildi! 🏷️",
                            "${items.size} items saved to your inventory! 🏷️"
                        )
                    )
                }
                _effects.send(
                    FoodEffect.ShowToast(
                        trEn(
                            "${items.size} ürün envanterinize başarıyla eklendi!",
                            "${items.size} items successfully added to your inventory!"
                        )
                    )
                )
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isAiReviewSheetOpen = false,
                        pendingAiResult = null,
                        errorMessage = trEn(
                            "Ürünler envantere kaydedilirken hata oluştu: ${e.message}",
                            "Error saving items to inventory: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    private fun saveSingleAiItemToInventory(item: FoodItem) {
        val userId = tokenStorage.getUserId()
        val now = currentTime()
        viewModelScope.launch {
            try {
                val customId = "custom_" + Uuid.random().toString().take(12)
                val customItem = CustomFoodItem(
                    id = customId,
                    userId = userId,
                    name = item.name.trim(),
                    brand = null,
                    portionUnit = item.formattedPortion.ifBlank { trEn("1 Porsiyon", "1 Serving") },
                    unitStep = 1,
                    calories = item.calories,
                    proteinG = item.proteinG,
                    carbsG = item.carbsG,
                    fatG = item.fatG,
                    fiberG = item.fiberG,
                    micronutrients = item.micronutrients,
                    createdAt = now,
                    updatedAt = now
                )
                database.upsertCustomFood(customItem)
                _effects.send(
                    FoodEffect.ShowToast(
                        trEn(
                            "${item.name} envanterinize eklendi! 🏷️",
                            "${item.name} added to your inventory! 🏷️"
                        )
                    )
                )
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        errorMessage = trEn(
                            "Ürün eklenirken hata: ${e.message}",
                            "Error adding item: ${e.message}"
                        )
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
                _effects.send(
                    FoodEffect.ShowToast(
                        trEn(
                            "Lütfen göndermek için en az bir malzeme seçin.",
                            "Please select at least one ingredient to send."
                        )
                    )
                )
            }
            return
        }
        val title = _state.value.groceryListTitle.ifBlank {
            trEn("Market Alışveriş Listesi", "Grocery Shopping List")
        }
        val success = appLauncher.sendToPlanoraTodo(title, itemsToSend)
        _state.update {
            it.copy(
                isGroceryListSheetOpen = false,
                isSmartMealSheetOpen = false
            )
        }
        viewModelScope.launch {
            if (success) {
                _effects.send(
                    FoodEffect.ShowToast(
                        trEn(
                            "${itemsToSend.size} malzeme Planora görev listesine aktarıldı.",
                            "${itemsToSend.size} ingredients sent to Planora to-do list."
                        )
                    )
                )
            } else {
                _effects.send(
                    FoodEffect.ShowToast(
                        trEn(
                            "Planora cihazınızda yüklü değil. Liste panoya kopyalandı.",
                            "Planora is not installed on your device. List copied to clipboard."
                        )
                    )
                )
            }
        }
    }
}
