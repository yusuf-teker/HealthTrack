package com.yusufteker.healthtrack.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

enum class ToastType {
    SUCCESS,
    ERROR,
    INFO
}

private data class ToastVisuals(
    val backgroundColor: Color,
    val borderColor: Color,
    val icon: ImageVector,
    val iconColor: Color,
    val textColor: Color
)

/**
 * Modern floating glassmorphic Toast Notification.
 * Automatically slides in from the top, stays for [durationMs] and dismisses automatically.
 */
@Composable
fun HealthToastHost(
    message: String?,
    type: ToastType = ToastType.INFO,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    durationMs: Long = 3500L
) {
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            delay(durationMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(280)
        ) + fadeIn(animationSpec = tween(280)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(280)
        ) + fadeOut(animationSpec = tween(280)),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(100f)
    ) {
        if (!message.isNullOrBlank()) {
            val visuals = when (type) {
                ToastType.SUCCESS -> ToastVisuals(
                    backgroundColor = Color(0xFF0D2818).copy(alpha = 0.95f),
                    borderColor = Color(0xFF10B981).copy(alpha = 0.5f),
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF34D399),
                    textColor = Color(0xFFECFDF5)
                )
                ToastType.ERROR -> ToastVisuals(
                    backgroundColor = Color(0xFF301214).copy(alpha = 0.95f),
                    borderColor = Color(0xFFEF4444).copy(alpha = 0.5f),
                    icon = Icons.Default.ErrorOutline,
                    iconColor = Color(0xFFF87171),
                    textColor = Color(0xFFFEF2F2)
                )
                ToastType.INFO -> ToastVisuals(
                    backgroundColor = Color(0xFF131D2D).copy(alpha = 0.95f),
                    borderColor = Color(0xFF3B82F6).copy(alpha = 0.5f),
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFF60A5FA),
                    textColor = Color(0xFFEFF6FF)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = visuals.backgroundColor,
                    border = BorderStroke(1.dp, visuals.borderColor),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onDismiss() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = visuals.icon,
                            contentDescription = null,
                            tint = visuals.iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = message,
                            color = visuals.textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

