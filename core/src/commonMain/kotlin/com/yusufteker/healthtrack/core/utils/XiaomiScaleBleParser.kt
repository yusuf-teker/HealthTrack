package com.yusufteker.healthtrack.core.utils

import kotlinx.serialization.Serializable

/**
 * Live or stabilized measurement decoded from a BLE smart scale advertisement packet.
 */
@Serializable
data class BleScaleReading(
    val weightKg: Double,
    val impedanceOhm: Int? = null,
    val isStabilized: Boolean = false,
    val isWeightRemoved: Boolean = false,
    val bodyFatPercent: Double? = null,
    val muscleMassKg: Double? = null,
    val musclePercent: Double? = null,
    val leanBodyMassKg: Double? = null,
    val boneMassKg: Double? = null,
    val scaleModel: String = "Xiaomi Mi Scale"
)

/**
 * Pure Kotlin BLE advertisement service-data parser and BIA body composition calculator
 * for Xiaomi Mi Smart Scale (1st Gen, GATT `0x181D`) and Mi Body Composition Scale 2 (GATT `0x181B`).
 *
 * Works across both Android `ScanRecord.serviceData` and iOS CoreBluetooth `CBAdvertisementDataServiceDataKey`
 * without requiring pairing or intermediate bridging apps (Zepp Life / Google Fit).
 */
object XiaomiScaleBleParser {

    /** Bluetooth SIG Weight Scale service (used by Xiaomi Mi Smart Scale 1 / standard BLE weight scales). */
    const val SERVICE_UUID_WEIGHT_SCALE = 0x181D

    /** Bluetooth SIG Body Composition service (used by Xiaomi Mi Body Composition Scale 2). */
    const val SERVICE_UUID_BODY_COMPOSITION = 0x181B

    private const val LBS_TO_KG = 0.45359237

    /**
     * Parses raw BLE advertisement service data for short 16-bit UUID `0x181B` or `0x181D`.
     *
     * @param serviceUuidShort Short 16-bit GATT service UUID (`0x181B` or `0x181D`).
     * @param data Raw service payload bytes from BLE advertisement.
     * @param heightCm User height in centimeters (used for BIA equations on `0x181B`).
     * @param age User age in years (used for BIA equations on `0x181B`).
     * @param isMale User biological sex flag (used for BIA equations on `0x181B`).
     * @return Decoded [BleScaleReading], or `null` if the payload is unsupported, truncated, or out of range.
     */
    fun parseServiceData(
        serviceUuidShort: Int,
        data: ByteArray,
        heightCm: Double,
        age: Int,
        isMale: Boolean
    ): BleScaleReading? {
        return when (serviceUuidShort) {
            SERVICE_UUID_BODY_COMPOSITION -> parseMiScale2BodyComposition(data, heightCm, age, isMale)
            SERVICE_UUID_WEIGHT_SCALE -> parseMiScale1Weight(data)
            else -> null
        }
    }

    private fun parseMiScale2BodyComposition(
        data: ByteArray,
        heightCm: Double,
        age: Int,
        isMale: Boolean
    ): BleScaleReading? {
        if (data.size < 13) return null

        val ctrlByte0 = data[0].toInt() and 0xFF
        val ctrlByte1 = data[1].toInt() and 0xFF

        val isLbs = (ctrlByte0 and 0x01) != 0
        val isCatty = (ctrlByte0 and 0x10) != 0
        val hasImpedance = (ctrlByte1 and 0x02) != 0
        val isStabilized = (ctrlByte1 and 0x20) != 0
        val isWeightRemoved = (ctrlByte1 and 0x80) != 0

        val impedanceRaw = (data[9].toInt() and 0xFF) or ((data[10].toInt() and 0xFF) shl 8)
        val rawWeight = (data[11].toInt() and 0xFF) or ((data[12].toInt() and 0xFF) shl 8)

        val weightKg = convertRawWeightToKg(rawWeight = rawWeight, isLbs = isLbs, isCatty = isCatty)
            ?: return null

        val validImpedance = if (hasImpedance && impedanceRaw in 100..2500) impedanceRaw else null
        val bia = validImpedance?.let { ohm ->
            computeXiaomiBia(
                weightKg = weightKg,
                heightCm = heightCm,
                age = age,
                isMale = isMale,
                impedanceOhm = ohm
            )
        }

        return BleScaleReading(
            weightKg = weightKg,
            impedanceOhm = validImpedance,
            isStabilized = isStabilized,
            isWeightRemoved = isWeightRemoved,
            bodyFatPercent = bia?.bodyFatPercent,
            muscleMassKg = bia?.muscleMassKg,
            musclePercent = bia?.musclePercent,
            leanBodyMassKg = bia?.leanBodyMassKg,
            boneMassKg = bia?.boneMassKg,
            scaleModel = "Xiaomi Mi Scale 2"
        )
    }

