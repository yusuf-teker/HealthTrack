package com.yusufteker.healthtrack.core.utils

import kotlin.math.roundToInt

/**
 * Resolved body composition metrics normalized to 1 decimal place.
 */
data class BodyCompositionResult(
    val bodyFatPercent: Double? = null,
    val muscleMassKg: Double? = null,
    val musclePercent: Double? = null,
    val leanBodyMassKg: Double? = null,
    val boneMassKg: Double? = null
)

/**
 * Pure body composition resolution, unit conversion, and legacy note backfill helper.
 *
 * Bridges heterogeneous smart scale hubs (Apple HealthKit, Android Health Connect, Xiaomi BLE BIA)
 * into consistent `bodyFatPercent`, `muscleMassKg`, and `musclePercent` metrics on [WeightEntry].
 */
object BodyCompositionCalculator {

    private val trFatRegex = Regex("""%\s*(\d+(?:[.,]\d+)?)\s*Yağ""", RegexOption.IGNORE_CASE)
    private val enFatRegex = Regex("""(\d+(?:[.,]\d+)?)\s*%\s*Body\s*Fat""", RegexOption.IGNORE_CASE)
    private val trLbmRegex = Regex("""(\d+(?:[.,]\d+)?)\s*kg\s*Yağsız\s*Kütle""", RegexOption.IGNORE_CASE)
    private val enLbmRegex = Regex("""(\d+(?:[.,]\d+)?)\s*kg\s*Lean\s*Mass""", RegexOption.IGNORE_CASE)

    /**
     * Resolves and derives missing body composition fields from a smart scale or OS health hub record.
     *
     * Priority order for muscle resolution:
     * 1. Explicit [muscleMassKg] (`10.0..weightKg`) -> derives [musclePercent] if absent.
     * 2. Explicit [musclePercent] (`15.0..90.0`) -> derives [muscleMassKg].
     * 3. Explicit [leanBodyMassKg] + [boneMassKg] -> exact `leanBodyMassKg - boneMassKg`.
     * 4. Explicit [leanBodyMassKg] alone -> subtracts sex-adjusted estimated bone mass.
     * 5. Explicit [bodyFatPercent] -> derives lean body mass first, then applies step 4.
     *
     * @param weightKg Total body weight in kilograms.
     * @param bodyFatPercent Measured body fat percentage (`3.0..75.0`).
     * @param leanBodyMassKg Measured lean body mass in kg.
     * @param boneMassKg Measured bone mineral mass in kg.
     * @param muscleMassKg Measured muscle mass in kg.
     * @param musclePercent Measured muscle percentage.
     * @param isMale Used for sex-adjusted skeletal bone mass estimation when bone mass is absent.
     * @return Normalized [BodyCompositionResult] with all derivable values rounded to 1 decimal place.
     */
    fun resolveFromDevice(
        weightKg: Double,
        bodyFatPercent: Double? = null,
        leanBodyMassKg: Double? = null,
        boneMassKg: Double? = null,
        muscleMassKg: Double? = null,
        musclePercent: Double? = null,
        isMale: Boolean = true
    ): BodyCompositionResult {
        if (weightKg <= 0.0) return BodyCompositionResult()

        val validLbm = leanBodyMassKg?.takeIf { it in 15.0..<weightKg }?.let(::round1)
        val rawValidFat = bodyFatPercent?.takeIf { it in 3.0..75.0 }?.let(::round1)
        val resolvedFat = rawValidFat ?: validLbm?.let { lbm ->
            val derivedFat = ((weightKg - lbm) / weightKg) * 100.0
            derivedFat.takeIf { it in 3.0..75.0 }?.let(::round1)
        }

        val resolvedLbm = validLbm ?: resolvedFat?.let { fat ->
            (weightKg * (1.0 - fat / 100.0)).takeIf { it in 15.0..<weightKg }?.let(::round1)
        }

        val validBone = boneMassKg?.takeIf { it in 0.5..8.0 && it < (resolvedLbm ?: weightKg) }?.let(::round1)

        var resolvedMuscleKg: Double? = null
        var resolvedMusclePct: Double? = null

        val explicitMuscleKg = muscleMassKg?.takeIf { it in 10.0..weightKg }?.let(::round1)
        val explicitMusclePct = musclePercent?.takeIf { it in 15.0..90.0 }?.let(::round1)

        when {
            explicitMuscleKg != null -> {
                resolvedMuscleKg = explicitMuscleKg
                resolvedMusclePct = explicitMusclePct ?: round1(((explicitMuscleKg / weightKg) * 100.0).coerceIn(15.0, 90.0))
            }
            explicitMusclePct != null -> {
                resolvedMusclePct = explicitMusclePct
                resolvedMuscleKg = round1((weightKg * explicitMusclePct / 100.0).coerceIn(10.0, weightKg))
            }
            resolvedLbm != null && validBone != null && resolvedLbm > validBone -> {
                val mKg = (resolvedLbm - validBone).coerceIn(10.0, weightKg)
                resolvedMuscleKg = round1(mKg)
                resolvedMusclePct = round1(((mKg / weightKg) * 100.0).coerceIn(15.0, 90.0))
            }
            resolvedLbm != null -> {
                val estBone = (weightKg * (if (isMale) 0.042 else 0.036)).coerceIn(1.8, 4.5)
                val mKg = (resolvedLbm - estBone).coerceAtLeast(weightKg * 0.20).coerceAtMost(weightKg)
                resolvedMuscleKg = round1(mKg)
                resolvedMusclePct = round1(((mKg / weightKg) * 100.0).coerceIn(15.0, 90.0))
            }
        }

        return BodyCompositionResult(
            bodyFatPercent = resolvedFat,
            muscleMassKg = resolvedMuscleKg,
            musclePercent = resolvedMusclePct,
            leanBodyMassKg = resolvedLbm,
            boneMassKg = validBone
        )
    }

