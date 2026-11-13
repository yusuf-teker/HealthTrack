package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import com.yusufteker.healthtrack.core.utils.formatDisplayDateTr
import com.yusufteker.healthtrack.core.utils.formatWeekRangeTr
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Generates personalized Daily and Weekly AI Health, Nutrition & Workout Reports.
 */
@Suppress("UNUSED_PARAMETER")
class HealthReportAiGenerator(
    private val apiClient: HealthApiClient
) {

    suspend fun generateDailyReport(
        userId: Int,
        dateIso: String,
        foodLogs: List<FoodLog>,
        workoutLogs: List<WorkoutLog>,
        weights: List<WeightEntry>,
        goal: NutritionGoal,
        fitnessGoal: FitnessGoal,
        userWeightKg: Double,
        targetWeightKg: Double,
        language: AppLanguage = LocalizationManager.currentLanguage
    ): HealthAiReport {
        val dayFoods = foodLogs.filter { it.date == dateIso && !it.isDeleted }
        val dayWorkouts = workoutLogs.filter { it.date == dateIso && !it.isDeleted }
        val allFoodItems = dayFoods.flatMap { it.items }

        val caloriesIntake = dayFoods.sumOf { it.totalCalories }
        val caloriesBurned = dayWorkouts.sumOf { it.caloriesBurned }
        val workoutMinutes = dayWorkouts.sumOf { it.durationMinutes }
        val netCalories = caloriesIntake - caloriesBurned

        val proteinG = roundOne(allFoodItems.sumOf { it.proteinG })
        val carbsG = roundOne(allFoodItems.sumOf { it.carbsG })
        val fatG = roundOne(allFoodItems.sumOf { it.fatG })
        val fiberG = roundOne(allFoodItems.sumOf { it.fiberG })

        val allMicros = allFoodItems.flatMap { it.micronutrients }
        val vitCMg = allMicros.filter { it.name.contains("C Vit", ignoreCase = true) || it.name.contains("Vitamin C", ignoreCase = true) }.sumOf { it.amount }
        val vitDUg = allMicros.filter { it.name.contains("D Vit", ignoreCase = true) || it.name.contains("Vitamin D", ignoreCase = true) }.sumOf { it.amount }
        val magnesiumMg = allMicros.filter { it.name.contains("Magnezyum", ignoreCase = true) || it.name.contains("Magnesium", ignoreCase = true) }.sumOf { it.amount }

        val exactWeight = weights.filter { !it.isDeleted && it.date <= dateIso }
            .maxByOrNull { it.date }?.weightKg ?: userWeightKg
        val targetProteinG = calculateProteinTarget(exactWeight, fitnessGoal)
        val targetCalories = goal.calories.coerceAtLeast(1200)

        // Score calculation (0..100)
        val calRatio = if (targetCalories > 0) caloriesIntake.toDouble() / targetCalories else 0.0
        val calScore = when {
            caloriesIntake == 0 -> 15
            fitnessGoal == FitnessGoal.LOSE_WEIGHT && netCalories in (targetCalories - 650)..(targetCalories + 50) -> 35
            fitnessGoal == FitnessGoal.BUILD_MUSCLE && caloriesIntake in (targetCalories - 150)..(targetCalories + 400) -> 35
            abs(calRatio - 1.0) <= 0.15 -> 35
            abs(calRatio - 1.0) <= 0.30 -> 24
            else -> 14
        }

        val proteinRatio = if (targetProteinG > 0) (proteinG / targetProteinG).coerceIn(0.0, 1.3) else 0.0
        val proteinScore = (proteinRatio.coerceAtMost(1.0) * 35.0).roundToInt()

        val workoutScore = when {
            workoutMinutes >= 30 || caloriesBurned >= 200 -> 20
            workoutMinutes > 0 || caloriesBurned > 0 -> 14
            else -> 6
        }

        val fiberAndMicroScore = (
            (if (fiberG >= goal.fiberG * 0.7) 5 else 2) +
                (if (vitCMg >= goal.vitaminCMg * 0.5 || magnesiumMg >= goal.magnesiumMg * 0.5) 5 else 2)
            )

        val totalScore = (calScore + proteinScore + workoutScore + fiberAndMicroScore).coerceIn(10, 100)

        val highlights = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        val goalTitle = fitnessGoal.localizedTitle(language)

        if (proteinG >= targetProteinG * 0.9) {
            highlights.add(
                trEn(
                    "Günlük protein hedefinizi (${proteinG.toInt()}g / ${targetProteinG.toInt()}g) başarıyla yakaladınız ($goalTitle hedefiyle tam uyumlu).",
                    "You successfully hit your daily protein target (${proteinG.toInt()}g / ${targetProteinG.toInt()}g), aligned with your $goalTitle goal.",
                    language
                )
            )
        } else {
            val missingProtein = (targetProteinG - proteinG).roundToInt().coerceAtLeast(5)
            warnings.add(
                trEn(
                    "Protein alımı hedefin ${missingProtein}g altında kaldı (${proteinG.toInt()}g / ${targetProteinG.toInt()}g).",
                    "Protein intake was ${missingProtein}g below target (${proteinG.toInt()}g / ${targetProteinG.toInt()}g).",
                    language
                )
            )
            recommendations.add(
                trEn(
                    "Kas dokusunu korumak için akşam/ara öğüne 150g tavuk göğsü, süzme yoğurt veya 1 ölçek whey protein (+25-30g protein) ekleyebilirsiniz.",
                    "To protect muscle mass, add 150g chicken breast, Greek yogurt, or 1 scoop of whey protein (+25–30g protein) to your evening meal or snack.",
                    language
                )
            )
        }

        if (dayWorkouts.isNotEmpty()) {
            highlights.add(
                trEn(
                    "Bugün ${dayWorkouts.size} antrenman ile toplam $workoutMinutes dk aktif kaldınız ve $caloriesBurned kcal yaktınız.",
                    "You stayed active with ${dayWorkouts.size} workout(s) totaling $workoutMinutes min and burned $caloriesBurned kcal today.",
                    language
                )
            )
        } else {
            warnings.add(
                trEn(
                    "Bugün için kayıtlı bir egzersiz veya yürüyüş bulunmuyor.",
                    "No exercise or walk has been logged for today yet.",
                    language
                )
            )
            recommendations.add(
                trEn(
                    "Metabolik hızı desteklemek için en az 25-30 dakikalık tempolu yürüyüş veya mobilite egzersizi ekleyin.",
                    "Add at least a 25–30 minute brisk walk or mobility session to support your metabolic rate.",
                    language
                )
            )
        }

        when (fitnessGoal) {
            FitnessGoal.LOSE_WEIGHT -> {
                if (caloriesIntake > 0 && netCalories <= targetCalories) {
                    highlights.add(
                        trEn(
                            "Yağ yakımı için ideal net kalori dengesinde ($netCalories kcal) kaldınız.",
                            "You stayed in an ideal net calorie balance ($netCalories kcal) for fat loss.",
                            language
                        )
                    )
                } else if (netCalories > targetCalories + 200) {
                    warnings.add(
                        trEn(
                            "Net kalori ($netCalories kcal), kilo verme hedefinizin ($targetCalories kcal) üzerinde.",
                            "Net calories ($netCalories kcal) exceeded your fat-loss target ($targetCalories kcal).",
                            language
                        )
                    )
                    recommendations.add(
                        trEn(
                            "Yarın karbonhidrat kaynaklarını kompleks lifli sebzelerle dengeleyerek 300 kcal açık oluşturun.",
                            "Tomorrow, balance carb sources with fibrous vegetables to create a 300 kcal deficit.",
                            language
                        )
                    )
                }
            }
            FitnessGoal.BUILD_MUSCLE -> {
                if (caloriesIntake >= targetCalories * 0.9 && proteinG >= targetProteinG * 0.85) {
                    highlights.add(
                        trEn(
                            "Hipertrofi ve kas inşası için yeterli enerji ve aminoasit havuzu sağlandı.",
                            "Sufficient energy and amino acids were supplied for hypertrophy and muscle growth.",
                            language
                        )
                    )
                } else if (caloriesIntake < targetCalories * 0.8) {
                    warnings.add(
                        trEn(
                            "Kas gelişimi için alınan kalori ($caloriesIntake kcal) hedefin altında kaldı.",
                            "Calorie intake ($caloriesIntake kcal) was below target for muscle building.",
                            language
                        )
                    )
                    recommendations.add(
                        trEn(
                            "Antrenman sonrası öğününe yulaf, muz veya fıstık ezmesi gibi temiz kalori kaynakları ekleyin.",
                            "Add clean energy sources like oats, banana, or peanut butter to your post-workout meal.",
                            language
                        )
                    )
                }
            }
            FitnessGoal.MAINTAIN -> {
                if (abs(netCalories - targetCalories) <= 250) {
                    highlights.add(
                        trEn(
                            "Form koruma dengeniz ($netCalories net kcal) hedefinizle uyumlu.",
                            "Your maintenance balance ($netCalories net kcal) aligns well with your goal.",
                            language
                        )
                    )
                }
            }
        }

        if (fiberG < goal.fiberG * 0.6) {
            warnings.add(
                trEn(
                    "Lif alımı düşük (${fiberG.toInt()}g / ${goal.fiberG.toInt()}g).",
                    "Fiber intake is low (${fiberG.toInt()}g / ${goal.fiberG.toInt()}g).",
                    language
                )
            )
            recommendations.add(
                trEn(
                    "Sindirim ve tokluk süresi için öğünlerinize yeşillik, chia/keten tohumu veya baklagil ekleyin.",
                    "Add leafy greens, chia/flax seeds, or legumes to your meals for better digestion and satiety.",
                    language
                )
            )
        } else {
            highlights.add(
                trEn(
                    "Lif tüketiminiz (${fiberG.toInt()}g) kan şekeri dengesini destekliyor.",
                    "Your fiber intake (${fiberG.toInt()}g) supports steady blood sugar levels.",
                    language
                )
            )
        }

        if (vitDUg < goal.vitaminDUg * 0.4 || magnesiumMg < goal.magnesiumMg * 0.5) {
            recommendations.add(
                trEn(
                    "Mikro besin dengesi için D vitamini ve Magnezyum (${magnesiumMg.toInt()}/${goal.magnesiumMg.toInt()} mg) seviyenizi desteklemeyi unutmayın.",
                    "Remember to support your Vitamin D and Magnesium (${magnesiumMg.toInt()}/${goal.magnesiumMg.toInt()} mg) intake for micronutrient balance.",
                    language
                )
            )
        }

        val headline = when {
            caloriesIntake == 0 && dayWorkouts.isEmpty() -> trEn("Kayıt Bekleniyor: $dateIso", "Awaiting Logs: $dateIso", language)
            totalScore >= 85 -> trEn("Mükemmel Biyotelemetri Uyumu! 🔥", "Outstanding Bio-Telemetry Alignment! 🔥", language)
            totalScore >= 70 -> trEn("Dengeli ve Hedefe Odaklı Gün 💪", "Balanced & Goal-Focused Day 💪", language)
            totalScore >= 50 -> trEn("Geliştirilebilir Günlük Performans ⚡", "Room to Improve Daily Performance ⚡", language)
            else -> trEn("Hedeflerden Sapma Var — Yarın Dengeleyelim 🎯", "Off Target Today — Let's Rebalance Tomorrow 🎯", language)
        }

        val formattedDate = formatDisplayDateTr(dateIso, language)
        val proteinPct = ((proteinG / targetProteinG.coerceAtLeast(1.0)) * 100).roundToInt()
        val baseSummary = trEn(
            "$formattedDate tarihinde toplam $caloriesIntake kcal besin aldınız ve egzersizlerle $caloriesBurned kcal yaktınız (Net: $netCalories kcal). $goalTitle hedefiniz doğrultusunda günlük protein ihtiyacınızın %$proteinPct kadarı karşılandı.",
            "On $formattedDate, you consumed $caloriesIntake kcal and burned $caloriesBurned kcal through exercise (Net: $netCalories kcal). Toward your $goalTitle goal, $proteinPct% of your daily protein target was met.",
            language
        )

        return HealthAiReport(
            id = "report_daily_${userId}_$dateIso",
            userId = userId,
            periodType = AnalysisPeriodType.DAILY,
            startDate = dateIso,
            endDate = dateIso,
            periodLabel = formattedDate,
            score = totalScore,
            headline = headline,
            summary = baseSummary,
            highlights = highlights,
            warnings = warnings,
            recommendations = recommendations,
            avgCaloriesIntake = caloriesIntake,
            targetCalories = targetCalories,
            totalCaloriesBurned = caloriesBurned,
            totalWorkoutMinutes = workoutMinutes,
            workoutCount = dayWorkouts.size,
            avgProteinG = proteinG,
            targetProteinG = targetProteinG,
            avgCarbsG = carbsG,
            avgFatG = fatG,
            avgFiberG = fiberG,
            startWeightKg = exactWeight,
            endWeightKg = exactWeight,
            targetWeightKg = targetWeightKg,
            loggedDaysCount = if (dayFoods.isNotEmpty() || dayWorkouts.isNotEmpty()) 1 else 0,
            isAutoGenerated = false,
            createdAt = currentTimeMillis()
        )
    }

    suspend fun generateWeeklyReport(
        userId: Int,
        mondayIso: String,
        sundayIso: String,
        foodLogs: List<FoodLog>,
        workoutLogs: List<WorkoutLog>,
        weights: List<WeightEntry>,
        goal: NutritionGoal,
        fitnessGoal: FitnessGoal,
        userWeightKg: Double,
        targetWeightKg: Double,
        isAutoGenerated: Boolean = false,
        language: AppLanguage = LocalizationManager.currentLanguage
    ): HealthAiReport {
        val weekFoods = foodLogs.filter { !it.isDeleted && it.date in mondayIso..sundayIso }
        val weekWorkouts = workoutLogs.filter { !it.isDeleted && it.date in mondayIso..sundayIso }
        val weekWeights = weights.filter { !it.isDeleted && it.date in mondayIso..sundayIso }.sortedBy { it.date }

        val activeFoodDays = weekFoods.filter { it.totalCalories > 0 }.map { it.date }.distinct()
        val divisor = activeFoodDays.size.coerceAtLeast(1)

        val totalCaloriesIntake = weekFoods.sumOf { it.totalCalories }
        val avgCaloriesIntake = (totalCaloriesIntake.toDouble() / divisor).roundToInt()

        val totalCaloriesBurned = weekWorkouts.sumOf { it.caloriesBurned }
        val totalWorkoutMinutes = weekWorkouts.sumOf { it.durationMinutes }
        val workoutCount = weekWorkouts.size

        val allItems = weekFoods.flatMap { it.items }
        val avgProteinG = roundOne(allItems.sumOf { it.proteinG } / divisor)
        val avgCarbsG = roundOne(allItems.sumOf { it.carbsG } / divisor)
        val avgFatG = roundOne(allItems.sumOf { it.fatG } / divisor)
        val avgFiberG = roundOne(allItems.sumOf { it.fiberG } / divisor)

        val startWeight = weekWeights.firstOrNull()?.weightKg
            ?: weights.filter { !it.isDeleted && it.date <= mondayIso }.maxByOrNull { it.date }?.weightKg
            ?: userWeightKg
        val endWeight = weekWeights.lastOrNull()?.weightKg
            ?: weights.filter { !it.isDeleted && it.date <= sundayIso }.maxByOrNull { it.date }?.weightKg
            ?: userWeightKg

        val weightDelta = roundOne(endWeight - startWeight)
        val targetProteinG = calculateProteinTarget(endWeight, fitnessGoal)
        val targetCalories = goal.calories.coerceAtLeast(1200)

        val consistencyScore = ((activeFoodDays.size / 7.0) * 25.0).roundToInt().coerceIn(5, 25)
        val proteinRatio = (avgProteinG / targetProteinG.coerceAtLeast(1.0)).coerceIn(0.0, 1.1)
        val proteinScore = (proteinRatio.coerceAtMost(1.0) * 30.0).roundToInt()
        val workoutScore = when {
            workoutCount >= 4 || totalWorkoutMinutes >= 150 -> 25
            workoutCount >= 2 || totalWorkoutMinutes >= 75 -> 18
            workoutCount >= 1 -> 12
            else -> 4
        }
        val calDiffRatio = if (targetCalories > 0) abs(avgCaloriesIntake - targetCalories).toDouble() / targetCalories else 0.5
        val calorieScore = when {
            activeFoodDays.isEmpty() -> 5
            calDiffRatio <= 0.12 -> 20
            calDiffRatio <= 0.25 -> 14
            else -> 8
        }

        val totalScore = (consistencyScore + proteinScore + workoutScore + calorieScore).coerceIn(12, 100)

        val highlights = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        val goalTitle = fitnessGoal.localizedTitle(language)

        if (activeFoodDays.size >= 5) {
            highlights.add(
                trEn(
                    "Haftanın ${activeFoodDays.size}/7 günü düzenli beslenme kaydı tutarak yüksek disiplin gösterdiniz.",
                    "Great consistency! You logged nutrition on ${activeFoodDays.size}/7 days this week.",
                    language
                )
            )
        } else {
            warnings.add(
                trEn(
                    "Bu hafta yalnızca ${activeFoodDays.size}/7 gün beslenme kaydı girildi.",
                    "Nutrition was only logged on ${activeFoodDays.size}/7 days this week.",
                    language
                )
            )
            recommendations.add(
                trEn(
                    "AI analizinin doğruluğu için haftanın en az 6 günü öğünlerinizi kaydetmeye özen gösterin.",
                    "Aim to log meals at least 6 days a week for sharper AI trend accuracy.",
                    language
                )
            )
        }

        val weeklyProtPct = ((avgProteinG / targetProteinG.coerceAtLeast(1.0)) * 100).roundToInt()
        if (avgProteinG >= targetProteinG * 0.88) {
            highlights.add(
                trEn(
                    "Haftalık ortalama protein alımınız (${avgProteinG.toInt()}g/gün), $goalTitle hedefinizle (%$weeklyProtPct) uyumlu.",
                    "Your weekly average protein (${avgProteinG.toInt()}g/day) aligns with your $goalTitle goal ($weeklyProtPct%).",
                    language
                )
            )
        } else {
            warnings.add(
                trEn(
                    "Haftalık ortalama protein (${avgProteinG.toInt()}g/gün), hedefinizin (${targetProteinG.toInt()}g) altında kaldı.",
                    "Weekly average protein (${avgProteinG.toInt()}g/day) fell short of your ${targetProteinG.toInt()}g target.",
                    language
                )
            )
            recommendations.add(
                trEn(
                    "Yeni haftada kahvaltı ve antrenman sonrası öğünlerinize 20-25g ekstra protein kaynağı sabitleyin.",
                    "Next week, anchor an extra 20–25g protein source at breakfast and post-workout.",
                    language
                )
            )
        }

        if (workoutCount >= 3 || totalWorkoutMinutes >= 120) {
            highlights.add(
                trEn(
                    "Hafta boyunca $workoutCount antrenman ile toplam $totalWorkoutMinutes dk spor yaptınız ve $totalCaloriesBurned kcal yaktınız.",
                    "Across $workoutCount workout(s), you trained for $totalWorkoutMinutes min and burned $totalCaloriesBurned kcal this week.",
                    language
                )
            )
        } else {
            warnings.add(
                trEn(
                    "Haftalık egzersiz hacmi ($workoutCount antrenman, $totalWorkoutMinutes dk) ideal seviyenin (150 dk/hafta) altında.",
                    "Weekly exercise volume ($workoutCount workouts, $totalWorkoutMinutes min) is below the 150 min/week benchmark.",
                    language
                )
            )
            recommendations.add(
                trEn(
                    "Önümüzdeki hafta en az 3 gün 40-45 dakikalık antrenman veya tempolu yürüyüş planlayın.",
                    "Schedule at least 3 days of 40–45 minute workouts or brisk walks next week.",
                    language
                )
            )
        }

        when (fitnessGoal) {
            FitnessGoal.LOSE_WEIGHT -> {
                if (weightDelta < 0.0) {
                    highlights.add(
                        trEn(
                            "Kilo verme hedefiniz doğrultusunda bu hafta ${abs(weightDelta)} kg düşüş sağladınız ($startWeight → $endWeight kg).",
                            "Great progress toward fat loss: you dropped ${abs(weightDelta)} kg this week ($startWeight → $endWeight kg).",
                            language
                        )
                    )
                } else if (weightDelta > 0.3) {
                    warnings.add(
                        trEn(
                            "Kilo verme hedefinde olmanıza rağmen bu hafta +$weightDelta kg artış gözlemlendi.",
                            "Weight increased by +$weightDelta kg this week despite a fat-loss goal.",
                            language
                        )
                    )
                    recommendations.add(
                        trEn(
                            "Akşam saatlerindeki sodyum/karbonhidrat tüketimini azaltıp günlük 350 kcal net açık hedefleyin.",
                            "Trim late-evening sodium and refined carbs and aim for a 350 kcal daily net deficit.",
                            language
                        )
                    )
                }
            }
            FitnessGoal.BUILD_MUSCLE -> {
                if (weightDelta >= 0.0 && avgProteinG >= targetProteinG * 0.85) {
                    highlights.add(
                        trEn(
                            "Kas inşası sürecinde vücut kütleniz ($endWeight kg) ve protein sentez dengeniz olumlu ilerliyor.",
                            "Body mass ($endWeight kg) and protein synthesis are trending positively for muscle gain.",
                            language
                        )
                    )
                } else if (weightDelta < -0.5) {
                    warnings.add(
                        trEn(
                            "Kaslanma döneminde haftalık ${abs(weightDelta)} kg kayıp yaşandı; kalori açığı oluşmuş olabilir.",
                            "You lost ${abs(weightDelta)} kg during a muscle-gain phase; you may be in a calorie deficit.",
                            language
                        )
                    )
                    recommendations.add(
                        trEn(
                            "Günlük kalori alımınızı +250-300 kcal artırarak kas kaybını önleyin.",
                            "Increase daily intake by +250–300 kcal to prevent lean mass loss.",
                            language
                        )
                    )
                }
            }
            FitnessGoal.MAINTAIN -> {
                if (abs(weightDelta) <= 0.4) {
                    highlights.add(
                        trEn(
                            "Haftalık kilo dengeniz ($endWeight kg, değişim: $weightDelta kg) koruma hedefinizle tam uyumlu.",
                            "Your weekly weight stability ($endWeight kg, delta: $weightDelta kg) matches your maintenance target.",
                            language
                        )
                    )
                }
            }
        }

        val periodLabel = formatWeekRangeTr(mondayIso, sundayIso, language)
        val headline = when {
            activeFoodDays.isEmpty() && weekWorkouts.isEmpty() -> trEn(
                "$periodLabel Haftası İçin Kayıt Yok",
                "No Logs for Week of $periodLabel",
                language
            )
            totalScore >= 85 -> trEn("Şampiyon Haftası! Hedeflerle Tam Uyum 🏆", "Champion Week! Full Target Alignment 🏆", language)
            totalScore >= 70 -> trEn("İstikrarlı ve Güçlü Haftalık Performans 💪", "Consistent & Strong Weekly Performance 💪", language)
            totalScore >= 50 -> trEn("Dengeli Hafta — Yeni Haftada Vites Artıralım ⚡", "Balanced Week — Let's Level Up Next Week ⚡", language)
            else -> trEn("Kayıt ve Makro Disiplinini Artırma Zamanı 🎯", "Time to Boost Logging & Macro Discipline 🎯", language)
        }

        val deltaText = if (weightDelta >= 0) "+$weightDelta" else "$weightDelta"
        val summary = trEn(
            "$periodLabel haftasında ${activeFoodDays.size} gün beslenme ve $workoutCount antrenman kaydedildi. Günlük ortalama $avgCaloriesIntake kcal alındı, toplam $totalCaloriesBurned kcal egzersizle yakıldı. Kilo trendi: $startWeight kg → $endWeight kg ($deltaText kg).",
            "During $periodLabel, ${activeFoodDays.size} nutrition day(s) and $workoutCount workout(s) were logged. Daily average intake was $avgCaloriesIntake kcal with $totalCaloriesBurned kcal burned through exercise. Weight trend: $startWeight kg → $endWeight kg ($deltaText kg).",
            language
        )

        return HealthAiReport(
            id = "report_weekly_${userId}_$mondayIso",
            userId = userId,
            periodType = AnalysisPeriodType.WEEKLY,
            startDate = mondayIso,
            endDate = sundayIso,
            periodLabel = periodLabel,
            score = totalScore,
            headline = headline,
            summary = summary,
            highlights = highlights,
            warnings = warnings,
            recommendations = recommendations,
            avgCaloriesIntake = avgCaloriesIntake,
            targetCalories = targetCalories,
            totalCaloriesBurned = totalCaloriesBurned,
            totalWorkoutMinutes = totalWorkoutMinutes,
            workoutCount = workoutCount,
            avgProteinG = avgProteinG,
            targetProteinG = targetProteinG,
            avgCarbsG = avgCarbsG,
            avgFatG = avgFatG,
            avgFiberG = avgFiberG,
            startWeightKg = startWeight,
            endWeightKg = endWeight,
            targetWeightKg = targetWeightKg,
            loggedDaysCount = activeFoodDays.size,
            isAutoGenerated = isAutoGenerated,
            createdAt = currentTimeMillis()
        )
    }

    private fun roundOne(v: Double): Double = (v * 10.0).roundToInt() / 10.0
}