    private fun parseMiScale1Weight(data: ByteArray): BleScaleReading? {
        if (data.size < 10) return null

        val ctrlByte = data[0].toInt() and 0xFF
        val isLbs = (ctrlByte and 0x01) != 0
        val isCatty = (ctrlByte and 0x10) != 0
        val isStabilized = (ctrlByte and 0x20) != 0
        val isWeightRemoved = (ctrlByte and 0x80) != 0

        val rawWeight = (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
        val weightKg = convertRawWeightToKg(rawWeight = rawWeight, isLbs = isLbs, isCatty = isCatty)
            ?: return null

        return BleScaleReading(
            weightKg = weightKg,
            isStabilized = isStabilized,
            isWeightRemoved = isWeightRemoved,
            scaleModel = "Xiaomi Mi Scale"
        )
    }

    private fun convertRawWeightToKg(rawWeight: Int, isLbs: Boolean, isCatty: Boolean): Double? {
        if (rawWeight <= 0) return null
        val kg = when {
            isLbs -> (rawWeight / 100.0) * LBS_TO_KG
            isCatty -> (rawWeight / 100.0) * 0.5
            else -> rawWeight / 200.0
        }
        val rounded = BodyCompositionCalculator.round1(kg)
        return rounded.takeIf { it in 15.0..250.0 }
    }

    /**
     * Computes Xiaomi Mi Body Composition Scale 2 BIA metrics (body fat %, bone mass kg,
     * muscle mass kg, muscle %, lean body mass kg) from weight, user anthropometrics, and raw impedance.
     *
     * Follows the standard Xiaomi/Huami LBM + skeletal bone mass BIA equations.
     *
     * @param weightKg Stabilized weight in kg.
     * @param heightCm User height in cm (clamped to `100.0..220.0`).
     * @param age User age in years (clamped to `10..99`).
     * @param isMale Biological sex selector.
     * @param impedanceOhm Bio-electrical impedance in Ohms (`100..2500`).
     * @return Computed [BodyCompositionResult] rounded to 1 decimal place.
     */
    fun computeXiaomiBia(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        isMale: Boolean,
        impedanceOhm: Int
    ): BodyCompositionResult {
        val h = heightCm.coerceIn(100.0, 220.0)
        val a = age.coerceIn(10, 99)

        val lbm = (h * 9.058 / 100.0) * (h / 100.0) +
            weightKg * 0.32 +
            12.226 -
            impedanceOhm * 0.0068 -
            a * 0.0542

        val constVal = when {
            isMale -> 0.8
            a <= 49 -> 9.25
            else -> 7.25
        }

        val coeff = when {
            isMale && weightKg < 61.0 -> 0.98
            !isMale && weightKg > 60.0 -> 0.96 * (if (h > 160.0) 1.03 else 1.0)
            !isMale && weightKg < 50.0 -> 1.02 * (if (h > 160.0) 1.03 else 1.0)
            else -> 1.0
        }

        val rawBodyFat = (1.0 - (((lbm - constVal) * coeff) / weightKg)) * 100.0
        val bodyFat = (if (rawBodyFat > 63.0) 75.0 else rawBodyFat).coerceIn(5.0, 75.0)

        val base = if (isMale) 0.18016894 else 0.245691014
        val rawBone = (base - lbm * 0.05158) * -1.0
        val boneMass = (if (rawBone > 2.2) rawBone + 0.1 else rawBone - 0.1).coerceIn(0.5, 8.0)

        val muscleMass = (weightKg - (bodyFat * 0.01 * weightKg) - boneMass).coerceIn(10.0, 120.0)
        val musclePercent = ((muscleMass / weightKg) * 100.0).coerceIn(15.0, 90.0)
        val leanBodyMassKg = (weightKg * (1.0 - bodyFat / 100.0)).coerceIn(15.0, weightKg)

        return BodyCompositionResult(
            bodyFatPercent = BodyCompositionCalculator.round1(bodyFat),
            muscleMassKg = BodyCompositionCalculator.round1(muscleMass),
            musclePercent = BodyCompositionCalculator.round1(musclePercent),
            leanBodyMassKg = BodyCompositionCalculator.round1(leanBodyMassKg),
            boneMassKg = BodyCompositionCalculator.round1(boneMass)
        )
    }
}