    /**
     * Resolves body composition from explicit manual user entries without fabricating unentered metrics.
     *
     * @param weightKg User's entered body weight in kg.
     * @param bodyFatPercent Optional user-entered body fat percentage (`3.0..75.0`).
     * @param muscleValue Optional user-entered muscle value (either % or kg depending on [isMuscleUnitPercent]).
     * @param isMuscleUnitPercent True if [muscleValue] represents a percentage; false if kilograms.
     * @return Exact converted [BodyCompositionResult] leaving omitted dimensions as `null`.
     */
    fun resolveFromManualInput(
        weightKg: Double,
        bodyFatPercent: Double?,
        muscleValue: Double?,
        isMuscleUnitPercent: Boolean
    ): BodyCompositionResult {
        val validFat = bodyFatPercent?.takeIf { it in 3.0..75.0 }?.let(::round1)
        if (muscleValue == null || weightKg <= 0.0) {
            return BodyCompositionResult(bodyFatPercent = validFat)
        }

        return if (isMuscleUnitPercent) {
            val pct = muscleValue.takeIf { it in 15.0..90.0 }?.let(::round1)
            val kg = pct?.let { round1(weightKg * it / 100.0) }
            BodyCompositionResult(
                bodyFatPercent = validFat,
                muscleMassKg = kg,
                musclePercent = pct
            )
        } else {
            val kg = muscleValue.takeIf { it in 10.0..weightKg && it <= 120.0 }?.let(::round1)
            val pct = kg?.let { round1(((it / weightKg) * 100.0).coerceIn(15.0, 95.0)) }
            BodyCompositionResult(
                bodyFatPercent = validFat,
                muscleMassKg = kg,
                musclePercent = pct
            )
        }
    }

    /**
     * Parses legacy body fat / lean body mass strings stored inside `WeightEntry.note`
     * so existing user weigh-ins automatically backfill structured body composition charts.
     *
     * @param note Raw note string from [com.yusufteker.healthtrack.core.model.WeightEntry.note].
     * @param weightKg Weight in kg for the entry.
     * @param isMale User's gender flag for accurate bone-mass offset estimation.
     * @return Parsed [BodyCompositionResult], or `null` if the note contains no legacy composition tags.
     */
    fun parseLegacyBodyCompositionFromNote(
        note: String?,
        weightKg: Double,
        isMale: Boolean = true
    ): BodyCompositionResult? {
        if (note.isNullOrBlank() || weightKg <= 0.0) return null

        val parsedFat = (trFatRegex.find(note) ?: enFatRegex.find(note))
            ?.groupValues?.getOrNull(1)
            ?.replace(',', '.')
            ?.toDoubleOrNull()

        val parsedLbm = (trLbmRegex.find(note) ?: enLbmRegex.find(note))
            ?.groupValues?.getOrNull(1)
            ?.replace(',', '.')
            ?.toDoubleOrNull()

        if (parsedFat == null && parsedLbm == null) return null

        return resolveFromDevice(
            weightKg = weightKg,
            bodyFatPercent = parsedFat,
            leanBodyMassKg = parsedLbm,
            isMale = isMale
        )
    }

    /**
     * Strips redundant legacy body fat / lean mass tags from a note string for cleaner UI presentation
     * when structured composition pills are displayed separately.
     *
     * @param note Raw note text.
     * @return Cleaned note text, or `null` if nothing meaningful remains.
     */
    fun stripLegacyBodyCompFromNote(note: String?): String? {
        if (note.isNullOrBlank()) return null
        val cleaned = note
            .replace(trFatRegex, "")
            .replace(enFatRegex, "")
            .replace(trLbmRegex, "")
            .replace(enLbmRegex, "")
            .replace(Regex("""\s*•\s*•\s*"""), " • ")
            .replace(Regex("""^\s*•\s*|\s*•\s*$"""), "")
            .replace(Regex("""\(\s*•\s*"""), "(")
            .replace(Regex("""\s*•\s*\)"""), ")")
            .replace(Regex("""\(\s*\)"""), "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
        return cleaned.takeIf { it.isNotBlank() }
    }

    fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0
}
