package com.yusufteker.healthtrack.core.utils

/**
 * Unified passive BLE advertisement parser for popular Turkey & global smart scales.
 *
 * Supported zero-pairing broadcast protocols (Android `ScanRecord` & iOS CoreBluetooth):
 * 1. **Xiaomi / Bluetooth SIG** (`0x181B` Body Composition, `0x181D` Weight Scale)
 *    - Mi Body Composition Scale 2, Mi Smart Scale 1, standard SIG BLE scales
 * 2. **OKOK / Chipsea** (`0xC0` and `0xCA` Manufacturer Specific Data with XOR checksum)
 *    - Polosmart (PSC05/PSC06/PSC26), Kiwi, Fakir, Sinbo, Vestel, Arzum, Silvercrest, OKOK International
 * 3. **Fitdays / Lefu / ICOMON** (`0xAC 0x02` Manufacturer Specific Data with additive checksum)
 *    - Fitdays, Renpho, Arboleaf, Fitindex, Yolanda broadcast models
 *
 * Zero external dependencies — pure Kotlin byte decoding.
 */
object SmartScaleBleParser {

    private const val LBS_TO_KG = 0.45359237

    /**
     * Decodes a BLE advertisement packet from any supported smart scale.
     *
     * @param serviceData181B Optional GATT `0x181B` service data payload.
     * @param serviceData181D Optional GATT `0x181D` service data payload.
     * @param manufacturerDataList List of raw Manufacturer Specific Data frames where indices `0..1`
     *   are the 2-byte little-endian Company Identifier followed by payload bytes (matching iOS
     *   `CBAdvertisementDataManufacturerDataKey` and reconstructed Android `manufacturerSpecificData`).
     * @param deviceName Optional advertised BLE local name (used only for friendly model labeling).
     */
    fun parseAdvertisement(
        serviceData181B: ByteArray? = null,
        serviceData181D: ByteArray? = null,
        manufacturerDataList: List<ByteArray> = emptyList(),
        deviceName: String? = null,
        heightCm: Double,
        age: Int,
        isMale: Boolean
    ): BleScaleReading? {
        if (serviceData181B != null) {
            XiaomiScaleBleParser.parseServiceData(
                serviceUuidShort = XiaomiScaleBleParser.SERVICE_UUID_BODY_COMPOSITION,
                data = serviceData181B,
                heightCm = heightCm,
                age = age,
                isMale = isMale
            )?.let { return it }
        }

        if (serviceData181D != null) {
            XiaomiScaleBleParser.parseServiceData(
                serviceUuidShort = XiaomiScaleBleParser.SERVICE_UUID_WEIGHT_SCALE,
                data = serviceData181D,
                heightCm = heightCm,
                age = age,
                isMale = isMale
            )?.let { return it }
        }

        for (raw in manufacturerDataList) {
            parseManufacturerData(
                raw = raw,
                deviceName = deviceName,
                heightCm = heightCm,
                age = age,
                isMale = isMale
            )?.let { return it }
        }

        return null
    }

    /**
     * Attempts to decode a single Manufacturer Specific Data byte array (including the 2-byte company prefix).
     */
    fun parseManufacturerData(
        raw: ByteArray,
        deviceName: String? = null,
        heightCm: Double,
        age: Int,
        isMale: Boolean
    ): BleScaleReading? {
        if (raw.size < 8) return null
        val b0 = raw[0].toInt() and 0xFF
        val b1 = raw[1].toInt() and 0xFF

        // 1. OKOK / Chipsea 0xC0 broadcast protocol (Polosmart, Kiwi, Fakir, Sinbo, OKOK)
        if (b0 == 0xC0 && raw.size >= 10) {
            parseOkokC0Frame(raw, deviceName, heightCm, age, isMale)?.let { return it }
        }

        // 2. OKOK / Chipsea 0xCA V1.1 / V2.0 broadcast protocol (Chipsea-BLE, Polosmart, Yoda0)
        if (b0 == 0xCA && b1 in setOf(0x10, 0x11, 0x20) && raw.size >= 10) {
            parseOkokCaFrame(raw, deviceName, heightCm, age, isMale)?.let { return it }
        }

        // 3. Fitdays / Lefu / ICOMON 0xAC 0x02 broadcast protocol (Fitdays, Renpho, Arboleaf, Yolanda)
        if (b0 == 0xAC && b1 == 0x02 && raw.size >= 8) {
            parseFitdaysLefuFrame(raw, deviceName)?.let { return it }
        }

        return null
    }

