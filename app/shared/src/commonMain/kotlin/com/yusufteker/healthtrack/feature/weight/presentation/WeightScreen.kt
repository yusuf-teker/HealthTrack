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
import com.yusufteker.healthtrack.core.model.HealthPlatformStatus
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.utils.formatDisplayDateTr
import com.yusufteker.healthtrack.core.utils.rememberDeviceHealthBridge
import com.yusufteker.healthtrack.feature.weight.presentation.components.EditTargetWeightDialog
import com.yusufteker.healthtrack.feature.weight.presentation.components.WeightLineChart
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
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
    var showAddWeightDialog by remember { mutableStateOf(false) }

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

    if (showAddWeightDialog) {
        AddWeightDialog(
            state = state,
            onIntent = onIntent,
            onDismiss = { showAddWeightDialog = false }
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
                onOpenAddWeight = { showAddWeightDialog = true },
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
                    text = "Kilo Geçmişi",
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
                        text = "${state.entries.size} Kayıt",
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
                            text = "Henüz tartım kaydı bulunmuyor.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { showAddWeightDialog = true },
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("İlk Kilonuzu Ekleyin", fontWeight = FontWeight.SemiBold)
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
            onClick = { showAddWeightDialog = true },
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
                contentDescription = "Kilo Ekle",
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

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonitorWeight,
                        contentDescription = null,
                        tint = HealthColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Kilo Kaydet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearFocusOnTap()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.inputWeight,
                    onValueChange = { onIntent(WeightIntent.OnInputWeightChanged(it)) },
                    label = { Text("Kilo (kg)") },
                    placeholder = { Text("Örn: 74.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Opsiyonel Tarih ve Not Açıcı (Varsayılan olarak gizli, kalabalık yapmaz)
                Surface(
                    onClick = { showExtraFields = !showExtraFields },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().bounceClick()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 Tarih: ${state.inputDate}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (showExtraFields) "Gizle" else "Tarih / Not Değiştir",
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
                            label = { Text("Tarih (GG.AA.YYYY)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.inputNote,
                            onValueChange = { onIntent(WeightIntent.OnInputNoteChanged(it)) },
                            label = { Text("Not (Opsiyonel)") },
                            placeholder = { Text("Aç karna, antrenman sonrası...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onIntent(WeightIntent.OnSaveWeightClicked)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HealthColors.Primary,
                    contentColor = HealthColors.OnPrimary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kaydet", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
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
            // Header: Title & Trend Badge
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
                        text = "Vücut Ağırlığı",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        text = "Güncel Kilo",
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
                                text = "Hedef: $target kg",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = HealthColors.Primary
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Hedefi Düzenle",
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (percentage > 0) "%$percentage tamamlandı" else "Hedef belirlendi",
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
                        text = "Başlangıç: ${initial ?: "–"} kg",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Hedef: $target kg",
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
                    Text("Kilo Ekle", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            text = if (state.isSyncingScale) "Okunuyor..." else "⚖️ Tartıdan Çek",
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
                            contentDescription = "Tartı Nasıl Bağlanır?",
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

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = formatDisplayDateTr(entry.date),
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
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
