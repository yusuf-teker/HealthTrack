package com.yusufteker.healthtrack.feature.analytics.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.AnalysisPeriodType
import com.yusufteker.healthtrack.core.model.HealthAiReport
import com.yusufteker.healthtrack.core.utils.formatWeekRangeTr
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.core.utils.getWeekBoundsIso
import com.yusufteker.healthtrack.core.utils.rememberExternalAppLauncher
import com.yusufteker.healthtrack.core.utils.rememberWeeklyReportNotifier
import com.yusufteker.healthtrack.core.utils.shiftDateIso
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.DaysStripSelector
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.HealthToastHost
import com.yusufteker.healthtrack.ui.components.ShimmeringAiButton
import com.yusufteker.healthtrack.ui.components.SpotlightTargetId
import com.yusufteker.healthtrack.ui.components.ToastType
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import com.yusufteker.healthtrack.ui.components.spotlightTarget

private val WhatsAppGreen = Color(0xFF25D366)

@Composable
fun AnalyticsScreen(
    state: AnalyticsState,
    onIntent: (AnalyticsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = appLang()
    val appLauncher = rememberExternalAppLauncher()
    val reportNotifier = rememberWeeklyReportNotifier()
    val scrollState = rememberScrollState()

    var testNotificationInfo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.autoWeeklyWhatsApp) {
        if (state.autoWeeklyWhatsApp) {
            reportNotifier.scheduleSundayEveningReminder(hour = 21, minute = 0)
        } else {
            reportNotifier.cancelSundayEveningReminder()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. Otomatik Hafta Sonu Raporu Hazır Banner'ı (Varsa) ──
            AnimatedVisibility(
                visible = state.pendingAutoWeeklyReport != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.pendingAutoWeeklyReport?.let { autoReport ->
                    AutoWeeklyReportReadyBanner(
                        report = autoReport,
                        onViewInApp = {
                            onIntent(AnalyticsIntent.OnSelectSavedReport(autoReport))
                            onIntent(AnalyticsIntent.OnDismissPendingAutoReport)
                        },
                        onSendWhatsApp = {
                            val formatted = autoReport.toWhatsAppMessage(state.userName, lang)
                            appLauncher.shareToWhatsApp(formatted, state.whatsAppPhone)
                            onIntent(AnalyticsIntent.OnReportSentToWhatsApp(autoReport.id))
                        },
                        onDismiss = { onIntent(AnalyticsIntent.OnDismissPendingAutoReport) }
                    )
                }
            }

            // ── 2. Dönem Seçici: Günlük Analiz vs. Haftalık Analiz ──
            PeriodModeToggleCard(
                selectedPeriod = state.periodType,
                onSelectPeriod = { onIntent(AnalyticsIntent.OnPeriodTypeChanged(it)) }
            )

            // ── 3. Tarih / Hafta Seçim Kontrolü ──
            if (state.periodType == AnalysisPeriodType.DAILY) {
                // Günlük Seçim: DaysStripSelector + Önceki/Sonraki Gün
                DaysStripSelector(
                    selectedDate = state.selectedDate,
                    onDateSelected = { onIntent(AnalyticsIntent.OnDateSelected(it)) }
                )
            } else {
                // Haftalık Seçim: Önceki Hafta / Seçili Hafta / Sonraki Hafta + Son 4 Hafta Çipleri
                WeekSelectorCard(
                    selectedMonday = state.selectedWeekMonday,
                    selectedWeekLabel = state.localizedSelectedWeekLabel(lang),
                    onShiftWeek = { delta -> onIntent(AnalyticsIntent.OnShiftWeek(delta)) },
                    onSelectWeekMonday = { monday -> onIntent(AnalyticsIntent.OnSelectWeekByDate(monday)) }
                )
            }

            // ── 4. Seçili Gün / Hafta Canlı Özet & AI Analiz Başlatma Kartı ──
            SelectionSummaryAndGenerateCard(
                state = state,
                onGenerateClick = { onIntent(AnalyticsIntent.OnGenerateReportClicked) }
            )

            // ── 5. Aktif AI Raporu Detay Görünümü + WhatsApp Paylaşım ──
            val activeReport = state.currentReport
            if (activeReport != null) {
                AiReportDetailCard(
                    report = activeReport,
                    onShareWhatsApp = {
                        val formatted = activeReport.toWhatsAppMessage(state.userName, lang)
                        appLauncher.shareToWhatsApp(formatted, state.whatsAppPhone)
                        onIntent(AnalyticsIntent.OnReportSentToWhatsApp(activeReport.id))
                    },
                    modifier = Modifier.spotlightTarget(SpotlightTargetId.ANALYTICS_REPORT_CARD)
                )
            }

            var hasNotificationPerm by remember { mutableStateOf(reportNotifier.hasPermission) }
            val permGrantedMsg = str(
                "🔔 Bildirim izni verildi! Haftalık raporlarınız kilit ekranınıza gelecek.",
                "🔔 Notification permission granted! Your weekly reports will arrive on your lock screen."
            )
            val permDeniedMsg = str(
                "⚠️ Bildirim izni verilmedi. Ayarlardan bildirimlere izin verebilirsiniz.",
                "⚠️ Notification permission denied. You can enable notifications in Settings."
            )
            val testNotifSentMsg = str(
                "🔔 Test bildirimi gönderildi! Bildirime dokunarak doğrudan WhatsApp'ı açabilirsiniz.",
                "🔔 Test notification sent! Tap the notification to open WhatsApp directly."
            )
            val reportPreparingMsg = str(
                "Rapor hazırlanıyor, tamamlanınca bildirimi test edebilirsiniz.",
                "Generating report, you can test the notification once it is ready."
            )

            // ── 6. WhatsApp Otomatik Haftalık Rapor & Push Bildirim Kartı ──
            WhatsAppAutomationCard(
                whatsAppPhone = state.whatsAppPhone,
                autoWeeklyEnabled = state.autoWeeklyWhatsApp,
                hasNotificationPermission = hasNotificationPerm,
                onRequestNotificationPermission = {
                    reportNotifier.requestPermission { granted ->
                        hasNotificationPerm = granted
                        if (granted) {
                            onIntent(AnalyticsIntent.OnShowInfoMessage(permGrantedMsg))
                        } else {
                            onIntent(AnalyticsIntent.OnShowInfoMessage(permDeniedMsg))
                        }
                    }
                },
                onSendTestNotification = {
                    val targetReport = state.currentReport
                        ?: state.recentReports.firstOrNull { it.periodType == AnalysisPeriodType.WEEKLY }
                        ?: state.recentReports.firstOrNull()
                    if (targetReport != null) {
                        reportNotifier.sendReportNotification(
                            report = targetReport,
                            whatsAppPhone = state.whatsAppPhone,
                            userName = state.userName
                        )
                        onIntent(AnalyticsIntent.OnShowInfoMessage(testNotifSentMsg))
                    } else {
                        onIntent(AnalyticsIntent.OnPeriodTypeChanged(AnalysisPeriodType.WEEKLY))
                        onIntent(AnalyticsIntent.OnGenerateReportClicked)
                        onIntent(AnalyticsIntent.OnShowInfoMessage(reportPreparingMsg))
                    }
                },
                onPhoneChange = { onIntent(AnalyticsIntent.OnWhatsAppPhoneChanged(it)) },
                onAutoToggle = { onIntent(AnalyticsIntent.OnAutoWeeklyWhatsAppToggled(it)) },
                onSaveSettings = { onIntent(AnalyticsIntent.OnSaveWhatsAppSettingsClicked) },
                onSendLatestWeeklyNow = {
                    val weekly = state.currentReport?.takeIf { it.periodType == AnalysisPeriodType.WEEKLY }
                        ?: state.recentReports.firstOrNull { it.periodType == AnalysisPeriodType.WEEKLY }
                        ?: state.currentReport
                    if (weekly != null) {
                        val formatted = weekly.toWhatsAppMessage(state.userName, lang)
                        appLauncher.shareToWhatsApp(formatted, state.whatsAppPhone)
                        onIntent(AnalyticsIntent.OnReportSentToWhatsApp(weekly.id))
                    } else {
                        onIntent(AnalyticsIntent.OnPeriodTypeChanged(AnalysisPeriodType.WEEKLY))
                        onIntent(AnalyticsIntent.OnGenerateReportClicked)
                    }
                }
            )

            // ── 7. Kayıtlı AI Rapor Geçmişi (Hem Günlük Hem Haftalık) ──
            if (state.recentReports.isNotEmpty()) {
                Text(
                    text = str(
                        "Kayıtlı AI Rapor Arşivi (${state.recentReports.size})",
                        "Saved AI Report Archive (${state.recentReports.size})"
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.recentReports.forEach { item ->
                        SavedReportHistoryRow(
                            report = item,
                            isSelected = state.currentReport?.id == item.id,
                            onSelect = { onIntent(AnalyticsIntent.OnSelectSavedReport(item)) },
                            onShareWhatsApp = {
                                val formatted = item.toWhatsAppMessage(state.userName, lang)
                                appLauncher.shareToWhatsApp(formatted, state.whatsAppPhone)
                                onIntent(AnalyticsIntent.OnReportSentToWhatsApp(item.id))
                            },
                            onDelete = { onIntent(AnalyticsIntent.OnDeleteReport(item.id)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        HealthToastHost(
            message = state.errorMessage,
            type = ToastType.ERROR,
            onDismiss = { onIntent(AnalyticsIntent.OnDismissErrorMessage) }
        )
        HealthToastHost(
            message = state.infoMessage,
            type = ToastType.SUCCESS,
            onDismiss = { onIntent(AnalyticsIntent.OnDismissInfoMessage) }
        )
    }
}

@Composable
private fun AutoWeeklyReportReadyBanner(
    report: HealthAiReport,
    onViewInApp: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = WhatsAppGreen.copy(alpha = 0.14f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, WhatsAppGreen.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = str("Haftalık Otomatik Raporunuz Hazır!", "Your Automatic Weekly Report Is Ready!"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = str(
                                "${report.periodLabel} • Skor: ${report.score}/100",
                                "${report.periodLabel} • Score: ${report.score}/100"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Close, contentDescription = str("Kapat", "Close"), modifier = Modifier.size(16.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewInApp,
                    modifier = Modifier.weight(1f).bounceClick(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(str("Raporu İncele", "View Report"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onSendWhatsApp,
                    modifier = Modifier.weight(1f).bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(str("WhatsApp'a At", "Send to WhatsApp"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PeriodModeToggleCard(
    selectedPeriod: AnalysisPeriodType,
    onSelectPeriod: (AnalysisPeriodType) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isDaily = selectedPeriod == AnalysisPeriodType.DAILY
            val isWeekly = selectedPeriod == AnalysisPeriodType.WEEKLY

            PeriodTabPill(
                title = str("Günlük AI Analiz", "Daily AI Analysis"),
                subtitle = str("İstediğin günü seç", "Pick any day"),
                icon = Icons.Rounded.Today,
                isSelected = isDaily,
                modifier = Modifier.weight(1f),
                onClick = { onSelectPeriod(AnalysisPeriodType.DAILY) }
            )

            PeriodTabPill(
                title = str("Haftalık AI Rapor", "Weekly AI Report"),
                subtitle = str("İstediğin haftayı seç", "Pick any week"),
                icon = Icons.Rounded.DateRange,
                isSelected = isWeekly,
                modifier = Modifier.weight(1f),
                onClick = { onSelectPeriod(AnalysisPeriodType.WEEKLY) }
            )
        }
    }
}

@Composable
private fun PeriodTabPill(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.bounceClick(),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeekSelectorCard(
    selectedMonday: String,
    selectedWeekLabel: String,
    onShiftWeek: (Int) -> Unit,
    onSelectWeekMonday: (String) -> Unit
) {
    val lang = appLang()
    val today = getTodayIso()
    val (thisWeekMonday, _) = getWeekBoundsIso(today)
    val recentWeeks = remember(thisWeekMonday, lang) {
        (0..3).map { offset ->
            val mon = shiftDateIso(thisWeekMonday, -offset * 7)
            val sun = shiftDateIso(mon, 6)
            val label = when (offset) {
                0 -> if (lang.code == "en") "This Week" else "Bu Hafta"
                1 -> if (lang.code == "en") "Last Week" else "Geçen Hafta"
                else -> formatWeekRangeTr(mon, sun, lang)
            }
            Triple(mon, sun, label)
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledIconButton(
                    onClick = { onShiftWeek(-1) },
                    modifier = Modifier.size(36.dp).bounceClick(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = str("Önceki Hafta", "Previous Week"))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = str("SEÇİLİ HAFTA ARALIĞI", "SELECTED WEEK RANGE"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = selectedWeekLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledIconButton(
                    onClick = { onShiftWeek(1) },
                    modifier = Modifier.size(36.dp).bounceClick(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = str("Sonraki Hafta", "Next Week"))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentWeeks.forEach { (mon, _, chipTitle) ->
                    val isSelected = mon == selectedMonday
                    Surface(
                        onClick = { onSelectWeekMonday(mon) },
                        shape = RoundedCornerShape(999.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        modifier = Modifier.bounceClick()
                    ) {
                        Text(
                            text = chipTitle,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionSummaryAndGenerateCard(
    state: AnalyticsState,
    onGenerateClick: () -> Unit
) {
    val lang = appLang()
    val isDaily = state.periodType == AnalysisPeriodType.DAILY
    val periodTitle = if (isDaily) state.localizedSelectedDayLabel(lang) else state.localizedSelectedWeekLabel(lang)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isDaily) str("GÜNLÜK TELEMETRİ ÖZETİ", "DAILY TELEMETRY SUMMARY") else str("7 GÜNLÜK HAFTALIK ÖZET", "7-DAY WEEKLY SUMMARY"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = HealthColors.Primary
                    )
                    Text(
                        text = periodTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = HealthColors.Primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isDaily) {
                            if (state.previewLoggedDays > 0) str("Veri Mevcut", "Data Available") else str("Kayıt Bekleniyor", "Awaiting Logs")
                        } else {
                            str("${state.previewLoggedDays} / 7 Gün Kayıtlı", "${state.previewLoggedDays} / 7 Days Logged")
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = HealthColors.Primary
                    )
                }
            }

            // 4 Quick Telemetry Boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniMetricBox(
                    label = if (isDaily) str("Alınan", "Intake") else str("Ort. Kalori", "Avg. Calories"),
                    value = "${state.previewCaloriesIntake}",
                    unit = "/ ${state.previewTargetCalories} kcal",
                    accent = HealthColors.Primary,
                    modifier = Modifier.weight(1f)
                )
                MiniMetricBox(
                    label = if (isDaily) "Protein" else str("Ort. Protein", "Avg. Protein"),
                    value = "${state.previewProteinG}g",
                    unit = "/ ${state.previewTargetProteinG}g",
                    accent = HealthColors.NutrientProtein,
                    modifier = Modifier.weight(1f)
                )
                MiniMetricBox(
                    label = str("Spor Yakımı", "Workout Burn"),
                    value = "${state.previewCaloriesBurned}",
                    unit = str("${state.previewWorkoutMinutes} dk", "${state.previewWorkoutMinutes} min"),
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            ShimmeringAiButton(
                text = if (state.currentReport != null) {
                    if (isDaily) str("Günlük AI Analizini Yenile", "Refresh Daily AI Analysis") else str("Haftalık AI Raporunu Yenile", "Refresh Weekly AI Report")
                } else {
                    if (isDaily) str("Seçili Günü AI ile Analiz Et", "Analyze Selected Day with AI") else str("Seçili Haftayı AI ile Analiz Et", "Analyze Selected Week with AI")
                },
                isLoading = state.isGenerating,
                enabled = !state.isGenerating,
                onClick = onGenerateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .spotlightTarget(SpotlightTargetId.ANALYTICS_GENERATE_BUTTON),
                height = 50.dp
            )
        }
    }
}

@Composable
private fun MiniMetricBox(
    label: String,
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = accent
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AiReportDetailCard(
    report: HealthAiReport,
    onShareWhatsApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val scoreProgress = (report.score / 100f).coerceIn(0f, 1f)
    val animatedScore by animateFloatAsState(
        targetValue = scoreProgress,
        animationSpec = tween(durationMillis = 700)
    )

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header + Score Gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = if (isDark) HealthColors.SurfaceContainerHighest else MaterialTheme.colorScheme.surfaceVariant
                    val scoreColor = when {
                        report.score >= 80 -> HealthColors.Primary
                        report.score >= 60 -> HealthColors.NutrientCarbs
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 8.dp.toPx()
                        val d = size.minDimension - stroke
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(d, d),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = scoreColor,
                            startAngle = -90f,
                            sweepAngle = animatedScore * 360f,
                            useCenter = false,
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(d, d),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${report.score}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "/100",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (report.periodType == AnalysisPeriodType.DAILY) str("GÜNLÜK RAPOR", "DAILY REPORT") else str("HAFTALIK RAPOR", "WEEKLY REPORT"),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (report.isAutoGenerated) {
                            Surface(
                                color = WhatsAppGreen.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = str("OTOMATİK", "AUTO"),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = WhatsAppGreen
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = report.headline,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = report.periodLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Executive AI Summary
            Text(
                text = report.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Highlights
            if (report.highlights.isNotEmpty()) {
                ReportSectionBlock(
                    title = str("✅ Güçlü Yönler & Başarılar", "✅ Strengths & Achievements"),
                    items = report.highlights,
                    accentColor = HealthColors.Primary
                )
            }

            // Warnings
            if (report.warnings.isNotEmpty()) {
                ReportSectionBlock(
                    title = str("⚠️ Dikkat Edilmesi Gerekenler", "⚠️ Things to Watch"),
                    items = report.warnings,
                    accentColor = MaterialTheme.colorScheme.tertiary
                )
            }

            // Recommendations
            if (report.recommendations.isNotEmpty()) {
                ReportSectionBlock(
                    title = str("🎯 AI Koç Eylem Tavsiyeleri", "🎯 AI Coach Action Plan"),
                    items = report.recommendations,
                    accentColor = HealthColors.NutrientProtein
                )
            }

            // Share to WhatsApp Button
            Button(
                onClick = onShareWhatsApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhatsAppGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = str("📲 Raporu WhatsApp ile Gönder", "📲 Send Report via WhatsApp"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ReportSectionBlock(
    title: String,
    items: List<String>,
    accentColor: Color
) {
    Surface(
        color = accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            items.forEach { line ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun WhatsAppAutomationCard(
    whatsAppPhone: String,
    autoWeeklyEnabled: Boolean,
    hasNotificationPermission: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onSendTestNotification: () -> Unit = {},
    onPhoneChange: (String) -> Unit,
    onAutoToggle: (Boolean) -> Unit,
    onSaveSettings: () -> Unit,
    onSendLatestWeeklyNow: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(WhatsAppGreen.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Chat,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = str("Haftalık WhatsApp Otomasyonu", "Weekly WhatsApp Automation"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (autoWeeklyEnabled) {
                                str("Aktif (Pazar 21:00) • Ayarlar için dokunun", "Active (Sunday 21:00) • Tap for settings")
                            } else {
                                str("Kapalı • Ayarlar için dokunun", "Off • Tap for settings")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Rounded.Tune,
                            contentDescription = str("Otomasyon Ayarları", "Automation Settings"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Switch(
                        checked = autoWeeklyEnabled,
                        onCheckedChange = onAutoToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WhatsAppGreen
                        )
                    )
                }
            }

            // Collapsible Settings & Test Actions
            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    if (!hasNotificationPermission) {
                        Surface(
                            onClick = onRequestNotificationPermission,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth().bounceClick()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = str("Bildirim İznini Etkinleştir", "Enable Notification Permission"),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = str(
                                            "Rapor hazır olunca kilit ekranından tek dokunuşla WhatsApp açılsın.",
                                            "Open WhatsApp with a single tap from your lock screen when the report is ready."
                                        ),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = whatsAppPhone,
                        onValueChange = onPhoneChange,
                        label = { Text(str("Hedef WhatsApp Numarası (Opsiyonel)", "Target WhatsApp Number (Optional)")) },
                        placeholder = { Text(str("Örn: 05551234567", "e.g., +905551234567")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            TextButton(onClick = {
                                onSaveSettings()
                                isExpanded = false
                            }) {
                                Text(str("Kaydet", "Save"), fontWeight = FontWeight.Bold)
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSendTestNotification,
                            modifier = Modifier.weight(1f).bounceClick(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = str("Bildirim Testi", "Test Notification"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OutlinedButton(
                            onClick = onSendLatestWeeklyNow,
                            modifier = Modifier.weight(1f).bounceClick(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = str("Şimdi Gönder", "Send Now"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedReportHistoryRow(
    report: HealthAiReport,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${report.score}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = report.periodLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = if (report.periodType == AnalysisPeriodType.WEEKLY) {
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                            } else {
                                HealthColors.Primary.copy(alpha = 0.14f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (report.periodType == AnalysisPeriodType.WEEKLY) str("Haftalık", "Weekly") else str("Günlük", "Daily"),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = if (report.periodType == AnalysisPeriodType.WEEKLY) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    HealthColors.Primary
                                }
                            )
                        }
                    }
                    Text(
                        text = report.headline,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onShareWhatsApp,
                    modifier = Modifier.size(32.dp).bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = str("WhatsApp ile Gönder", "Send via WhatsApp"),
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = str("Sil", "Delete"),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

