package com.yusufteker.healthtrack.feature.analytics.presentation

import com.yusufteker.healthtrack.core.model.AnalysisPeriodType
import com.yusufteker.healthtrack.core.model.HealthAiReport
import com.yusufteker.healthtrack.core.utils.formatDisplayDateTr
import com.yusufteker.healthtrack.core.utils.formatWeekRangeTr
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.core.utils.getWeekBoundsIso

data class AnalyticsState(
    val userName: String = "",
    val periodType: AnalysisPeriodType = AnalysisPeriodType.DAILY,
    val selectedDate: String = getTodayIso(),
    val selectedWeekMonday: String = getWeekBoundsIso(getTodayIso()).first,
    val selectedWeekSunday: String = getWeekBoundsIso(getTodayIso()).second,
    val currentReport: HealthAiReport? = null,
    val recentReports: List<HealthAiReport> = emptyList(),
    val pendingAutoWeeklyReport: HealthAiReport? = null,
    val whatsAppPhone: String = "",
    val autoWeeklyWhatsApp: Boolean = true,
    val isGenerating: Boolean = false,
    // Live preview stats for selected period before/during report generation
    val previewCaloriesIntake: Int = 0,
    val previewTargetCalories: Int = 2000,
    val previewCaloriesBurned: Int = 0,
    val previewWorkoutMinutes: Int = 0,
    val previewProteinG: Int = 0,
    val previewTargetProteinG: Int = 100,
    val previewWeightKg: Double? = null,
    val previewLoggedDays: Int = 0,
    val infoMessage: String? = null,
    val errorMessage: String? = null
) {
    val selectedDayLabel: String
        get() = formatDisplayDateTr(selectedDate)

    val selectedWeekLabel: String
        get() = formatWeekRangeTr(selectedWeekMonday, selectedWeekSunday)
}

sealed interface AnalyticsIntent {
    data class OnPeriodTypeChanged(val periodType: AnalysisPeriodType) : AnalyticsIntent
    data class OnDateSelected(val dateIso: String) : AnalyticsIntent
    data class OnShiftDay(val deltaDays: Int) : AnalyticsIntent
    data class OnShiftWeek(val deltaWeeks: Int) : AnalyticsIntent
    data class OnSelectWeekByDate(val dateInWeekIso: String) : AnalyticsIntent
    data object OnGenerateReportClicked : AnalyticsIntent
    data class OnSelectSavedReport(val report: HealthAiReport) : AnalyticsIntent
    data class OnDeleteReport(val reportId: String) : AnalyticsIntent
    data class OnWhatsAppPhoneChanged(val phone: String) : AnalyticsIntent
    data class OnAutoWeeklyWhatsAppToggled(val enabled: Boolean) : AnalyticsIntent
    data object OnSaveWhatsAppSettingsClicked : AnalyticsIntent
    data class OnReportSentToWhatsApp(val reportId: String) : AnalyticsIntent
    data object OnDismissPendingAutoReport : AnalyticsIntent
    data object OnDismissInfoMessage : AnalyticsIntent
    data object OnDismissErrorMessage : AnalyticsIntent
    data class OnShowInfoMessage(val message: String) : AnalyticsIntent
}

sealed interface AnalyticsEffect {
    data class ShareToWhatsApp(val message: String, val phoneNumber: String?, val reportId: String) : AnalyticsEffect
}

