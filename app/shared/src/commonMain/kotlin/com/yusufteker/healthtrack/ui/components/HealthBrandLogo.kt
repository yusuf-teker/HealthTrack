package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.theme.HealthColors

/**
 * HealthTrack brand mark.
 * Flat, bold straight "H" mark styled identically to the ecosystem pod format.
 */
@Composable
fun HealthBrandLogo(
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val cornerRadius = (size * 12f / 38f).coerceAtLeast(8.dp)
    val gradientColors = HealthColors.GradientPrimary

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    listOf(
                        gradientColors.first().copy(alpha = 0.22f),
                        gradientColors.last().copy(alpha = 0.18f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val fontSize = (size.value * 0.58f).sp
        GradientText(
            text = "H",
            colors = gradientColors,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.Black
            )
        )
    }
}
