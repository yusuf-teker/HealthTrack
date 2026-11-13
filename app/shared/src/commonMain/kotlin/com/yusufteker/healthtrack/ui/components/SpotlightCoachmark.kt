package com.yusufteker.healthtrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.str
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt

enum class SpotlightTargetId {
    FOOD_INPUT_FIELD,
    FOOD_CALCULATE_BUTTON,
    FOOD_REVIEW_CONFIRM,
    FOOD_CALORIE_GAUGE,

    WORKOUT_INPUT_FIELD,
    WORKOUT_CALCULATE_BUTTON,
    WORKOUT_HERO_GAUGE,

    WEIGHT_ADD_BUTTON,
    WEIGHT_CHART_CARD,

    ANALYTICS_GENERATE_BUTTON,
    ANALYTICS_REPORT_CARD
}

@Stable
class SpotlightController {
    internal val boundsMap = mutableStateMapOf<SpotlightTargetId, Rect>()
    internal val requesters = mutableMapOf<SpotlightTargetId, BringIntoViewRequester>()
    internal val sizesMap = mutableMapOf<SpotlightTargetId, IntSize>()

    var activeTargetId by mutableStateOf<SpotlightTargetId?>(null)
    var outsideTapPulse by mutableIntStateOf(0)
        private set

    fun triggerPulse() {
        outsideTapPulse++
    }

    internal fun updateTarget(id: SpotlightTargetId, bounds: Rect, size: IntSize) {
        boundsMap[id] = bounds
        sizesMap[id] = size
    }

    internal fun clearTarget(id: SpotlightTargetId) {
        boundsMap.remove(id)
        sizesMap.remove(id)
        requesters.remove(id)
    }

    fun hasTarget(id: SpotlightTargetId): Boolean = boundsMap.containsKey(id)

    suspend fun bringTargetIntoView(
        id: SpotlightTargetId,
        viewportHeightPx: Float,
        preferTop: Boolean = false
    ) {
        val requester = requesters[id] ?: return
        val size = sizesMap[id] ?: return
        val targetWidth = size.width.toFloat().coerceAtLeast(1f)
        val targetHeight = size.height.toFloat().coerceAtLeast(1f)

        val safeViewport = viewportHeightPx.coerceAtLeast(targetHeight)
        val extraRoom = (safeViewport - targetHeight).coerceAtLeast(0f)

        val (topPad, bottomPad) = if (preferTop) {
            val top = min(extraRoom * 0.14f, 48f)
            val bottom = (extraRoom - top).coerceAtMost(extraRoom * 0.78f)
            top to bottom
        } else {
            val bottom = (extraRoom * 0.52f)
            val top = (extraRoom - bottom).coerceAtMost(extraRoom * 0.42f)
            top to bottom
        }

        val paddedRect = Rect(
            left = 0f,
            top = -topPad,
            right = targetWidth,
            bottom = targetHeight + bottomPad
        )
        requester.bringIntoView(paddedRect)
    }
}

val LocalSpotlightController = compositionLocalOf<SpotlightController?> { null }

@Composable
fun rememberSpotlightController(): SpotlightController = remember { SpotlightController() }

/**
 * Registers this composable as a spotlight & auto-scroll target when inside an onboarding
 * [LocalSpotlightController]. Zero overhead in normal screens when controller is null.
 */
fun Modifier.spotlightTarget(id: SpotlightTargetId): Modifier = composed {
    val controller = LocalSpotlightController.current ?: return@composed this
    val requester = remember(id) { BringIntoViewRequester() }

    DisposableEffect(controller, id, requester) {
        controller.requesters[id] = requester
        onDispose {
            if (controller.requesters[id] === requester) {
                controller.clearTarget(id)
            }
        }
    }

    this
        .bringIntoViewRequester(requester)
        .onGloballyPositioned { coords ->
            if (coords.isAttached) {
                controller.updateTarget(id, coords.boundsInRoot(), coords.size)
            }
        }
}

data class SpotlightBubbleSpec(
    val badge: String,
    val title: String,
    val description: String,
    val feedbackPill: String? = null,
    val primaryActionLabel: String? = null,
    val isPrimaryLoading: Boolean = false,
    val isPrimaryEnabled: Boolean = true,
    val secondarySkipLabel: String? = null,
    val showBackButton: Boolean = false,
    val preferAbove: Boolean = false
)

/**
 * Full-screen dimmed spotlight overlay with:
 * - Animated transparent rounded cutout (`BlendMode.Clear` on offscreen layer)
 * - 4 non-overlapping outside touch-guard boxes so touches INSIDE the cutout pass natively to the real UI
 * - Smart-positioned floating Coachmark Bubble with directional pointer arrow
 */
