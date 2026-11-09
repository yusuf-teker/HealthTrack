package com.yusufteker.healthtrack.feature.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.HealthReportAiGenerator
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.core.utils.getWeekBoundsIso
import com.yusufteker.healthtrack.core.utils.shiftDateIso
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AnalyticsViewModel(
    private val database: HealthDatabase,
    private val tokenStorage: TokenStorage,
    private val reportGenerator: HealthReportAiGenerator
) : ViewModel() {

    private val _state = MutableStateFlow(
        AnalyticsState(
            userName = tokenStorage.getName().ifBlank { tokenStorage.getUsername() },
            whatsAppPhone = tokenStorage.getWhatsAppNumber(),
            autoWeeklyWhatsApp = tokenStorage.isAutoWeeklyWhatsAppEnabled()
        )
    )
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    private val _effects = Channel<AnalyticsEffect>(Channel.BUFFERED)
    val effects: Flow<AnalyticsEffect> = _effects.receiveAsFlow()

    private var observationJob: Job? = null

    private var cachedFoods: List<FoodLog> = emptyList()
    private var cachedWorkouts: List<WorkoutLog> = emptyList()
    private var cachedWeights: List<WeightEntry> = emptyList()
    private var cachedGoal: NutritionGoal = NutritionGoal(id = "default", userId = 0)
    private var cachedReports: List<HealthAiReport> = emptyList()

    init {
        viewModelScope.launch {
            tokenStorage.sessionFlow.collect { session ->
                if (session.isLoggedIn && session.userId > 0) {
                    _state.update {
                        it.copy(
                            userName = session.name.ifBlank { session.username },
                            whatsAppPhone = tokenStorage.getWhatsAppNumber(),
                            autoWeeklyWhatsApp = tokenStorage.isAutoWeeklyWhatsAppEnabled()
                        )
                    }
                    observeAllTelemetry(session.userId)
                }
            }
        }
    }

    private fun observeAllTelemetry(userId: Int) {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            combine(
                database.observeAllFoods(userId),
                database.observeAllWorkouts(userId),
                database.observeWeights(userId),
                database.observeNutritionGoal(userId),
                database.observeAiReports(userId)
            ) { foods, workouts, weights, goal, reports ->
                TelemetryBundle(foods, workouts, weights, goal, reports)
            }.collect { bundle ->
                cachedFoods = bundle.foods
                cachedWorkouts = bundle.workouts
                cachedWeights = bundle.weights
                cachedGoal = bundle.goal
                cachedReports = bundle.reports

                refreshCurrentSelectionPreview()
                checkAndGenerateAutoWeeklyReportIfNeeded(userId)
            }
        }
    }

    private data class TelemetryBundle(
        val foods: List<FoodLog>,
        val workouts: List<WorkoutLog>,
        val weights: List<WeightEntry>,
        val goal: NutritionGoal,
        val reports: List<HealthAiReport>
    )

    fun onIntent(intent: AnalyticsIntent) {
        when (intent) {
            is AnalyticsIntent.OnPeriodTypeChanged -> {
                _state.update { it.copy(periodType = intent.periodType) }
                refreshCurrentSelectionPreview()
            }
            is AnalyticsIntent.OnDateSelected -> {
                val (mon, sun) = getWeekBoundsIso(intent.dateIso)
                _state.update {
                    it.copy(
                        selectedDate = intent.dateIso,
                        selectedWeekMonday = mon,
                        selectedWeekSunday = sun
                    )
                }
                refreshCurrentSelectionPreview()
            }
            is AnalyticsIntent.OnShiftDay -> {
                val nextDate = shiftDateIso(_state.value.selectedDate, intent.deltaDays)
                val (mon, sun) = getWeekBoundsIso(nextDate)
                _state.update {
                    it.copy(
                        selectedDate = nextDate,
                        selectedWeekMonday = mon,
                        selectedWeekSunday = sun
                    )
                }
                refreshCurrentSelectionPreview()
            }
            is AnalyticsIntent.OnShiftWeek -> {
                val nextMon = shiftDateIso(_state.value.selectedWeekMonday, intent.deltaWeeks * 7)
                val nextSun = shiftDateIso(nextMon, 6)
                _state.update {
                    it.copy(
                        selectedWeekMonday = nextMon,
                        selectedWeekSunday = nextSun,
                        selectedDate = nextMon
                    )
                }
                refreshCurrentSelectionPreview()
            }
            is AnalyticsIntent.OnSelectWeekByDate -> {
                val (mon, sun) = getWeekBoundsIso(intent.dateInWeekIso)
                _state.update {
                    it.copy(
                        selectedWeekMonday = mon,
                        selectedWeekSunday = sun
                    )
                }
                refreshCurrentSelectionPreview()
            }
            AnalyticsIntent.OnGenerateReportClicked -> generateReportForSelection()
            is AnalyticsIntent.OnSelectSavedReport -> {
                val report = intent.report
                val (mon, sun) = if (report.periodType == AnalysisPeriodType.WEEKLY) {
                    report.startDate to report.endDate
                } else {
                    getWeekBoundsIso(report.startDate)
                }
                _state.update {
                    it.copy(
                        periodType = report.periodType,
                        selectedDate = report.startDate,
                        selectedWeekMonday = mon,
                        selectedWeekSunday = sun,
                        currentReport = report
                    )
                }
                refreshCurrentSelectionPreview()
            }
            is AnalyticsIntent.OnDeleteReport -> {
                viewModelScope.launch {
                    database.deleteAiReport(intent.reportId)
                    _state.update { it.copy(infoMessage = "Rapor silindi.") }
                }
            }
            is AnalyticsIntent.OnWhatsAppPhoneChanged -> {
                _state.update { it.copy(whatsAppPhone = intent.phone) }
            }
            is AnalyticsIntent.OnAutoWeeklyWhatsAppToggled -> {
                tokenStorage.setAutoWeeklyWhatsAppEnabled(intent.enabled)
                _state.update {
                    it.copy(
                        autoWeeklyWhatsApp = intent.enabled,
                        infoMessage = if (intent.enabled) {
                            "Otomatik haftalık rapor oluşturma aktif edildi."
                        } else {
                            "Otomatik haftalık rapor kapatıldı."
                        }
                    )
                }
            }
            AnalyticsIntent.OnSaveWhatsAppSettingsClicked -> {
                val phone = _state.value.whatsAppPhone.trim()
                tokenStorage.setWhatsAppNumber(phone)
                _state.update {
                    it.copy(
                        infoMessage = if (phone.isNotBlank()) {
                            "WhatsApp rapor numarası ($phone) kaydedildi."
                        } else {
                            "WhatsApp kişi seçici modu aktif edildi."
                        }
                    )
                }
            }
            is AnalyticsIntent.OnReportSentToWhatsApp -> {
                viewModelScope.launch {
                    database.markReportSentToWhatsApp(intent.reportId)
                    _state.update { current ->
                        current.copy(
                            pendingAutoWeeklyReport = current.pendingAutoWeeklyReport?.takeIf { it.id != intent.reportId }
                        )
                    }
                }
            }
            AnalyticsIntent.OnDismissPendingAutoReport -> {
                _state.update { it.copy(pendingAutoWeeklyReport = null) }
            }
            AnalyticsIntent.OnDismissInfoMessage -> _state.update { it.copy(infoMessage = null) }
            AnalyticsIntent.OnDismissErrorMessage -> _state.update { it.copy(errorMessage = null) }
            is AnalyticsIntent.OnShowInfoMessage -> _state.update { it.copy(infoMessage = intent.message) }
        }
    }

    private fun refreshCurrentSelectionPreview() {
        val s = _state.value
        val fitnessGoal = tokenStorage.getUserFitnessGoal()
        val fallbackWeight = tokenStorage.getUserWeight().takeIf { it > 0.0 } ?: 72.0

        if (s.periodType == AnalysisPeriodType.DAILY) {
            val dayFoods = cachedFoods.filter { it.date == s.selectedDate && !it.isDeleted }
            val dayWorkouts = cachedWorkouts.filter { it.date == s.selectedDate && !it.isDeleted }
            val dayWeight = cachedWeights.filter { !it.isDeleted && it.date <= s.selectedDate }
                .maxByOrNull { it.date }?.weightKg ?: fallbackWeight

            val calIn = dayFoods.sumOf { it.totalCalories }
            val calBurn = dayWorkouts.sumOf { it.caloriesBurned }
            val workMin = dayWorkouts.sumOf { it.durationMinutes }
            val prot = dayFoods.flatMap { it.items }.sumOf { it.proteinG }.roundToInt()
            val targetProt = calculateProteinTarget(dayWeight, fitnessGoal).roundToInt()

            val matchingReport = cachedReports.firstOrNull {
                it.periodType == AnalysisPeriodType.DAILY && it.startDate == s.selectedDate
            }

            _state.update {
                it.copy(
                    currentReport = matchingReport,
                    recentReports = cachedReports,
                    previewCaloriesIntake = calIn,
                    previewTargetCalories = cachedGoal.calories.coerceAtLeast(1200),
                    previewCaloriesBurned = calBurn,
                    previewWorkoutMinutes = workMin,
                    previewProteinG = prot,
                    previewTargetProteinG = targetProt,
                    previewWeightKg = dayWeight,
                    previewLoggedDays = if (dayFoods.isNotEmpty() || dayWorkouts.isNotEmpty()) 1 else 0
                )
            }
        } else {
            val mon = s.selectedWeekMonday
            val sun = s.selectedWeekSunday
            val weekFoods = cachedFoods.filter { !it.isDeleted && it.date in mon..sun }
            val weekWorkouts = cachedWorkouts.filter { !it.isDeleted && it.date in mon..sun }
            val loggedDays = weekFoods.filter { it.totalCalories > 0 }.map { it.date }.distinct().size
            val divisor = loggedDays.coerceAtLeast(1)

            val avgCalIn = (weekFoods.sumOf { it.totalCalories }.toDouble() / divisor).roundToInt()
            val totalBurn = weekWorkouts.sumOf { it.caloriesBurned }
            val totalMin = weekWorkouts.sumOf { it.durationMinutes }
            val avgProt = (weekFoods.flatMap { it.items }.sumOf { it.proteinG } / divisor).roundToInt()
            val endWeight = cachedWeights.filter { !it.isDeleted && it.date <= sun }
                .maxByOrNull { it.date }?.weightKg ?: fallbackWeight
            val targetProt = calculateProteinTarget(endWeight, fitnessGoal).roundToInt()

            val matchingReport = cachedReports.firstOrNull {
                it.periodType == AnalysisPeriodType.WEEKLY && it.startDate == mon
            }

            _state.update {
                it.copy(
                    currentReport = matchingReport,
                    recentReports = cachedReports,
                    previewCaloriesIntake = avgCalIn,
                    previewTargetCalories = cachedGoal.calories.coerceAtLeast(1200),
                    previewCaloriesBurned = totalBurn,
                    previewWorkoutMinutes = totalMin,
                    previewProteinG = avgProt,
                    previewTargetProteinG = targetProt,
                    previewWeightKg = endWeight,
                    previewLoggedDays = loggedDays
                )
            }
        }
    }

    private fun generateReportForSelection() {
        val s = _state.value
        val userId = tokenStorage.getUserId().takeIf { it > 0 } ?: 1
        val fitnessGoal = tokenStorage.getUserFitnessGoal()
        val userWeight = tokenStorage.getUserWeight().takeIf { it > 0.0 } ?: 72.0
        val targetWeight = tokenStorage.getUserTargetWeight().takeIf { it > 0.0 } ?: 70.0

        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true, errorMessage = null, infoMessage = null) }
            try {
                val report = if (s.periodType == AnalysisPeriodType.DAILY) {
                    reportGenerator.generateDailyReport(
                        userId = userId,
                        dateIso = s.selectedDate,
                        foodLogs = cachedFoods,
                        workoutLogs = cachedWorkouts,
                        weights = cachedWeights,
                        goal = cachedGoal,
                        fitnessGoal = fitnessGoal,
                        userWeightKg = userWeight,
                        targetWeightKg = targetWeight
                    )
                } else {
                    reportGenerator.generateWeeklyReport(
                        userId = userId,
                        mondayIso = s.selectedWeekMonday,
                        sundayIso = s.selectedWeekSunday,
                        foodLogs = cachedFoods,
                        workoutLogs = cachedWorkouts,
                        weights = cachedWeights,
                        goal = cachedGoal,
                        fitnessGoal = fitnessGoal,
                        userWeightKg = userWeight,
                        targetWeightKg = targetWeight,
                        isAutoGenerated = false
                    )
                }

                database.upsertAiReport(report)
                _state.update {
                    it.copy(
                        isGenerating = false,
                        currentReport = report,
                        infoMessage = "${report.periodLabel} AI analizi oluşturuldu (Skor: ${report.score}/100)."
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = "Analiz sırasında hata: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Automatically generates the completed week's report at the end of the week if
     * [TokenStorage.isAutoWeeklyWhatsAppEnabled] is true and a report doesn't exist yet.
     */
    private suspend fun checkAndGenerateAutoWeeklyReportIfNeeded(userId: Int) {
        if (!tokenStorage.isAutoWeeklyWhatsAppEnabled()) return
        val today = getTodayIso()
        val (currentMonday, _) = getWeekBoundsIso(today)
        val prevWeekMonday = shiftDateIso(currentMonday, -7)
        val prevWeekSunday = shiftDateIso(currentMonday, -1)

        // Check if previous completed week already has a saved report
        val existingPrevWeekReport = cachedReports.firstOrNull {
            it.periodType == AnalysisPeriodType.WEEKLY && it.startDate == prevWeekMonday
        }

        if (existingPrevWeekReport != null) {
            if (existingPrevWeekReport.isAutoGenerated && !existingPrevWeekReport.isSentToWhatsApp) {
                _state.update { it.copy(pendingAutoWeeklyReport = existingPrevWeekReport) }
            }
            return
        }

        // Only auto-generate if the user actually logged at least one food or workout during the previous week
        val hasActivityInPrevWeek = cachedFoods.any { !it.isDeleted && it.date in prevWeekMonday..prevWeekSunday && it.totalCalories > 0 } ||
            cachedWorkouts.any { !it.isDeleted && it.date in prevWeekMonday..prevWeekSunday }

        if (!hasActivityInPrevWeek) return
        if (tokenStorage.getLastAutoWeeklyReportMonday() == prevWeekMonday) return

        val fitnessGoal = tokenStorage.getUserFitnessGoal()
        val userWeight = tokenStorage.getUserWeight().takeIf { it > 0.0 } ?: 72.0
        val targetWeight = tokenStorage.getUserTargetWeight().takeIf { it > 0.0 } ?: 70.0

        val autoReport = reportGenerator.generateWeeklyReport(
            userId = userId,
            mondayIso = prevWeekMonday,
            sundayIso = prevWeekSunday,
            foodLogs = cachedFoods,
            workoutLogs = cachedWorkouts,
            weights = cachedWeights,
            goal = cachedGoal,
            fitnessGoal = fitnessGoal,
            userWeightKg = userWeight,
            targetWeightKg = targetWeight,
            isAutoGenerated = true
        )

        tokenStorage.setLastAutoWeeklyReportMonday(prevWeekMonday)
        database.upsertAiReport(autoReport)
        _state.update {
            it.copy(
                pendingAutoWeeklyReport = autoReport,
                infoMessage = "Geçen haftanın (${autoReport.periodLabel}) AI raporu otomatik oluşturuldu!"
            )
        }
    }
}

