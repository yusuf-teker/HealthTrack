package com.yusufteker.healthtrack.core.utils

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.trEn
import kotlin.math.abs
import kotlin.math.roundToInt

enum class MetricRangeStatus {
    LOW,
    NORMAL,
    HIGH,
    VERY_HIGH;

    fun label(lang: AppLanguage): String = when (this) {
        LOW -> trEn("Düşük", "Low", lang)
        NORMAL -> trEn("İdeal", "Ideal", lang)
        HIGH -> trEn("Yüksek", "High", lang)
        VERY_HIGH -> trEn("Çok Yüksek", "Very High", lang)
    }
}

enum class ZeppBodyType(val titleTr: String, val titleEn: String) {
    OBESE("Obez", "Obese"),
    OVERWEIGHT("Fazla Kilolu", "Overweight"),
    THICK_SET("İri Yapılı", "Thick-set"),
    LACK_EXERCISE("Düşük Kaslı", "Lack Exercise"),
    BALANCED("Dengeli", "Balanced"),
    BALANCED_MUSCULAR("Kaslı & Dengeli", "Balanced Muscular"),
    SKINNY("Zayıf", "Skinny"),
    BALANCED_SKINNY("İnce & Dengeli", "Balanced Skinny"),
    SKINNY_MUSCULAR("Atletik & Kaslı", "Skinny Muscular");

    fun label(lang: AppLanguage): String = trEn(titleTr, titleEn, lang)
}

/**
 * Comprehensive Zepp Life / Xiaomi Mi Scale 2 compatible body composition report.
 * Computed from weight + user anthropometrics (height, age, sex) and optional BIA metrics / impedance.
 */
data class FullBodyCompositionReport(
    val weightKg: Double,
    val heightCm: Double,
    val age: Int,
    val isMale: Boolean,
    val impedanceOhm: Int? = null,
    val bmi: Double,
    val bmiStatus: MetricRangeStatus,
    val bodyScore: Int,
    val bodyType: ZeppBodyType,
    val bodyFatPercent: Double?,
    val fatMassKg: Double?,
    val bodyFatStatus: MetricRangeStatus?,
    val muscleMassKg: Double?,
    val musclePercent: Double?,
    val muscleStatus: MetricRangeStatus?,
    val waterPercent: Double?,
    val waterStatus: MetricRangeStatus?,
    val boneMassKg: Double?,
    val boneStatus: MetricRangeStatus?,
    val leanBodyMassKg: Double?,
    val proteinPercent: Double?,
    val proteinStatus: MetricRangeStatus?,
    val visceralFat: Int,
    val visceralFatStatus: MetricRangeStatus,
    val bmrKcal: Int,
    val bodyAge: Int,
    val idealWeightKg: Double,
    val weightDeltaFromIdealKg: Double
)

object FullBodyCompositionAnalytics {

