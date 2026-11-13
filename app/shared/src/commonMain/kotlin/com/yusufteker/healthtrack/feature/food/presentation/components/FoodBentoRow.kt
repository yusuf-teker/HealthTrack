package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.NutrientProgress
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick
import kotlin.math.roundToInt

/**
 * Bento Split Row matching home_summary_dashboard/screen.png:
 * - Card 1 (Left): 3 Vital Micronutrient Gauge Capsules (Fiber, Iron, Magnesium) -> Click opens General Micros
 * - Card 2 (Right): Weight Scale metric with glowing wave sparkline -> Click opens Weight Screen
 */
@Composable
fun FoodBentoRow(
    nutrientBars: List<NutrientProgress>,
    latestWeightKg: Double?,
    weightDifferenceKg: Double?,
    recentWeights: List<Double>,
    selectedDate: String = getTodayIso(),
    isWeightExactForDate: Boolean = false,
    weightRecordDate: String? = null,
    onMicrosClick: () -> Unit = {},
    onWeightClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Card 1 (Left): Micronutrient Gauge Capsules ──
        MicronutrientsBentoCard(
            nutrientBars = nutrientBars,
            onClick = onMicrosClick,
            modifier = Modifier.weight(1f)
        )

        // ── Card 2 (Right): Weight Scale Sparkline ──
        WeightScaleBentoCard(
            latestWeightKg = latestWeightKg,
            weightDifferenceKg = weightDifferenceKg,
            recentWeights = recentWeights,
            selectedDate = selectedDate,
            isWeightExactForDate = isWeightExactForDate,
            weightRecordDate = weightRecordDate,
            onClick = onWeightClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MicronutrientsBentoCard(
    nutrientBars: List<NutrientProgress>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current

    // Extract top 3 vital micros: Fiber, Iron, Magnesium (or fallback defaults)
    val fiberBar = nutrientBars.firstOrNull { it.key == "fiber" }
    val ironBar = nutrientBars.firstOrNull { it.key == "iron" }
    val mgBar = nutrientBars.firstOrNull { it.key == "magnesium" || it.key == "vitamin_c" }

    val fiberPct = (fiberBar?.percentage ?: 0.0f).toInt().coerceIn(0, 100)
    val ironPct = (ironBar?.percentage ?: 0.0f).toInt().coerceIn(0, 100)
    val mgPct = (mgBar?.percentage ?: 0.0f).toInt().coerceIn(0, 100)

    GlassCard(
        modifier = modifier
            .height(170.dp)
            .bounceClick()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = str("MİKROLAR", "MICROS"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = str("Genel Mikrolar", "General Micros"),
                        tint = HealthColors.Primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Biotech,
                    contentDescription = null,
                    tint = HealthColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // 3 Vertical Capsules
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Bottom
            ) {
                MicroCapsule(
                    label = str("Lif", "Fiber"),
                    percentage = fiberPct,
                    color = HealthColors.Primary,
                    isDark = isDark
                )
                MicroCapsule(
                    label = str("Demir", "Iron"),
                    percentage = ironPct,
                    color = HealthColors.Secondary,
                    isDark = isDark
                )
                MicroCapsule(
                    label = "Mg",
                    percentage = mgPct,
                    color = HealthColors.Tertiary,
                    isDark = isDark
                )
            }

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = str("Genel Mikrolar", "General Micros"),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = str("Tümünü Gör", "See All"),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = HealthColors.Primary
                )
            }
        }
    }
}

