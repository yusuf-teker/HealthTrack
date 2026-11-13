package com.yusufteker.healthtrack.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme

/**
 * Attention-grabbing, periodically shimmering streak pill for TopAppBar & summary cards.
 * Features:
 * - Periodic diagonal light sweep (shimmer glint)
 * - Gentle breathing glow halo behind the flame icon
 * - Twinkling 4-point sparkle stars around active flames
 */
@Composable
fun ShimmeringStreakPill(
    streakDays: Int,
    isActiveToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    level: Int? = null
) {
    val isDark = LocalIsDarkTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "streak_shimmer")

    // Periodic diagonal glint every ~3.4s
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3400
                -0.6f at 0 using LinearEasing
                -0.6f at 2100 using LinearEasing
                1.8f at 3200 using FastOutSlowInEasing
                1.8f at 3400 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_sweep"
    )

    // Subtle flame pulse
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse"
    )

    // Twinkling stars phase
    val sparklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_phase"
    )

    val (flamePrimary, flameSecondary) = when {
        streakDays <= 0 -> HealthColors.PrimaryGlow to HealthColors.Primary
        streakDays < 7 -> Color(0xFFFB923C) to Color(0xFFF59E0B)
        streakDays < 14 -> Color(0xFFFACC15) to Color(0xFFEA580C)
        else -> Color(0xFF38BDF8) to Color(0xFFC084FC)
    }

    val containerBg = if (isDark) {
        flameSecondary.copy(alpha = if (isActiveToday) 0.16f else 0.10f)
    } else {
        flameSecondary.copy(alpha = if (isActiveToday) 0.14f else 0.08f)
    }
    val borderColor = flamePrimary.copy(alpha = if (isActiveToday) 0.55f else 0.30f)

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerBg,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .bounceClick()
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .drawWithContent {
                drawContent()
                // Shimmer diagonal glint sweep
                if (shimmerProgress in -0.4f..1.5f) {
                    val startX = size.width * shimmerProgress
                    val shimmerBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (isDark) 0.28f else 0.48f),
                            Color.Transparent
                        ),
                        start = Offset(startX - size.height, 0f),
                        end = Offset(startX + size.height * 0.4f, size.height)
                    )
                    drawRect(brush = shimmerBrush)
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier.size(22.dp),
                contentAlignment = Alignment.Center
            ) {
                // Breathing glow halo
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            scaleX = flameScale * 1.15f
                            scaleY = flameScale * 1.15f
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    flamePrimary.copy(alpha = if (streakDays > 0) 0.45f else 0.20f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Twinkling 4-point stars around the flame
                if (streakDays > 0) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val s1 = 0.3f + 0.7f * sparklePhase
                        val s2 = 0.3f + 0.7f * (1f - sparklePhase)
                        drawFourPointStar(
                            center = Offset(size.width * 0.85f, size.height * 0.18f),
                            radius = 3.2.dp.toPx() * s1,
                            color = Color(0xFFFEF08A).copy(alpha = s1)
                        )
                        drawFourPointStar(
                            center = Offset(size.width * 0.15f, size.height * 0.28f),
                            radius = 2.4.dp.toPx() * s2,
                            color = flamePrimary.copy(alpha = s2)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = if (streakDays > 0 && isActiveToday) {
                        flamePrimary
                    } else if (streakDays > 0) {
                        flamePrimary.copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .size(17.dp)
                        .graphicsLayer {
                            if (streakDays > 0) {
                                scaleX = flameScale
                                scaleY = flameScale
                            }
                        }
                )
            }

            Text(
                text = if (level != null) "$streakDays • Lv.$level" else "$streakDays",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                ),
                color = if (streakDays > 0) {
                    if (isDark) Color(0xFFFDF4E7) else flameSecondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFourPointStar(
    center: Offset,
    radius: Float,
    color: Color
) {
    if (radius <= 0.4f) return
    val inner = radius * 0.32f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + inner, center.y - inner)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + inner, center.y + inner)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - inner, center.y + inner)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - inner, center.y - inner)
        close()
    }
    drawPath(path = path, color = color)
}

