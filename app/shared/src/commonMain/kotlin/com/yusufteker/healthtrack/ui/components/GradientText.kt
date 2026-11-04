package com.yusufteker.healthtrack.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import com.yusufteker.healthtrack.theme.HealthColors

/**
 * Text component that renders content with a multi-color gradient brush,
 * matching Planora's Brand Mark and Header Typography.
 */
@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    colors: List<Color> = HealthColors.GradientPrimary,
    style: TextStyle = TextStyle.Default
) {
    Text(
        text = text,
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithCache {
                val brush = Brush.linearGradient(
                    colors = colors,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(brush = brush, blendMode = BlendMode.SrcIn)
                }
            },
        style = style
    )
}

