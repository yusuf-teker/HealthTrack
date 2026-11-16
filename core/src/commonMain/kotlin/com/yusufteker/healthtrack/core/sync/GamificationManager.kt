package com.yusufteker.healthtrack.core.sync

import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.utils.getTodayIso
import kotlinx.coroutines.flow.*

class GamificationManager(
    private val database: HealthDatabase,
    private val settings: Settings,
    private val tokenStorage: TokenStorage? = null
) {
    private val _celebrationQueue = MutableStateFlow<List<CelebrationEvent>>(emptyList())
    private val _currentCelebration = MutableStateFlow<CelebrationEvent?>(null)
    val currentCelebration: StateFlow<CelebrationEvent?> = _currentCelebration.asStateFlow()

    private val _currentMotivationalToast = MutableStateFlow<MotivationalToastEvent?>(null)
    val currentMotivationalToast: StateFlow<MotivationalToastEvent?> = _currentMotivationalToast.asStateFlow()

    private var quietDepth: Int = 0
    private var pendingQuietPreSnapshot: QuietDbSnapshot? = null

    private val isQuietWindowActive: Boolean
        get() = quietDepth > 0

    private var lastObservedUserId: Int = -1
    private var wasAllDailyDoneToday: Boolean = false
    private var wasWorkoutGoalDoneToday: Boolean = false
    private var wasWaterGoalDoneToday: Boolean = false
    private var lastDailyDateIso: String = ""
    private var hasSessionBaseline: Boolean = false
    private var lastTodaySnapshot: TodayActivitySnapshot = TodayActivitySnapshot()
    private var lastActiveWorkoutIds: Set<String> = emptySet()

    private data class TodayActivitySnapshot(
        val foodsCount: Int = 0,
        val calories: Int = 0,
        val explicitWorkoutsCount: Int = 0,
        val explicitBurned: Int = 0,
        val totalBurnedWithSteps: Int = 0,
        val waterMl: Int = 0,
        val weightsCount: Int = 0
    )

    private data class QuietDbSnapshot(
        val userId: Int,
        val todayIso: String,
        val unlockedStageKeys: Set<String>,
        val workoutIds: Set<String>,
        val todaySnapshot: TodayActivitySnapshot,
        val summary: GamificationSummary
    )

    /**
     * Suppresses motivational toasts & confetti popups strictly while background cloud pull
     * or device health backfill writes to the local database, updating baseline deltas atomically.
     */
    suspend fun <T> runQuietly(block: suspend () -> T): T {
        val isOutermost = quietDepth == 0
        if (isOutermost && lastObservedUserId > 0) {
            pendingQuietPreSnapshot = captureDbSnapshot(lastObservedUserId)
        }
        quietDepth++
        return try {
            block()
        } finally {
            quietDepth = (quietDepth - 1).coerceAtLeast(0)
            if (quietDepth == 0) {
                val preSnap = pendingQuietPreSnapshot
                pendingQuietPreSnapshot = null
                val uid = lastObservedUserId
                if (preSnap != null && uid > 0 && preSnap.userId == uid) {
                    applyQuietDelta(preSnap, captureDbSnapshot(uid))
                }
            }
        }
    }

    private fun captureDbSnapshot(userId: Int): QuietDbSnapshot {
        val todayIso = getTodayIso()
        val foods = database.getAllFoods(userId)
        val workouts = database.getAllWorkouts(userId)
        val weights = database.getWeights(userId)
        val waters = database.getAllWaterSync(userId)
        val goal = database.getNutritionGoalForSync(userId) ?: NutritionGoal(
            id = "goal_$userId",
            userId = userId
        )
        val hasConnectedDevice = settings.getBoolean(TokenStorage.KEY_HAS_CONNECTED_DEVICE, false)
        val latestWeight = weights.firstOrNull { !it.isDeleted && it.weightKg > 0.0 }?.weightKg
        val summary = GamificationEngine.evaluate(
            foods = foods,
            workouts = workouts,
            weights = weights,
            waters = waters,
            nutritionGoal = goal,
            fitnessGoal = goal.parsedFitnessGoal,
            latestWeightKg = latestWeight,
            todayIso = todayIso,
            hasConnectedDevice = hasConnectedDevice
        )
        val nonStepWorkouts = workouts.filter {
            !it.isDeleted && !it.isDailySteps && (it.caloriesBurned > 0 || it.durationMinutes > 0)
        }
        val todaySnap = buildTodaySnapshot(foods, workouts, weights, waters, todayIso)
        val stageKeys = summary.achievements.flatMap { ach ->
            (1..ach.currentLevel).map { lvl -> "${ach.badgeId.name}:$lvl" }
        }.toSet()
        return QuietDbSnapshot(
            userId = userId,
            todayIso = todayIso,
            unlockedStageKeys = stageKeys,
            workoutIds = nonStepWorkouts.map { it.id }.toSet(),
            todaySnapshot = todaySnap,
            summary = summary
        )
    }

    private fun applyQuietDelta(pre: QuietDbSnapshot, post: QuietDbSnapshot) {
        val userId = post.userId
        val seenBadgesKey = "ht_gamify_badges_$userId"
        val seenLevelKey = "ht_gamify_level_$userId"
        val seenStreakKey = "ht_gamify_streak_$userId"

        val quietAddedWorkouts = post.workoutIds - pre.workoutIds
        if (quietAddedWorkouts.isNotEmpty()) {
            lastActiveWorkoutIds = lastActiveWorkoutIds + quietAddedWorkouts
        }

        val quietNewStages = post.unlockedStageKeys - pre.unlockedStageKeys
        if (quietNewStages.isNotEmpty()) {
            val existingSeen = settings.getString(seenBadgesKey, "")
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableSet()
            existingSeen.addAll(quietNewStages)
            settings.putString(seenBadgesKey, existingSeen.joinToString(","))
        }

        if (post.summary.level > pre.summary.level) {
            settings.putInt(seenLevelKey, maxOf(settings.getInt(seenLevelKey, 1), post.summary.level))
        }
        if (post.summary.currentStreakDays > pre.summary.currentStreakDays) {
            settings.putInt(seenStreakKey, maxOf(settings.getInt(seenStreakKey, 0), post.summary.currentStreakDays))
        }

        if (post.todayIso == pre.todayIso && post.todayIso == lastDailyDateIso) {
            val dFoods = (post.todaySnapshot.foodsCount - pre.todaySnapshot.foodsCount).coerceAtLeast(0)
            val dCal = (post.todaySnapshot.calories - pre.todaySnapshot.calories).coerceAtLeast(0)
            val dWorkouts = (post.todaySnapshot.explicitWorkoutsCount - pre.todaySnapshot.explicitWorkoutsCount).coerceAtLeast(0)
            val dBurned = (post.todaySnapshot.explicitBurned - pre.todaySnapshot.explicitBurned).coerceAtLeast(0)
            val dTotalBurned = (post.todaySnapshot.totalBurnedWithSteps - pre.todaySnapshot.totalBurnedWithSteps).coerceAtLeast(0)
            val dWater = (post.todaySnapshot.waterMl - pre.todaySnapshot.waterMl).coerceAtLeast(0)
            val dWeights = (post.todaySnapshot.weightsCount - pre.todaySnapshot.weightsCount).coerceAtLeast(0)
            lastTodaySnapshot = lastTodaySnapshot.copy(
                foodsCount = lastTodaySnapshot.foodsCount + dFoods,
                calories = lastTodaySnapshot.calories + dCal,
                explicitWorkoutsCount = lastTodaySnapshot.explicitWorkoutsCount + dWorkouts,
                explicitBurned = lastTodaySnapshot.explicitBurned + dBurned,
                totalBurnedWithSteps = lastTodaySnapshot.totalBurnedWithSteps + dTotalBurned,
                waterMl = lastTodaySnapshot.waterMl + dWater,
                weightsCount = lastTodaySnapshot.weightsCount + dWeights
            )
        }
    }

    private fun buildTodaySnapshot(
        foods: List<FoodLog>,
        workouts: List<WorkoutLog>,
        weights: List<WeightEntry>,
        waters: List<WaterEntry>,
        todayIso: String
    ): TodayActivitySnapshot {
        val todayFoods = foods.filter { !it.isDeleted && it.date == todayIso && it.totalCalories > 0 }
        val todayExplicitWorkouts = workouts.filter {
            !it.isDeleted && !it.isDailySteps && it.date == todayIso && (it.caloriesBurned > 0 || it.durationMinutes > 0)
        }
        val todayStepLog = workouts.firstOrNull { !it.isDeleted && it.isDailySteps && it.date == todayIso }
        val todayWaterEntry = waters.firstOrNull { it.date == todayIso }
        val todayWeightsCount = weights.count { !it.isDeleted && it.date == todayIso && it.weightKg > 0.0 }
        return TodayActivitySnapshot(
            foodsCount = todayFoods.size,
            calories = todayFoods.sumOf { it.totalCalories },
            explicitWorkoutsCount = todayExplicitWorkouts.size,
            explicitBurned = todayExplicitWorkouts.sumOf { it.caloriesBurned.coerceAtLeast(0) },
            totalBurnedWithSteps = WorkoutTelemetryHelper.calculateEffectiveDailyBurn(todayExplicitWorkouts, todayStepLog),
            waterMl = todayWaterEntry?.amountMl ?: 0,
            weightsCount = todayWeightsCount
        )
    }

    private data class CombinedHealthInputs(
        val foods: List<FoodLog>,
        val workouts: List<WorkoutLog>,
        val weights: List<WeightEntry>,
        val waters: List<WaterEntry>,
        val goal: NutritionGoal
    )

    fun observeSummary(userId: Int): Flow<GamificationSummary> {
        if (userId <= 0) return flowOf(GamificationSummary())

        val deviceConnectedFlow = tokenStorage?.hasConnectedDeviceFlow
            ?: flowOf(settings.getBoolean(TokenStorage.KEY_HAS_CONNECTED_DEVICE, false))

        return combine(
            database.observeAllFoods(userId),
            database.observeAllWorkouts(userId),
            database.observeWeights(userId),
            database.observeAllWater(userId),
            database.observeNutritionGoal(userId)
        ) { foods, workouts, weights, waters, goal ->
            CombinedHealthInputs(foods, workouts, weights, waters, goal)
        }.combine(deviceConnectedFlow) { inputs, hasConnectedDevice ->
            val foods = inputs.foods
            val workouts = inputs.workouts
            val weights = inputs.weights
            val waters = inputs.waters
            val goal = inputs.goal
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
                todayIso = todayIso,
                hasConnectedDevice = hasConnectedDevice
            )

            val nonStepWorkouts = workouts.filter {
                !it.isDeleted && !it.isDailySteps && (it.caloriesBurned > 0 || it.durationMinutes > 0)
            }
            val todayWaterEntry = waters.firstOrNull { it.date == todayIso }
            val todaySnapshot = buildTodaySnapshot(foods, workouts, weights, waters, todayIso)
            val calTarget = goal.calories.coerceAtLeast(1000)
            val waterTarget = (todayWaterEntry?.targetMl ?: 2500).coerceAtLeast(1500)
            val workoutTarget = 300

            evaluateCelebrationsAndMotivation(
                userId = userId,
                todayIso = todayIso,
                summary = summary,
                nonStepWorkouts = nonStepWorkouts,
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

    fun triggerHealthyMealCelebration(evaluation: MealHealthEvaluation) {
        if (isQuietWindowActive || !evaluation.isHealthyCelebrationWorthy) return
        _currentMotivationalToast.value = null
        enqueueCelebration(
            CelebrationEvent(
                id = "healthy_meal_${kotlin.random.Random.nextLong()}",
                type = CelebrationType.HEALTHY_MEAL_LOGGED,
                titleTr = evaluation.badgeTitleTr,
                titleEn = evaluation.badgeTitleEn,
                subtitleTr = evaluation.encouragingQuoteTr,
                subtitleEn = evaluation.encouragingQuoteEn,
                autoDismissMs = 2000L,
                healthScore = evaluation.score
            ),
            prioritizeFront = true
        )
    }

    private fun enqueueCelebration(event: CelebrationEvent, prioritizeFront: Boolean = false) {
        val current = _celebrationQueue.value
        if (current.any { it.id == event.id }) return
        val updated = if (prioritizeFront) {
            listOf(event) + current
        } else {
            current + event
        }
        _celebrationQueue.value = updated
        if (prioritizeFront || _currentCelebration.value == null) {
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
        nonStepWorkouts: List<WorkoutLog>,
        todaySnapshot: TodayActivitySnapshot,
        calTarget: Int,
        waterTarget: Int,
        workoutTarget: Int
    ) {
        val initKey = "ht_gamify_init_$userId"
        val rulesV2Key = "ht_gamify_rules_v2_$userId"
        val rulesV3Key = "ht_gamify_rules_v3_tiered_$userId"
        val seenBadgesKey = "ht_gamify_badges_$userId"
        val seenLevelKey = "ht_gamify_level_$userId"
        val seenStreakKey = "ht_gamify_streak_$userId"
        val seenDailyDoneKey = "ht_gamify_daily_done_${userId}_$todayIso"
        val seenWorkoutGoalKey = "ht_gamify_workout_goal_${userId}_$todayIso"
        val seenWaterGoalKey = "ht_gamify_water_goal_${userId}_$todayIso"

        val currentWorkoutIds = nonStepWorkouts.map { it.id }.toSet()
        val unlockedStageKeys = summary.achievements.flatMap { ach ->
            (1..ach.currentLevel).map { lvl -> "${ach.badgeId.name}:$lvl" }
        }.toSet()
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

        val isSeededV3 = settings.getBoolean(initKey, false) &&
            settings.getBoolean(rulesV2Key, false) &&
            settings.getBoolean(rulesV3Key, false)
        if (!isSeededV3) {
            // Seed or re-seed baseline cleanly under tiered rules so existing progress
            // never triggers retroactive confetti on upgrade or historical backfill.
            settings.putBoolean(initKey, true)
            settings.putBoolean(rulesV2Key, true)
            settings.putBoolean(rulesV3Key, true)
            settings.putString(seenBadgesKey, unlockedStageKeys.joinToString(","))
            settings.putInt(seenLevelKey, summary.level)
            settings.putInt(seenStreakKey, summary.currentStreakDays)
            if (allDailyDone) settings.putBoolean(seenDailyDoneKey, true)
            if (isWorkoutGoalDoneNow) settings.putBoolean(seenWorkoutGoalKey, true)
            if (isWaterGoalDoneNow) settings.putBoolean(seenWaterGoalKey, true)
            wasAllDailyDoneToday = allDailyDone
            wasWorkoutGoalDoneToday = isWorkoutGoalDoneNow
            wasWaterGoalDoneToday = isWaterGoalDoneNow
            lastTodaySnapshot = todaySnapshot
            lastActiveWorkoutIds = currentWorkoutIds
            hasSessionBaseline = true
            return
        }

        if (!hasSessionBaseline || isQuietWindowActive) {
            // Update seen baseline silently during initial emission or background sync
            val mergedBadges = settings.getString(seenBadgesKey, "")
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableSet()
            mergedBadges.addAll(unlockedStageKeys)
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
            lastActiveWorkoutIds = currentWorkoutIds
            hasSessionBaseline = true
            return
        }

        // --- Active user action path (outside quiet background sync) ---
        val prev = lastTodaySnapshot
        val prevWorkoutIds = lastActiveWorkoutIds
        lastTodaySnapshot = todaySnapshot
        lastActiveWorkoutIds = currentWorkoutIds

        val addedWorkouts = nonStepWorkouts.filter { it.id !in prevWorkoutIds }

        // 1. Check tiered milestone badges (pruning any stages dropped when a log was deleted)
        val rawSeenBadges = settings.getString(seenBadgesKey, "")
            .split(",")
            .filter { it.isNotBlank() }
        val seenBadges = rawSeenBadges.filter { it in unlockedStageKeys }.toMutableSet()
        if (seenBadges.size != rawSeenBadges.size) {
            settings.putString(seenBadgesKey, seenBadges.joinToString(","))
        }

        // Evaluate sport badges first so newly logged sport medals headline the popup queue
        val orderedAchievements = summary.achievements.sortedByDescending { it.badgeId.isSportBadge }
        val unlockedSportBadgesThisCycle = mutableSetOf<BadgeId>()
        var anyNewBadgeStage = false
        val pendingSecondaryCelebrations = mutableListOf<CelebrationEvent>()
        val pendingSportEvents = mutableListOf<CelebrationEvent>()

        for (ach in orderedAchievements) {
            if (ach.currentLevel <= 0) continue
            val newLevels = (1..ach.currentLevel).filter { lvl ->
                !seenBadges.contains("${ach.badgeId.name}:$lvl")
            }
            if (newLevels.isEmpty()) continue
            anyNewBadgeStage = true
            newLevels.forEach { lvl -> seenBadges.add("${ach.badgeId.name}:$lvl") }

            val highestLvl = newLevels.max()
            val stageTarget = ach.badgeId.stageForLevel(highestLvl)?.targetCount ?: ach.progressTarget
            val earnedStageXp = newLevels.sumOf { lvl -> ach.badgeId.stageForLevel(lvl)?.xpReward ?: 0 }

            if (ach.badgeId.isSportBadge) {
                unlockedSportBadgesThisCycle.add(ach.badgeId)
                val rank = ach.badgeId.rankForLevel(highestLvl)
                val prevStageTarget = if (highestLvl > 1) {
                    ach.badgeId.stageForLevel(highestLvl - 1)?.targetCount ?: 0
                } else 0
                val stepTotal = (stageTarget - prevStageTarget).coerceAtLeast(1)
                val nextStage = ach.badgeId.stageForLevel(highestLvl + 1)
                val nextStageRemaining = nextStage?.let { (it.targetCount - ach.rawProgress).coerceAtLeast(1) }

                val titleTr = if (highestLvl == 1) {
                    "${ach.badgeId.titleTr} • Sv. 1 Açıldı!"
                } else {
                    "Yeni Madalya Kazandın! • Sv. $highestLvl"
                }
                val titleEn = if (highestLvl == 1) {
                    "${ach.badgeId.titleEn} • Lv. 1 Unlocked!"
                } else {
                    "Medal Upgraded! • Lv. $highestLvl"
                }
                val subtitleTr = if (highestLvl == 1) {
                    if (nextStageRemaining != null) {
                        "İlk antrenmanını tamamladın ve ${rank.labelTr} Madalya kazandın! Sv. 2 için $nextStageRemaining antrenman daha yap."
                    } else {
                        "İlk antrenmanını tamamladın ve ${rank.labelTr} Madalya kazandın!"
                    }
                } else {
                    if (nextStageRemaining != null) {
                        "${ach.badgeId.titleTr} Sv. $highestLvl (${rank.labelTr} Madalya) açıldı! $stepTotal/$stepTotal tamamlandı."
                    } else {
                        "${ach.badgeId.titleTr} Sv. $highestLvl (${rank.labelTr} Madalya) ile zirveye ulaştın!"
                    }
                }
                val subtitleEn = if (highestLvl == 1) {
                    if (nextStageRemaining != null) {
                        "Completed your first session and earned the ${rank.labelEn} Medal! $nextStageRemaining more for Lv. 2."
                    } else {
                        "Completed your first session and earned the ${rank.labelEn} Medal!"
                    }
                } else {
                    if (nextStageRemaining != null) {
                        "${ach.badgeId.titleEn} Lv. $highestLvl (${rank.labelEn} Medal) unlocked! $stepTotal/$stepTotal completed."
                    } else {
                        "${ach.badgeId.titleEn} Lv. $highestLvl (${rank.labelEn} Medal) — peak mastery!"
                    }
                }

                pendingSportEvents.add(
                    CelebrationEvent(
                        id = "badge_${ach.badgeId.name}_lvl_${highestLvl}_${ach.rawProgress}",
                        type = CelebrationType.BADGE_UNLOCKED,
                        titleTr = titleTr,
                        titleEn = titleEn,
                        subtitleTr = subtitleTr,
                        subtitleEn = subtitleEn,
                        xpBonus = earnedStageXp,
                        badgeId = ach.badgeId,
                        badgeStageLevel = highestLvl,
                        nextStageLevel = highestLvl,
                        progressCurrent = stepTotal,
                        progressTarget = stepTotal,
                        previousProgressFraction = if (highestLvl == 1) 0f else ((stepTotal - 1).coerceAtLeast(0).toFloat() / stepTotal.toFloat()),
                        targetProgressFraction = 1f,
                        showConfetti = true
                    )
                )
            } else {
                val isUpgrade = highestLvl > 1 && ach.maxLevel > 1
                val stageSuffix = if (ach.maxLevel > 1) " (Sv. $highestLvl/${ach.maxLevel})" else ""
                val stageSuffixEn = if (ach.maxLevel > 1) " (Lv. $highestLvl/${ach.maxLevel})" else ""
                pendingSecondaryCelebrations.add(
                    CelebrationEvent(
                        id = "badge_${ach.badgeId.name}_lvl_$highestLvl",
                        type = CelebrationType.BADGE_UNLOCKED,
                        titleTr = if (isUpgrade) "Rozet Seviyesi Yükseldi!" else "Yeni Rozet Açıldı!",
                        titleEn = if (isUpgrade) "Badge Leveled Up!" else "New Badge Unlocked!",
                        subtitleTr = "${ach.badgeId.titleTr}$stageSuffix • $stageTarget ${ach.badgeId.unitTr} hedefine ulaştın!",
                        subtitleEn = "${ach.badgeId.titleEn}$stageSuffixEn • Reached $stageTarget ${ach.badgeId.unitEn}!",
                        xpBonus = earnedStageXp,
                        badgeId = ach.badgeId,
                        badgeStageLevel = highestLvl,
                        showConfetti = true
                    )
                )
            }
        }
        if (anyNewBadgeStage) {
            settings.putString(seenBadgesKey, seenBadges.joinToString(","))
        }

        // 1b. Check per-sport stage progress for newly added workouts whose sport medal didn't just level up
        var hasSportProgressPopup = false
        if (addedWorkouts.isNotEmpty()) {
            val matchedSports = addedWorkouts
                .flatMap { SportType.detectAll(it.rawText) }
                .distinct()

            for (sport in matchedSports) {
                val sportBadgeId = BadgeId.fromSportType(sport) ?: continue
                if (sportBadgeId in unlockedSportBadgesThisCycle) continue
                val ach = summary.achievements.firstOrNull { it.badgeId == sportBadgeId } ?: continue
                if (ach.isMaxed) continue

                hasSportProgressPopup = true
                val nextLvl = ach.currentLevel + 1
                val stepCur = ach.stageStepCurrent.coerceAtLeast(1)
                val stepTotal = ach.stageStepTarget.coerceAtLeast(1)
                val prevStep = (stepCur - 1).coerceAtLeast(0)
                val remaining = (stepTotal - stepCur).coerceAtLeast(0)
                val nextRank = sportBadgeId.rankForLevel(nextLvl)

                pendingSportEvents.add(
                    CelebrationEvent(
                        id = "sport_prog_${sportBadgeId.name}_${ach.rawProgress}",
                        type = CelebrationType.SPORT_PROGRESS,
                        titleTr = "${sportBadgeId.titleTr} • Sv. $nextLvl İlerlemesi",
                        titleEn = "${sportBadgeId.titleEn} • Lv. $nextLvl Progress",
                        subtitleTr = "Seviye $nextLvl (${nextRank.labelTr} Madalya) için $stepCur/$stepTotal tamamlandı • $remaining antrenman kaldı!",
                        subtitleEn = "$stepCur/$stepTotal completed toward Lv. $nextLvl (${nextRank.labelEn} Medal) • $remaining left!",
                        xpBonus = 30,
                        badgeId = sportBadgeId,
                        badgeStageLevel = ach.currentLevel,
                        nextStageLevel = nextLvl,
                        progressCurrent = stepCur,
                        progressTarget = stepTotal,
                        previousProgressFraction = prevStep.toFloat() / stepTotal.toFloat(),
                        targetProgressFraction = stepCur.toFloat() / stepTotal.toFloat(),
                        showConfetti = false
                    )
                )
            }
        }

        // Enqueue sport events first so the user immediately sees their sport medal/progress popup
        pendingSportEvents.forEach { event ->
            enqueueCelebration(event)
        }
        pendingSecondaryCelebrations.forEach { event ->
            enqueueCelebration(event)
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

        var triggeredGoalConfettiThisCycle = anyNewBadgeStage || hasSportProgressPopup || summary.level > seenLevel

        // 2. Check Workout action & Daily Calorie Burn Goal
        val workoutAdded = addedWorkouts.isNotEmpty() ||
            todaySnapshot.explicitWorkoutsCount > prev.explicitWorkoutsCount ||
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
        if (foodAdded && !triggeredGoalConfettiThisCycle && !workoutAdded &&
            _celebrationQueue.value.none { it.type == CelebrationType.HEALTHY_MEAL_LOGGED }
        ) {
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
