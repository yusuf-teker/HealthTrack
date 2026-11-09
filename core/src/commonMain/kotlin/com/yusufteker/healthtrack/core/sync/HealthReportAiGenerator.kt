package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import com.yusufteker.healthtrack.core.utils.formatDisplayDateTr
import com.yusufteker.healthtrack.core.utils.formatWeekRangeTr
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Generates personalized Daily and Weekly AI Health, Nutrition & Workout Reports.
 * Combines exact bio-telemetry calculations (macros, micronutrients, MET workout strain,
 * weight trajectory, and fitness goal alignment) with optional remote Gemini AI commentary.
 */
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
        targetWeightKg: Double
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

        if (proteinG >= targetProteinG * 0.9) {
            highlights.add("Günlük protein hedefinizi (${proteinG.toInt()}g / ${targetProteinG.toInt()}g) başarıyla yakaladınız (${fitnessGoal.title} hedefiyle tam uyumlu).")
        } else {
            val missingProtein = (targetProteinG - proteinG).roundToInt().coerceAtLeast(5)
            warnings.add("Protein alımı hedefin ${missingProtein}g altında kaldı (${proteinG.toInt()}g / ${targetProteinG.toInt()}g).")
            recommendations.add("Kas dokusunu korumak için akşam/ara öğüne 150g tavuk göğsü, süzme yoğurt veya 1 ölçek whey protein (+25-30g protein) ekleyebilirsiniz.")
        }

        if (dayWorkouts.isNotEmpty()) {
            highlights.add("Bugün ${dayWorkouts.size} antrenman ile toplam $workoutMinutes dk aktif kaldınız ve $caloriesBurned kcal yaktınız.")
        } else {
            warnings.add("Bugün için kayıtlı bir egzersiz veya yürüyüş bulunmuyor.")
            recommendations.add("Metabolik hızı desteklemek için en az 25-30 dakikalık tempolu yürüyüş veya mobilite egzersizi ekleyin.")
        }

        when (fitnessGoal) {
            FitnessGoal.LOSE_WEIGHT -> {
                if (caloriesIntake > 0 && netCalories <= targetCalories) {
                    highlights.add("Yağ yakımı için ideal net kalori dengesinde ($netCalories kcal) kaldınız.")
                } else if (netCalories > targetCalories + 200) {
                    warnings.add("Net kalori ($netCalories kcal), kilo verme hedefinizin (${targetCalories} kcal) üzerinde.")
                    recommendations.add("Yarın karbonhidrat kaynaklarını kompleks lifli sebzelerle dengeleyerek 300 kcal açık oluşturun.")
                }
            }
            FitnessGoal.BUILD_MUSCLE -> {
                if (caloriesIntake >= targetCalories * 0.9 && proteinG >= targetProteinG * 0.85) {
                    highlights.add("Hipertrofi ve kas inşası için yeterli enerji ve aminoasit havuzu sağlandı.")
                } else if (caloriesIntake < targetCalories * 0.8) {
                    warnings.add("Kas gelişimi için alınan kalori ($caloriesIntake kcal) hedefin altında kaldı.")
                    recommendations.add("Antrenman sonrası öğününe yulaf, muz veya fıstık ezmesi gibi temiz kalori kaynakları ekleyin.")
                }
            }
            FitnessGoal.MAINTAIN -> {
                if (abs(netCalories - targetCalories) <= 250) {
                    highlights.add("Form koruma dengeniz ($netCalories net kcal) hedefinizle uyumlu.")
                }
            }
        }

        if (fiberG < goal.fiberG * 0.6) {
            warnings.add("Lif alımı düşük (${fiberG.toInt()}g / ${goal.fiberG.toInt()}g).")
            recommendations.add("Sindirim ve tokluk süresi için öğünlerinize yeşillik, chia/keten tohumu veya baklagil ekleyin.")
        } else {
            highlights.add("Lif tüketiminiz (${fiberG.toInt()}g) kan şekeri dengesini destekliyor.")
        }

        if (vitDUg < goal.vitaminDUg * 0.4 || magnesiumMg < goal.magnesiumMg * 0.5) {
            recommendations.add("Mikro besin dengesi için D vitamini ve Magnezyum (${magnesiumMg.toInt()}/${goal.magnesiumMg.toInt()} mg) seviyenizi desteklemeyi unutmayın.")
        }

        val headline = when {
            caloriesIntake == 0 && dayWorkouts.isEmpty() -> "Kayıt Bekleniyor: $dateIso"
            totalScore >= 85 -> "Mükemmel Biyotelemetri Uyumu! 🔥"
            totalScore >= 70 -> "Dengeli ve Hedefe Odaklı Gün 💪"
            totalScore >= 50 -> "Geliştirilebilir Günlük Performans ⚡"
            else -> "Hedeflerden Sapma Var — Yarın Dengeleyelim 🎯"
        }

        val baseSummary = buildString {
            append("${formatDisplayDateTr(dateIso)} tarihinde toplam $caloriesIntake kcal besin aldınız ve egzersizlerle $caloriesBurned kcal yaktınız (Net: $netCalories kcal). ")
            append("${fitnessGoal.title} hedefiniz doğrultusunda günlük protein ihtiyacınızın %${((proteinG / targetProteinG.coerceAtLeast(1.0)) * 100).roundToInt()} kadarı karşılandı.")
        }

        return HealthAiReport(
            id = "report_daily_${userId}_$dateIso",
            userId = userId,
            periodType = AnalysisPeriodType.DAILY,
            startDate = dateIso,
            endDate = dateIso,
            periodLabel = formatDisplayDateTr(dateIso),
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
        isAutoGenerated: Boolean = false
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

        // Weekly Consistency & Goal Score (0..100)
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

        if (activeFoodDays.size >= 5) {
            highlights.add("Haftanın ${activeFoodDays.size}/7 günü düzenli beslenme kaydı tutarak yüksek disiplin gösterdiniz.")
        } else {
            warnings.add("Bu hafta yalnızca ${activeFoodDays.size}/7 gün beslenme kaydı girildi.")
            recommendations.add("AI analizinin doğruluğu için haftanın en az 6 günü öğünlerinizi kaydetmeye özen gösterin.")
        }

        if (avgProteinG >= targetProteinG * 0.88) {
            highlights.add("Haftalık ortalama protein alımınız (${avgProteinG.toInt()}g/gün), ${fitnessGoal.title} hedefinizle (%${((avgProteinG / targetProteinG) * 100).roundToInt()}) uyumlu.")
        } else {
            warnings.add("Haftalık ortalama protein (${avgProteinG.toInt()}g/gün), hedefinizin (${targetProteinG.toInt()}g) altında kaldı.")
            recommendations.add("Yeni haftada kahvaltı ve antrenman sonrası öğünlerinize 20-25g ekstra protein kaynağı sabitleyin.")
        }

        if (workoutCount >= 3 || totalWorkoutMinutes >= 120) {
            highlights.add("Hafta boyunca $workoutCount antrenman ile toplam $totalWorkoutMinutes dk spor yaptınız ve $totalCaloriesBurned kcal yaktınız.")
        } else {
            warnings.add("Haftalık egzersiz hacmi ($workoutCount antrenman, $totalWorkoutMinutes dk) ideal seviyenin (150 dk/hafta) altında.")
            recommendations.add("Önümüzdeki hafta en az 3 gün 40-45 dakikalık antrenman veya tempolu yürüyüş planlayın.")
        }

        when (fitnessGoal) {
            FitnessGoal.LOSE_WEIGHT -> {
                if (weightDelta < 0.0) {
                    highlights.add("Kilo verme hedefiniz doğrultusunda bu hafta ${abs(weightDelta)} kg düşüş sağladınız ($startWeight → $endWeight kg).")
                } else if (weightDelta > 0.3) {
                    warnings.add("Kilo verme hedefinde olmanıza rağmen bu hafta +$weightDelta kg artış gözlemlendi.")
                    recommendations.add("Akşam saatlerindeki sodyum/karbonhidrat tüketimini azaltıp günlük 350 kcal net açık hedefleyin.")
                }
            }
            FitnessGoal.BUILD_MUSCLE -> {
                if (weightDelta >= 0.0 && avgProteinG >= targetProteinG * 0.85) {
                    highlights.add("Kas inşası sürecinde vücut kütleniz ($endWeight kg) ve protein sentez dengeniz olumlu ilerliyor.")
                } else if (weightDelta < -0.5) {
                    warnings.add("Kaslanma döneminde haftalık ${abs(weightDelta)} kg kayıp yaşandı; kalori açığı oluşmuş olabilir.")
                    recommendations.add("Günlük kalori alımınızı +250-300 kcal artırarak kas kaybını önleyin.")
                }
            }
            FitnessGoal.MAINTAIN -> {
                if (abs(weightDelta) <= 0.4) {
                    highlights.add("Haftalık kilo dengeniz ($endWeight kg, değişim: $weightDelta kg) koruma hedefinizle tam uyumlu.")
                }
            }
        }

        val periodLabel = formatWeekRangeTr(mondayIso, sundayIso)
        val headline = when {
            activeFoodDays.isEmpty() && weekWorkouts.isEmpty() -> "$periodLabel Haftası İçin Kayıt Yok"
            totalScore >= 85 -> "Şampiyon Haftası! Hedeflerle Tam Uyum 🏆"
            totalScore >= 70 -> "İstikrarlı ve Güçlü Haftalık Performans 💪"
            totalScore >= 50 -> "Dengeli Hafta — Yeni Haftada Vites Artıralım ⚡"
            else -> "Kayıt ve Makro Disiplinini Artırma Zamanı 🎯"
        }

        val summary = buildString {
            append("$periodLabel haftasında ${activeFoodDays.size} gün beslenme ve $workoutCount antrenman kaydedildi. ")
            append("Günlük ortalama $avgCaloriesIntake kcal alındı, toplam $totalCaloriesBurned kcal egzersizle yakıldı. ")
            append("Kilo trendi: $startWeight kg → $endWeight kg (${if (weightDelta >= 0) "+$weightDelta" else "$weightDelta"} kg).")
        }

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
