package com.yusufteker.healthtrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yusufteker.healthtrack.core.utils.CropRect
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import kotlin.math.abs

enum class CropRatioMode(val label: String) {
    FREE("Serbest 📐"),
    RECEIPT("Etiket / Fiş 🧾"),
    SQUARE("Kare 1:1 🔲"),
    TABLE("Yatay Tablo 📊"),
    FULL("Tamamı 🖼️")
}

private enum class DragHandle {
    NONE,
    BODY,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    EDGE_LEFT,
    EDGE_TOP,
    EDGE_RIGHT,
    EDGE_BOTTOM
}

@Composable
fun ImageCropDialog(
    photo: ImageBitmap,
    isProcessing: Boolean = false,
    onConfirmCrop: (cropRect: CropRect, rotationDegrees: Int) -> Unit,
    onScanFull: (rotationDegrees: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    var rotationDegrees by remember { mutableStateOf(0) }
    var selectedRatioMode by remember { mutableStateOf(CropRatioMode.FREE) }

    // Normalized coordinates (0f .. 1f)
    var cropLeft by remember { mutableStateOf(0.10f) }
    var cropTop by remember { mutableStateOf(0.12f) }
    var cropRight by remember { mutableStateOf(0.90f) }
    var cropBottom by remember { mutableStateOf(0.88f) }

    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }

    // Apply ratio mode presets
    fun applyRatioPreset(mode: CropRatioMode) {
        selectedRatioMode = mode
        when (mode) {
            CropRatioMode.FULL -> {
                cropLeft = 0.02f
                cropTop = 0.02f
                cropRight = 0.98f
                cropBottom = 0.98f
            }
            CropRatioMode.SQUARE -> {
                val half = 0.35f
                cropLeft = (0.5f - half).coerceAtLeast(0.04f)
                cropRight = (0.5f + half).coerceAtMost(0.96f)
                cropTop = (0.5f - half).coerceAtLeast(0.04f)
                cropBottom = (0.5f + half).coerceAtMost(0.96f)
            }
            CropRatioMode.RECEIPT -> {
                cropLeft = 0.15f
                cropRight = 0.85f
                cropTop = 0.04f
                cropBottom = 0.96f
            }
            CropRatioMode.TABLE -> {
                cropLeft = 0.05f
                cropRight = 0.95f
                cropTop = 0.20f
                cropBottom = 0.80f
            }
            CropRatioMode.FREE -> {
                // Keep current bounds
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(22.dp)),
            color = if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ── Header ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(HealthColors.Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Taranacak Alanı Kırpın ✂️",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Köşelerden ve kenarlardan sürükleyerek boyutu ayarlayın",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isProcessing,
                        modifier = Modifier.size(32.dp).bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Interactive Crop Canvas Viewport ──
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val containerWidth = constraints.maxWidth.toFloat()
                    val containerHeight = constraints.maxHeight.toFloat()

                    val isSwapped = (rotationDegrees / 90) % 2 != 0
                    val naturalW = if (isSwapped) photo.height.toFloat() else photo.width.toFloat()
                    val naturalH = if (isSwapped) photo.width.toFloat() else photo.height.toFloat()

                    val scale = minOf(containerWidth / naturalW, containerHeight / naturalH)
                    val dispW = naturalW * scale
                    val dispH = naturalH * scale

                    val dispLeft = (containerWidth - dispW) / 2f
                    val dispTop = (containerHeight - dispH) / 2f

                    // Display Image with current rotation
                    Image(
                        bitmap = photo,
                        contentDescription = "Fotoğraf",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(
                                width = (if (isSwapped) dispH else dispW).dp,
                                height = (if (isSwapped) dispW else dispH).dp
                            )
                            .graphicsLayer {
                                rotationZ = rotationDegrees.toFloat()
                            }
                    )

                    // Overlay with Clear Cutout and 8 Interactive Crop Handles
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(dispW, dispH, dispLeft, dispTop) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        if (dispW <= 0f || dispH <= 0f) return@detectDragGestures
                                        val pxL = dispLeft + (cropLeft * dispW)
                                        val pxT = dispTop + (cropTop * dispH)
                                        val pxR = dispLeft + (cropRight * dispW)
                                        val pxB = dispTop + (cropBottom * dispH)

                                        val x = offset.x
                                        val y = offset.y

                                        val cornerHit = 36.dp.toPx()
                                        val edgeHit = 28.dp.toPx()

                                        activeHandle = when {
                                            // 1. Four Corners (Highest Priority)
                                            abs(x - pxL) <= cornerHit && abs(y - pxT) <= cornerHit -> DragHandle.TOP_LEFT
                                            abs(x - pxR) <= cornerHit && abs(y - pxT) <= cornerHit -> DragHandle.TOP_RIGHT
                                            abs(x - pxL) <= cornerHit && abs(y - pxB) <= cornerHit -> DragHandle.BOTTOM_LEFT
                                            abs(x - pxR) <= cornerHit && abs(y - pxB) <= cornerHit -> DragHandle.BOTTOM_RIGHT

                                            // 2. Four Edges
                                            abs(x - pxL) <= edgeHit && y in (pxT - 10f)..(pxB + 10f) -> DragHandle.EDGE_LEFT
                                            abs(x - pxR) <= edgeHit && y in (pxT - 10f)..(pxB + 10f) -> DragHandle.EDGE_RIGHT
                                            abs(y - pxT) <= edgeHit && x in (pxL - 10f)..(pxR + 10f) -> DragHandle.EDGE_TOP
                                            abs(y - pxB) <= edgeHit && x in (pxL - 10f)..(pxR + 10f) -> DragHandle.EDGE_BOTTOM

                                            // 3. Inside Crop Area -> Pan / Move
                                            x in pxL..pxR && y in pxT..pxB -> DragHandle.BODY

                                            else -> DragHandle.NONE
                                        }
                                    },
                                    onDragEnd = {
                                        activeHandle = DragHandle.NONE
                                    },
                                    onDragCancel = {
                                        activeHandle = DragHandle.NONE
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (dispW <= 0f || dispH <= 0f || activeHandle == DragHandle.NONE) return@detectDragGestures

                                        val deltaX = dragAmount.x / dispW
                                        val deltaY = dragAmount.y / dispH
                                        val minSize = 0.05f // Minimum allowed frame size (5%)

                                        // Any corner or edge resize immediately allows freeform adjustment
                                        if (activeHandle != DragHandle.BODY) {
                                            selectedRatioMode = CropRatioMode.FREE
                                        }

                                        when (activeHandle) {
                                            DragHandle.BODY -> {
                                                val curWidth = cropRight - cropLeft
                                                val curHeight = cropBottom - cropTop
                                                val newL = (cropLeft + deltaX).coerceIn(0f, 1f - curWidth)
                                                val newT = (cropTop + deltaY).coerceIn(0f, 1f - curHeight)
                                                cropLeft = newL
                                                cropRight = newL + curWidth
                                                cropTop = newT
                                                cropBottom = newT + curHeight
                                            }
                                            DragHandle.TOP_LEFT -> {
                                                cropLeft = (cropLeft + deltaX).coerceIn(0f, cropRight - minSize)
                                                cropTop = (cropTop + deltaY).coerceIn(0f, cropBottom - minSize)
                                            }
                                            DragHandle.TOP_RIGHT -> {
                                                cropRight = (cropRight + deltaX).coerceIn(cropLeft + minSize, 1f)
                                                cropTop = (cropTop + deltaY).coerceIn(0f, cropBottom - minSize)
                                            }
                                            DragHandle.BOTTOM_LEFT -> {
                                                cropLeft = (cropLeft + deltaX).coerceIn(0f, cropRight - minSize)
                                                cropBottom = (cropBottom + deltaY).coerceIn(cropTop + minSize, 1f)
                                            }
                                            DragHandle.BOTTOM_RIGHT -> {
                                                cropRight = (cropRight + deltaX).coerceIn(cropLeft + minSize, 1f)
                                                cropBottom = (cropBottom + deltaY).coerceIn(cropTop + minSize, 1f)
                                            }
                                            DragHandle.EDGE_LEFT -> {
                                                cropLeft = (cropLeft + deltaX).coerceIn(0f, cropRight - minSize)
                                            }
                                            DragHandle.EDGE_RIGHT -> {
                                                cropRight = (cropRight + deltaX).coerceIn(cropLeft + minSize, 1f)
                                            }
                                            DragHandle.EDGE_TOP -> {
                                                cropTop = (cropTop + deltaY).coerceIn(0f, cropBottom - minSize)
                                            }
                                            DragHandle.EDGE_BOTTOM -> {
                                                cropBottom = (cropBottom + deltaY).coerceIn(cropTop + minSize, 1f)
                                            }
                                            DragHandle.NONE -> {}
                                        }
                                    }
                                )
                            }
                    ) {
                        val pxLeft = dispLeft + (cropLeft * dispW)
                        val pxTop = dispTop + (cropTop * dispH)
                        val pxRight = dispLeft + (cropRight * dispW)
                        val pxBottom = dispTop + (cropBottom * dispH)
                        val pxW = pxRight - pxLeft
                        val pxH = pxBottom - pxTop

                        val darkOverlay = Color.Black.copy(alpha = 0.65f)

                        // 4 Surrounding Dark Bars (outside crop frame)
                        drawRect(
                            color = darkOverlay,
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width, pxTop)
                        )
                        drawRect(
                            color = darkOverlay,
                            topLeft = Offset(0f, pxBottom),
                            size = Size(size.width, size.height - pxBottom)
                        )
                        drawRect(
                            color = darkOverlay,
                            topLeft = Offset(0f, pxTop),
                            size = Size(pxLeft, pxH)
                        )
                        drawRect(
                            color = darkOverlay,
                            topLeft = Offset(pxRight, pxTop),
                            size = Size(size.width - pxRight, pxH)
                        )

                        // Bounding Box Accent Border
                        drawRect(
                            color = HealthColors.Primary,
                            topLeft = Offset(pxLeft, pxTop),
                            size = Size(pxW, pxH),
                            style = Stroke(width = 2.5f.dp.toPx())
                        )

                        // Rule of Thirds subtle lines
                        val thirdW = pxW / 3f
                        val thirdH = pxH / 3f
                        val gridColor = Color.White.copy(alpha = 0.35f)
                        val gridStroke = Stroke(width = 1.dp.toPx())

                        drawLine(gridColor, Offset(pxLeft + thirdW, pxTop), Offset(pxLeft + thirdW, pxBottom), strokeWidth = gridStroke.width)
                        drawLine(gridColor, Offset(pxLeft + 2 * thirdW, pxTop), Offset(pxLeft + 2 * thirdW, pxBottom), strokeWidth = gridStroke.width)
                        drawLine(gridColor, Offset(pxLeft, pxTop + thirdH), Offset(pxRight, pxTop + thirdH), strokeWidth = gridStroke.width)
                        drawLine(gridColor, Offset(pxLeft, pxTop + 2 * thirdH), Offset(pxRight, pxTop + 2 * thirdH), strokeWidth = gridStroke.width)

                        // ── 4 Corner Brackets + Circular Touch Nodes ──
                        val cornerLen = 18.dp.toPx()
                        val cornerStroke = 4.dp.toPx()
                        val cornerColor = HealthColors.Primary
                        val nodeRadius = 8.dp.toPx()

                        // Top-Left
                        drawLine(cornerColor, Offset(pxLeft - 2f, pxTop), Offset(pxLeft + cornerLen, pxTop), cornerStroke)
                        drawLine(cornerColor, Offset(pxLeft, pxTop - 2f), Offset(pxLeft, pxTop + cornerLen), cornerStroke)
                        drawCircle(color = HealthColors.Primary, radius = nodeRadius, center = Offset(pxLeft, pxTop))
                        drawCircle(color = Color.White, radius = nodeRadius, center = Offset(pxLeft, pxTop), style = Stroke(2.dp.toPx()))

                        // Top-Right
                        drawLine(cornerColor, Offset(pxRight - cornerLen, pxTop), Offset(pxRight + 2f, pxTop), cornerStroke)
                        drawLine(cornerColor, Offset(pxRight, pxTop - 2f), Offset(pxRight, pxTop + cornerLen), cornerStroke)
                        drawCircle(color = HealthColors.Primary, radius = nodeRadius, center = Offset(pxRight, pxTop))
                        drawCircle(color = Color.White, radius = nodeRadius, center = Offset(pxRight, pxTop), style = Stroke(2.dp.toPx()))

                        // Bottom-Left
                        drawLine(cornerColor, Offset(pxLeft - 2f, pxBottom), Offset(pxLeft + cornerLen, pxBottom), cornerStroke)
                        drawLine(cornerColor, Offset(pxLeft, pxBottom - cornerLen), Offset(pxLeft, pxBottom + 2f), cornerStroke)
                        drawCircle(color = HealthColors.Primary, radius = nodeRadius, center = Offset(pxLeft, pxBottom))
                        drawCircle(color = Color.White, radius = nodeRadius, center = Offset(pxLeft, pxBottom), style = Stroke(2.dp.toPx()))

                        // Bottom-Right
                        drawLine(cornerColor, Offset(pxRight - cornerLen, pxBottom), Offset(pxRight + 2f, pxBottom), cornerStroke)
                        drawLine(cornerColor, Offset(pxRight, pxBottom - cornerLen), Offset(pxRight, pxBottom + 2f), cornerStroke)
                        drawCircle(color = HealthColors.Primary, radius = nodeRadius, center = Offset(pxRight, pxBottom))
                        drawCircle(color = Color.White, radius = nodeRadius, center = Offset(pxRight, pxBottom), style = Stroke(2.dp.toPx()))

                        // ── 4 Midpoint Edge Touch Bars ──
                        val edgeBarLen = 22.dp.toPx()
                        val edgeBarThick = 4.dp.toPx()
                        val edgeBarColor = Color.White.copy(alpha = 0.9f)
                        val edgeRadius = CornerRadius(3.dp.toPx())

                        // Mid Top
                        drawRoundRect(
                            color = edgeBarColor,
                            topLeft = Offset((pxLeft + pxRight) / 2f - edgeBarLen / 2f, pxTop - edgeBarThick / 2f),
                            size = Size(edgeBarLen, edgeBarThick),
                            cornerRadius = edgeRadius
                        )
                        // Mid Bottom
                        drawRoundRect(
                            color = edgeBarColor,
                            topLeft = Offset((pxLeft + pxRight) / 2f - edgeBarLen / 2f, pxBottom - edgeBarThick / 2f),
                            size = Size(edgeBarLen, edgeBarThick),
                            cornerRadius = edgeRadius
                        )
                        // Mid Left
                        drawRoundRect(
                            color = edgeBarColor,
                            topLeft = Offset(pxLeft - edgeBarThick / 2f, (pxTop + pxBottom) / 2f - edgeBarLen / 2f),
                            size = Size(edgeBarThick, edgeBarLen),
                            cornerRadius = edgeRadius
                        )
                        // Mid Right
                        drawRoundRect(
                            color = edgeBarColor,
                            topLeft = Offset(pxRight - edgeBarThick / 2f, (pxTop + pxBottom) / 2f - edgeBarLen / 2f),
                            size = Size(edgeBarThick, edgeBarLen),
                            cornerRadius = edgeRadius
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Preset Ratio Chips & Rotation ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rotate Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HealthColors.Primary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HealthColors.Primary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                rotationDegrees = (rotationDegrees + 90) % 360
                            }
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = "Döndür",
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Döndür 90°",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = HealthColors.Primary
                            )
                        }
                    }

                    CropRatioMode.entries.forEach { mode ->
                        val isSelected = selectedRatioMode == mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { applyRatioPreset(mode) }
                                .bounceClick()
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Action Buttons ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onScanFull(rotationDegrees) },
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tümünü Tara",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            val rect = CropRect(
                                left = cropLeft.coerceIn(0f, 1f),
                                top = cropTop.coerceIn(0f, 1f),
                                right = cropRight.coerceIn(cropLeft + 0.01f, 1f),
                                bottom = cropBottom.coerceIn(cropTop + 0.01f, 1f)
                            )
                            onConfirmCrop(rect, rotationDegrees)
                        },
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HealthColors.Primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .bounceClick()
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Taranıyor...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kırp ve Tara",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
