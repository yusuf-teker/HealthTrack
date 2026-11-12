package com.yusufteker.healthtrack.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme

/**
 * Dynamic metabolic telemetry color palette for daily calorie tracking:
 * - Blue (Mavi): Initial baseline intake (< 50% of target)
 * - Yellow (Sarı): Balanced intake approaching target (50% - 75%)
 * - Orange (Turuncu): Near upper limit (75% - 100%)
 * - Red (Kırmızı): Target exceeded (> 100%)
 */
object CalorieTelemetryColors {
    val Blue = Color(0xFF0284C7)         // Sky / Ocean Blue
    val BlueGlow = Color(0xFF38BDF8)
    val BlueBg = Color(0xFF0284C7).copy(alpha = 0.16f)

    val Yellow = Color(0xFFEAB308)       // Amber Yellow
    val YellowGlow = Color(0xFFFDE047)
    val YellowBg = Color(0xFFEAB308).copy(alpha = 0.18f)

    val Orange = Color(0xFFF97316)       // Vibrant Coral Orange
    val OrangeGlow = Color(0xFFFB923C)
    val OrangeBg = Color(0xFFF97316).copy(alpha = 0.18f)

    val Red = Color(0xFFEF4444)          // Warning Red
    val RedGlow = Color(0xFFF87171)
    val RedBg = Color(0xFFEF4444).copy(alpha = 0.20f)

    fun getColorForRatio(ratio: Float): Color = when {
        ratio > 1.0f -> Red
        ratio >= 0.75f -> Orange
        ratio >= 0.50f -> Yellow
        else -> Blue
    }

    fun getGlowForRatio(ratio: Float): Color = when {
        ratio > 1.0f -> RedGlow
        ratio >= 0.75f -> OrangeGlow
        ratio >= 0.50f -> YellowGlow
        else -> BlueGlow
    }

    fun getBgForRatio(ratio: Float): Color = when {
        ratio > 1.0f -> RedBg
        ratio >= 0.75f -> OrangeBg
        ratio >= 0.50f -> YellowBg
        else -> BlueBg
    }
}

/**
 * Concentric Circular Calorie Telemetry Hero Card.
 * Compact height optimization:
 * - Centered main ring gauge.
 * - Protein, Karb, Yağ placed directly under the main ring without any card wrapper.
 * - Format: "Protein 0/126g" with dynamic target adapting to user's daily weight & fitness goal.
 */
