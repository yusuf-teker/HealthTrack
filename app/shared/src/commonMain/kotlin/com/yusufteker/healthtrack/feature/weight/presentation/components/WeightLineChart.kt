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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.WeightChartMetric
import com.yusufteker.healthtrack.core.model.WeightChartPeriod
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.utils.BodyCompositionCalculator
import com.yusufteker.healthtrack.core.utils.formatDisplayDateTr
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick
import kotlin.math.abs

/**
 * Stitch Luxury Interactive Weight & Body Composition Trajectory Line Chart
 * with bioluminescent area glow, neon curve, and Weight / Body Fat % / Muscle % metric switching.
 */
@Composable
fun WeightLineChart(
    entries: List<WeightEntry>,
    targetWeightKg: Double,
    selectedPeriod: WeightChartPeriod,
    onPeriodSelect: (WeightChartPeriod) -> Unit,
    selectedMetric: WeightChartMetric = WeightChartMetric.WEIGHT,
    onMetricSelect: (WeightChartMetric) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val lang = appLang()
    var selectedIndex by remember(entries, selectedPeriod, selectedMetric) { mutableStateOf<Int?>(null) }

    val chartPoints = remember(entries, selectedMetric) {
        entries.mapNotNull { entry ->
            val value = when (selectedMetric) {
                WeightChartMetric.WEIGHT -> entry.weightKg
                WeightChartMetric.BODY_FAT -> entry.bodyFatPercent
                WeightChartMetric.MUSCLE -> entry.musclePercent
            }
            value?.let { entry to it }
        }
    }

    val activePair = if (selectedIndex != null && selectedIndex!! in chartPoints.indices) {
        chartPoints[selectedIndex!!]
    } else {
        chartPoints.lastOrNull()
    }
    val activePrevPair = if (selectedIndex != null && selectedIndex!! > 0) {
        chartPoints[selectedIndex!! - 1]
    } else if (chartPoints.size > 1) {
        chartPoints[chartPoints.size - 2]
    } else null

    val periodDelta = if (chartPoints.size >= 2) {
        chartPoints.last().second - chartPoints.first().second
    } else null
    val deltaToDisplay = if (selectedIndex != null) {
        if (activePrevPair != null && activePair != null) activePair.second - activePrevPair.second else null
    } else periodDelta

    val primaryColor = when (selectedMetric) {
        WeightChartMetric.WEIGHT -> HealthColors.Primary
        WeightChartMetric.BODY_FAT -> HealthColors.NutrientCarbs
        WeightChartMetric.MUSCLE -> HealthColors.NutrientProtein
    }
    val strokeGradient = when (selectedMetric) {
        WeightChartMetric.WEIGHT -> listOf(HealthColors.PrimaryGlow, HealthColors.Primary, HealthColors.PrimaryVariant)
        WeightChartMetric.BODY_FAT -> HealthColors.GradientCarbs
        WeightChartMetric.MUSCLE -> HealthColors.GradientProtein
    }
    val chartTitle = when (selectedMetric) {
        WeightChartMetric.WEIGHT -> str("Kilo Eğrisi", "Weight Curve")
        WeightChartMetric.BODY_FAT -> str("Vücut Yağ Eğrisi", "Body Fat Curve")
        WeightChartMetric.MUSCLE -> str("Kas Oranı Eğrisi", "Muscle % Curve")
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Chart Header & Timeframe Switcher Tabs (clean stacked layout so pills never wrap)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chartTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (selectedIndex != null) str("Noktaya dokunuldu", "Point selected") else str("Trend & İnterpolasyon", "Trend & Interpolation"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (selectedIndex != null) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Full-width Pill Nav Switcher (Hafta | Ay | Yıl | Tümü)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isDark) HealthColors.SurfaceContainerLowest else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    PeriodPill(
                        label = str("Hafta", "Week"),
                        selected = selectedPeriod == WeightChartPeriod.WEEKLY,
                        accentColor = primaryColor,
                        useDarkSelectedText = selectedMetric != WeightChartMetric.WEIGHT,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedIndex = null
                            onPeriodSelect(WeightChartPeriod.WEEKLY)
                        }
                    )
                    PeriodPill(
                        label = str("Ay", "Month"),
                        selected = selectedPeriod == WeightChartPeriod.MONTHLY,
                        accentColor = primaryColor,
                        useDarkSelectedText = selectedMetric != WeightChartMetric.WEIGHT,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedIndex = null
                            onPeriodSelect(WeightChartPeriod.MONTHLY)
                        }
                    )
                    PeriodPill(
                        label = str("Yıl", "Year"),
                        selected = selectedPeriod == WeightChartPeriod.YEARLY,
                        accentColor = primaryColor,
                        useDarkSelectedText = selectedMetric != WeightChartMetric.WEIGHT,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedIndex = null
                            onPeriodSelect(WeightChartPeriod.YEARLY)
                        }
                    )
                    PeriodPill(
                        label = str("Tümü", "All"),
                        selected = selectedPeriod == WeightChartPeriod.ALL,
                        accentColor = primaryColor,
                        useDarkSelectedText = selectedMetric != WeightChartMetric.WEIGHT,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedIndex = null
                            onPeriodSelect(WeightChartPeriod.ALL)
                        }
                    )
                }
            }

            // Metric Selector Strip (Kilo | Yağ % | Kas %)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) HealthColors.SurfaceContainerLowest else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WeightChartMetric.entries.forEach { metric ->
                    val isMetricSelected = metric == selectedMetric
                    val metricColor = when (metric) {
                        WeightChartMetric.WEIGHT -> HealthColors.Primary
                        WeightChartMetric.BODY_FAT -> HealthColors.NutrientCarbs
                        WeightChartMetric.MUSCLE -> HealthColors.NutrientProtein
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isMetricSelected) metricColor.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable {
                                selectedIndex = null
                                onMetricSelect(metric)
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = metric.localizedTitle(lang),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = if (isMetricSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isMetricSelected) metricColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Active Tooltip Callout Overlay matching Stitch
            if (activePair != null) {
                val activeEntry = activePair.first
                val activeValue = BodyCompositionCalculator.round1(activePair.second)
                val formattedValue = when (selectedMetric) {
                    WeightChartMetric.WEIGHT -> "$activeValue kg"
                    WeightChartMetric.BODY_FAT -> "%$activeValue"
                    WeightChartMetric.MUSCLE -> {
                        val kgPart = activeEntry.muscleMassKg?.let { " · $it kg" }.orEmpty()
                        "%$activeValue$kgPart"
                    }
                }

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
                                .background(primaryColor)
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
                            text = formattedValue,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        if (deltaToDisplay != null) {
                            val roundedDelta = BodyCompositionCalculator.round1(deltaToDisplay)
                            val sign = if (roundedDelta > 0) "+" else ""
                            val unitLabel = when (selectedMetric) {
                                WeightChartMetric.WEIGHT -> " kg"
                                WeightChartMetric.BODY_FAT, WeightChartMetric.MUSCLE -> "%"
                            }
                            val suffix = if (selectedIndex != null) {
                                str("önceki", "vs prev")
                            } else when (selectedPeriod) {
                                WeightChartPeriod.WEEKLY -> str("bu hafta", "this week")
                                WeightChartPeriod.MONTHLY -> str("bu ay", "this month")
                                WeightChartPeriod.YEARLY -> str("bu yıl", "this year")
                                WeightChartPeriod.ALL -> str("toplam", "overall")
                            }
                            val isFavorable = when (selectedMetric) {
                                WeightChartMetric.WEIGHT, WeightChartMetric.BODY_FAT -> roundedDelta <= 0.0
                                WeightChartMetric.MUSCLE -> roundedDelta >= 0.0
                            }
                            Text(
                                text = "($sign$roundedDelta$unitLabel $suffix)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFavorable) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )
                        } else if (selectedIndex == null && chartPoints.size == 1) {
                            Text(
                                text = str("(Kayıtlı)", "(Logged)"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (chartPoints.isEmpty()) {
                val emptyText = when (selectedMetric) {
                    WeightChartMetric.WEIGHT -> str(
                        "Grafik için henüz yeterli kilo kaydı yok.",
                        "Not enough weigh-ins for a chart yet."
                    )
                    WeightChartMetric.BODY_FAT -> str(
                        "Henüz yağ oranı kaydı yok. Bluetooth tartı ile ölçün veya manuel ekleyin.",
                        "No body fat readings yet. Measure via Bluetooth scale or enter manually."
                    )
                    WeightChartMetric.MUSCLE -> str(
                        "Henüz kas oranı kaydı yok. Akıllı tartıdan okuyun veya manuel ekleyin.",
                        "No muscle readings yet. Measure via smart scale or enter manually."
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Interactive SVG/Canvas Smooth Bezier Telemetry Chart
                val targetLineColor = MaterialTheme.colorScheme.tertiary
                val gridColor = if (isDark) HealthColors.SurfaceContainerHighest.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

                val values = chartPoints.map { it.second }
                val minValue = if (selectedMetric == WeightChartMetric.WEIGHT) {
                    (values.minOrNull() ?: targetWeightKg).coerceAtMost(targetWeightKg) - 1.5
                } else {
                    (values.minOrNull() ?: 10.0) - 1.5
                }
                val maxValue = if (selectedMetric == WeightChartMetric.WEIGHT) {
                    (values.maxOrNull() ?: targetWeightKg).coerceAtLeast(targetWeightKg) + 1.5
                } else {
                    (values.maxOrNull() ?: 50.0) + 1.5
                }
                val valueRange = (maxValue - minValue).coerceAtLeast(1.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .pointerInput(chartPoints) {
                            detectTapGestures { offset ->
                                if (chartPoints.isNotEmpty()) {
                                    val canvasW = size.width.toFloat()
                                    val stepX = if (chartPoints.size > 1) canvasW / (chartPoints.size - 1) else canvasW / 2f
                                    val closestIndex = chartPoints.indices.minByOrNull { i ->
                                        val nodeX = if (chartPoints.size > 1) i.toFloat() * stepX else canvasW / 2f
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

                    // 1. Target Dashed Reference Line (Weight metric only)
                    if (selectedMetric == WeightChartMetric.WEIGHT) {
                        val targetY = height - (((targetWeightKg - minValue) / valueRange).toFloat() * height)
                        drawLine(
                            color = targetLineColor.copy(alpha = 0.7f),
                            start = Offset(0f, targetY),
                            end = Offset(width, targetY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                        )
                    }

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
                    val stepX = if (chartPoints.size > 1) width / (chartPoints.size - 1) else width / 2f

                    chartPoints.forEachIndexed { index, (_, metricVal) ->
                        val x = if (chartPoints.size > 1) index * stepX else width / 2f
                        val y = height - (((metricVal - minValue) / valueRange).toFloat() * height)
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
                            brush = Brush.horizontalGradient(strokeGradient),
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
                            text = when (selectedMetric) {
                                WeightChartMetric.WEIGHT -> str("Kilo Takibi", "Weight Trend")
                                WeightChartMetric.BODY_FAT -> str("Vücut Yağ Oranı (%)", "Body Fat (%)")
                                WeightChartMetric.MUSCLE -> str("Kas Oranı (%)", "Muscle Percentage (%)")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selectedMetric == WeightChartMetric.WEIGHT) {
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
}

@Composable
private fun PeriodPill(
    label: String,
    selected: Boolean,
    accentColor: Color = HealthColors.Primary,
    useDarkSelectedText: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val selectedTextColor = if (useDarkSelectedText) HealthColors.OnBackgroundLight else HealthColors.OnPrimary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) accentColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .bounceClick(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) selectedTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
}
