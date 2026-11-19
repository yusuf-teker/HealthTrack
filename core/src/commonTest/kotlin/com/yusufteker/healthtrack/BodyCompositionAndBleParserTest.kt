package com.yusufteker.healthtrack

import com.yusufteker.healthtrack.core.utils.BodyCompositionCalculator
import com.yusufteker.healthtrack.core.utils.SmartScaleBleParser
import com.yusufteker.healthtrack.core.utils.XiaomiScaleBleParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BodyCompositionAndBleParserTest {

    private fun buildMiScale2Packet(
        ctrlByte0: Int,
        ctrlByte1: Int,
        impedance: Int,
        rawWeight: Int
    ): ByteArray {
        val bytes = ByteArray(13)
        bytes[0] = ctrlByte0.toByte()
        bytes[1] = ctrlByte1.toByte()
        bytes[9] = (impedance and 0xFF).toByte()
        bytes[10] = ((impedance shr 8) and 0xFF).toByte()
        bytes[11] = (rawWeight and 0xFF).toByte()
        bytes[12] = ((rawWeight shr 8) and 0xFF).toByte()
        return bytes
    }

    private fun buildMiScale1Packet(
        ctrlByte: Int,
        rawWeight: Int
    ): ByteArray {
        val bytes = ByteArray(10)
        bytes[0] = ctrlByte.toByte()
        bytes[1] = (rawWeight and 0xFF).toByte()
        bytes[2] = ((rawWeight shr 8) and 0xFF).toByte()
        return bytes
    }

    @Test
    fun `parses Xiaomi Mi Scale 2 0x181B kg stabilized packet with BIA impedance`() {
        // 74.4 kg -> rawWeight = 74.4 * 200 = 14880; ctrlByte1 = 0x22 (stabilized + hasImpedance)
        val packet = buildMiScale2Packet(
            ctrlByte0 = 0x00,
            ctrlByte1 = 0x22,
            impedance = 510,
            rawWeight = 14880
        )

        val reading = XiaomiScaleBleParser.parseServiceData(
            serviceUuidShort = XiaomiScaleBleParser.SERVICE_UUID_BODY_COMPOSITION,
            data = packet,
            heightCm = 178.0,
            age = 28,
            isMale = true
        )

        assertNotNull(reading)
        assertEquals(74.4, reading.weightKg)
        assertTrue(reading.isStabilized)
        assertFalse(reading.isWeightRemoved)
        assertEquals(510, reading.impedanceOhm)
        assertEquals("Xiaomi Mi Scale 2", reading.scaleModel)
        assertNotNull(reading.bodyFatPercent)
        assertTrue(reading.bodyFatPercent in 5.0..40.0)
        assertNotNull(reading.muscleMassKg)
        assertNotNull(reading.musclePercent)
        assertNotNull(reading.boneMassKg)
    }

    @Test
    fun `parses Xiaomi Mi Scale 2 0x181B in lbs and catty units`() {
        // 150.00 lbs -> 68.0 kg
        val lbsPacket = buildMiScale2Packet(
            ctrlByte0 = 0x01,
            ctrlByte1 = 0x20,
            impedance = 0,
            rawWeight = 15000
        )
        val lbsReading = XiaomiScaleBleParser.parseServiceData(
            XiaomiScaleBleParser.SERVICE_UUID_BODY_COMPOSITION,
            lbsPacket,
            heightCm = 175.0,
            age = 25,
            isMale = false
        )
        assertNotNull(lbsReading)
        assertEquals(68.0, lbsReading.weightKg)
        assertTrue(lbsReading.isStabilized)
        assertNull(lbsReading.impedanceOhm)
        assertNull(lbsReading.bodyFatPercent)

        // 120.00 catty -> 60.0 kg
        val cattyPacket = buildMiScale2Packet(
            ctrlByte0 = 0x10,
            ctrlByte1 = 0xA0, // stabilized + weightRemoved
            impedance = 0,
            rawWeight = 12000
        )
        val cattyReading = XiaomiScaleBleParser.parseServiceData(
            XiaomiScaleBleParser.SERVICE_UUID_BODY_COMPOSITION,
            cattyPacket,
            heightCm = 165.0,
            age = 30,
            isMale = false
        )
        assertNotNull(cattyReading)
        assertEquals(60.0, cattyReading.weightKg)
        assertTrue(cattyReading.isStabilized)
        assertTrue(cattyReading.isWeightRemoved)
    }

    @Test
    fun `parses Xiaomi Mi Scale 1 0x181D weight packet`() {
        // 70.0 kg -> rawWeight = 14000; ctrlByte = 0x20 (stabilized)
        val packet = buildMiScale1Packet(ctrlByte = 0x20, rawWeight = 14000)
        val reading = XiaomiScaleBleParser.parseServiceData(
            serviceUuidShort = XiaomiScaleBleParser.SERVICE_UUID_WEIGHT_SCALE,
            data = packet,
            heightCm = 175.0,
            age = 28,
            isMale = true
        )

        assertNotNull(reading)
        assertEquals(70.0, reading.weightKg)
        assertTrue(reading.isStabilized)
        assertEquals("Xiaomi Mi Scale", reading.scaleModel)
        assertNull(reading.bodyFatPercent)
    }

    @Test
    fun `rejects truncated malformed or out of range packets`() {
        assertNull(
            XiaomiScaleBleParser.parseServiceData(
                XiaomiScaleBleParser.SERVICE_UUID_BODY_COMPOSITION,
                ByteArray(5),
                175.0,
                25,
                true
            )
        )
        assertNull(
            XiaomiScaleBleParser.parseServiceData(
                0x180D, // unrelated HR service
                ByteArray(13),
                175.0,
                25,
                true
            )
        )
        // Out-of-range zero weight
        val zeroPacket = buildMiScale1Packet(ctrlByte = 0x20, rawWeight = 0)
        assertNull(
            XiaomiScaleBleParser.parseServiceData(
                XiaomiScaleBleParser.SERVICE_UUID_WEIGHT_SCALE,
                zeroPacket,
                175.0,
                25,
                true
            )
        )
    }

    @Test
    fun `manual body composition resolves percentage and mass accurately without fabricating missing muscle`() {
        val percentMode = BodyCompositionCalculator.resolveFromManualInput(
            weightKg = 75.0,
            bodyFatPercent = 16.0,
            muscleValue = 76.0,
            isMuscleUnitPercent = true
        )
        assertEquals(16.0, percentMode.bodyFatPercent)
        assertEquals(76.0, percentMode.musclePercent)
        assertEquals(57.0, percentMode.muscleMassKg)

        val kgMode = BodyCompositionCalculator.resolveFromManualInput(
            weightKg = 75.0,
            bodyFatPercent = null,
            muscleValue = 57.0,
            isMuscleUnitPercent = false
        )
        assertNull(kgMode.bodyFatPercent)
        assertEquals(57.0, kgMode.muscleMassKg)
        assertEquals(76.0, kgMode.musclePercent)

        val omittedMuscle = BodyCompositionCalculator.resolveFromManualInput(
            weightKg = 75.0,
            bodyFatPercent = 18.2,
            muscleValue = null,
            isMuscleUnitPercent = true
        )
        assertEquals(18.2, omittedMuscle.bodyFatPercent)
        assertNull(omittedMuscle.muscleMassKg)
        assertNull(omittedMuscle.musclePercent)
    }

    @Test
    fun `parses OKOK C0 broadcast packet with weight impedance and checksum`() {
        // 74.5 kg -> 745 (0x02E9 LE -> E9, 02), impedance 512 Ohm (0x0200 LE -> 00, 02), stabilized (flags=0x01)
        val payload = byteArrayOf(
            0xC0.toByte(),
            0x04, // seq
            0xE9.toByte(), 0x02, // weight = 745 -> 74.5 kg
            0x00, 0x02, // impedance = 512 Ohm
            0x12, 0x34, // product id
            0x01, // flags: stabilized=1, kg, div=10
            0x00 // placeholder checksum
        )
        var xor = 0
        for (i in 1 until payload.lastIndex) xor = xor xor (payload[i].toInt() and 0xFF)
        payload[payload.lastIndex] = xor.toByte()

        val reading = SmartScaleBleParser.parseAdvertisement(
            manufacturerDataList = listOf(payload),
            deviceName = "Polosmart PSC05",
            heightCm = 178.0,
            age = 28,
            isMale = true
        )

        assertNotNull(reading)
        assertEquals(74.5, reading.weightKg)
        assertEquals(512, reading.impedanceOhm)
        assertTrue(reading.isStabilized)
        assertEquals("Polosmart Smart Scale", reading.scaleModel)
        assertNotNull(reading.bodyFatPercent)
        assertNotNull(reading.muscleMassKg)

        // Corrupt checksum must be rejected
        payload[payload.lastIndex] = (xor xor 0xFF).toByte()
        assertNull(
            SmartScaleBleParser.parseAdvertisement(
                manufacturerDataList = listOf(payload),
                deviceName = "Polosmart PSC05",
                heightCm = 178.0,
                age = 28,
                isMale = true
            )
        )
    }

    @Test
    fun `parses Chipsea CA V20 and Fitdays Lefu AC02 broadcast packets`() {
        // Chipsea CA V2.0: 81.2 kg (0x032C BE), impedance 4850 (485.0 Ohm -> 485 Ohm), locked=0x01
        val caFrame = byteArrayOf(
            0xCA.toByte(), 0x20, 0x0B,
            0x00, 0x00, 0x00, 0x00, // product id
            0x00, // flags (kg)
            0x01, // locked/stabilized
            0x05, // seq
            0x03, 0x2C, // weight BE = 812 -> 81.2 kg
            0x12, 0xF2.toByte(), // impedance BE = 4850 -> 485 Ohm
            0x00 // placeholder XOR
        )
        var caXor = 0
        for (i in 0 until caFrame.lastIndex) caXor = caXor xor (caFrame[i].toInt() and 0xFF)
        caFrame[caFrame.lastIndex] = caXor.toByte()

        val caReading = SmartScaleBleParser.parseManufacturerData(
            raw = caFrame,
            deviceName = "Chipsea-BLE",
            heightCm = 180.0,
            age = 30,
            isMale = true
        )
        assertNotNull(caReading)
        assertEquals(81.2, caReading.weightKg)
        assertEquals(485, caReading.impedanceOhm)
        assertTrue(caReading.isStabilized)
        assertNotNull(caReading.bodyFatPercent)

        // Fitdays / Lefu AC 02: 84.1 kg (0x0349 BE), status=0xCA (stable), checksum=(0x03+0x49+0+0+0xCA)&0xFF = 0x16
        val fitdaysFrame = byteArrayOf(
            0xAC.toByte(), 0x02,
            0x03, 0x49,
            0x00, 0x00,
            0xCA.toByte(),
            0x16
        )
        val fitdaysReading = SmartScaleBleParser.parseManufacturerData(
            raw = fitdaysFrame,
            deviceName = "Renpho-Scale",
            heightCm = 175.0,
            age = 28,
            isMale = true
        )
        assertNotNull(fitdaysReading)
        assertEquals(84.1, fitdaysReading.weightKg)
        assertTrue(fitdaysReading.isStabilized)
        assertEquals("Renpho Smart Scale", fitdaysReading.scaleModel)
    }

    @Test
    fun `parses and strips legacy Turkish and English body composition notes cleanly`() {
        val trNote = "Aç karna (⚖️ Zepp Life (Xiaomi Tartı) • %16.2 Yağ • 60.5kg Yağsız Kütle)"
        val parsedTr = BodyCompositionCalculator.parseLegacyBodyCompositionFromNote(
            note = trNote,
            weightKg = 75.0,
            isMale = true
        )
        assertNotNull(parsedTr)
        assertEquals(16.2, parsedTr.bodyFatPercent)
        assertEquals(60.5, parsedTr.leanBodyMassKg)
        assertNotNull(parsedTr.muscleMassKg)
        assertNotNull(parsedTr.musclePercent)

        val strippedTr = BodyCompositionCalculator.stripLegacyBodyCompFromNote(trNote)
        assertEquals("Aç karna (⚖️ Zepp Life (Xiaomi Tartı))", strippedTr)

        val enNote = "18.5% Body Fat • 58.0kg Lean Mass"
        val parsedEn = BodyCompositionCalculator.parseLegacyBodyCompositionFromNote(
            note = enNote,
            weightKg = 72.0,
            isMale = false
        )
        assertNotNull(parsedEn)
        assertEquals(18.5, parsedEn.bodyFatPercent)
        assertEquals(58.0, parsedEn.leanBodyMassKg)
        assertNull(BodyCompositionCalculator.stripLegacyBodyCompFromNote(enNote))
    }
}
