package com.yusufteker.healthtrack.feature.weight.presentation

import com.yusufteker.healthtrack.core.model.DeviceHealthSnapshot
import com.yusufteker.healthtrack.core.model.WeightChartMetric
import com.yusufteker.healthtrack.core.model.WeightChartPeriod
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.utils.BleScaleReading
import com.yusufteker.healthtrack.core.utils.formatShortDateTr
import com.yusufteker.healthtrack.core.utils.getTodayIso

data class WeightState(
    val entries: List<WeightEntry> = emptyList(),
    val filteredEntries: List<WeightEntry> = emptyList(),
    val selectedPeriod: WeightChartPeriod = WeightChartPeriod.WEEKLY,
    val selectedMetric: WeightChartMetric = WeightChartMetric.WEIGHT,
    val currentWeightKg: Double? = null,
    val initialWeightKg: Double? = null,
    val weightChangeKg: Double? = null,
    val targetWeightKg: Double = 70.0,
    val currentBodyFatPercent: Double? = null,
    val bodyFatChangePercent: Double? = null,
    val currentMuscleMassKg: Double? = null,
    val currentMusclePercent: Double? = null,
    val musclePercentChange: Double? = null,
    val inputDate: String = formatShortDateTr(getTodayIso()),
    val inputWeight: String = "",
    val inputBodyFat: String = "",
    val inputMuscleValue: String = "",
    val inputMuscleIsPercent: Boolean = true,
    val inputNote: String = "",
    val isAddDialogOpen: Boolean = false,
    val isBleScaleSheetOpen: Boolean = false,
    val userHeightCm: Double = 175.0,
    val userAge: Int = 26,
    val userIsMale: Boolean = true,
    val isEditingTarget: Boolean = false,
    val isSaving: Boolean = false,
    val isSyncingScale: Boolean = false,
    val reminderFrequency: com.yusufteker.healthtrack.core.model.WeightReminderFrequency = com.yusufteker.healthtrack.core.model.WeightReminderFrequency.EVERY_3_DAYS,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val isReminderDialogOpen: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

sealed interface WeightIntent {
    data class OnPeriodSelected(val period: WeightChartPeriod) : WeightIntent
    data class OnMetricSelected(val metric: WeightChartMetric) : WeightIntent
    data class OnInputDateChanged(val date: String) : WeightIntent
    data class OnInputWeightChanged(val weight: String) : WeightIntent
    data class OnInputBodyFatChanged(val bodyFat: String) : WeightIntent
    data class OnInputMuscleValueChanged(val muscleValue: String) : WeightIntent
    data object OnToggleInputMuscleUnit : WeightIntent
    data class OnInputNoteChanged(val note: String) : WeightIntent
    data object OnSaveWeightClicked : WeightIntent
    data class OnSetBleScaleSheetOpen(val isOpen: Boolean) : WeightIntent
    data class OnSaveBleScaleReading(val reading: BleScaleReading) : WeightIntent
    data object OnScaleSyncStarted : WeightIntent
    data class OnScaleSnapshotReceived(
        val result: Result<DeviceHealthSnapshot>,
        val isSilent: Boolean = false
    ) : WeightIntent
    data class OnDeleteWeight(val id: String) : WeightIntent
    data class OnSetAddDialogOpen(val isOpen: Boolean) : WeightIntent
    data class OnSetEditingTarget(val isEditing: Boolean) : WeightIntent
    data class OnSaveTargetWeight(val newTargetWeight: Double) : WeightIntent
    data class OnReminderFrequencyChanged(val frequency: com.yusufteker.healthtrack.core.model.WeightReminderFrequency) : WeightIntent
    data class OnReminderSettingsChanged(
        val frequency: com.yusufteker.healthtrack.core.model.WeightReminderFrequency,
        val hour: Int,
        val minute: Int
    ) : WeightIntent
    data class OnSetReminderDialogOpen(val isOpen: Boolean) : WeightIntent
    data object OnDismissErrorMessage : WeightIntent
    data object OnDismissInfoMessage : WeightIntent
}

sealed interface WeightEffect {
    data class ShowToast(val message: String) : WeightEffect
}