    /**
     * Parses OKOK / Chipsea `0xC0` broadcast packet.
     * Layout: `[0]=0xC0, [1]=seq, [2..3]=weight LE, [4..5]=impedance LE, [6..7]=productId, [8]=flags, ..., [last]=XOR(1..last-1)`
     */
    private fun parseOkokC0Frame(
        data: ByteArray,
        deviceName: String?,
        heightCm: Double,
        age: Int,
        isMale: Boolean
    ): BleScaleReading? {
        if (!verifyXorChecksum(data, startIndex = 1, endExclusive = data.lastIndex, expected = data.last())) {
            return null
        }

        val rawWeight = u16Le(data[2], data[3])
        val rawImpedance = u16Le(data[4], data[5])
        val flags = data[8].toInt() and 0xFF

        val isStabilized = (flags and 0x01) != 0
        val divisor = if ((flags and 0x06) == 0x02) 100.0 else 10.0
        val unitCode = (flags shr 3) and 0x03 // 0=kg, 1=jin, 2=lb

        val weightKg = convertToKg(rawWeight, divisor, unitCode) ?: return null
        val impedanceOhm = normalizeImpedanceOhm(rawImpedance)
        val bia = impedanceOhm?.let { ohm ->
            XiaomiScaleBleParser.computeXiaomiBia(
                weightKg = weightKg,
                heightCm = heightCm,
                age = age,
                isMale = isMale,
                impedanceOhm = ohm
            )
        }

        return BleScaleReading(
            weightKg = weightKg,
            impedanceOhm = impedanceOhm,
            isStabilized = isStabilized,
            isWeightRemoved = false,
            bodyFatPercent = bia?.bodyFatPercent,
            muscleMassKg = bia?.muscleMassKg,
            musclePercent = bia?.musclePercent,
            leanBodyMassKg = bia?.leanBodyMassKg,
            boneMassKg = bia?.boneMassKg,
            scaleModel = resolveFriendlyModelName(deviceName, defaultName = "OKOK / Polosmart Scale")
        )
    }

    /**
     * Parses OKOK / Chipsea `0xCA` broadcast packet (V1.1 and V2.0 variants).
     * - V2.0 (`data[1] == 0x20`, size >= 15): `[8]=lock, [10..11]=weight BE, [12..13]=impedance BE, [last]=XOR`
     * - V1.1 (`data[1] == 0x11` or `0x10`, size >= 10): `[3]=lock, [4..5]=weight BE, [6..7]=impedance BE, [last]=XOR`
     */
    private fun parseOkokCaFrame(
        data: ByteArray,
        deviceName: String?,
        heightCm: Double,
        age: Int,
        isMale: Boolean
    ): BleScaleReading? {
        val xorFrom0 = verifyXorChecksum(data, startIndex = 0, endExclusive = data.lastIndex, expected = data.last())
        val xorFrom1 = verifyXorChecksum(data, startIndex = 1, endExclusive = data.lastIndex, expected = data.last())
        if (!xorFrom0 && !xorFrom1) return null

        val version = data[1].toInt() and 0xFF
        val isV20 = version == 0x20 && data.size >= 15

        val isStabilized: Boolean
        val rawWeight: Int
        val rawImpedance: Int
        val unitCode: Int

        if (isV20) {
            val propFlags = data[7].toInt() and 0xFF
            isStabilized = (data[8].toInt() and 0xFF) == 0x01
            rawWeight = u16Be(data[10], data[11])
            rawImpedance = u16Be(data[12], data[13])
            unitCode = (propFlags shr 1) and 0x03
        } else {
            isStabilized = (data[3].toInt() and 0xFF) == 0x01
            rawWeight = u16Be(data[4], data[5])
            rawImpedance = if (data.size >= 9) u16Be(data[6], data[7]) else 0
            unitCode = 0
        }

        val weightKg = convertToKg(rawWeight, divisor = 10.0, unitCode = unitCode)
            ?: convertToKg(rawWeight, divisor = 100.0, unitCode = unitCode)
            ?: return null

        val impedanceOhm = normalizeImpedanceOhm(rawImpedance)
        val bia = impedanceOhm?.let { ohm ->
            XiaomiScaleBleParser.computeXiaomiBia(
                weightKg = weightKg,
                heightCm = heightCm,
                age = age,
                isMale = isMale,
                impedanceOhm = ohm
            )
        }

        return BleScaleReading(
            weightKg = weightKg,
            impedanceOhm = impedanceOhm,
            isStabilized = isStabilized,
            isWeightRemoved = false,
            bodyFatPercent = bia?.bodyFatPercent,
            muscleMassKg = bia?.muscleMassKg,
            musclePercent = bia?.musclePercent,
            leanBodyMassKg = bia?.leanBodyMassKg,
            boneMassKg = bia?.boneMassKg,
            scaleModel = resolveFriendlyModelName(deviceName, defaultName = "Chipsea / Polosmart Scale")
        )
    }

