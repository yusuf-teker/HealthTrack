package com.yusufteker.healthtrack.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yusufteker.healthtrack.theme.HealthColors

/**
 * Eye-catching luxury pill button for AI actions ("AI ile Hesapla" / "AI ile Analiz Et").
 * Features a periodic diagonal shimmer light sweep, glowing gradient border, and a pulsing
 * sparkle icon to draw attention without visual noise.
 */
@Composable
fun ShimmeringAiButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_btn_shimmer")

    // Continuous 2.8s timeline; beam sweeps swiftly in the first ~45%, pauses calmly in the rest.
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ai_shimmer_phase"
    )

    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_sparkle_scale"
    )

    val baseGradient = if (enabled) {
        listOf(
            HealthColors.Primary,
            Color(0xFF0EA5E9),
            Color(0xFF6366F1)
        )
    } else {
        listOf(
            HealthColors.Primary.copy(alpha = 0.45f),
            Color(0xFF0EA5E9).copy(alpha = 0.38f),
            Color(0xFF6366F1).copy(alpha = 0.35f)
        )
    }

    val borderBrush = Brush.horizontalGradient(
        colors = if (enabled) {
            listOf(
                Color.White.copy(alpha = 0.48f),
                HealthColors.PrimaryGlow.copy(alpha = 0.72f),
                Color(0xFF38BDF8).copy(alpha = 0.65f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.22f),
                HealthColors.PrimaryGlow.copy(alpha = 0.35f),
                Color(0xFF38BDF8).copy(alpha = 0.28f)
            )
        }
    )

    val shape = RoundedCornerShape(999.dp)

    Box(
        modifier = modifier
            .height(height)
            .then(if (enabled && !isLoading) Modifier.bounceClick() else Modifier)
            .clip(shape)
            .background(Brush.horizontalGradient(baseGradient))
            .border(width = if (enabled) 1.25.dp else 1.dp, brush = borderBrush, shape = shape)
            .drawWithContent {
                drawContent()
                if (shimmerPhase in -0.35f..1.35f) {
                    val beamWidth = size.width * 0.42f
                    val startX = (size.width + beamWidth) * shimmerPhase - beamWidth
                    val alpha = if (enabled) 0.34f else 0.14f
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = alpha),
                                Color.Transparent
                            ),
                            start = Offset(startX, 0f),
                            end = Offset(startX + beamWidth, size.height)
                        )
                    )
                }
            }
            .clickable(
                enabled = enabled && !isLoading,
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFDE047),
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            scaleX = sparkleScale
                            scaleY = sparkleScale
                        }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