    fun calculate(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        isMale: Boolean,
        bodyFatPercent: Double? = null,
        muscleMassKg: Double? = null,
        musclePercent: Double? = null,
        impedanceOhm: Int? = null
    ): FullBodyCompositionReport? {
        if (weightKg <= 0.0 || heightCm <= 50.0) return null

        val h = heightCm.coerceIn(100.0, 220.0)
        val a = age.coerceIn(10, 99)
        val hM = h / 100.0

        // 1. BMI (VKİ)
        val bmi = round1((weightKg / (hM * hM)).coerceIn(10.0, 90.0))
        val bmiStatus = when {
            bmi < 18.5 -> MetricRangeStatus.LOW
            bmi < 25.0 -> MetricRangeStatus.NORMAL
            bmi < 30.0 -> MetricRangeStatus.HIGH
            else -> MetricRangeStatus.VERY_HIGH
        }

        // 2. Resolve BIA core values (use exact Xiaomi BIA if impedance provided and fat/muscle absent)
        val biaFromImpedance = impedanceOhm?.takeIf { it in 100..2500 }?.let { ohm ->
            XiaomiScaleBleParser.computeXiaomiBia(
                weightKg = weightKg,
                heightCm = h,
                age = a,
                isMale = isMale,
                impedanceOhm = ohm
            )
        }

        val resolved = BodyCompositionCalculator.resolveFromDevice(
            weightKg = weightKg,
            bodyFatPercent = bodyFatPercent ?: biaFromImpedance?.bodyFatPercent,
            leanBodyMassKg = biaFromImpedance?.leanBodyMassKg,
            boneMassKg = biaFromImpedance?.boneMassKg,
            muscleMassKg = muscleMassKg ?: biaFromImpedance?.muscleMassKg,
            musclePercent = musclePercent ?: biaFromImpedance?.musclePercent,
            isMale = isMale
        )

        val fatPct = resolved.bodyFatPercent
        val fatKg = fatPct?.let { round1(weightKg * it / 100.0) }
        val lbmKg = resolved.leanBodyMassKg
        val mKg = resolved.muscleMassKg
        val mPct = resolved.musclePercent
        val boneKg = resolved.boneMassKg ?: lbmKg?.let {
            round1((weightKg * (if (isMale) 0.042 else 0.036)).coerceIn(1.8, 4.5))
        }

        // 3. Body Fat Status (Zepp Life sex & age adjusted brackets)
        val fatNormalLow = if (isMale) 11.0 else 21.0
        val fatNormalHigh = when {
            isMale && a < 40 -> 21.0
            isMale -> 23.0
            !isMale && a < 40 -> 31.0
            else -> 33.0
        }
        val fatVeryHigh = if (isMale) 26.0 else 36.0
        val bodyFatStatus = fatPct?.let {
            when {
                it < fatNormalLow -> MetricRangeStatus.LOW
                it <= fatNormalHigh -> MetricRangeStatus.NORMAL
                it <= fatVeryHigh -> MetricRangeStatus.HIGH
                else -> MetricRangeStatus.VERY_HIGH
            }
        }

        // 4. Muscle Status (Zepp Life height & sex reference thresholds)
        val (muscleLow, muscleHigh) = when {
            isMale && h < 160.0 -> 38.5 to 46.5
            isMale && h < 170.0 -> 44.0 to 52.4
            isMale -> 49.4 to 59.4
            !isMale && h < 150.0 -> 29.1 to 34.7
            !isMale && h < 160.0 -> 32.9 to 37.5
            else -> 36.5 to 42.5
        }
        val muscleStatus = mKg?.let {
            when {
                it < muscleLow -> MetricRangeStatus.LOW
                it <= muscleHigh -> MetricRangeStatus.NORMAL
                else -> MetricRangeStatus.HIGH
            }
        }

        // 5. Water Percentage (Exact Zepp Life / Mi Scale 2 formula)
        val waterPct = fatPct?.let { fat ->
            val rawWater = (100.0 - fat) * 0.7
            val coeff = if (rawWater <= 50.0) 1.02 else 0.98
            val adjusted = rawWater * coeff
            val capped = if (adjusted >= 65.0) 75.0 else adjusted
            round1(capped.coerceIn(35.0, 75.0))
        }
        val waterLow = if (isMale) 55.0 else 45.0
        val waterHigh = if (isMale) 65.0 else 60.0
        val waterStatus = waterPct?.let {
            when {
                it < waterLow -> MetricRangeStatus.LOW
                it <= waterHigh -> MetricRangeStatus.NORMAL
                else -> MetricRangeStatus.HIGH
            }
        }

        // 6. Bone Mass Status (Zepp Life weight & sex thresholds)
        val boneLow = when {
            isMale && weightKg < 60.0 -> 2.5
            isMale && weightKg <= 75.0 -> 2.9
            isMale -> 3.2
            !isMale && weightKg < 45.0 -> 1.8
            !isMale && weightKg <= 60.0 -> 2.2
            else -> 2.5
        }
        val boneStatus = boneKg?.let {
            when {
                it < boneLow -> MetricRangeStatus.LOW
                else -> MetricRangeStatus.NORMAL
            }
        }

        // 7. Protein Percentage (Exact Zepp Life getProteinPercentage formula)
        val proteinPct = if (mKg != null && waterPct != null) {
            val rawProtein = ((mKg / weightKg) * 100.0) - waterPct
            round1(rawProtein.coerceIn(5.0, 32.0))
        } else null
        val proteinStatus = proteinPct?.let {
            when {
                it < 16.0 -> MetricRangeStatus.LOW
                it <= 20.0 -> MetricRangeStatus.NORMAL
                else -> MetricRangeStatus.HIGH
            }
        }

        // 8. Visceral Fat (Exact Zepp Life getVisceralFat formula)
        val rawVisceral = if (!isMale) {
            if (weightKg > (13.0 - (h * 0.5)) * -1.0) {
                val subsub = ((h * 1.45) + (h * 0.1158) * h) - 120.0
                val sub = (weightKg * 500.0) / subsub
                (sub - 6.0) + (a * 0.07)
            } else {
                val sub = 0.691 + (h * -0.0024) + (h * -0.0024)
                (((h * 0.027) - (sub * weightKg)) * -1.0) + (a * 0.07) - a
            }
        } else {
            if (h < weightKg * 1.6) {
                val sub = ((h * 0.4) - (h * (h * 0.0826))) * -1.0
                ((weightKg * 305.0) / (sub + 48.0)) - 2.9 + (a * 0.15)
            } else {
                val sub = 0.765 + h * -0.0015
                (((h * 0.143) - (weightKg * sub)) * -1.0) + (a * 0.15) - 5.0
            }
        }
        val visceralFat = rawVisceral.roundToInt().coerceIn(1, 50)
        val visceralStatus = when {
            visceralFat <= 9 -> MetricRangeStatus.NORMAL
            visceralFat <= 14 -> MetricRangeStatus.HIGH
            else -> MetricRangeStatus.VERY_HIGH
        }

        // 9. BMR (Exact Zepp Life getBMR formula)
        val rawBmr = if (!isMale) {
            864.6 + weightKg * 10.2036 - h * 0.39336 - a * 6.204
        } else {
            877.8 + weightKg * 14.916 - h * 0.726 - a * 8.976
        }
        val bmrKcal = rawBmr.roundToInt().coerceIn(500, 5000)

        // 10. Body Age / Metabolic Age (Exact Zepp Life getMetabolicAge formula when impedance known)
        val rawBodyAge = if (impedanceOhm != null && impedanceOhm in 100..2500) {
            if (!isMale) {
                (h * -1.1165) + (weightKg * 1.5784) + (a * 0.4615) + (impedanceOhm * 0.0415) + 83.2548
            } else {
                (h * -0.7471) + (weightKg * 0.9161) + (a * 0.4184) + (impedanceOhm * 0.0517) + 54.2267
            }
        } else {
            // Estimate equivalent impedance (~500 Ohm baseline) shifted by BMI & body fat deviation
            val bmiOffset = (bmi - 22.0) * 0.85
            val fatOffset = if (fatPct != null) (fatPct - ((fatNormalLow + fatNormalHigh) / 2.0)) * 0.35 else 0.0
            a + bmiOffset + fatOffset
        }
        val bodyAge = rawBodyAge.roundToInt().coerceIn(15, 80)

        // 11. Ideal Weight (Exact Zepp Life getIdealWeight formula)
        val idealWeightKg = round1(
            if (isMale) ((h - 80.0) * 0.7).coerceIn(40.0, 120.0)
            else ((h - 70.0) * 0.6).coerceIn(35.0, 110.0)
        )
        val weightDeltaFromIdealKg = round1(weightKg - idealWeightKg)

        // 12. Body Type (Zepp Life 3x3 Fat x Muscle Matrix)
        val bodyType = resolveBodyType(
            fatStatus = bodyFatStatus ?: bmiStatus,
            muscleStatus = muscleStatus ?: MetricRangeStatus.NORMAL
        )

        // 13. Body Score (Zepp Life 100 - malus composite scoring)
        val bodyScore = computeBodyScore(
            bmi = bmi,
            fatPct = fatPct,
            fatNormalLow = fatNormalLow,
            fatNormalHigh = fatNormalHigh,
            muscleKg = mKg,
            muscleLow = muscleLow,
            waterPct = waterPct,
            waterLow = waterLow,
            visceralFat = visceralFat,
            boneKg = boneKg,
            boneLow = boneLow,
            proteinPct = proteinPct
        )

        return FullBodyCompositionReport(
            weightKg = round1(weightKg),
            heightCm = round1(h),
            age = a,
            isMale = isMale,
            impedanceOhm = impedanceOhm,
            bmi = bmi,
            bmiStatus = bmiStatus,
            bodyScore = bodyScore,
            bodyType = bodyType,
            bodyFatPercent = fatPct,
            fatMassKg = fatKg,
            bodyFatStatus = bodyFatStatus,
            muscleMassKg = mKg,
            musclePercent = mPct,
            muscleStatus = muscleStatus,
            waterPercent = waterPct,
            waterStatus = waterStatus,
            boneMassKg = boneKg,
            boneStatus = boneStatus,
            leanBodyMassKg = lbmKg,
            proteinPercent = proteinPct,
            proteinStatus = proteinStatus,
            visceralFat = visceralFat,
            visceralFatStatus = visceralStatus,
            bmrKcal = bmrKcal,
            bodyAge = bodyAge,
            idealWeightKg = idealWeightKg,
            weightDeltaFromIdealKg = weightDeltaFromIdealKg
        )
    }

