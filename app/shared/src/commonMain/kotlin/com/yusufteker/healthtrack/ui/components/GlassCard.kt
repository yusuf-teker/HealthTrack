package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme

/**
 * Smoked Obsidian & Glassmorphic Card matching Stitch's Bio-Telemetry aesthetic.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color? = null,
    borderGradient: List<Color>? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    val glassBg = backgroundColor ?: if (isDark) {
        HealthColors.SurfaceContainer.copy(alpha = 0.82f)
    } else {
        HealthColors.SurfaceLight.copy(alpha = 0.95f)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val actualBorderBrush = remember(isDark, borderGradient, primaryColor) {
        val colors = borderGradient ?: if (isDark) {
            listOf(
                Color.White.copy(alpha = 0.12f),
                HealthColors.Primary.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.85f),
                primaryColor.copy(alpha = 0.15f)
            )
        }
        Brush.linearGradient(colors)
    }

    // In dark mode on obsidian background, glowing border + translucent surface define depth
    // without forcing an expensive hardware RenderNode shadow layer on every card.
    val shadowModifier = if (!isDark && elevation > 0.dp) {
        Modifier.shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = primaryColor.copy(alpha = 0.08f),
            spotColor = primaryColor.copy(alpha = 0.12f)
        )
    } else {
        Modifier
    }

    val cardModifier = modifier
        .then(shadowModifier)
        .clip(shape)
        .background(glassBg)
        .border(width = borderWidth, brush = actualBorderBrush, shape = shape)
        .then(
            if (onClick != null) {
                Modifier
                    .bounceClick()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
            } else {
                Modifier
            }
        )

    Box(
        modifier = cardModifier,
        content = content
    )
}
