package com.yusufteker.healthtrack.feature.weight.presentation

import com.yusufteker.healthtrack.core.model.WeightChartPeriod
import com.yusufteker.healthtrack.core.model.WeightEntry

data class WeightState(
    val entries: List<WeightEntry> = emptyList(),
    val filteredEntries: List<WeightEntry> = emptyList(),
    val selectedPeriod: WeightChartPeriod = WeightChartPeriod.WEEKLY,
    val currentWeightKg: Double? = null,
    val initialWeightKg: Double? = null,
    val weightChangeKg: Double? = null,
    val targetWeightKg: Double = 70.0,
    val inputDate: String = "2026-11-04",
    val inputWeight: String = "",
    val inputNote: String = "",
    val inputTargetWeight: String = "70.0",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

sealed interface WeightIntent {
    data class OnPeriodSelected(val period: WeightChartPeriod) : WeightIntent
    data class OnInputDateChanged(val date: String) : WeightIntent
    data class OnInputWeightChanged(val weight: String) : WeightIntent
    data class OnInputNoteChanged(val note: String) : WeightIntent
    data class OnInputTargetWeightChanged(val target: String) : WeightIntent
    data object OnSaveWeightClicked : WeightIntent
    data class OnDeleteWeight(val id: String) : WeightIntent
    data object OnDismissErrorMessage : WeightIntent
    data object OnDismissInfoMessage : WeightIntent
}

sealed interface WeightEffect {
    data class ShowToast(val message: String) : WeightEffect
}
