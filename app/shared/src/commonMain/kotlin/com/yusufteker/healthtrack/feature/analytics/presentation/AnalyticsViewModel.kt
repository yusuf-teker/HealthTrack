package com.yusufteker.healthtrack.feature.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.network.formatActionableApiError
import com.yusufteker.healthtrack.core.sync.HealthReportAiGenerator
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.core.utils.getWeekBoundsIso
import com.yusufteker.healthtrack.core.utils.shiftDateIso
import kotlinx.coroutines.CancellationException
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
    private var cachedWaters: List<WaterEntry> = emptyList()
    private var cachedGoal: NutritionGoal = NutritionGoal(id = "default", userId = 0)
    private var cachedReports: List<HealthAiReport> = emptyList()

    init {
        viewModelScope.launch {
            tokenStorage.sessionFlow
                .distinctUntilChangedBy { it.hasActiveSession to it.userId }
                .collect { session ->
                    if (session.hasActiveSession && session.userId != 0) {
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
                TelemetryBundle(foods, workouts, weights, emptyList(), goal, reports)
            }.combine(database.observeAllWater(userId)) { bundle, waters ->
                bundle.copy(waters = waters)
            }.collect { bundle ->
                cachedFoods = bundle.foods
                cachedWorkouts = bundle.workouts
                cachedWeights = bundle.weights
                cachedWaters = bundle.waters
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
        val waters: List<WaterEntry>,
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
                    _state.update { it.copy(infoMessage = trEn("Rapor silindi.", "Report deleted.")) }
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
                            trEn(
                                "Otomatik haftalık rapor oluşturma aktif edildi.",
                                "Automatic weekly report generation enabled."
                            )
                        } else {
                            trEn(
                                "Otomatik haftalık rapor kapatıldı.",
                                "Automatic weekly report disabled."
                            )
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
                            trEn(
                                "WhatsApp rapor numarası ($phone) kaydedildi.",
                                "WhatsApp report number ($phone) saved."
                            )
                        } else {
                            trEn(
                                "WhatsApp kişi seçici modu aktif edildi.",
                                "WhatsApp contact picker mode enabled."
                            )
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
        val targetCalories = cachedGoal.calories.coerceAtLeast(1200)
        val targetCarbsG = cachedGoal.carbsG.roundToInt().coerceAtLeast(60)
        val targetFatG = cachedGoal.fatG.roundToInt().coerceAtLeast(30)
        val targetFiberG = cachedGoal.fiberG.roundToInt().coerceAtLeast(25)

        val dayLabelsTr = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
        val dayLabelsEn = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val weekBars = (0 until 7).map { offset ->
            val dateIso = shiftDateIso(s.selectedWeekMonday, offset)
            val fDay = cachedFoods.filter { !it.isDeleted && it.date == dateIso }
            val wDay = cachedWorkouts.filter { !it.isDeleted && it.date == dateIso }
            val explicitW = wDay.filter { !it.isDailySteps }
            val stepW = wDay.firstOrNull { it.isDailySteps }
            val burned = WorkoutTelemetryHelper.calculateEffectiveDailyBurn(explicitW, stepW)
            AnalyticsDayBar(
                dateIso = dateIso,
                shortLabelTr = dayLabelsTr[offset],
                shortLabelEn = dayLabelsEn[offset],
                intakeKcal = fDay.sumOf { it.totalCalories },
                burnedKcal = burned,
                targetKcal = targetCalories,
                isSelected = s.periodType == AnalysisPeriodType.DAILY && dateIso == s.selectedDate
            )
        }

        if (s.periodType == AnalysisPeriodType.DAILY) {
            val dayFoods = cachedFoods.filter { it.date == s.selectedDate && !it.isDeleted }
            val dayWorkouts = cachedWorkouts.filter { it.date == s.selectedDate && !it.isDeleted }
            val explicitWorkouts = dayWorkouts.filter { !it.isDailySteps }
            val stepLog = dayWorkouts.firstOrNull { it.isDailySteps }
            val calBurn = WorkoutTelemetryHelper.calculateEffectiveDailyBurn(explicitWorkouts, stepLog)
            val activeWeightsBeforeOrOnDay = cachedWeights
                .filter { !it.isDeleted && it.date <= s.selectedDate && it.weightKg > 0.0 }
                .sortedBy { it.date }
            val dayWeight = activeWeightsBeforeOrOnDay.lastOrNull()?.weightKg ?: fallbackWeight
            val prevWeight = activeWeightsBeforeOrOnDay.dropLast(1).lastOrNull()?.weightKg
            val weightDelta = prevWeight?.let { ((dayWeight - it) * 10.0).roundToInt() / 10.0 }

            val allItems = dayFoods.flatMap { it.items }
            val calIn = dayFoods.sumOf { it.totalCalories }
            val workMin = explicitWorkouts.sumOf { it.durationMinutes }
            val prot = allItems.sumOf { it.proteinG }.roundToInt()
            val carbs = allItems.sumOf { it.carbsG }.roundToInt()
            val fat = allItems.sumOf { it.fatG }.roundToInt()
            val fiber = allItems.sumOf { it.fiberG }.roundToInt()
            val targetProt = calculateProteinTarget(dayWeight, fitnessGoal).roundToInt().coerceAtLeast(40)

            val dayWater = cachedWaters.firstOrNull { it.date == s.selectedDate }
            val waterMl = dayWater?.amountMl ?: 0
            val waterTarget = (dayWater?.targetMl ?: 2500).coerceAtLeast(1500)

            val matchingReport = cachedReports.firstOrNull {
                it.periodType == AnalysisPeriodType.DAILY && it.startDate == s.selectedDate
            }

            val insights = buildDeterministicInsights(
                isDaily = true,
                calIntake = calIn,
                targetCalories = targetCalories,
                calBurned = calBurn,
                proteinG = prot,
                targetProteinG = targetProt,
                waterMl = waterMl,
                waterTargetMl = waterTarget,
                loggedDays = if (dayFoods.isNotEmpty() || explicitWorkouts.isNotEmpty()) 1 else 0,
                workoutCount = explicitWorkouts.size
            )

            _state.update {
                it.copy(
                    currentReport = matchingReport,
                    recentReports = cachedReports,
                    previewCaloriesIntake = calIn,
                    previewTargetCalories = targetCalories,
                    previewCaloriesBurned = calBurn,
                    previewWorkoutMinutes = workMin,
                    previewProteinG = prot,
                    previewTargetProteinG = targetProt,
                    previewCarbsG = carbs,
                    previewTargetCarbsG = targetCarbsG,
                    previewFatG = fat,
                    previewTargetFatG = targetFatG,
                    previewFiberG = fiber,
                    previewTargetFiberG = targetFiberG,
                    previewWaterMl = waterMl,
                    previewWaterTargetMl = waterTarget,
                    previewWeightKg = dayWeight,
                    previewWeightDeltaKg = weightDelta,
                    previewLoggedDays = if (dayFoods.isNotEmpty() || dayWorkouts.isNotEmpty()) 1 else 0,
                    weeklyDailyBars = weekBars,
                    deterministicInsights = insights
                )
            }
        } else {
            val mon = s.selectedWeekMonday
            val sun = s.selectedWeekSunday
            val weekFoods = cachedFoods.filter { !it.isDeleted && it.date in mon..sun }
            val weekWorkouts = cachedWorkouts.filter { !it.isDeleted && it.date in mon..sun }
            val explicitWeekWorkouts = weekWorkouts.filter { !it.isDailySteps }
            val loggedDays = weekFoods.filter { it.totalCalories > 0 }.map { it.date }.distinct().size
            val divisor = loggedDays.coerceAtLeast(1)

            val allItems = weekFoods.flatMap { it.items }
            val avgCalIn = (weekFoods.sumOf { it.totalCalories }.toDouble() / divisor).roundToInt()
            val totalBurn = weekBars.sumOf { it.burnedKcal }
            val totalMin = explicitWeekWorkouts.sumOf { it.durationMinutes }
            val avgProt = (allItems.sumOf { it.proteinG } / divisor).roundToInt()
            val avgCarbs = (allItems.sumOf { it.carbsG } / divisor).roundToInt()
            val avgFat = (allItems.sumOf { it.fatG } / divisor).roundToInt()
            val avgFiber = (allItems.sumOf { it.fiberG } / divisor).roundToInt()

            val weekWeights = cachedWeights
                .filter { !it.isDeleted && it.date in mon..sun && it.weightKg > 0.0 }
                .sortedBy { it.date }
            val endWeight = cachedWeights.filter { !it.isDeleted && it.date <= sun && it.weightKg > 0.0 }
                .maxByOrNull { it.date }?.weightKg ?: fallbackWeight
            val weekDelta = if (weekWeights.size >= 2) {
                ((weekWeights.last().weightKg - weekWeights.first().weightKg) * 10.0).roundToInt() / 10.0
            } else null
            val targetProt = calculateProteinTarget(endWeight, fitnessGoal).roundToInt().coerceAtLeast(40)

            val weekWaters = cachedWaters.filter { it.date in mon..sun && it.amountMl > 0 }
            val avgWaterMl = if (weekWaters.isNotEmpty()) {
                (weekWaters.sumOf { it.amountMl }.toDouble() / weekWaters.size).roundToInt()
            } else 0
            val avgWaterTarget = (weekWaters.lastOrNull()?.targetMl ?: 2500).coerceAtLeast(1500)

            val matchingReport = cachedReports.firstOrNull {
                it.periodType == AnalysisPeriodType.WEEKLY && it.startDate == mon
            }

            val insights = buildDeterministicInsights(
                isDaily = false,
                calIntake = avgCalIn,
                targetCalories = targetCalories,
                calBurned = totalBurn / 7,
                proteinG = avgProt,
                targetProteinG = targetProt,
                waterMl = avgWaterMl,
                waterTargetMl = avgWaterTarget,
                loggedDays = loggedDays,
                workoutCount = explicitWeekWorkouts.size
            )

            _state.update {
                it.copy(
                    currentReport = matchingReport,
                    recentReports = cachedReports,
                    previewCaloriesIntake = avgCalIn,
                    previewTargetCalories = targetCalories,
                    previewCaloriesBurned = totalBurn,
                    previewWorkoutMinutes = totalMin,
                    previewProteinG = avgProt,
                    previewTargetProteinG = targetProt,
                    previewCarbsG = avgCarbs,
                    previewTargetCarbsG = targetCarbsG,
                    previewFatG = avgFat,
                    previewTargetFatG = targetFatG,
                    previewFiberG = avgFiber,
                    previewTargetFiberG = targetFiberG,
                    previewWaterMl = avgWaterMl,
                    previewWaterTargetMl = avgWaterTarget,
                    previewWeightKg = endWeight,
                    previewWeightDeltaKg = weekDelta,
                    previewLoggedDays = loggedDays,
                    weeklyDailyBars = weekBars,
                    deterministicInsights = insights
                )
            }
        }
    }

    private fun buildDeterministicInsights(
        isDaily: Boolean,
        calIntake: Int,
        targetCalories: Int,
        calBurned: Int,
        proteinG: Int,
        targetProteinG: Int,
        waterMl: Int,
        waterTargetMl: Int,
        loggedDays: Int,
        workoutCount: Int
    ): List<AnalyticsInstantInsight> {
        if (loggedDays == 0 && workoutCount == 0 && waterMl == 0) {
            return listOf(
                AnalyticsInstantInsight(
                    titleTr = "Henüz bu dönem için veri yok",
                    titleEn = "No telemetry yet for this period",
                    detailTr = "Öğün, su veya antrenman ekledikçe AI kullanmadan anlık grafik ve içgörüler burada oluşur.",
                    detailEn = "Log a meal, water, or workout to see live charts and instant rule-based insights without AI.",
                    tone = InsightTone.INFO
                )
            )
        }

        val list = mutableListOf<AnalyticsInstantInsight>()
        val netCalories = calIntake - calBurned
        val calRatio = if (targetCalories > 0) calIntake.toDouble() / targetCalories else 0.0

        when {
            calIntake == 0 -> list.add(
                AnalyticsInstantInsight(
                    titleTr = "Kalori girişi bekleniyor",
                    titleEn = "Waiting for meal intake",
                    detailTr = "Günlük net enerji dengesini hesaplamak için öğün ekleyebilirsiniz.",
                    detailEn = "Log meals to compute your net calorie balance.",
                    tone = InsightTone.INFO
                )
            )
            calRatio in 0.85..1.12 -> list.add(
                AnalyticsInstantInsight(
                    titleTr = "Kalori hedefi dengede ✓",
                    titleEn = "Calorie intake on target ✓",
                    detailTr = "${calIntake} / ${targetCalories} kcal (Net: $netCalories kcal). Sürdürülebilir aralıktasın.",
                    detailEn = "${calIntake} / ${targetCalories} kcal (Net: $netCalories kcal). Right inside your sweet spot.",
                    tone = InsightTone.POSITIVE
                )
            )
            calRatio < 0.85 -> list.add(
                AnalyticsInstantInsight(
                    titleTr = "Hedefin altında alım",
                    titleEn = "Below calorie target",
                    detailTr = "Alım ($calIntake kcal) hedefin %${((1.0 - calRatio) * 100).roundToInt()} altında. Kas koruması için dengeli ara öğün değerlendirilebilir.",
                    detailEn = "Intake ($calIntake kcal) is ${((1.0 - calRatio) * 100).roundToInt()}% under target. Consider a balanced snack to support recovery.",
                    tone = InsightTone.ATTENTION
                )
            )
            else -> list.add(
                AnalyticsInstantInsight(
                    titleTr = "Kalori hedefi aşıldı",
                    titleEn = "Above calorie target",
                    detailTr = "Alım hedefin ${calIntake - targetCalories} kcal üzerinde; aktif yürüyüş veya hafif kardiyo net dengeyi yumuşatabilir.",
                    detailEn = "${calIntake - targetCalories} kcal over target; brisk walking or light cardio can smooth your net balance.",
                    tone = InsightTone.ATTENTION
                )
            )
        }

        val protRatio = if (targetProteinG > 0) proteinG.toDouble() / targetProteinG else 0.0
        if (proteinG > 0 || calIntake > 0) {
            if (protRatio >= 0.85) {
                list.add(
                    AnalyticsInstantInsight(
                        titleTr = "Protein uyumu güçlü (%${(protRatio * 100).roundToInt()})",
                        titleEn = "Strong protein compliance (${(protRatio * 100).roundToInt()}%)",
                        detailTr = "${proteinG}g / ${targetProteinG}g protein ile toparlanma ve tokluk desteği iyi.",
                        detailEn = "${proteinG}g / ${targetProteinG}g protein supports satiety and muscle recovery.",
                        tone = InsightTone.POSITIVE
                    )
                )
            } else {
                val missingP = (targetProteinG - proteinG).coerceAtLeast(10)
                list.add(
                    AnalyticsInstantInsight(
                        titleTr = "Protein açığı: +${missingP}g önerilir",
                        titleEn = "Protein gap: +${missingP}g recommended",
                        detailTr = "Hedef ${targetProteinG}g iken ${proteinG}g alındı. Yoğurt, yumurta, lor veya tavuk ekleyebilirsin.",
                        detailEn = "Currently ${proteinG}g vs ${targetProteinG}g goal. Eggs, Greek yogurt, or lean meat can close the gap.",
                        tone = InsightTone.ATTENTION
                    )
                )
            }
        }

        val waterPct = if (waterTargetMl > 0) (waterMl * 100) / waterTargetMl else 0
        if (!isDaily) {
            list.add(
                AnalyticsInstantInsight(
                    titleTr = "Haftalık hareket: $workoutCount seans",
                    titleEn = "Weekly activity: $workoutCount sessions",
                    detailTr = "Ort. günlük su alımı: ${waterMl}ml (%$waterPct). Haftalık aktif gün: $loggedDays/7.",
                    detailEn = "Avg hydration: ${waterMl}ml ($waterPct%). Active nutrition days: $loggedDays/7.",
                    tone = if (workoutCount >= 3 && waterPct >= 75) InsightTone.POSITIVE else InsightTone.INFO
                )
            )
        } else if (waterMl > 0 || workoutCount > 0) {
            list.add(
                AnalyticsInstantInsight(
                    titleTr = "Hidrasyon %$waterPct • Aktivite $workoutCount seans",
                    titleEn = "Hydration $waterPct% • $workoutCount workout(s)",
                    detailTr = "${waterMl} / ${waterTargetMl} ml su ve toplam $calBurned kcal hareket yakımı kaydedildi.",
                    detailEn = "${waterMl} / ${waterTargetMl} ml water and $calBurned kcal active burn recorded.",
                    tone = if (waterPct >= 80) InsightTone.POSITIVE else InsightTone.INFO
                )
            )
        }

        return list.take(3)
    }

    private fun generateReportForSelection() {
        val s = _state.value
        val userId = tokenStorage.getUserId().takeIf { it != 0 } ?: 1
        val fitnessGoal = tokenStorage.getUserFitnessGoal()
        val userWeight = tokenStorage.getUserWeight().takeIf { it > 0.0 } ?: 72.0
        val targetWeight = tokenStorage.getUserTargetWeight().takeIf { it > 0.0 } ?: 70.0

        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true, errorMessage = null, infoMessage = null) }
            try {
                val lang = LocalizationManager.currentLanguage
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
                        targetWeightKg = targetWeight,
                        language = lang
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
                        isAutoGenerated = false,
                        language = lang
                    )
                }

                database.upsertAiReport(report)
                _state.update {
                    it.copy(
                        isGenerating = false,
                        currentReport = report,
                        infoMessage = trEn(
                            "${report.periodLabel} AI analizi oluşturuldu (Skor: ${report.score}/100).",
                            "${report.periodLabel} AI analysis created (Score: ${report.score}/100)."
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = formatActionableApiError(
                            error = e,
                            operationTr = "Sağlık raporu analizi",
                            operationEn = "Health report analysis"
                        )
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

        val lang = LocalizationManager.currentLanguage
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
            isAutoGenerated = true,
            language = lang
        )

        tokenStorage.setLastAutoWeeklyReportMonday(prevWeekMonday)
        database.upsertAiReport(autoReport)
        _state.update {
            it.copy(
                pendingAutoWeeklyReport = autoReport,
                infoMessage = trEn(
                    "Geçen haftanın (${autoReport.periodLabel}) AI raporu otomatik oluşturuldu!",
                    "Last week's (${autoReport.periodLabel}) AI report was automatically generated!"
                )
            )
        }
    }
}

