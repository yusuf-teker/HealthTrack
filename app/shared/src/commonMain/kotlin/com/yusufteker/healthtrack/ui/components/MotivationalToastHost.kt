package com.yusufteker.healthtrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yusufteker.healthtrack.core.model.MotivationalToastEvent
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import kotlinx.coroutines.delay

/**
 * Floating luxury bio-telemetry toast triggered on every active meal, workout, water, or weight entry
 * that gives immediate XP feedback and shows daily goal progress even before 100% completion.
 */
@Composable
fun MotivationalToastHost(
    event: MotivationalToastEvent?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = appLang()
    val isDark = LocalIsDarkTheme.current

    LaunchedEffect(event?.id) {
        if (event != null) {
            delay(3400L)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = event != null,
        enter = slideInVertically(
            initialOffsetY = { -it - 40 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { -it - 40 },
            animationSpec = tween(220)
        ) + fadeOut(tween(180)),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 60.dp, start = 16.dp, end = 16.dp)
            .zIndex(150f)
    ) {
        if (event != null) {
            val accent = Color(event.accentColorHex)
            val xpScale = remember(event.id) { Animatable(0.6f) }

            LaunchedEffect(event.id) {
                xpScale.snapTo(0.6f)
                xpScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() },
                shape = RoundedCornerShape(20.dp),
                borderWidth = 1.25.dp,
                borderGradient = listOf(
                    accent.copy(alpha = 0.72f),
                    Color(0xFFFACC15).copy(alpha = 0.45f)
                ),
                backgroundColor = if (isDark) {
                    Color(0xFF101727).copy(alpha = 0.94f)
                } else {
                    Color.White.copy(alpha = 0.97f)
                },
                elevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.localizedTitle(lang),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = event.localizedSubtitle(lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFFACC15).copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color(0xFFFACC15).copy(alpha = 0.55f)),
                            modifier = Modifier.graphicsLayer {
                                scaleX = xpScale.value
                                scaleY = xpScale.value
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFFEAB308),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "+${event.xpEarned} XP",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (isDark) Color(0xFFFDE047) else Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    val targetProgress = event.progressFraction
                    if (targetProgress != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val animatedProgress by animateFloatAsState(
                            targetValue = targetProgress.coerceIn(0f, 1f),
                            animationSpec = tween(durationMillis = 550),
                            label = "toast_progress"
                        )
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = accent,
                            trackColor = accent.copy(alpha = 0.16f),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

