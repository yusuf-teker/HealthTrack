package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.FoodLog
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.core.model.WaterEntry
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.model.WorkoutTelemetryHelper
import com.yusufteker.healthtrack.core.model.calculateProteinTarget
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.utils.formatWeekRangeTr
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.core.utils.getWeekBoundsIso
import com.yusufteker.healthtrack.core.utils.shiftDateIso
import kotlin.math.abs
import kotlin.math.roundToInt

data class WeeklyDaySummaryBar(
    val dateIso: String,
    val shortLabelTr: String,
    val shortLabelEn: String,
    val intakeKcal: Int,
    val burnedKcal: Int,
    val hasActivity: Boolean
) {
    fun localizedLabel(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(shortLabelTr, shortLabelEn, language)
}

data class WeeklySummarySnapshot(
    val weekMondayIso: String,
    val weekSundayIso: String,
    val periodLabelTr: String,
    val periodLabelEn: String,
    val activeDays: Int,
    val foodLoggedDays: Int,
    val avgDailyCalories: Int,
    val targetCalories: Int,
    val avgDailyProteinG: Int,
    val targetProteinG: Int,
    val totalBurnedCalories: Int,
    val workoutSessions: Int,
    val totalWorkoutMinutes: Int,
    val avgDailyWaterMl: Int,
    val waterTargetMl: Int,
    val weightStartKg: Double?,
    val weightEndKg: Double?,
    val weightDeltaKg: Double?,
    val consistencyScore: Int,
    val currentStreakDays: Int,
    val dailyBars: List<WeeklyDaySummaryBar>,
    val highlightTr: String,
    val highlightEn: String,
    val nextWeekTipTr: String,
    val nextWeekTipEn: String,
    val hasEnoughData: Boolean
) {
    fun localizedPeriodLabel(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(periodLabelTr, periodLabelEn, language)

    fun localizedHighlight(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(highlightTr, highlightEn, language)

    fun localizedNextWeekTip(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(nextWeekTipTr, nextWeekTipEn, language)

    companion object {
        fun sample(): WeeklySummarySnapshot = WeeklySummarySnapshot(
            weekMondayIso = "2026-11-16",
            weekSundayIso = "2026-11-22",
            periodLabelTr = "16 - 22 Kas 2026",
            periodLabelEn = "16 - 22 Nov 2026",
            activeDays = 6,
            foodLoggedDays = 6,
            avgDailyCalories = 1940,
            targetCalories = 2100,
            avgDailyProteinG = 128,
            targetProteinG = 140,
            totalBurnedCalories = 1860,
            workoutSessions = 4,
            totalWorkoutMinutes = 165,
            avgDailyWaterMl = 2350,
            waterTargetMl = 2500,
            weightStartKg = 74.8,
            weightEndKg = 74.3,
            weightDeltaKg = -0.5,
            consistencyScore = 88,
            currentStreakDays = 9,
            dailyBars = listOf(
                WeeklyDaySummaryBar("2026-11-16", "Pzt", "Mon", 2050, 320, true),
                WeeklyDaySummaryBar("2026-11-17", "Sal", "Tue", 1890, 410, true),
                WeeklyDaySummaryBar("2026-11-18", "Çar", "Wed", 2120, 120, true),
                WeeklyDaySummaryBar("2026-11-19", "Per", "Thu", 1980, 380, true),
                WeeklyDaySummaryBar("2026-11-20", "Cum", "Fri", 2010, 420, true),
                WeeklyDaySummaryBar("2026-11-21", "Cmt", "Sat", 1590, 210, true),
                WeeklyDaySummaryBar("2026-11-22", "Paz", "Sun", 0, 0, false)
            ),
            highlightTr = "6/7 aktif gün ve 4 antrenmanla harika bir istikrar yakaladın!",
            highlightEn = "Great consistency with 6/7 active days and 4 workouts!",
            nextWeekTipTr = "Günlük ortalama proteinini +12g artırarak hedefine tam ulaşabilirsin.",
            nextWeekTipEn = "Boost daily protein by +12g to lock in your target every day.",
            hasEnoughData = true
        )
    }
}

/**
 * Pure deterministic builder for the one-time previous-week summary popup card (0 AI quota cost).
 */
class WeeklySummaryBuilder(
    private val database: HealthDatabase,
    private val tokenStorage: TokenStorage
) {
    fun buildPreviousWeekSummary(
        userId: Int = tokenStorage.getUserId(),
        todayIso: String = getTodayIso()
    ): WeeklySummarySnapshot? {
        if (userId == 0) return null
        val (currentMonday, _) = getWeekBoundsIso(todayIso)
        val prevMonday = shiftDateIso(currentMonday, -7)
        val prevSunday = shiftDateIso(currentMonday, -1)

        val foods = database.getAllFoods(userId)
        val workouts = database.getAllWorkouts(userId)
        val weights = database.getWeights(userId)
        val waters = database.getAllWaterSync(userId)
        val goal = database.getNutritionGoalForSync(userId) ?: NutritionGoal(
            id = "goal_$userId",
            userId = userId
        )
        val fitnessGoal = if (goal.fitnessGoal.isNotBlank() && goal.fitnessGoal != FitnessGoal.MAINTAIN.id) {
            goal.parsedFitnessGoal
        } else {
            tokenStorage.getUserFitnessGoal()
        }
        val fallbackWeight = tokenStorage.getUserWeight().takeIf { it > 0.0 } ?: 72.0

        return buildForRange(
            weekMondayIso = prevMonday,
            weekSundayIso = prevSunday,
            todayIso = todayIso,
            allFoods = foods,
            allWorkouts = workouts,
            allWeights = weights,
            allWaters = waters,
            goal = goal,
            fitnessGoal = fitnessGoal,
            fallbackWeightKg = fallbackWeight,
            hasConnectedDevice = tokenStorage.hasConnectedDevice()
        )
    }

    companion object {
        private val DAY_LABELS_TR = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
        private val DAY_LABELS_EN = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        fun buildForRange(
            weekMondayIso: String,
            weekSundayIso: String,
            todayIso: String,
            allFoods: List<FoodLog>,
            allWorkouts: List<WorkoutLog>,
            allWeights: List<WeightEntry>,
            allWaters: List<WaterEntry>,
            goal: NutritionGoal,
            fitnessGoal: FitnessGoal,
            fallbackWeightKg: Double = 72.0,
            hasConnectedDevice: Boolean = false
        ): WeeklySummarySnapshot {
            val activeFoods = allFoods.filter { !it.isDeleted }
            val activeWorkouts = allWorkouts.filter { !it.isDeleted }
            val activeWeights = allWeights.filter { !it.isDeleted && it.weightKg > 0.0 }

            val weekFoods = activeFoods.filter { it.date in weekMondayIso..weekSundayIso }
            val weekWorkouts = activeWorkouts.filter { it.date in weekMondayIso..weekSundayIso }
            val weekWeights = activeWeights.filter { it.date in weekMondayIso..weekSundayIso }.sortedBy { it.date }
            val weekWaters = allWaters.filter { it.date in weekMondayIso..weekSundayIso && it.amountMl > 0 }

            val dailyBars = (0 until 7).map { offset ->
                val dateIso = shiftDateIso(weekMondayIso, offset)
                val dayFoods = weekFoods.filter { it.date == dateIso }
                val dayWorkouts = weekWorkouts.filter { it.date == dateIso }
                val explicitDayWorkouts = dayWorkouts.filter { !it.isDailySteps }
                val stepLog = dayWorkouts.firstOrNull { it.isDailySteps }
                val intake = dayFoods.sumOf { it.totalCalories }
                val burned = WorkoutTelemetryHelper.calculateEffectiveDailyBurn(explicitDayWorkouts, stepLog)
                val hasWater = weekWaters.any { it.date == dateIso && it.amountMl > 0 }
                val hasWeight = weekWeights.any { it.date == dateIso }
                WeeklyDaySummaryBar(
                    dateIso = dateIso,
                    shortLabelTr = DAY_LABELS_TR[offset],
                    shortLabelEn = DAY_LABELS_EN[offset],
                    intakeKcal = intake,
                    burnedKcal = burned,
                    hasActivity = intake > 0 || explicitDayWorkouts.isNotEmpty() || burned > 0 || hasWater || hasWeight
                )
            }

            val activeDays = dailyBars.count { it.hasActivity }
            val foodLoggedDays = dailyBars.count { it.intakeKcal > 0 }
            val foodDivisor = foodLoggedDays.coerceAtLeast(1)

            val allWeekItems = weekFoods.flatMap { it.items }
            val avgDailyCalories = if (foodLoggedDays > 0) {
                (weekFoods.sumOf { it.totalCalories }.toDouble() / foodDivisor).roundToInt()
            } else 0
            val avgDailyProteinG = if (foodLoggedDays > 0) {
                (allWeekItems.sumOf { it.proteinG } / foodDivisor).roundToInt()
            } else 0

            val explicitWorkouts = weekWorkouts.filter { !it.isDailySteps }
            val totalBurnedCalories = dailyBars.sumOf { it.burnedKcal }
            val workoutSessions = explicitWorkouts.size
            val totalWorkoutMinutes = explicitWorkouts.sumOf { it.durationMinutes }

            val avgDailyWaterMl = if (weekWaters.isNotEmpty()) {
                (weekWaters.sumOf { it.amountMl }.toDouble() / weekWaters.size).roundToInt()
            } else 0
            val waterTargetMl = (weekWaters.lastOrNull()?.targetMl ?: 2500).coerceAtLeast(1500)

            val latestBeforeOrInWeek = activeWeights.filter { it.date <= weekSundayIso }.maxByOrNull { it.date }?.weightKg
            val weightStartKg = weekWeights.firstOrNull()?.weightKg
            val weightEndKg = weekWeights.lastOrNull()?.weightKg ?: latestBeforeOrInWeek
            val weightDeltaKg = if (weekWeights.size >= 2) {
                ((weekWeights.last().weightKg - weekWeights.first().weightKg) * 10.0).roundToInt() / 10.0
            } else {
                val priorWeight = activeWeights.filter { it.date < weekMondayIso }.maxByOrNull { it.date }?.weightKg
                if (priorWeight != null && weekWeights.isNotEmpty()) {
                    ((weekWeights.last().weightKg - priorWeight) * 10.0).roundToInt() / 10.0
                } else null
            }

            val targetCalories = goal.calories.coerceAtLeast(1200)
            val effectiveWeightForProtein = weightEndKg ?: fallbackWeightKg
            val targetProteinG = calculateProteinTarget(effectiveWeightForProtein, fitnessGoal).roundToInt().coerceAtLeast(40)

            // Deterministic weekly score (0..100)
            val activityPoints = ((activeDays / 7.0) * 35.0).roundToInt().coerceIn(0, 35)
            val caloriePoints = if (foodLoggedDays > 0 && targetCalories > 0) {
                val deviation = abs(avgDailyCalories - targetCalories).toDouble() / targetCalories
                ((1.0 - (deviation / 0.35).coerceIn(0.0, 1.0)) * 25.0).roundToInt()
            } else 0
            val proteinPoints = if (foodLoggedDays > 0 && targetProteinG > 0) {
                val ratio = (avgDailyProteinG.toDouble() / targetProteinG).coerceIn(0.0, 1.0)
                (ratio * 20.0).roundToInt()
            } else 0
            val workoutPoints = ((workoutSessions.coerceAtMost(4) / 4.0) * 12.0).roundToInt()
            val waterPoints = if (waterTargetMl > 0 && avgDailyWaterMl > 0) {
                ((avgDailyWaterMl.toDouble() / waterTargetMl).coerceIn(0.0, 1.0) * 8.0).roundToInt()
            } else 0
            val consistencyScore = (activityPoints + caloriePoints + proteinPoints + workoutPoints + waterPoints).coerceIn(0, 100)

            val gamification = GamificationEngine.evaluate(
                foods = activeFoods,
                workouts = activeWorkouts,
                weights = activeWeights,
                waters = allWaters,
                nutritionGoal = goal,
                fitnessGoal = fitnessGoal,
                latestWeightKg = latestBeforeOrInWeek ?: fallbackWeightKg,
                todayIso = todayIso,
                hasConnectedDevice = hasConnectedDevice
            )

            val (highlightTr, highlightEn) = buildHighlight(
                activeDays = activeDays,
                workoutSessions = workoutSessions,
                totalBurnedCalories = totalBurnedCalories,
                avgDailyProteinG = avgDailyProteinG,
                targetProteinG = targetProteinG,
                avgDailyCalories = avgDailyCalories,
                targetCalories = targetCalories,
                weightDeltaKg = weightDeltaKg
            )
            val (tipTr, tipEn) = buildNextWeekTip(
                activeDays = activeDays,
                foodLoggedDays = foodLoggedDays,
                avgDailyProteinG = avgDailyProteinG,
                targetProteinG = targetProteinG,
                avgDailyWaterMl = avgDailyWaterMl,
                waterTargetMl = waterTargetMl,
                workoutSessions = workoutSessions
            )

            val hasEnoughData = activeDays >= 1

            return WeeklySummarySnapshot(
                weekMondayIso = weekMondayIso,
                weekSundayIso = weekSundayIso,
                periodLabelTr = formatWeekRangeTr(weekMondayIso, weekSundayIso, AppLanguage.TR),
                periodLabelEn = formatWeekRangeTr(weekMondayIso, weekSundayIso, AppLanguage.EN),
                activeDays = activeDays,
                foodLoggedDays = foodLoggedDays,
                avgDailyCalories = avgDailyCalories,
                targetCalories = targetCalories,
                avgDailyProteinG = avgDailyProteinG,
                targetProteinG = targetProteinG,
                totalBurnedCalories = totalBurnedCalories,
                workoutSessions = workoutSessions,
                totalWorkoutMinutes = totalWorkoutMinutes,
                avgDailyWaterMl = avgDailyWaterMl,
                waterTargetMl = waterTargetMl,
                weightStartKg = weightStartKg,
                weightEndKg = weightEndKg,
                weightDeltaKg = weightDeltaKg,
                consistencyScore = consistencyScore,
                currentStreakDays = gamification.currentStreakDays,
                dailyBars = dailyBars,
                highlightTr = highlightTr,
                highlightEn = highlightEn,
                nextWeekTipTr = tipTr,
                nextWeekTipEn = tipEn,
                hasEnoughData = hasEnoughData
            )
        }

        private fun buildHighlight(
            activeDays: Int,
            workoutSessions: Int,
            totalBurnedCalories: Int,
            avgDailyProteinG: Int,
            targetProteinG: Int,
            avgDailyCalories: Int,
            targetCalories: Int,
            weightDeltaKg: Double?
        ): Pair<String, String> {
            return when {
                activeDays >= 6 && workoutSessions >= 3 ->
                    "$activeDays/7 aktif gün ve $workoutSessions antrenmanla harika bir disiplin yakaladın!" to
                        "Awesome discipline with $activeDays/7 active days and $workoutSessions workouts!"
                workoutSessions >= 3 ->
                    "Geçen hafta $workoutSessions antrenmanla toplam $totalBurnedCalories kcal yaktın!" to
                        "You completed $workoutSessions workouts and burned $totalBurnedCalories kcal last week!"
                avgDailyProteinG >= (targetProteinG * 0.85).roundToInt() && avgDailyProteinG > 0 ->
                    "Günlük ortalama ${avgDailyProteinG}g proteinle kas ve tokluk desteğin çok güçlüydü." to
                        "Strong protein compliance averaging ${avgDailyProteinG}g/day for recovery and satiety."
                avgDailyCalories in (targetCalories * 0.85).roundToInt()..(targetCalories * 1.12).roundToInt() ->
                    "Ortalama $avgDailyCalories kcal ile kalori hedefini dengede tuttun." to
                        "You kept calories right in your sweet spot at an average of $avgDailyCalories kcal."
                weightDeltaKg != null && weightDeltaKg < 0.0 ->
                    "Hafta içinde ${abs(weightDeltaKg)} kg değişimle trendini net şekilde takip ettin." to
                        "You tracked a ${abs(weightDeltaKg)} kg shift across the week with clear momentum."
                else ->
                    "Geçen hafta $activeDays gün sağlık verini kaydederek ritmini canlı tuttun." to
                        "You logged health telemetry on $activeDays day(s) last week — keep building momentum!"
            }
        }

        private fun buildNextWeekTip(
            activeDays: Int,
            foodLoggedDays: Int,
            avgDailyProteinG: Int,
            targetProteinG: Int,
            avgDailyWaterMl: Int,
            waterTargetMl: Int,
            workoutSessions: Int
        ): Pair<String, String> {
            val proteinGap = (targetProteinG - avgDailyProteinG).coerceAtLeast(0)
            return when {
                foodLoggedDays < 4 ->
                    "Yeni haftada sık öğünleri Tek Tık Tekrarla ile ekleyerek 5+ kayıt gününe ulaş." to
                        "Use 1-Tap Repeat for frequent meals this week to easily hit 5+ logged days."
                proteinGap >= 15 ->
                    "Günlük ortalama proteinini +${proteinGap}g artırarak hedefine tam ulaşabilirsin." to
                        "Boost daily protein by +${proteinGap}g to lock in your target every day."
                avgDailyWaterMl in 1 until (waterTargetMl * 0.75).roundToInt() ->
                    "Su tüketimin ort. ${avgDailyWaterMl}ml; günde +2 bardak ekleyerek hidrasyonu güçlendir." to
                        "Hydration averaged ${avgDailyWaterMl}ml; add +2 glasses a day to hit your goal."
                workoutSessions < 2 ->
                    "Bu hafta en az 2 kısa yürüyüş veya egzersiz seansı ekleyerek net enerjini destekle." to
                        "Aim for at least 2 short walks or workouts this week to boost your net balance."
                activeDays < 6 ->
                    "Harika gidiyorsun — bu hafta 6+ aktif güne ulaşarak haftalık skorunu zirveye taşı!" to
                        "Great pace — aim for 6+ active days this week to push your score even higher!"
                else ->
                    "Müthiş bir tempo! Yeni haftada aynı rutini koruyup su ve protein dengesini sürdür." to
                        "Stellar momentum! Keep this rhythm going and maintain your water and protein balance."
            }
        }
    }
}
