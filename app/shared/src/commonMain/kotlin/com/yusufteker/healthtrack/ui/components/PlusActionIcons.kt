package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Custom '+' shaped FAB icon for Nutrition/Food screen:
 * Vertical element is an upright Fork (tines pointing up, handle down),
 * Horizontal element is a Spoon pointing right (handle on left, bowl pointing right).
 * Together, they intersect at the center forming a distinctive, elegant culinary '+' symbol.
 */
@Composable
fun ForkSpoonPlusIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokeW = (w * 0.088f).coerceAtLeast(2.2f)

        // ════════════════════════════════════════════════════════
        // 1. Horizontal Stem: Spoon Pointing Right (Handle Left -> Bowl Right)
        // ════════════════════════════════════════════════════════
        val spoonHandleStart = w * 0.12f
        // Draw spoon handle with rounded cap
        drawLine(
            color = tint,
            start = Offset(spoonHandleStart, cy),
            end = Offset(cx, cy),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Spoon bowl (egg/teardrop silhouette pointing right)
        val bowlLeft = cx + w * 0.04f
        val bowlRight = w * 0.88f
        val bowlWidth = bowlRight - bowlLeft
        val bowlHeight = h * 0.30f

        // Connect neck to bowl base
        drawLine(
            color = tint,
            start = Offset(cx, cy),
            end = Offset(bowlLeft + bowlWidth * 0.20f, cy),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Sculpted spoon bowl path (wider near neck, gracefully rounded tip at right)
        val spoonBowlPath = Path().apply {
            moveTo(bowlLeft, cy - bowlHeight * 0.18f)
            cubicTo(
                bowlLeft + bowlWidth * 0.28f, cy - bowlHeight * 0.52f,
                bowlRight - bowlWidth * 0.08f, cy - bowlHeight * 0.44f,
                bowlRight, cy
            )
            cubicTo(
                bowlRight - bowlWidth * 0.08f, cy + bowlHeight * 0.44f,
                bowlLeft + bowlWidth * 0.28f, cy + bowlHeight * 0.52f,
                bowlLeft, cy + bowlHeight * 0.18f
            )
            close()
        }
        drawPath(path = spoonBowlPath, color = tint)

        // ════════════════════════════════════════════════════════
        // 2. Vertical Stem: Fork Pointing Up (Handle Down -> Tines Up)
        // ════════════════════════════════════════════════════════
        // Fork handle extending downwards
        val forkHandleEnd = h * 0.88f
        drawLine(
            color = tint,
            start = Offset(cx, cy),
            end = Offset(cx, forkHandleEnd),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Fork neck up to base of tines
        val forkBaseY = h * 0.38f
        drawLine(
            color = tint,
            start = Offset(cx, cy),
            end = Offset(cx, forkBaseY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Tines
        val tineTopY = h * 0.10f
        val tineSpacing = w * 0.11f
        val tineStroke = (strokeW * 0.78f).coerceAtLeast(1.8f)

        // Fork head arch/bridge
        val bridgePath = Path().apply {
            moveTo(cx - tineSpacing, forkBaseY - h * 0.035f)
            quadraticTo(cx, forkBaseY, cx + tineSpacing, forkBaseY - h * 0.035f)
        }
        drawPath(
            path = bridgePath,
            color = tint,
            style = Stroke(width = tineStroke * 1.35f, cap = StrokeCap.Round)
        )

        // Left Tine
        drawLine(
            color = tint,
            start = Offset(cx - tineSpacing, forkBaseY - h * 0.035f),
            end = Offset(cx - tineSpacing, tineTopY),
            strokeWidth = tineStroke,
            cap = StrokeCap.Round
        )
        // Center Tine
        drawLine(
            color = tint,
            start = Offset(cx, forkBaseY - h * 0.01f),
            end = Offset(cx, tineTopY),
            strokeWidth = tineStroke,
            cap = StrokeCap.Round
        )
        // Right Tine
        drawLine(
            color = tint,
            start = Offset(cx + tineSpacing, forkBaseY - h * 0.035f),
            end = Offset(cx + tineSpacing, tineTopY),
            strokeWidth = tineStroke,
            cap = StrokeCap.Round
        )

        // Smooth central intersection
        drawCircle(
            color = tint,
            radius = strokeW * 0.55f,
            center = Offset(cx, cy)
        )
    }
}

/**
 * Custom '+' shaped FAB icon for Workout/Exercise screen:
 * Two barbell bars (one horizontal, one vertical) crossed at 90 degrees like a '+' symbol,
 * each with weight plates (disks) on both ends.
 */
@Composable
fun CrossedBarbellPlusIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val barStroke = (w * 0.092f).coerceAtLeast(2.4f)

        val mainLength = w * 0.32f // from center to first weight plate
        val plateThickness = (w * 0.065f).coerceAtLeast(2f)
        val bigPlateHalf = h * 0.13f
        val smallPlateHalf = h * 0.085f

        // 1. Horizontal Bar Shaft (Left to Right)
        drawLine(
            color = tint,
            start = Offset(cx - mainLength - plateThickness * 1.5f, cy),
            end = Offset(cx + mainLength + plateThickness * 1.5f, cy),
            strokeWidth = barStroke,
            cap = StrokeCap.Round
        )

        // 2. Vertical Bar Shaft (Top to Bottom)
        drawLine(
            color = tint,
            start = Offset(cx, cy - mainLength - plateThickness * 1.5f),
            end = Offset(cx, cy + mainLength + plateThickness * 1.5f),
            strokeWidth = barStroke,
            cap = StrokeCap.Round
        )

        // Center knurling / joint accent
        drawCircle(
            color = tint,
            radius = barStroke * 0.85f,
            center = Offset(cx, cy)
        )

        // Horizontal Bar Weight Plates (Left & Right)
        // Left - Big Plate
        drawRoundRect(
            color = tint,
            topLeft = Offset(cx - mainLength - plateThickness / 2f, cy - bigPlateHalf),
            size = Size(plateThickness, bigPlateHalf * 2f),
            cornerRadius = CornerRadius(plateThickness / 2f, plateThickness / 2f)
        )
        // Left - Small Plate
        drawRoundRect(
            color = tint,
            topLeft = Offset(cx - mainLength - plateThickness * 1.45f, cy - smallPlateHalf),
            size = Size(plateThickness * 0.85f, smallPlateHalf * 2f),
            cornerRadius = CornerRadius(plateThickness / 2f, plateThickness / 2f)
        )

        // Right - Big Plate
        drawRoundRect(
            color = tint,
            topLeft = Offset(cx + mainLength - plateThickness / 2f, cy - bigPlateHalf),
            size = Size(plateThickness, bigPlateHalf * 2f),
            cornerRadius = CornerRadius(plateThickness / 2f, plateThickness / 2f)
        )
        // Right - Small Plate
        drawRoundRect(
            color = tint,
            topLeft = Offset(cx + mainLength + plateThickness * 0.60f, cy - smallPlateHalf),
            size = Size(plateThickness * 0.85f, smallPlateHalf * 2f),
            cornerRadius = CornerRadius(plateThickness / 2f, plateThickness / 2f)
        )

        // Vertical Bar Weight Plates (Top & Bottom)
        // Top - Big Plate
        drawRoundRect(
            color = tint,
            topLeft = Offset(cx - bigPlateHalf, cy - mainLength - plateThickness / 2f),
            size = Size(bigPlateHalf * 2f, plateThickness),
            cornerRadius = CornerRadius(plateThickness / 2f, plateThickness / 2f)
        )
        // Top - Small Plate
        drawRoundRect(
            color = tint,
            topLeft = Offset(cx - smallPlateHalf, cy - mainLength - plateThickness * 1.45f),
            size = Size(smallPlateHalf * 2f, plateThickness * 0.85f),
            cornerRadius = CornerRadius(plateThickness / 2f, plateThickness / 2f)
        )

        // Bottom - Big Plate
        drawRoundRect(
            color = tint,
            topLeft = Offset(cx - bigPlateHalf, cy + mainLength - plateThickness / 2f),
            size = Size(bigPlateHalf * 2f, plateThickness),
            cornerRadius = CornerRadius(plateThickness / 2f, plateThickness / 2f)
        )
        // Bottom - Small Plate
        drawRoundRect(
            color = tint,
            topLeft = Offset(cx - smallPlateHalf, cy + mainLength + plateThickness * 0.60f),
            size = Size(smallPlateHalf * 2f, plateThickness * 0.85f),
            cornerRadius = CornerRadius(plateThickness / 2f, plateThickness / 2f)
        )
    }
}
