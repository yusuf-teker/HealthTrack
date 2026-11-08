package com.yusufteker.healthtrack.feature.weight.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.utils.formatDisplayDateTr
import com.yusufteker.healthtrack.feature.weight.presentation.components.EditTargetWeightDialog
import io.github.aakira.napier.Napier
import com.yusufteker.healthtrack.feature.weight.presentation.components.WeightLineChart
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.HealthToastHost
import com.yusufteker.healthtrack.ui.components.ToastType
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

@Composable
fun WeightScreen(
    state: WeightState,
    onIntent: (WeightIntent) -> Unit,
    modifier: Modifier = Modifier
) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

        // ── Current Bio-Mass Hero Card with Segmented Goal Progress (Stitch) ──
        CurrentBioMassHeroCard(
            state = state,
            onEditTarget = { onIntent(WeightIntent.OnSetEditingTarget(true)) }
        )

        // ── Interactive Telemetry Line Chart ──
        WeightLineChart(
            entries = state.filteredEntries,
            targetWeightKg = state.targetWeightKg,
            selectedPeriod = state.selectedPeriod,
            onPeriodSelect = { onIntent(WeightIntent.OnPeriodSelected(it)) }
        )

        // ── New Weight Input Form Card ──
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(HealthColors.Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = HealthColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Günlük Kilo Ekle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.33f)
                val calmFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HealthColors.Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = state.inputDate,
                        onValueChange = { onIntent(WeightIntent.OnInputDateChanged(it)) },
                        label = { Text("Tarih (GG.AA.YYYY)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = calmFieldColors,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.inputWeight,
                        onValueChange = { onIntent(WeightIntent.OnInputWeightChanged(it)) },
                        label = { Text("Kilo (kg)") },
                        placeholder = { Text("Örn: 74.5", color = placeholderColor) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = calmFieldColors,
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = state.inputNote,
                    onValueChange = { onIntent(WeightIntent.OnInputNoteChanged(it)) },
                    label = { Text("Not (Opsiyonel)") },
                    placeholder = { Text("Aç karna, antrenman sonrası...", color = placeholderColor) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = calmFieldColors,
                    singleLine = true
                )

                Button(
                    onClick = { onIntent(WeightIntent.OnSaveWeightClicked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HealthColors.Primary,
                        contentColor = HealthColors.OnPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kilo Kaydet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        // ── History List ──
        if (state.entries.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.entries.reversed().forEach { item ->
                    LuxuryWeightHistoryItem(
                        entry = item,
                        onDelete = { onIntent(WeightIntent.OnDeleteWeight(item.id)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
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

/**
 * Stitch Current Bio-Mass Hero Card with Goal Progress Track.
 */
@Composable
private fun CurrentBioMassHeroCard(
    state: WeightState,
    onEditTarget: () -> Unit
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = "Vücut Ağırlığı & Kompozisyon",
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
                    Text(
                        text = if (current != null) "$current" else "–",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "kg • Güncel Kilo",
                        style = MaterialTheme.typography.bodySmall,
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
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = HealthColors.Primary
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Hedefi Düzenle",
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(14.dp)
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(HealthColors.Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MonitorWeight,
                        contentDescription = null,
                        tint = HealthColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = formatDisplayDateTr(entry.date),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    entry.note?.let { n ->
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