    /**
     * Parses Fitdays / Lefu / ICOMON `0xAC 0x02` 8-byte broadcast packet.
     * Layout: `[0]=0xAC, [1]=0x02, [2..3]=weight BE (x10), [4..5]=reserved, [6]=status (0xCA=stable, 0xCE=measuring), [7]=sum(2..6)&0xFF`
     */
    private fun parseFitdaysLefuFrame(
        data: ByteArray,
        deviceName: String?
    ): BleScaleReading? {
        val status = data[6].toInt() and 0xFF
        if (status != 0xCA && status != 0xCE) return null

        val sum = ((data[2].toInt() and 0xFF) +
            (data[3].toInt() and 0xFF) +
            (data[4].toInt() and 0xFF) +
            (data[5].toInt() and 0xFF) +
            status) and 0xFF
        val expected = data[7].toInt() and 0xFF
        if (sum != expected) return null

        val rawWeight = u16Be(data[2], data[3])
        val weightKg = convertToKg(rawWeight, divisor = 10.0, unitCode = 0) ?: return null

        return BleScaleReading(
            weightKg = weightKg,
            impedanceOhm = null,
            isStabilized = status == 0xCA,
            isWeightRemoved = false,
            scaleModel = resolveFriendlyModelName(deviceName, defaultName = "Fitdays / Renpho Scale")
        )
    }

    private fun verifyXorChecksum(data: ByteArray, startIndex: Int, endExclusive: Int, expected: Byte): Boolean {
        if (startIndex < 0 || endExclusive <= startIndex || endExclusive > data.size) return false
        var xor = 0
        for (i in startIndex until endExclusive) {
            xor = xor xor (data[i].toInt() and 0xFF)
        }
        return xor == (expected.toInt() and 0xFF)
    }

    private fun u16Le(lo: Byte, hi: Byte): Int =
        (lo.toInt() and 0xFF) or ((hi.toInt() and 0xFF) shl 8)

    private fun u16Be(hi: Byte, lo: Byte): Int =
        ((hi.toInt() and 0xFF) shl 8) or (lo.toInt() and 0xFF)

    private fun convertToKg(rawWeight: Int, divisor: Double, unitCode: Int): Double? {
        if (rawWeight <= 0 || divisor <= 0.0) return null
        val raw = rawWeight / divisor
        val kg = when (unitCode) {
            1 -> raw * 0.5 // Jin / Catty
            2 -> raw * LBS_TO_KG // Pounds
            else -> raw
        }
        val rounded = BodyCompositionCalculator.round1(kg)
        return rounded.takeIf { it in 15.0..250.0 }
    }

    private fun normalizeImpedanceOhm(rawImpedance: Int): Int? {
        if (rawImpedance <= 0) return null
        // Some Chipsea/OKOK scales report impedance in tenths of an ohm (e.g. 5230 -> 523 Ohm)
        val normalized = if (rawImpedance > 2500) rawImpedance / 10 else rawImpedance
        return normalized.takeIf { it in 100..2500 }
    }

    private fun resolveFriendlyModelName(deviceName: String?, defaultName: String): String {
        val clean = deviceName?.trim().orEmpty()
        if (clean.isBlank()) return defaultName
        val lower = clean.lowercase()
        return when {
            lower.contains("polo") -> "Polosmart Smart Scale"
            lower.contains("kiwi") -> "Kiwi Smart Scale"
            lower.contains("fakir") -> "Fakir Smart Scale"
            lower.contains("sinbo") -> "Sinbo Smart Scale"
            lower.contains("vestel") -> "Vestel Smart Scale"
            lower.contains("renpho") -> "Renpho Smart Scale"
            lower.contains("fitdays") -> "Fitdays Smart Scale"
            lower.contains("arboleaf") -> "Arboleaf Smart Scale"
            lower.contains("yunmai") -> "Yunmai Smart Scale"
            lower.contains("eufy") -> "Eufy Smart Scale"
            lower.contains("okok") || lower.contains("chipsea") -> "OKOK / Polosmart Scale"
            else -> clean.take(28)
        }
    }
}