@Composable
private fun MicroCapsule(
    label: String,
    percentage: Int,
    color: Color,
    isDark: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(durationMillis = 700)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = str("%$percentage", "$percentage%"),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Vertical Pill Well
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (isDark) HealthColors.SurfaceContainerHighest.copy(alpha = 0.35f)
                    else HealthColors.SurfaceVariantLight
                )
                .padding(2.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedProgress)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeightScaleBentoCard(
    latestWeightKg: Double?,
    weightDifferenceKg: Double?,
    recentWeights: List<Double>,
    selectedDate: String = getTodayIso(),
    isWeightExactForDate: Boolean = false,
    weightRecordDate: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val displayWeight = latestWeightKg
    val isToday = selectedDate == getTodayIso()
    val cardTitle = if (isToday) str("GÜNCEL KİLO", "CURRENT WEIGHT") else str("O GÜNÜN KİLOSU", "WEIGHT ON DAY")
    val displayDiff = if (isToday) weightDifferenceKg else null
    val isDrop = (displayDiff ?: 0.0) <= 0.0

    val sparklineData = remember(recentWeights, latestWeightKg) {
        when {
            recentWeights.size >= 2 -> recentWeights
            recentWeights.size == 1 -> listOf(recentWeights.first(), recentWeights.first())
            latestWeightKg != null -> listOf(latestWeightKg, latestWeightKg)
            else -> emptyList()
        }
    }

    val cardModifier = if (onClick != null) {
        modifier
            .height(170.dp)
            .bounceClick()
            .clickable(onClick = onClick)
    } else {
        modifier.height(170.dp)
    }

    GlassCard(
        modifier = cardModifier,
        shape = RoundedCornerShape(22.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = cardTitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (onClick != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = str("Kilo Detayı", "Weight Details"),
                            tint = HealthColors.Primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.MonitorWeight,
                    contentDescription = null,
                    tint = HealthColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Figure & Badge
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (displayWeight != null) "${((displayWeight * 10).roundToInt() / 10.0)}" else "--.-",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "kg",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                if (displayDiff != null) {
                    val diffRounded = (displayDiff * 10).roundToInt() / 10.0
                    val sign = if (diffRounded > 0) "+" else ""
                    val badgeColor = if (diffRounded <= 0) HealthColors.Primary else MaterialTheme.colorScheme.error
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (isDrop) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = str("$sign$diffRounded kg bu hafta", "$sign$diffRounded kg this week"),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                } else {
                    val badgeText = when {
                        isWeightExactForDate -> if (isToday) str("Bugün Tartıldı", "Weighed Today") else str("📅 Tartım Kaydı", "📅 Weigh-in Log")
                        weightRecordDate != null -> str("Son: $weightRecordDate", "Last: $weightRecordDate")
                        displayWeight != null -> str("Profil Kilosu", "Profile Weight")
                        else -> str("Kayıt Bekleniyor", "Waiting for Log")
                    }
                    val badgeBg = if (isWeightExactForDate) {
                        HealthColors.Primary.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                    val badgeTextColor = if (isWeightExactForDate) {
                        HealthColors.Primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = if (isWeightExactForDate) FontWeight.Bold else FontWeight.Medium,
                            color = badgeTextColor
                        )
                    }
                }
            }

            // Glowing Wave Sparkline Canvas
            GlowingSparkline(
                data = sparklineData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            )
        }
    }
}

@Composable
private fun GlowingSparkline(
    data: List<Double>,
    modifier: Modifier = Modifier
) {
    val primaryColor = HealthColors.Primary
    val glowColor = HealthColors.PrimaryGlow

    Canvas(modifier = modifier) {
        if (data.isEmpty()) {
            drawLine(
                color = primaryColor.copy(alpha = 0.25f),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
            return@Canvas
        }

        if (data.size < 2) return@Canvas

        val width = size.width
        val height = size.height

        val minVal = (data.minOrNull() ?: 0.0) - 0.2
        val maxVal = (data.maxOrNull() ?: 1.0) + 0.2
        val range = if (maxVal - minVal > 0.001) maxVal - minVal else 1.0

        val stepX = width / (data.size - 1)
        val points = data.mapIndexed { index, value ->
            val normY = ((value - minVal) / range).toFloat()
            val y = height - (normY * height * 0.85f) - (height * 0.05f)
            Offset(x = index * stepX, y = y)
        }

        // Build smooth line path
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlX = (p0.x + p1.x) / 2f
                cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
            }
        }

        // Build filled gradient path
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, height)
            lineTo(points.first().x, height)
            close()
        }

        // Gradient Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height
            )
        )

        // Wave Stroke
        drawPath(
            path = linePath,
            color = primaryColor,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Glowing Pinpoint Circle at the last point
        val lastPoint = points.last()
        drawCircle(
            color = glowColor.copy(alpha = 0.4f),
            radius = 6.dp.toPx(),
            center = lastPoint
        )
        drawCircle(
            color = primaryColor,
            radius = 3.5.dp.toPx(),
            center = lastPoint
        )
    }
}