@Composable
fun SpotlightOverlay(
    controller: SpotlightController,
    targetId: SpotlightTargetId?,
    preferTopScroll: Boolean,
    bubbleSpec: SpotlightBubbleSpec,
    onPrimaryAction: () -> Unit,
    onSecondarySkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val isDark = LocalIsDarkTheme.current
    val primaryGradient = HealthColors.GradientPrimary

    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var overlayRootOffset by remember { mutableStateOf(Offset.Zero) }

    // Update controller's active target ID so dialogs/sheets can also react
    LaunchedEffect(targetId) {
        controller.activeTargetId = targetId
    }

    // Wait briefly for tab/screen mount, then scroll target cleanly into view
    LaunchedEffect(targetId, preferTopScroll, overlaySize.height) {
        if (targetId == null || overlaySize.height <= 0) return@LaunchedEffect
        var tries = 0
        while (!controller.hasTarget(targetId) && tries < 30) {
            delay(16L)
            tries++
        }
        if (controller.hasTarget(targetId)) {
            delay(40L)
            controller.bringTargetIntoView(
                id = targetId,
                viewportHeightPx = overlaySize.height.toFloat(),
                preferTop = preferTopScroll
            )
        }
    }

    val targetBoundsInRoot = targetId?.let { controller.boundsMap[it] }
    val cutoutPadPx = with(density) { 8.dp.toPx() }
    val cutoutCornerPx = with(density) { 18.dp.toPx() }

    val localCutoutRect = remember(targetBoundsInRoot, overlayRootOffset, overlaySize) {
        if (targetBoundsInRoot == null || overlaySize == IntSize.Zero) {
            null
        } else {
            val l = (targetBoundsInRoot.left - overlayRootOffset.x - cutoutPadPx).coerceIn(0f, overlaySize.width.toFloat())
            val t = (targetBoundsInRoot.top - overlayRootOffset.y - cutoutPadPx).coerceIn(0f, overlaySize.height.toFloat())
            val r = (targetBoundsInRoot.right - overlayRootOffset.x + cutoutPadPx).coerceIn(l, overlaySize.width.toFloat())
            val b = (targetBoundsInRoot.bottom - overlayRootOffset.y + cutoutPadPx).coerceIn(t, overlaySize.height.toFloat())
            if (r - l < 4f || b - t < 4f) null else Rect(l, t, r, b)
        }
    }

    val animSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val fallbackX = overlaySize.width / 2f
    val fallbackY = overlaySize.height / 2f
    val animLeft by animateFloatAsState(targetValue = localCutoutRect?.left ?: fallbackX, animationSpec = animSpec)
    val animTop by animateFloatAsState(targetValue = localCutoutRect?.top ?: fallbackY, animationSpec = animSpec)
    val animRight by animateFloatAsState(targetValue = localCutoutRect?.right ?: fallbackX, animationSpec = animSpec)
    val animBottom by animateFloatAsState(targetValue = localCutoutRect?.bottom ?: fallbackY, animationSpec = animSpec)

    val infinitePulse = rememberInfiniteTransition(label = "spotlight_glow")
    val glowAlpha by infinitePulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    var outsideNudge by remember { mutableStateOf(false) }
    LaunchedEffect(controller.outsideTapPulse) {
        if (controller.outsideTapPulse > 0) {
            outsideNudge = true
            delay(280L)
            outsideNudge = false
        }
    }
    val nudgeStrokeWidth by animateFloatAsState(
        targetValue = if (outsideNudge) with(density) { 4.dp.toPx() } else with(density) { 2.2.dp.toPx() },
        animationSpec = tween(180)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                overlaySize = coords.size
                overlayRootOffset = coords.positionInRoot()
            }
    ) {
        val hasCutout = localCutoutRect != null && animRight > animLeft + 4f && animBottom > animTop + 4f

        // 1. Visual Dimmed Backdrop + Transparent Cutout Hole (non-touch-blocking Canvas)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color.Black.copy(alpha = if (isDark) 0.74f else 0.64f))
            if (hasCutout) {
                val rectSize = Size((animRight - animLeft).coerceAtLeast(1f), (animBottom - animTop).coerceAtLeast(1f))
                val topLeft = Offset(animLeft, animTop)
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = topLeft,
                    size = rectSize,
                    cornerRadius = CornerRadius(cutoutCornerPx, cutoutCornerPx),
                    blendMode = BlendMode.Clear
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryGradient.first().copy(alpha = glowAlpha),
                            primaryGradient.last().copy(alpha = glowAlpha)
                        ),
                        start = topLeft,
                        end = Offset(animRight, animBottom)
                    ),
                    topLeft = topLeft,
                    size = rectSize,
                    cornerRadius = CornerRadius(cutoutCornerPx, cutoutCornerPx),
                    style = Stroke(width = nudgeStrokeWidth)
                )
            }
        }

        // 2. Four Non-Overlapping Touch-Guard Regions Around the Cutout
        // Inside the cutout, touches pass 100% natively to the underlying TextField / Button!
        if (hasCutout && overlaySize.width > 0 && overlaySize.height > 0) {
            val wPx = overlaySize.width.toFloat()
            val hPx = overlaySize.height.toFloat()
            val cLeft = animLeft.coerceIn(0f, wPx)
            val cTop = animTop.coerceIn(0f, hPx)
            val cRight = animRight.coerceIn(cLeft, wPx)
            val cBottom = animBottom.coerceIn(cTop, hPx)

            // Top band
            if (cTop > 1f) {
                TouchBlockerRegion(
                    x = 0,
                    y = 0,
                    widthPx = overlaySize.width,
                    heightPx = cTop.roundToInt(),
                    onTap = { controller.triggerPulse() }
                )
            }
            // Bottom band
            if (hPx - cBottom > 1f) {
                TouchBlockerRegion(
                    x = 0,
                    y = cBottom.roundToInt(),
                    widthPx = overlaySize.width,
                    heightPx = (hPx - cBottom).roundToInt(),
                    onTap = { controller.triggerPulse() }
                )
            }
            // Left band
            if (cLeft > 1f && cBottom > cTop) {
                TouchBlockerRegion(
                    x = 0,
                    y = cTop.roundToInt(),
                    widthPx = cLeft.roundToInt(),
                    heightPx = (cBottom - cTop).roundToInt(),
                    onTap = { controller.triggerPulse() }
                )
            }
            // Right band
            if (wPx - cRight > 1f && cBottom > cTop) {
                TouchBlockerRegion(
                    x = cRight.roundToInt(),
                    y = cTop.roundToInt(),
                    widthPx = (wPx - cRight).roundToInt(),
                    heightPx = (cBottom - cTop).roundToInt(),
                    onTap = { controller.triggerPulse() }
                )
            }
        } else {
            // While waiting a frame for target measurement, block stray taps
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { } }
            )
        }

        // 3. Smart-Positioned Floating Coachmark Bubble ("Baloncuk")
        if (overlaySize.width > 0 && overlaySize.height > 0) {
            var bubbleHeightPx by remember { mutableIntStateOf(0) }
            val horizontalMarginPx = with(density) { 14.dp.toPx() }
            val verticalGapPx = with(density) { 10.dp.toPx() }
            val edgePaddingPx = with(density) { 12.dp.toPx() }

            val spaceAbove = if (hasCutout) animTop else overlaySize.height * 0.5f
            val spaceBelow = if (hasCutout) overlaySize.height - animBottom else overlaySize.height * 0.5f
            val neededHeight = (bubbleHeightPx.toFloat().coerceAtLeast(with(density) { 160.dp.toPx() })) + verticalGapPx + edgePaddingPx

            val placeAbove = when {
                !hasCutout -> false
                bubbleSpec.preferAbove && spaceAbove >= neededHeight -> true
                !bubbleSpec.preferAbove && spaceBelow >= neededHeight -> false
                else -> spaceAbove > spaceBelow
            }

            val rawBubbleY = if (!hasCutout) {
                (overlaySize.height - bubbleHeightPx) / 2f
            } else if (placeAbove) {
                animTop - bubbleHeightPx - verticalGapPx
            } else {
                animBottom + verticalGapPx
            }
            val maxY = (overlaySize.height - bubbleHeightPx - edgePaddingPx).coerceAtLeast(edgePaddingPx)
            val bubbleY = rawBubbleY.coerceIn(edgePaddingPx, maxY)

            val cutoutCenterX = if (hasCutout) (animLeft + animRight) / 2f else overlaySize.width / 2f
            val bubbleWidthPx = (overlaySize.width - horizontalMarginPx * 2f).coerceAtLeast(1f)
            val arrowRelativeX = ((cutoutCenterX - horizontalMarginPx) / bubbleWidthPx).coerceIn(0.12f, 0.88f)

            AnimatedVisibility(
                visible = hasCutout || targetId == null,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(150)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .offset { IntOffset(0, bubbleY.roundToInt()) }
                    .onSizeChanged { bubbleHeightPx = it.height }
            ) {
                SpotlightBubbleCard(
                    spec = bubbleSpec,
                    arrowDirection = when {
                        !hasCutout -> ArrowDirection.NONE
                        placeAbove -> ArrowDirection.DOWN
                        else -> ArrowDirection.UP
                    },
                    arrowBiasX = arrowRelativeX,
                    onPrimaryAction = onPrimaryAction,
                    onSecondarySkip = onSecondarySkip,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun TouchBlockerRegion(
    x: Int,
    y: Int,
    widthPx: Int,
    heightPx: Int,
    onTap: () -> Unit
) {
    val density = LocalDensity.current
    val widthDp = with(density) { widthPx.coerceAtLeast(0).toDp() }
    val heightDp = with(density) { heightPx.coerceAtLeast(0).toDp() }
    Box(
        modifier = Modifier
            .offset { IntOffset(x, y) }
            .size(width = widthDp, height = heightDp)
            .pointerInput(Unit) {
                detectTapGestures { onTap() }
            }
    )
}

enum class ArrowDirection {
    UP,
    DOWN,
    NONE
}

@Composable
fun SpotlightBubbleCard(
    spec: SpotlightBubbleSpec,
    arrowDirection: ArrowDirection,
    arrowBiasX: Float = 0.5f,
    onPrimaryAction: () -> Unit,
    onSecondarySkip: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) HealthColors.SurfaceContainerHigh else Color.White
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (arrowDirection == ArrowDirection.UP) {
            BubblePointerArrow(
                pointingUp = true,
                biasX = arrowBiasX,
                fillColor = cardBg,
                borderColor = borderColor
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            shadowElevation = 18.dp,
            border = BorderStroke(1.25.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row: Step Badge + Optional Feedback Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = spec.badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.7.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (!spec.feedbackPill.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.16f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = spec.feedbackPill,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF10B981),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = spec.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                // Action Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (spec.showBackButton) {
                        OutlinedButton(
                            onClick = onBack,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = str("Geri", "Back"),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (!spec.primaryActionLabel.isNullOrBlank()) {
                        Button(
                            onClick = onPrimaryAction,
                            enabled = spec.isPrimaryEnabled && !spec.isPrimaryLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .bounceClick()
                        ) {
                            if (spec.isPrimaryLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = spec.primaryActionLabel,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!spec.secondarySkipLabel.isNullOrBlank()) {
                        TextButton(
                            onClick = onSecondarySkip,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text(
                                text = spec.secondarySkipLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (arrowDirection == ArrowDirection.DOWN) {
            BubblePointerArrow(
                pointingUp = false,
                biasX = arrowBiasX,
                fillColor = cardBg,
                borderColor = borderColor
            )
        }
    }
}

@Composable
private fun BubblePointerArrow(
    pointingUp: Boolean,
    biasX: Float,
    fillColor: Color,
    borderColor: Color
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val arrowW = with(density) { 20.dp.toPx() }
        val arrowH = with(density) { 10.dp.toPx() }
        val centerX = (widthPx * biasX).coerceIn(arrowW, (widthPx - arrowW).coerceAtLeast(arrowW))

        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                if (pointingUp) {
                    moveTo(centerX - arrowW / 2f, arrowH)
                    lineTo(centerX, 0f)
                    lineTo(centerX + arrowW / 2f, arrowH)
                } else {
                    moveTo(centerX - arrowW / 2f, 0f)
                    lineTo(centerX, arrowH)
                    lineTo(centerX + arrowW / 2f, 0f)
                }
                close()
            }
            drawPath(path = path, color = fillColor)
            val borderPath = Path().apply {
                if (pointingUp) {
                    moveTo(centerX - arrowW / 2f, arrowH)
                    lineTo(centerX, 0f)
                    lineTo(centerX + arrowW / 2f, arrowH)
                } else {
                    moveTo(centerX - arrowW / 2f, 0f)
                    lineTo(centerX, arrowH)
                    lineTo(centerX + arrowW / 2f, 0f)
                }
            }
            drawPath(
                path = borderPath,
                color = borderColor,
                style = Stroke(width = 1.25.dp.toPx())
            )
        }
    }
}

/**
 * Compact inline pointer bubble for ModalBottomSheets and Dialogs (which render in separate Popup windows).
 */
@Composable
fun SpotlightInlineGuideBubble(
    badge: String,
    title: String,
    description: String,
    pointingDown: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val bg = if (isDark) Color(0xFF172336) else Color(0xFFF0FDF9)
    val border = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)

    Column(modifier = modifier.fillMaxWidth()) {
        if (!pointingDown) {
            BubblePointerArrow(
                pointingUp = true,
                biasX = 0.5f,
                fillColor = bg,
                borderColor = border
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = bg,
            border = BorderStroke(1.4.dp, border),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (pointingDown) {
            BubblePointerArrow(
                pointingUp = false,
                biasX = 0.5f,
                fillColor = bg,
                borderColor = border
            )
        }
    }
}

