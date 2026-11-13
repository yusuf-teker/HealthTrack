package com.yusufteker.healthtrack.feature.weight.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yusufteker.healthtrack.core.model.HealthPlatformStatus
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.utils.formatDisplayDateTr
import com.yusufteker.healthtrack.core.utils.rememberDeviceHealthBridge
import com.yusufteker.healthtrack.feature.weight.presentation.components.EditTargetWeightDialog
import com.yusufteker.healthtrack.feature.weight.presentation.components.WeightLineChart
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.DeviceConnectionGuideDialog
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.HealthToastHost
import com.yusufteker.healthtrack.ui.components.ToastType
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import io.github.aakira.napier.Napier

@Composable
fun WeightScreen(
    state: WeightState,
    onIntent: (WeightIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceHealthBridge = rememberDeviceHealthBridge()
    var showDeviceGuideDialog by remember { mutableStateOf(false) }

    // Auto-sync smart scale & weight silently on screen entry if permissions are already authorized
    LaunchedEffect(Unit) {
        deviceHealthBridge.syncSilentlyIfAuthorized(days = 30) { result ->
            onIntent(WeightIntent.OnScaleSnapshotReceived(result = result, isSilent = true))
        }
    }

    if (showDeviceGuideDialog) {
        DeviceConnectionGuideDialog(
            platformName = deviceHealthBridge.platformName,
            onOpenHealthSettings = { deviceHealthBridge.openHealthSettings() },
            onSyncNow = {
                onIntent(WeightIntent.OnScaleSyncStarted)
                deviceHealthBridge.syncLastDays(days = 30) { result ->
                    onIntent(WeightIntent.OnScaleSnapshotReceived(result))
                }
            },
            onDismiss = { showDeviceGuideDialog = false }
        )
    }

    LaunchedEffect(state.isEditingTarget) {
        if (state.isEditingTarget) Napier.d("SCREEN: EditTargetWeightDialog açıldı")
    }

    if (state.isEditingTarget) {
        EditTargetWeightDialog(
            currentTargetWeight = state.targetWeightKg,
            onDismiss = { onIntent(WeightIntent.OnSetEditingTarget(false)) },
            onSaveTargetWeight = { newTarget ->
                onIntent(WeightIntent.OnSaveTargetWeight(newTarget))
            }
        )
    }

    if (state.isAddDialogOpen) {
        AddWeightDialog(
            state = state,
            onIntent = onIntent,
            onDismiss = { onIntent(WeightIntent.OnSetAddDialogOpen(false)) }
        )
    }

    if (state.isReminderDialogOpen) {
        com.yusufteker.healthtrack.feature.weight.presentation.components.WeightReminderDialog(
            currentFrequency = state.reminderFrequency,
            onDismiss = { onIntent(WeightIntent.OnSetReminderDialogOpen(false)) },
            onSave = { onIntent(WeightIntent.OnReminderFrequencyChanged(it)) }
        )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Current Bio-Mass Hero Card with Quick Actions ──
            CurrentBioMassHeroCard(
                state = state,
                showScaleSync = deviceHealthBridge.status != HealthPlatformStatus.NOT_SUPPORTED,
                onEditTarget = { onIntent(WeightIntent.OnSetEditingTarget(true)) },
                onOpenAddWeight = { onIntent(WeightIntent.OnSetAddDialogOpen(true)) },
                onOpenReminder = { onIntent(WeightIntent.OnSetReminderDialogOpen(true)) },
                onSyncScale = {
                    if (!state.isSyncingScale) {
                        onIntent(WeightIntent.OnScaleSyncStarted)
                        deviceHealthBridge.syncLastDays(days = 30) { result ->
                            onIntent(WeightIntent.OnScaleSnapshotReceived(result))
                        }
                    }
                },
                onOpenGuide = { showDeviceGuideDialog = true }
            )

            // ── Interactive Telemetry Line Chart ──
            WeightLineChart(
                entries = state.filteredEntries,
                targetWeightKg = state.targetWeightKg,
                selectedPeriod = state.selectedPeriod,
                onPeriodSelect = { onIntent(WeightIntent.OnPeriodSelected(it)) }
            )

            // ── History List (Directly Visible Above the Fold) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = str("Kilo Geçmişi", "Weight History"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = str("${state.entries.size} Kayıt", "${state.entries.size} Entries"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.entries.isEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = str("Henüz tartım kaydı bulunmuyor.", "No weigh-in entries yet."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { onIntent(WeightIntent.OnSetAddDialogOpen(true)) },
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(str("İlk Kilonuzu Ekleyin", "Add Your First Weigh-in"), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.entries.reversed().forEach { item ->
                        LuxuryWeightHistoryItem(
                            entry = item,
                            onDelete = { onIntent(WeightIntent.OnDeleteWeight(item.id)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }

        // ── Floating Action Button: Clean '+' FAB ──
        FloatingActionButton(
            onClick = { onIntent(WeightIntent.OnSetAddDialogOpen(true)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .size(56.dp)
                .bounceClick(),
            containerColor = HealthColors.Primary,
            contentColor = HealthColors.OnPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = str("Kilo Ekle", "Add Weight"),
                tint = HealthColors.OnPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        // ── Floating Toast Notifications (Auto-dismissing) ──
        HealthToastHost(
            message = state.errorMessage,
            type = ToastType.ERROR,
            onDismiss = { onIntent(WeightIntent.OnDismissErrorMessage) }
        )
        HealthToastHost(
            message = state.infoMessage,
            type = ToastType.SUCCESS,
            onDismiss = { onIntent(WeightIntent.OnDismissInfoMessage) }
        )
    }
}

@Composable
private fun AddWeightDialog(
    state: WeightState,
    onIntent: (WeightIntent) -> Unit,
    onDismiss: () -> Unit
) {
    var showExtraFields by remember { mutableStateOf(false) }
    val isDark = LocalIsDarkTheme.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .imePadding()
                .clearFocusOnTap(),
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) HealthColors.SurfaceContainer else Color.White,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(HealthColors.Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonitorWeight,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = str("Kilo Kaydet", "Log Weight"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = str("Günlük tartım verinizi girin", "Enter your daily weigh-in"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).bounceClick()
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = str("Kapat", "Close"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Input Field
                OutlinedTextField(
                    value = state.inputWeight,
                    onValueChange = { onIntent(WeightIntent.OnInputWeightChanged(it)) },
                    label = { Text(str("Kilo (kg)", "Weight (kg)")) },
                    placeholder = { Text(str("Örn: 74.5", "e.g. 74.5")) },
                    isError = state.errorMessage != null,
                    supportingText = state.errorMessage?.let { err -> { Text(err) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Optional Date and Note toggle
                Surface(
                    onClick = { showExtraFields = !showExtraFields },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().bounceClick()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = str("📅 Tarih: ${state.inputDate}", "📅 Date: ${state.inputDate}"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (showExtraFields) str("Gizle", "Hide") else str("Tarih / Not Değiştir", "Edit Date / Note"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = HealthColors.Primary
                        )
                    }
                }

                AnimatedVisibility(visible = showExtraFields) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = state.inputDate,
                            onValueChange = { onIntent(WeightIntent.OnInputDateChanged(it)) },
                            label = { Text(str("Tarih (GG.AA.YYYY)", "Date (DD.MM.YYYY)")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.inputNote,
                            onValueChange = { onIntent(WeightIntent.OnInputNoteChanged(it)) },
                            label = { Text(str("Not (Opsiyonel)", "Note (Optional)")) },
                            placeholder = { Text(str("Aç karna, antrenman sonrası...", "Fasted morning, post-workout...")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(str("Vazgeç", "Cancel"))
                    }

                    Button(
                        onClick = {
                            onIntent(WeightIntent.OnSaveWeightClicked)
                        },
                        modifier = Modifier.weight(1.3f).height(48.dp).bounceClick(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HealthColors.Primary,
                            contentColor = HealthColors.OnPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(str("Kaydet", "Save"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Stitch Current Bio-Mass Hero Card with Goal Progress Track & Quick Actions.
 */
@Composable
private fun CurrentBioMassHeroCard(
    state: WeightState,
    showScaleSync: Boolean,
    onEditTarget: () -> Unit,
    onOpenAddWeight: () -> Unit,
    onOpenReminder: () -> Unit,
    onSyncScale: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val current = state.currentWeightKg
    val initial = state.initialWeightKg
    val target = state.targetWeightKg

    // Calculate goal progress percentage
    val progress = if (initial != null && current != null && initial != target) {
        val totalDelta = initial - target
        val currentDelta = initial - current
        (currentDelta / totalDelta).coerceIn(0.0, 1.0).toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800)
    )
    val percentage = (progress * 100).toInt()

    val change = state.weightChangeKg
    val changeRounded = if (change != null) ((change * 10).toInt() / 10.0) else null

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title & Trend Badge & Reminder Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonitorWeight,
                        contentDescription = null,
                        tint = HealthColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = str("Vücut Ağırlığı", "Body Weight"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Reminder Pill
                    val isReminderOff = state.reminderFrequency == com.yusufteker.healthtrack.core.model.WeightReminderFrequency.OFF

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isReminderOff) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else HealthColors.Primary.copy(alpha = 0.15f),
                        border = BorderStroke(
                            1.dp,
                            if (isReminderOff) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f) else HealthColors.Primary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onOpenReminder() }
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = str("Tartı Hatırlatıcısı", "Weigh-in Reminder"),
                                tint = if (isReminderOff) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                } else {
                                    HealthColors.Primary
                                },
                                modifier = Modifier.size(13.dp)
                            )

                        }
                    }

                    if (changeRounded != null) {
                        val isLoss = changeRounded <= 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (isLoss) Color(0xFF4CAF50).copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isLoss) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isLoss) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${if (changeRounded > 0) "+" else ""}$changeRounded kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLoss) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Big Metric Hero Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (current != null) "$current" else "–",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "kg",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                    Text(
                        text = str("Güncel Kilo", "Current Weight"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        onClick = onEditTarget,
                        shape = RoundedCornerShape(12.dp),
                        color = HealthColors.Primary.copy(alpha = 0.12f),
                        modifier = Modifier.bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = str("Hedef: $target kg", "Goal: $target kg"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = HealthColors.Primary
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = str("Hedefi Düzenle", "Edit Goal"),
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (percentage > 0) str("%$percentage tamamlandı", "$percentage% completed") else str("Hedef belirlendi", "Goal set"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Segmented Goal Progress Track
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isDark) HealthColors.SurfaceContainerHighest.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(HealthColors.Primary, HealthColors.PrimaryContainer)
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = str("Başlangıç: ${initial ?: "–"} kg", "Start: ${initial ?: "–"} kg"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = str("Hedef: $target kg", "Goal: $target kg"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = HealthColors.Primary
                    )
                }
            }

            // Compact Action Bar: [+ Kilo Ekle] & [⚖️ Tartıdan Çek]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenAddWeight,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HealthColors.Primary,
                        contentColor = HealthColors.OnPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(str("Kilo Ekle", "Add Weight"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (showScaleSync) {
                    OutlinedButton(
                        onClick = onSyncScale,
                        modifier = Modifier
                            .height(42.dp)
                            .bounceClick(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HealthColors.Primary.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        if (state.isSyncingScale) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = HealthColors.Primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (state.isSyncingScale) str("Okunuyor...", "Reading...") else str("⚖️ Tartıdan Çek", "⚖️ Sync Scale"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = HealthColors.Primary
                        )
                    }

                    IconButton(
                        onClick = onOpenGuide,
                        modifier = Modifier.size(34.dp).bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = str("Tartı Nasıl Bağlanır?", "How to Connect Scale?"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LuxuryWeightHistoryItem(
    entry: WeightEntry,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(HealthColors.Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MonitorWeight,
                        contentDescription = null,
                        tint = HealthColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                val lang = appLang()
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = formatDisplayDateTr(entry.date, lang),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    entry.note?.takeIf { it.isNotBlank() }?.let { n ->
                        Text(
                            text = n,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${entry.weightKg} kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = str("Sil", "Delete"),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