@Composable
fun CalorieProgressGauge(
    consumedCalories: Int,
    targetCalories: Int = 2000,
    burnedCalories: Int = 0,
    proteinCurrent: Double = 0.0,
    proteinTarget: Double = 150.0,
    carbsCurrent: Double = 0.0,
    carbsTarget: Double = 250.0,
    fatCurrent: Double = 0.0,
    fatTarget: Double = 65.0,
    goalBadge: String? = null,
    onEditGoalsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val rawRatio = if (targetCalories > 0) consumedCalories.toFloat() / targetCalories else 0f
    val isExceeded = rawRatio > 1.0f
    val progress = rawRatio.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800)
    )

    val remaining = (targetCalories - consumedCalories).coerceAtLeast(0)
    val percentage = (rawRatio * 100).toInt()

    val activeColor = CalorieTelemetryColors.getColorForRatio(rawRatio)
    val activeBg = CalorieTelemetryColors.getBgForRatio(rawRatio)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Top Header: Metric Label & Remaining Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = activeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Metabolik Yakıt",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!goalBadge.isNullOrBlank()) {
                        Text(
                            text = "• $goalBadge",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = HealthColors.NutrientProtein
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(activeBg)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when {
                            isExceeded -> "+${consumedCalories - targetCalories} kcal aşıldı"
                            remaining > 0 -> "$remaining kcal kaldı"
                            else -> "%$percentage tamamlandı"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = activeColor
                    )
                }
            }

            // Concentric Telemetry Ring (Centered alone, sağı solu boş, height optimized to 150dp)
            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = if (isDark) HealthColors.SurfaceContainerHighest.copy(alpha = 0.35f)
                else HealthColors.SurfaceVariantLight

                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val strokeWidth = 9.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)

                    // Outer Track Ring
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Over-limit Warning Halo
                    if (isExceeded) {
                        drawArc(
                            color = CalorieTelemetryColors.Red.copy(alpha = 0.22f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth + 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Active Progress Ring: Starts from Blue (0%) -> Yellow (50%) -> Orange (80%) -> Red (100%+)
                    // Note: Wrap points at 0.0f and 1.0f match to prevent StrokeCap.Round backward bleed
                    if (animatedProgress > 0f) {
                        rotate(degrees = -90f, pivot = center) {
                            val sweepBrush = when {
                                isExceeded -> Brush.sweepGradient(
                                    0.0f to CalorieTelemetryColors.Orange,
                                    0.5f to CalorieTelemetryColors.Red,
                                    1.0f to CalorieTelemetryColors.Orange
                                )
                                rawRatio >= 0.75f -> Brush.sweepGradient(
                                    0.0f to CalorieTelemetryColors.Blue,
                                    0.4f to CalorieTelemetryColors.Yellow,
                                    0.85f to CalorieTelemetryColors.Orange,
                                    1.0f to CalorieTelemetryColors.Blue
                                )
                                rawRatio >= 0.50f -> Brush.sweepGradient(
                                    0.0f to CalorieTelemetryColors.Blue,
                                    0.6f to CalorieTelemetryColors.Yellow,
                                    1.0f to CalorieTelemetryColors.Blue
                                )
                                else -> Brush.sweepGradient(
                                    0.0f to CalorieTelemetryColors.Blue,
                                    0.5f to CalorieTelemetryColors.BlueGlow,
                                    1.0f to CalorieTelemetryColors.Blue
                                )
                            }
                            drawArc(
                                brush = sweepBrush,
                                startAngle = 0f,
                                sweepAngle = animatedProgress * 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                // Centered Telemetry Figures
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isExceeded) "AŞILDI" else "ALINAN",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (isExceeded) CalorieTelemetryColors.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "$consumedCalories",
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                            fontWeight = FontWeight.Black,
                            color = if (isExceeded) CalorieTelemetryColors.Red else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "kcal",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (isExceeded) CalorieTelemetryColors.Red.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (isExceeded) CalorieTelemetryColors.Red else HealthColors.Tertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isExceeded) {
                                "/ $targetCalories kcal aşıldı"
                            } else if (burnedCalories > 0) {
                                "$burnedCalories kcal yakıldı"
                            } else {
                                "/ $targetCalories kcal"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = if (isExceeded) CalorieTelemetryColors.Red else HealthColors.Tertiary
                        )
                    }
                }
            }

            // ── Macros Directly Under Main Ring Without Any Card Wrapper ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .then(
                        if (onEditGoalsClick != null) Modifier.bounceClick().clickable { onEditGoalsClick() }
                        else Modifier
                    ),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MacroItem(
                    label = "Protein",
                    current = proteinCurrent,
                    target = proteinTarget,
                    color = HealthColors.NutrientProtein,
                    modifier = Modifier.weight(1f)
                )
                MacroItem(
                    label = "Karb",
                    current = carbsCurrent,
                    target = carbsTarget,
                    color = HealthColors.NutrientCarbs,
                    modifier = Modifier.weight(1f)
                )
                MacroItem(
                    label = "Yağ",
                    current = fatCurrent,
                    target = fatTarget,
                    color = HealthColors.NutrientLipids,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MacroItem(
    label: String,
    current: Double,
    target: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val progress = if (target > 0) (current / target).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600)
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // "Protein 0/80g"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "${current.toInt()}/${target.toInt()}g",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Hemen altında küçük yuvarlak (size 26.dp)
        Box(
            modifier = Modifier.size(26.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = if (isDark) HealthColors.SurfaceContainerHighest.copy(alpha = 0.45f)
            else HealthColors.SurfaceVariantLight

            Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                val strokeWidth = 2.8.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                // Track
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress
                if (animatedProgress > 0f) {
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Percentage readout inside circle
            Text(
                text = "%${(progress * 100).toInt()}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
