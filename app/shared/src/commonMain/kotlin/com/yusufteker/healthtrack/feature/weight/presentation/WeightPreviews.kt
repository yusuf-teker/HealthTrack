package com.yusufteker.healthtrack.feature.weight.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.model.WeightChartMetric
import com.yusufteker.healthtrack.core.model.WeightChartPeriod
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.utils.BleScaleLiveState
import com.yusufteker.healthtrack.core.utils.BleScaleReading
import com.yusufteker.healthtrack.core.utils.BleScaleScanState
import com.yusufteker.healthtrack.feature.weight.presentation.components.BleScaleMeasureDialogContent
import com.yusufteker.healthtrack.feature.weight.presentation.components.WeightLineChart
import com.yusufteker.healthtrack.theme.HealthTrackTheme

private val sampleWeightEntries = listOf(
    WeightEntry(
        id = "w1",
        userId = 1,
        date = "2026-11-01",
        weightKg = 74.2,
        targetWeightKg = 70.0,
        bodyFatPercent = 18.4,
        muscleMassKg = 57.5,
        musclePercent = 77.5,
        note = "Sabah aç karnına"
    ),
    WeightEntry(
        id = "w2",
        userId = 1,
        date = "2026-11-02",
        weightKg = 73.8,
        targetWeightKg = 70.0,
        bodyFatPercent = 18.1,
        muscleMassKg = 57.5,
        musclePercent = 77.9,
        note = "Kardiyo sonrası"
    ),
    WeightEntry(
        id = "w3",
        userId = 1,
        date = "2026-11-03",
        weightKg = 73.5,
        targetWeightKg = 70.0,
        bodyFatPercent = 17.8,
        muscleMassKg = 57.6,
        musclePercent = 78.4
    ),
    WeightEntry(
        id = "w4",
        userId = 1,
        date = "2026-11-04",
        weightKg = 73.1,
        targetWeightKg = 70.0,
        bodyFatPercent = 17.5,
        muscleMassKg = 57.6,
        musclePercent = 78.8
    ),
    WeightEntry(
        id = "w5",
        userId = 1,
        date = "2026-11-05",
        weightKg = 72.8,
        targetWeightKg = 70.0,
        bodyFatPercent = 17.1,
        muscleMassKg = 57.7,
        musclePercent = 79.3
    ),
    WeightEntry(
        id = "w6",
        userId = 1,
        date = "2026-11-06",
        weightKg = 72.4,
        targetWeightKg = 70.0,
        bodyFatPercent = 16.8,
        muscleMassKg = 57.7,
        musclePercent = 79.7,
        note = "Hedefe 2.4 kg kaldı!"
    )
)

private val populatedWeightState = WeightState(
    entries = sampleWeightEntries,
    filteredEntries = sampleWeightEntries,
    selectedPeriod = WeightChartPeriod.WEEKLY,
    selectedMetric = WeightChartMetric.WEIGHT,
    currentWeightKg = 72.4,
    initialWeightKg = 74.2,
    weightChangeKg = -1.8,
    targetWeightKg = 70.0,
    currentBodyFatPercent = 16.8,
    bodyFatChangePercent = -1.6,
    currentMuscleMassKg = 57.7,
    currentMusclePercent = 79.7,
    musclePercentChange = 2.2,
    inputDate = "06.11.2026",
    inputWeight = "72.4",
    inputNote = "İyi hissediyorum"
)

@Preview
@Composable
fun WeightScreenDarkPreview() {
    HealthTrackTheme(darkTheme = true) {
        WeightScreen(
            state = populatedWeightState,
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun WeightScreenLightPreview() {
    HealthTrackTheme(darkTheme = false) {
        WeightScreen(
            state = populatedWeightState,
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun WeightScreenEmptyPreview() {
    HealthTrackTheme(darkTheme = true) {
        WeightScreen(
            state = WeightState(),
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun WeightReminderDialogPreview() {
    HealthTrackTheme(darkTheme = true) {
        com.yusufteker.healthtrack.feature.weight.presentation.components.WeightReminderDialog(
            currentFrequency = com.yusufteker.healthtrack.core.model.WeightReminderFrequency.EVERY_3_DAYS,
            initialHour = 8,
            initialMinute = 0,
            onDismiss = {},
            onSave = { _, _, _ -> }
        )
    }
}

@Preview
@Composable
fun WeightLineChartPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            WeightLineChart(
                entries = sampleWeightEntries,
                targetWeightKg = 70.0,
                selectedPeriod = WeightChartPeriod.WEEKLY,
                onPeriodSelect = {}
            )
        }
    }
}

@Preview
@Composable
fun WeightLineChartBodyFatPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            WeightLineChart(
                entries = sampleWeightEntries,
                targetWeightKg = 70.0,
                selectedPeriod = WeightChartPeriod.WEEKLY,
                selectedMetric = WeightChartMetric.BODY_FAT,
                onPeriodSelect = {}
            )
        }
    }
}

@Preview
@Composable
fun WeightLineChartMusclePreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            WeightLineChart(
                entries = sampleWeightEntries,
                targetWeightKg = 70.0,
                selectedPeriod = WeightChartPeriod.WEEKLY,
                selectedMetric = WeightChartMetric.MUSCLE,
                onPeriodSelect = {}
            )
        }
    }
}

@Preview
@Composable
fun BleScaleMeasureSheetPreview() {
    HealthTrackTheme(darkTheme = true) {
        BleScaleMeasureDialogContent(
            bleState = BleScaleLiveState(
                scanState = BleScaleScanState.STABILIZED,
                liveReading = BleScaleReading(
                    weightKg = 72.4,
                    impedanceOhm = 512,
                    isStabilized = true,
                    bodyFatPercent = 16.8,
                    muscleMassKg = 57.7,
                    musclePercent = 79.7,
                    scaleModel = "Xiaomi Mi Scale 2"
                ),
                statusMessage = "Ölçüm tamamlandı! Yağ ve kas verisi alındı."
            ),
            userHeightCm = 178.0,
            userAge = 28,
            userIsMale = true,
            onRetryScan = {},
            onSaveReading = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
fun WeightScreenEnglishPreview() {
    HealthTrackTheme(darkTheme = true, language = AppLanguage.EN) {
        WeightScreen(
            state = populatedWeightState.copy(inputNote = "Feeling great"),
            onIntent = {}
        )
    }
}
