package com.yusufteker.healthtrack.feature.weight.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.WeightChartPeriod
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick

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
    val latestEntry = entries.lastOrNull()
    val prevEntry = if (entries.size > 1) entries[entries.size - 2] else null
    val deltaFromPrev = if (latestEntry != null && prevEntry != null) latestEntry.weightKg - prevEntry.weightKg else null

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
                        text = "Kilo Eğrisi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Trend & İnterpolasyon",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        label = "Hafta",
                        selected = selectedPeriod == WeightChartPeriod.WEEKLY,
                        onClick = { onPeriodSelect(WeightChartPeriod.WEEKLY) }
                    )
                    PeriodPill(
                        label = "Ay",
                        selected = selectedPeriod == WeightChartPeriod.MONTHLY,
                        onClick = { onPeriodSelect(WeightChartPeriod.MONTHLY) }
                    )
                    PeriodPill(
                        label = "Yıl",
                        selected = selectedPeriod == WeightChartPeriod.YEARLY,
                        onClick = { onPeriodSelect(WeightChartPeriod.YEARLY) }
                    )
                    PeriodPill(
                        label = "Tümü",
                        selected = selectedPeriod == WeightChartPeriod.ALL,
                        onClick = { onPeriodSelect(WeightChartPeriod.ALL) }
                    )
                }
            }

            // Active Tooltip Callout Overlay matching Stitch
            if (latestEntry != null) {
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
                            text = "Son: ${latestEntry.date}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${latestEntry.weightKg} kg",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HealthColors.Primary
                        )
                        if (deltaFromPrev != null) {
                            val roundedDelta = ((deltaFromPrev * 10).toInt()) / 10.0
                            val sign = if (roundedDelta > 0) "+" else ""
                            Text(
                                text = "($sign$roundedDelta kg)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (roundedDelta <= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
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
                        text = "Grafik için henüz yeterli kilo kaydı yok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Interactive SVG/Canvas Smooth Bezier Telemetry Chart
                val primaryColor = HealthColors.Primary
                val targetLineColor = HealthColors.TertiaryContainer
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
                        points.forEach { pt ->
                            // Outer Halo
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.3f),
                                radius = 7.dp.toPx(),
                                center = pt
                            )
                            // Outer Ring
                            drawCircle(
                                color = if (isDark) HealthColors.BackgroundDark else Color.White,
                                radius = 4.5.dp.toPx(),
                                center = pt
                            )
                            // Core
                            drawCircle(
                                color = primaryColor,
                                radius = 3.dp.toPx(),
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
                            text = "Kilo Takibi",
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
                            text = "Hedef ($targetWeightKg kg)",
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