    private fun resolveBodyType(
        fatStatus: MetricRangeStatus,
        muscleStatus: MetricRangeStatus
    ): ZeppBodyType {
        return when (fatStatus) {
            MetricRangeStatus.VERY_HIGH -> ZeppBodyType.OBESE
            MetricRangeStatus.HIGH -> when (muscleStatus) {
                MetricRangeStatus.HIGH -> ZeppBodyType.THICK_SET
                else -> ZeppBodyType.OVERWEIGHT
            }
            MetricRangeStatus.LOW -> when (muscleStatus) {
                MetricRangeStatus.HIGH -> ZeppBodyType.SKINNY_MUSCULAR
                MetricRangeStatus.NORMAL -> ZeppBodyType.BALANCED_SKINNY
                else -> ZeppBodyType.SKINNY
            }
            MetricRangeStatus.NORMAL -> when (muscleStatus) {
                MetricRangeStatus.HIGH -> ZeppBodyType.BALANCED_MUSCULAR
                MetricRangeStatus.LOW -> ZeppBodyType.LACK_EXERCISE
                else -> ZeppBodyType.BALANCED
            }
        }
    }

    private fun computeBodyScore(
        bmi: Double,
        fatPct: Double?,
        fatNormalLow: Double,
        fatNormalHigh: Double,
        muscleKg: Double?,
        muscleLow: Double,
        waterPct: Double?,
        waterLow: Double,
        visceralFat: Int,
        boneKg: Double?,
        boneLow: Double,
        proteinPct: Double?
    ): Int {
        var malus = 0.0

        // BMI penalty (up to 25 pts)
        malus += when {
            bmi in 18.5..24.9 -> 0.0
            bmi < 18.5 -> ((18.5 - bmi) * 3.5).coerceAtMost(20.0)
            else -> ((bmi - 24.9) * 2.2).coerceAtMost(25.0)
        }

        // Body Fat penalty (up to 25 pts)
        if (fatPct != null) {
            malus += when {
                fatPct in fatNormalLow..fatNormalHigh -> 0.0
                fatPct < fatNormalLow -> ((fatNormalLow - fatPct) * 1.5).coerceAtMost(12.0)
                else -> ((fatPct - fatNormalHigh) * 1.6).coerceAtMost(25.0)
            }
        }

        // Visceral Fat penalty (up to 15 pts)
        if (visceralFat > 9) {
            malus += ((visceralFat - 9) * 1.8).coerceAtMost(15.0)
        }

        // Muscle penalty (up to 12 pts)
        if (muscleKg != null && muscleKg < muscleLow) {
            malus += ((muscleLow - muscleKg) * 1.5).coerceAtMost(12.0)
        }

        // Water penalty (up to 8 pts)
        if (waterPct != null && waterPct < waterLow) {
            malus += ((waterLow - waterPct) * 0.8).coerceAtMost(8.0)
        }

        // Bone penalty (up to 5 pts)
        if (boneKg != null && boneKg < boneLow) {
            malus += ((boneLow - boneKg) * 4.0).coerceAtMost(5.0)
        }

        // Protein penalty (up to 5 pts)
        if (proteinPct != null && proteinPct < 16.0) {
            malus += ((16.0 - proteinPct) * 1.2).coerceAtMost(5.0)
        }

        return (100.0 - malus).roundToInt().coerceIn(25, 100)
    }

    private fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0
}
