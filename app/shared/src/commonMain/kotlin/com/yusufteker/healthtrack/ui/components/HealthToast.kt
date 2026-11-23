package com.yusufteker.healthtrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.str
import kotlinx.coroutines.delay

enum class ToastType {
    SUCCESS,
    ERROR,
    INFO
}

private data class ToastVisuals(
    val backgroundColor: Color,
    val borderGradient: List<Color>,
    val icon: ImageVector,
    val accentColor: Color,
    val textColor: Color
)

private val ERROR_KEYWORDS = listOf(
    "başarısız",
    "hata",
    "tamamlanamadı",
    "açılamadı",
    "yüklü değil",
    "verilmedi",
    "alınamadı",
    "lütfen",
    "geçersiz",
    "çevrimdışı",
    "failed",
    "error",
    "could not",
    "not installed",
    "declined",
    "please ",
    "invalid",
    "offline",
    "unable"
)

private val SUCCESS_KEYWORDS = listOf(
    "kaydedildi",
    "eklendi",
    "güncellendi",
    "tamamlandı",
    "aktarıldı",
    "aktif edildi",
    "açıldı",
    "bağlandı",
    "silindi",
    "sıfırlandı",
    "saved",
    "added",
    "updated",
    "completed",
    "transferred",
    "activated",
    "enabled",
    "connected",
    "deleted",
    "reset",
    "✓"
)

internal fun inferToastType(message: String): ToastType {
    val lower = message.lowercase().replace("\u0307", "")
    if (ERROR_KEYWORDS.any { lower.contains(it) }) return ToastType.ERROR
    if (SUCCESS_KEYWORDS.any { lower.contains(it) }) return ToastType.SUCCESS
    return ToastType.INFO
}

/**
 * Stitch Luxury glassmorphic floating pill toast adapting cleanly to Dark and Light themes.
 */
@Composable
fun HealthToastPill(
    message: String,
    type: ToastType = ToastType.INFO,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val accent = when (type) {
        ToastType.SUCCESS -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
        ToastType.ERROR -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
        ToastType.INFO -> primaryColor
    }
    val bg = when (type) {
        ToastType.SUCCESS -> if (isDark) Color(0xFF0E1D1B).copy(alpha = 0.94f) else Color(0xFFF4FBF8).copy(alpha = 0.97f)
        ToastType.ERROR -> if (isDark) Color(0xFF221218).copy(alpha = 0.95f) else Color(0xFFFEF5F6).copy(alpha = 0.97f)
        ToastType.INFO -> if (isDark) Color(0xFF121827).copy(alpha = 0.94f) else Color(0xFFFFFFFF).copy(alpha = 0.97f)
    }
    val icon = when (type) {
        ToastType.SUCCESS -> Icons.Rounded.CheckCircle
        ToastType.ERROR -> Icons.Rounded.ErrorOutline
        ToastType.INFO -> Icons.Rounded.Info
    }
    val borderGradient = if (isDark) {
        listOf(
            accent.copy(alpha = 0.55f),
            Color.White.copy(alpha = 0.12f),
            accent.copy(alpha = 0.18f)
        )
    } else {
        listOf(
            accent.copy(alpha = 0.40f),
            accent.copy(alpha = 0.14f)
        )
    }
    val visuals = ToastVisuals(
        backgroundColor = bg,
        borderGradient = borderGradient,
        icon = icon,
        accentColor = accent,
        textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    )

    GlassCard(
        modifier = modifier.widthIn(min = 160.dp, max = 380.dp),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = visuals.backgroundColor,
        borderGradient = visuals.borderGradient,
        borderWidth = 1.dp,
        elevation = if (isDark) 12.dp else 8.dp,
        onClick = onDismiss
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(visuals.accentColor.copy(alpha = if (isDark) 0.18f else 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = visuals.icon,
                    contentDescription = null,
                    tint = visuals.accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = message,
                color = visuals.textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = str("Kapat", "Close"),
                tint = if (isDark) Color(0xFF94A3B8).copy(alpha = 0.7f) else Color(0xFF64748B).copy(alpha = 0.7f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Custom glassmorphic SnackbarHost replacing Material3's default unstyled rectangle.
 */
@Composable
fun HealthSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        val type = inferToastType(data.visuals.message)
        LaunchedEffect(data) {
            delay(if (type == ToastType.ERROR) 3400L else 2400L)
            data.dismiss()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            HealthToastPill(
                message = data.visuals.message,
                type = type,
                onDismiss = { data.dismiss() }
            )
        }
    }
}

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
    durationMs: Long = 2800L
) {
    var displayedMessage by remember { mutableStateOf("") }
    if (!message.isNullOrBlank()) {
        displayedMessage = message
    }

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
            animationSpec = tween(260)
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(240)
        ) + fadeOut(animationSpec = tween(200)),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(100f)
    ) {
        if (displayedMessage.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                HealthToastPill(
                    message = displayedMessage,
                    type = type,
                    onDismiss = onDismiss
                )
            }
        }
    }
}
