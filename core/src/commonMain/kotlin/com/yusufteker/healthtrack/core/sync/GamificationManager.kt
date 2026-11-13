package com.yusufteker.healthtrack.core.sync

import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.utils.getTodayIso
import kotlinx.coroutines.flow.*
import kotlin.time.TimeSource

class GamificationManager(
    private val database: HealthDatabase,
    private val settings: Settings
) {
    private val _celebrationQueue = MutableStateFlow<List<CelebrationEvent>>(emptyList())
    private val _currentCelebration = MutableStateFlow<CelebrationEvent?>(null)
    val currentCelebration: StateFlow<CelebrationEvent?> = _currentCelebration.asStateFlow()

    private val _currentMotivationalToast = MutableStateFlow<MotivationalToastEvent?>(null)
    val currentMotivationalToast: StateFlow<MotivationalToastEvent?> = _currentMotivationalToast.asStateFlow()

    private var quietDepth: Int = 0
    private var lastQuietSyncMark: TimeSource.Monotonic.ValueTimeMark? = null

    private val isQuietWindowActive: Boolean
        get() = quietDepth > 0 || (lastQuietSyncMark?.elapsedNow()?.inWholeMilliseconds?.let { it < 900L } == true)

    private var lastObservedUserId: Int = -1
    private var wasAllDailyDoneToday: Boolean = false
    private var wasWorkoutGoalDoneToday: Boolean = false
    private var wasWaterGoalDoneToday: Boolean = false
    private var lastDailyDateIso: String = ""
    private var hasSessionBaseline: Boolean = false
    private var lastTodaySnapshot: TodayActivitySnapshot = TodayActivitySnapshot()

    private data class TodayActivitySnapshot(
        val foodsCount: Int = 0,
        val calories: Int = 0,
        val explicitWorkoutsCount: Int = 0,
        val explicitBurned: Int = 0,
        val totalBurnedWithSteps: Int = 0,
        val waterMl: Int = 0,
        val weightsCount: Int = 0
    )

    /**
     * Suppresses motivational toasts & confetti popups while background cloud sync or
     * historical device health backfill runs, and keeps a short cooldown for delayed Flow emissions.
     */
    suspend fun <T> runQuietly(block: suspend () -> T): T {
        quietDepth++
        return try {
            block()
        } finally {
            lastQuietSyncMark = TimeSource.Monotonic.markNow()
            quietDepth = (quietDepth - 1).coerceAtLeast(0)
        }
    }

    fun observeSummary(userId: Int): Flow<GamificationSummary> {
        if (userId <= 0) return flowOf(GamificationSummary())

        return combine(
            database.observeAllFoods(userId),
            database.observeAllWorkouts(userId),
            database.observeWeights(userId),
            database.observeAllWater(userId),
            database.observeNutritionGoal(userId)
        ) { foods, workouts, weights, waters, goal ->
            val latestWeight = weights.firstOrNull { !it.isDeleted && it.weightKg > 0.0 }?.weightKg
            val todayIso = getTodayIso()
            val summary = GamificationEngine.evaluate(
                foods = foods,
                workouts = workouts,
                weights = weights,
                waters = waters,
                nutritionGoal = goal,
                fitnessGoal = goal.parsedFitnessGoal,
                latestWeightKg = latestWeight,
                todayIso = todayIso
            )

            val todayFoods = foods.filter { !it.isDeleted && it.date == todayIso && it.totalCalories > 0 }
            val todayExplicitWorkouts = workouts.filter {
                !it.isDeleted && !it.isDailySteps && it.date == todayIso && (it.caloriesBurned > 0 || it.durationMinutes > 0)
            }
            val todayStepLog = workouts.firstOrNull { !it.isDeleted && it.isDailySteps && it.date == todayIso }
            val todayWaterEntry = waters.firstOrNull { it.date == todayIso }
            val todayWeightsCount = weights.count { !it.isDeleted && it.date == todayIso && it.weightKg > 0.0 }

            val todaySnapshot = TodayActivitySnapshot(
                foodsCount = todayFoods.size,
                calories = todayFoods.sumOf { it.totalCalories },
                explicitWorkoutsCount = todayExplicitWorkouts.size,
                explicitBurned = todayExplicitWorkouts.sumOf { it.caloriesBurned.coerceAtLeast(0) },
                totalBurnedWithSteps = WorkoutTelemetryHelper.calculateEffectiveDailyBurn(todayExplicitWorkouts, todayStepLog),
                waterMl = todayWaterEntry?.amountMl ?: 0,
                weightsCount = todayWeightsCount
            )
            val calTarget = goal.calories.coerceAtLeast(1000)
            val waterTarget = (todayWaterEntry?.targetMl ?: 2500).coerceAtLeast(1500)
            val workoutTarget = 300

            evaluateCelebrationsAndMotivation(
                userId = userId,
                todayIso = todayIso,
                summary = summary,
                todaySnapshot = todaySnapshot,
                calTarget = calTarget,
                waterTarget = waterTarget,
                workoutTarget = workoutTarget
            )
            summary
        }
    }

    fun dismissCurrentCelebration() {
        val currentQueue = _celebrationQueue.value
        val remaining = if (currentQueue.isNotEmpty()) currentQueue.drop(1) else emptyList()
        _celebrationQueue.value = remaining
        _currentCelebration.value = remaining.firstOrNull()
    }

    fun dismissMotivationalToast() {
        _currentMotivationalToast.value = null
    }

    fun clearAllCelebrations() {
        _celebrationQueue.value = emptyList()
        _currentCelebration.value = null
        _currentMotivationalToast.value = null
    }

    private fun enqueueCelebration(event: CelebrationEvent) {
        val current = _celebrationQueue.value
        if (current.any { it.id == event.id }) return
        val updated = current + event
        _celebrationQueue.value = updated
        if (_currentCelebration.value == null) {
            _currentCelebration.value = updated.firstOrNull()
        }
    }

    private fun showMotivationalToast(event: MotivationalToastEvent) {
        _currentMotivationalToast.value = event
    }

    private fun evaluateCelebrationsAndMotivation(
        userId: Int,
        todayIso: String,
        summary: GamificationSummary,
        todaySnapshot: TodayActivitySnapshot,
        calTarget: Int,
        waterTarget: Int,
        workoutTarget: Int
    ) {
        val initKey = "ht_gamify_init_$userId"
        val rulesV2Key = "ht_gamify_rules_v2_$userId"
        val seenBadgesKey = "ht_gamify_badges_$userId"
        val seenLevelKey = "ht_gamify_level_$userId"
        val seenStreakKey = "ht_gamify_streak_$userId"
        val seenDailyDoneKey = "ht_gamify_daily_done_${userId}_$todayIso"
        val seenWorkoutGoalKey = "ht_gamify_workout_goal_${userId}_$todayIso"
        val seenWaterGoalKey = "ht_gamify_water_goal_${userId}_$todayIso"

        val unlockedNames = summary.achievements.filter { it.isUnlocked }.map { it.badgeId.name }.toSet()
        val allDailyDone = summary.dailyQuests.isNotEmpty() && summary.dailyQuests.all { it.isCompleted }
        val isWorkoutGoalDoneNow = todaySnapshot.totalBurnedWithSteps >= workoutTarget
        val isWaterGoalDoneNow = todaySnapshot.waterMl >= waterTarget

        if (lastObservedUserId != userId || lastDailyDateIso != todayIso) {
            lastObservedUserId = userId
            lastDailyDateIso = todayIso
            hasSessionBaseline = false
            wasAllDailyDoneToday = settings.getBoolean(seenDailyDoneKey, allDailyDone)
            wasWorkoutGoalDoneToday = settings.getBoolean(seenWorkoutGoalKey, isWorkoutGoalDoneNow)
            wasWaterGoalDoneToday = settings.getBoolean(seenWaterGoalKey, isWaterGoalDoneNow)
        }

        val isSeededV2 = settings.getBoolean(initKey, false) && settings.getBoolean(rulesV2Key, false)
        if (!isSeededV2) {
            // Seed or re-seed baseline cleanly under v2 rules so historical/passive step entries
            // never leave stale streaks or trigger false 30-day badge confetti.
            settings.putBoolean(initKey, true)
            settings.putBoolean(rulesV2Key, true)
            settings.putString(seenBadgesKey, unlockedNames.joinToString(","))
            settings.putInt(seenLevelKey, summary.level)
            settings.putInt(seenStreakKey, summary.currentStreakDays)
            if (allDailyDone) settings.putBoolean(seenDailyDoneKey, true)
            if (isWorkoutGoalDoneNow) settings.putBoolean(seenWorkoutGoalKey, true)
            if (isWaterGoalDoneNow) settings.putBoolean(seenWaterGoalKey, true)
            wasAllDailyDoneToday = allDailyDone
            wasWorkoutGoalDoneToday = isWorkoutGoalDoneNow
            wasWaterGoalDoneToday = isWaterGoalDoneNow
            lastTodaySnapshot = todaySnapshot
            hasSessionBaseline = true
            return
        }

        if (!hasSessionBaseline || isQuietWindowActive) {
            // Update seen baseline silently during initial emission or background sync
            val mergedBadges = settings.getString(seenBadgesKey, "")
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableSet()
            mergedBadges.addAll(unlockedNames)
            settings.putString(seenBadgesKey, mergedBadges.joinToString(","))
            settings.putInt(seenLevelKey, maxOf(settings.getInt(seenLevelKey, 1), summary.level))
            settings.putInt(seenStreakKey, maxOf(settings.getInt(seenStreakKey, 0), summary.currentStreakDays))
            if (allDailyDone) {
                wasAllDailyDoneToday = true
                settings.putBoolean(seenDailyDoneKey, true)
            }
            if (isWorkoutGoalDoneNow) {
                wasWorkoutGoalDoneToday = true
                settings.putBoolean(seenWorkoutGoalKey, true)
            }
            if (isWaterGoalDoneNow) {
                wasWaterGoalDoneToday = true
                settings.putBoolean(seenWaterGoalKey, true)
            }
            lastTodaySnapshot = todaySnapshot
            hasSessionBaseline = true
            return
        }

        // --- Active user action path (outside quiet background sync) ---
        val prev = lastTodaySnapshot
        lastTodaySnapshot = todaySnapshot

        // 1. Check milestone badges, level-ups, streak milestones, all daily quests
        val seenBadges = settings.getString(seenBadgesKey, "")
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableSet()

        val newlyUnlocked = summary.achievements.filter {
            it.isUnlocked && !seenBadges.contains(it.badgeId.name)
        }
        for (ach in newlyUnlocked) {
            seenBadges.add(ach.badgeId.name)
            enqueueCelebration(
                CelebrationEvent(
                    id = "badge_${ach.badgeId.name}",
                    type = CelebrationType.BADGE_UNLOCKED,
                    titleTr = "Yeni Rozet Açıldı!",
                    titleEn = "New Badge Unlocked!",
                    subtitleTr = "${ach.badgeId.titleTr} • ${ach.badgeId.descriptionTr}",
                    subtitleEn = "${ach.badgeId.titleEn} • ${ach.badgeId.descriptionEn}",
                    xpBonus = ach.badgeId.xpReward,
                    badgeId = ach.badgeId
                )
            )
        }
        if (newlyUnlocked.isNotEmpty()) {
            settings.putString(seenBadgesKey, seenBadges.joinToString(","))
        }

        val seenLevel = settings.getInt(seenLevelKey, 1)
        if (summary.level > seenLevel) {
            settings.putInt(seenLevelKey, summary.level)
            enqueueCelebration(
                CelebrationEvent(
                    id = "level_${summary.level}",
                    type = CelebrationType.LEVEL_UP,
                    titleTr = "Seviye Atladın! Lv.${summary.level}",
                    titleEn = "Level Up! Lv.${summary.level}",
                    subtitleTr = "Yeni rütben: ${summary.tier.titleTr}. Harika gidiyorsun!",
                    subtitleEn = "Your rank: ${summary.tier.titleEn}. Keep crushing it!",
                    xpBonus = 0,
                    newLevel = summary.level
                )
            )
        }

        val seenStreak = settings.getInt(seenStreakKey, 0)
        val streakMilestones = setOf(3, 7, 14, 21, 30, 60, 100)
        if (summary.currentStreakDays > seenStreak && summary.currentStreakDays in streakMilestones) {
            settings.putInt(seenStreakKey, summary.currentStreakDays)
            enqueueCelebration(
                CelebrationEvent(
                    id = "streak_${summary.currentStreakDays}",
                    type = CelebrationType.STREAK_MILESTONE,
                    titleTr = "${summary.currentStreakDays} Günlük Seri!",
                    titleEn = "${summary.currentStreakDays}-Day Streak!",
                    subtitleTr = "İstikrarın ateş gibi parlıyor. Devam et!",
                    subtitleEn = "Your consistency is on fire. Keep going!",
                    xpBonus = 0,
                    streakDays = summary.currentStreakDays
                )
            )
        } else if (summary.currentStreakDays > seenStreak) {
            settings.putInt(seenStreakKey, summary.currentStreakDays)
        }

        var triggeredGoalConfettiThisCycle = newlyUnlocked.isNotEmpty() || summary.level > seenLevel

        // 2. Check Workout action & Daily Calorie Burn Goal
        val workoutAdded = todaySnapshot.explicitWorkoutsCount > prev.explicitWorkoutsCount ||
            todaySnapshot.explicitBurned > prev.explicitBurned
        if (workoutAdded) {
            if (isWorkoutGoalDoneNow && !wasWorkoutGoalDoneToday && !settings.getBoolean(seenWorkoutGoalKey, false)) {
                wasWorkoutGoalDoneToday = true
                settings.putBoolean(seenWorkoutGoalKey, true)
                triggeredGoalConfettiThisCycle = true
                enqueueCelebration(
                    CelebrationEvent(
                        id = "workout_goal_$todayIso",
                        type = CelebrationType.WORKOUT_GOAL_REACHED,
                        titleTr = "Günlük Yakım Hedefi Doldu!",
                        titleEn = "Daily Burn Goal Smashed!",
                        subtitleTr = "Bugün ${todaySnapshot.totalBurnedWithSteps} kcal yakarak hedefine ulaştın — ateş gibisin!",
                        subtitleEn = "Burned ${todaySnapshot.totalBurnedWithSteps} kcal today and hit your target!",
                        xpBonus = 40
                    )
                )
            } else if (!triggeredGoalConfettiThisCycle) {
                val pct = ((todaySnapshot.totalBurnedWithSteps.toFloat() / workoutTarget) * 100).toInt().coerceAtLeast(1)
                val remaining = (workoutTarget - todaySnapshot.totalBurnedWithSteps).coerceAtLeast(0)
                showMotivationalToast(
                    MotivationalToastEvent(
                        id = "toast_workout_${todaySnapshot.explicitWorkoutsCount}_${todaySnapshot.explicitBurned}",
                        xpEarned = 30,
                        titleTr = "Harika Antrenman! +30 XP",
                        titleEn = "Awesome Workout! +30 XP",
                        subtitleTr = if (remaining > 0) {
                            "Günlük hedefin %$pct tamamlandı • Kalan $remaining kcal"
                        } else {
                            "Hedefin üstüne çıktın! Toplam ${todaySnapshot.totalBurnedWithSteps} kcal"
                        },
                        subtitleEn = if (remaining > 0) {
                            "$pct% of daily burn goal done • $remaining kcal to go"
                        } else {
                            "Above daily goal! ${todaySnapshot.totalBurnedWithSteps} kcal burned"
                        },
                        progressFraction = (todaySnapshot.totalBurnedWithSteps.toFloat() / workoutTarget).coerceIn(0f, 1f),
                        accentColorHex = 0xFFF97316
                    )
                )
            }
        }

        // 3. Check Water action & Daily Water Goal
        val waterIncreased = todaySnapshot.waterMl > prev.waterMl
        if (waterIncreased) {
            val deltaMl = todaySnapshot.waterMl - prev.waterMl
            if (isWaterGoalDoneNow && !wasWaterGoalDoneToday && !settings.getBoolean(seenWaterGoalKey, false)) {
                wasWaterGoalDoneToday = true
                settings.putBoolean(seenWaterGoalKey, true)
                triggeredGoalConfettiThisCycle = true
                enqueueCelebration(
                    CelebrationEvent(
                        id = "water_goal_$todayIso",
                        type = CelebrationType.WATER_GOAL_REACHED,
                        titleTr = "Su Hedefine Ulaştın!",
                        titleEn = "Hydration Goal Reached!",
                        subtitleTr = "Bugün ${todaySnapshot.waterMl} ml su içtin — metabolizman canlandı!",
                        subtitleEn = "${todaySnapshot.waterMl} ml hydrated today — fueling your recovery!",
                        xpBonus = 25
                    )
                )
            } else if (!triggeredGoalConfettiThisCycle) {
                val remaining = (waterTarget - todaySnapshot.waterMl).coerceAtLeast(0)
                showMotivationalToast(
                    MotivationalToastEvent(
                        id = "toast_water_${todaySnapshot.waterMl}",
                        xpEarned = 15,
                        titleTr = "Su Eklendi! (+$deltaMl ml)",
                        titleEn = "Water Logged! (+$deltaMl ml)",
                        subtitleTr = if (remaining > 0) "Günlük hedefe sadece $remaining ml kaldı!" else "Su hedefin fazlasıyla tamam!",
                        subtitleEn = if (remaining > 0) "Only $remaining ml left to hit today's goal!" else "Daily hydration target exceeded!",
                        progressFraction = (todaySnapshot.waterMl.toFloat() / waterTarget).coerceIn(0f, 1f),
                        accentColorHex = 0xFF0EA5E9
                    )
                )
            }
        }

        // 4. Check Food action
        val foodAdded = todaySnapshot.foodsCount > prev.foodsCount || todaySnapshot.calories > prev.calories
        if (foodAdded && !triggeredGoalConfettiThisCycle && !workoutAdded) {
            val ratio = (todaySnapshot.calories.toFloat() / calTarget).coerceIn(0f, 1f)
            showMotivationalToast(
                MotivationalToastEvent(
                    id = "toast_food_${todaySnapshot.foodsCount}_${todaySnapshot.calories}",
                    xpEarned = 20,
                    titleTr = "Öğün Kaydedildi!",
                    titleEn = "Meal Logged!",
                    subtitleTr = "Bugünkü Biyo-Skor: ${summary.todayScore}/100 • Harika takip!",
                    subtitleEn = "Today's Bio-Score: ${summary.todayScore}/100 • Great tracking!",
                    progressFraction = ratio,
                    accentColorHex = 0xFF10B981
                )
            )
        }

        // 5. Check Weight action
        val weightAdded = todaySnapshot.weightsCount > prev.weightsCount
        if (weightAdded && !triggeredGoalConfettiThisCycle && !workoutAdded && !foodAdded) {
            showMotivationalToast(
                MotivationalToastEvent(
                    id = "toast_weight_${todaySnapshot.weightsCount}",
                    xpEarned = 10,
                    titleTr = "Tartım Kaydedildi!",
                    titleEn = "Weight Logged!",
                    subtitleTr = "Düzenli tartım gelişimini net görmeni sağlar!",
                    subtitleEn = "Consistent weigh-ins keep your progress crystal clear!",
                    progressFraction = null,
                    accentColorHex = 0xFF8B5CF6
                )
            )
        }

        // 6. All Daily Quests Completed celebration
        if (allDailyDone && !wasAllDailyDoneToday && !settings.getBoolean(seenDailyDoneKey, false)) {
            wasAllDailyDoneToday = true
            settings.putBoolean(seenDailyDoneKey, true)
            enqueueCelebration(
                CelebrationEvent(
                    id = "daily_done_$todayIso",
                    type = CelebrationType.DAILY_GOAL_COMPLETED,
                    titleTr = "Günlük Görevler Tamamlandı!",
                    titleEn = "Daily Quests Complete!",
                    subtitleTr = "Öğün, su ve hareket hedeflerini bugün eksiksiz tamamladın.",
                    subtitleEn = "You nailed meals, hydration, and movement today!",
                    xpBonus = 80
                )
            )
        }
    }
}
