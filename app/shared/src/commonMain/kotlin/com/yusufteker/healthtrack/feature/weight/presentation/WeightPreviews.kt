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
import com.yusufteker.healthtrack.core.model.WeightChartPeriod
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.feature.weight.presentation.components.WeightLineChart
import com.yusufteker.healthtrack.theme.HealthTrackTheme

private val sampleWeightEntries = listOf(
    WeightEntry(
        id = "w1",
        userId = 1,
        date = "2026-11-01",
        weightKg = 74.2,
        targetWeightKg = 70.0,
        note = "Sabah aç karnına"
    ),
    WeightEntry(
        id = "w2",
        userId = 1,
        date = "2026-11-02",
        weightKg = 73.8,
        targetWeightKg = 70.0,
        note = "Kardiyo sonrası"
    ),
    WeightEntry(
        id = "w3",
        userId = 1,
        date = "2026-11-03",
        weightKg = 73.5,
        targetWeightKg = 70.0
    ),
    WeightEntry(
        id = "w4",
        userId = 1,
        date = "2026-11-04",
        weightKg = 73.1,
        targetWeightKg = 70.0
    ),
    WeightEntry(
        id = "w5",
        userId = 1,
        date = "2026-11-05",
        weightKg = 72.8,
        targetWeightKg = 70.0
    ),
    WeightEntry(
        id = "w6",
        userId = 1,
        date = "2026-11-06",
        weightKg = 72.4,
        targetWeightKg = 70.0,
        note = "Hedefe 2.4 kg kaldı!"
    )
)

private val populatedWeightState = WeightState(
    entries = sampleWeightEntries,
    filteredEntries = sampleWeightEntries,
    selectedPeriod = WeightChartPeriod.WEEKLY,
    currentWeightKg = 72.4,
    initialWeightKg = 74.2,
    weightChangeKg = -1.8,
    targetWeightKg = 70.0,
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
            onDismiss = {},
            onSave = {}
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
fun WeightScreenEnglishPreview() {
    HealthTrackTheme(darkTheme = true, language = AppLanguage.EN) {
        WeightScreen(
            state = populatedWeightState.copy(inputNote = "Feeling great"),
            onIntent = {}
        )
    }
}
