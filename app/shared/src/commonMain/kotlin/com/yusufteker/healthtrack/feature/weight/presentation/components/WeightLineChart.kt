package com.yusufteker.healthtrack.feature.weight.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.WeightChartPeriod
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.utils.formatDisplayDateTr
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick
import kotlin.math.abs

/**
 * Stitch Luxury Interactive Weight Trajectory Line Chart with bioluminescent area glow and neon curve.
 */
@Composable
fun WeightLineChart(
    entries: List<WeightEntry>,
    targetWeightKg: Double,
    selectedPeriod: WeightChartPeriod,
    onPeriodSelect: (WeightChartPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val lang = appLang()
    var selectedIndex by remember(entries, selectedPeriod) { mutableStateOf<Int?>(null) }

    val latestEntry = entries.lastOrNull()
    val activeEntry = if (selectedIndex != null && selectedIndex!! in entries.indices) entries[selectedIndex!!] else latestEntry
    val activePrevEntry = if (selectedIndex != null && selectedIndex!! > 0) entries[selectedIndex!! - 1] else if (entries.size > 1) entries[entries.size - 2] else null

    val periodDelta = if (entries.size >= 2) entries.last().weightKg - entries.first().weightKg else null
    val deltaToDisplay = if (selectedIndex != null) {
        if (activePrevEntry != null && activeEntry != null) activeEntry.weightKg - activePrevEntry.weightKg else null
    } else periodDelta

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Chart Header & Timeframe Switcher Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = str("Kilo Eğrisi", "Weight Curve"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (selectedIndex != null) str("Noktaya dokunuldu", "Point selected") else str("Trend & İnterpolasyon", "Trend & Interpolation"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (selectedIndex != null) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Pill Nav Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isDark) HealthColors.SurfaceContainerLowest else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    PeriodPill(
                        label = str("Hafta", "Week"),
                        selected = selectedPeriod == WeightChartPeriod.WEEKLY,
                        onClick = {
                            selectedIndex = null
                            onPeriodSelect(WeightChartPeriod.WEEKLY)
                        }
                    )
                    PeriodPill(
                        label = str("Ay", "Month"),
                        selected = selectedPeriod == WeightChartPeriod.MONTHLY,
                        onClick = {
                            selectedIndex = null
                            onPeriodSelect(WeightChartPeriod.MONTHLY)
                        }
                    )
                    PeriodPill(
                        label = str("Yıl", "Year"),
                        selected = selectedPeriod == WeightChartPeriod.YEARLY,
                        onClick = {
                            selectedIndex = null
                            onPeriodSelect(WeightChartPeriod.YEARLY)
                        }
                    )
                    PeriodPill(
                        label = str("Tümü", "All"),
                        selected = selectedPeriod == WeightChartPeriod.ALL,
                        onClick = {
                            selectedIndex = null
                            onPeriodSelect(WeightChartPeriod.ALL)
                        }
                    )
                }
            }

            // Active Tooltip Callout Overlay matching Stitch
            if (activeEntry != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(HealthColors.Primary)
                        )
                        Text(
                            text = if (selectedIndex != null) {
                                str("Seçilen: ${formatDisplayDateTr(activeEntry.date, lang)}", "Selected: ${formatDisplayDateTr(activeEntry.date, lang)}")
                            } else {
                                str("Son: ${formatDisplayDateTr(activeEntry.date, lang)}", "Latest: ${formatDisplayDateTr(activeEntry.date, lang)}")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${activeEntry.weightKg} kg",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HealthColors.Primary
                        )
                        if (deltaToDisplay != null) {
                            val roundedDelta = ((deltaToDisplay * 10).toInt()) / 10.0
                            val sign = if (roundedDelta > 0) "+" else ""
                            val suffix = if (selectedIndex != null) {
                                str("önceki güne göre", "vs prev day")
                            } else when (selectedPeriod) {
                                WeightChartPeriod.WEEKLY -> str("bu hafta", "this week")
                                WeightChartPeriod.MONTHLY -> str("bu ay", "this month")
                                WeightChartPeriod.YEARLY -> str("bu yıl", "this year")
                                WeightChartPeriod.ALL -> str("toplam", "overall")
                            }
                            Text(
                                text = "($sign$roundedDelta kg $suffix)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (roundedDelta <= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )
                        } else if (selectedIndex == null && entries.size == 1) {
                            Text(
                                text = str("(Kayıtlı)", "(Logged)"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = str("Grafik için henüz yeterli kilo kaydı yok.", "Not enough weigh-ins for a chart yet."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Interactive SVG/Canvas Smooth Bezier Telemetry Chart
                val primaryColor = HealthColors.Primary
                val targetLineColor = MaterialTheme.colorScheme.tertiary
                val gridColor = if (isDark) HealthColors.SurfaceContainerHighest.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

                val weights = entries.map { it.weightKg }
                val minWeight = (weights.minOrNull() ?: targetWeightKg).coerceAtMost(targetWeightKg) - 1.5
                val maxWeight = (weights.maxOrNull() ?: targetWeightKg).coerceAtLeast(targetWeightKg) + 1.5
                val weightRange = (maxWeight - minWeight).coerceAtLeast(1.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .pointerInput(entries) {
                            detectTapGestures { offset ->
                                if (entries.isNotEmpty()) {
                                    val canvasW = size.width.toFloat()
                                    val stepX = if (entries.size > 1) canvasW / (entries.size - 1) else canvasW / 2f
                                    val closestIndex = entries.indices.minByOrNull { i ->
                                        val nodeX = if (entries.size > 1) i.toFloat() * stepX else canvasW / 2f
                                        abs(nodeX - offset.x)
                                    }
                                    if (closestIndex != null) {
                                        selectedIndex = if (selectedIndex == closestIndex) null else closestIndex
                                    }
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    // 1. Target Dashed Reference Line
                    val targetY = height - (((targetWeightKg - minWeight) / weightRange).toFloat() * height)
                    drawLine(
                        color = targetLineColor.copy(alpha = 0.7f),
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                    )

                    // 2. Horizontal Grid Lines
                    for (i in 1..3) {
                        val gridY = (height / 4f) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, gridY),
                            end = Offset(width, gridY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // 3. Data Points Coordinates with linear spacing
                    val points = mutableListOf<Offset>()
                    val stepX = if (entries.size > 1) width / (entries.size - 1) else width / 2f

                    entries.forEachIndexed { index, item ->
                        val x = if (entries.size > 1) index * stepX else width / 2f
                        val y = height - (((item.weightKg - minWeight) / weightRange).toFloat() * height)
                        points.add(Offset(x, y))
                    }

                    // 4. Bioluminescent Area Fill below curve
                    if (points.isNotEmpty()) {
                        val fillPath = Path().apply {
                            moveTo(points.first().x, height)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // 5. Solid Recorded Track with Neon Glow
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = strokePath,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    HealthColors.PrimaryGlow,
                                    HealthColors.Primary,
                                    HealthColors.PrimaryVariant
                                )
                            ),
                            style = Stroke(width = 3.5.dp.toPx())
                        )

                        // 6. Interactive Biometric Nodes
                        points.forEachIndexed { index, pt ->
                            val isSelected = index == selectedIndex
                            val haloRadius = if (isSelected) 12.dp.toPx() else 7.dp.toPx()
                            val ringRadius = if (isSelected) 6.5.dp.toPx() else 4.5.dp.toPx()
                            val coreRadius = if (isSelected) 4.5.dp.toPx() else 3.dp.toPx()

                            // Outer Halo
                            drawCircle(
                                color = if (isSelected) primaryColor.copy(alpha = 0.5f) else primaryColor.copy(alpha = 0.3f),
                                radius = haloRadius,
                                center = pt
                            )
                            // Outer Ring
                            drawCircle(
                                color = if (isDark) HealthColors.BackgroundDark else Color.White,
                                radius = ringRadius,
                                center = pt
                            )
                            // Core
                            drawCircle(
                                color = primaryColor,
                                radius = coreRadius,
                                center = pt
                            )
                        }
                    }
                }

                // Legend at Bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                        )
                        Text(
                            text = str("Kilo Takibi", "Weight Trend"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(2.dp)
                                .background(targetLineColor)
                        )
                        Text(
                            text = str("Hedef ($targetWeightKg kg)", "Goal ($targetWeightKg kg)"),
                            style = MaterialTheme.typography.labelSmall,
                            color = targetLineColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) HealthColors.Primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .bounceClick()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) HealthColors.OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
