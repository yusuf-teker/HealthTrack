package com.yusufteker.healthtrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.yusufteker.healthtrack.core.model.CelebrationEvent
import com.yusufteker.healthtrack.core.model.CelebrationType
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class ConfettiShape {
    RECT,
    CIRCLE,
    DIAMOND
}

private data class ConfettiParticle(
    val originXFraction: Float,
    val originYFraction: Float,
    val vx: Float,
    val vy: Float,
    val gravity: Float,
    val sizeDp: Float,
    val color: Color,
    val shape: ConfettiShape,
    val spinSpeed: Float,
    val flutterFreq: Float,
    val flutterAmp: Float,
    val phaseOffset: Float
)

private val confettiPalette = listOf(
    Color(0xFF06D6A0),
    Color(0xFF38BDF8),
    Color(0xFFFACC15),
    Color(0xFFFB923C),
    Color(0xFFC084FC),
    Color(0xFFEC4899),
    Color(0xFF34D399),
    Color(0xFFFDE047)
)

/**
 * Full-screen celebratory confetti burst + luxury glassmorphic achievement card.
 * Triggered automatically whenever the user unlocks a badge, levels up, hits a streak milestone,
 * or completes all daily quests.
 */
@Composable
fun ConfettiCelebrationOverlay(
    event: CelebrationEvent?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = appLang()
    val isDark = LocalIsDarkTheme.current

    AnimatedVisibility(
        visible = event != null,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(220)),
        modifier = modifier
            .fillMaxSize()
            .zIndex(200f)
    ) {
        if (event != null) {
            val particleProgress = remember(event.id) { Animatable(0f) }
            val cardScale = remember(event.id) { Animatable(0.72f) }

            val particles = remember(event.id) {
                val rng = Random(event.id.hashCode())
                List(88) { index ->
                    val sideBurst = index % 3
                    val originX = when (sideBurst) {
                        0 -> 0.5f + (rng.nextFloat() - 0.5f) * 0.15f
                        1 -> 0.18f + rng.nextFloat() * 0.15f
                        else -> 0.67f + rng.nextFloat() * 0.15f
                    }
                    val angle = (-PI / 2.0) + (rng.nextFloat() - 0.5f) * 1.45
                    val speed = 620f + rng.nextFloat() * 880f
                    ConfettiParticle(
                        originXFraction = originX,
                        originYFraction = 0.34f + (rng.nextFloat() - 0.5f) * 0.10f,
                        vx = (cos(angle) * speed).toFloat(),
                        vy = (sin(angle) * speed).toFloat(),
                        gravity = 1150f + rng.nextFloat() * 450f,
                        sizeDp = 6f + rng.nextFloat() * 6f,
                        color = confettiPalette[index % confettiPalette.size],
                        shape = ConfettiShape.entries[index % ConfettiShape.entries.size],
                        spinSpeed = (rng.nextFloat() - 0.5f) * 720f,
                        flutterFreq = 4f + rng.nextFloat() * 6f,
                        flutterAmp = 18f + rng.nextFloat() * 28f,
                        phaseOffset = rng.nextFloat() * (2f * PI.toFloat())
                    )
                }
            }

            LaunchedEffect(event.id) {
                particleProgress.snapTo(0f)
                cardScale.snapTo(0.72f)
                cardScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            LaunchedEffect(event.id) {
                particleProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 2800, easing = LinearEasing)
                )
            }

            val infiniteTransition = rememberInfiniteTransition(label = "celebration_glow")
            val haloRotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "halo_rotation"
            )
            val iconPulse by infiniteTransition.animateFloat(
                initialValue = 0.94f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "icon_pulse"
            )

            val accentColor = when {
                event.badgeId != null -> Color(event.badgeId!!.accentColorHex)
                event.type == CelebrationType.STREAK_MILESTONE -> Color(0xFFFB923C)
                event.type == CelebrationType.LEVEL_UP -> Color(0xFFA855F7)
                event.type == CelebrationType.WORKOUT_GOAL_REACHED -> Color(0xFFF97316)
                event.type == CelebrationType.WATER_GOAL_REACHED -> Color(0xFF0EA5E9)
                else -> HealthColors.PrimaryGlow
            }

            val iconVector = when (event.type) {
                CelebrationType.BADGE_UNLOCKED -> Icons.Rounded.WorkspacePremium
                CelebrationType.LEVEL_UP -> Icons.Rounded.AutoAwesome
                CelebrationType.STREAK_MILESTONE -> Icons.Rounded.LocalFireDepartment
                CelebrationType.DAILY_GOAL_COMPLETED -> Icons.Rounded.EmojiEvents
                CelebrationType.WORKOUT_GOAL_REACHED -> Icons.Rounded.LocalFireDepartment
                CelebrationType.WATER_GOAL_REACHED -> Icons.Rounded.WaterDrop
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Confetti Canvas behind & around card
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val tSeconds = particleProgress.value * 2.8f
                    val fadeOutAlpha = if (particleProgress.value > 0.72f) {
                        ((1f - particleProgress.value) / 0.28f).coerceIn(0f, 1f)
                    } else {
                        1f
                    }
                    if (fadeOutAlpha > 0.01f) {
                        for (p in particles) {
                            val px = size.width * p.originXFraction +
                                p.vx * tSeconds +
                                sin(tSeconds * p.flutterFreq + p.phaseOffset) * p.flutterAmp.dp.toPx()
                            val py = size.height * p.originYFraction +
                                p.vy * tSeconds +
                                0.5f * p.gravity * tSeconds * tSeconds

                            if (py > size.height + 40f) continue

                            val s = p.sizeDp.dp.toPx()
                            val flipFactor = cos(tSeconds * p.flutterFreq + p.phaseOffset)
                                .let { kotlin.math.abs(it).coerceAtLeast(0.22f) }
                            val c = p.color.copy(alpha = fadeOutAlpha)
                            val rot = p.spinSpeed * tSeconds

                            rotate(degrees = rot, pivot = Offset(px, py)) {
                                when (p.shape) {
                                    ConfettiShape.RECT -> {
                                        drawRect(
                                            color = c,
                                            topLeft = Offset(px - (s * flipFactor) / 2f, py - s * 0.35f),
                                            size = Size(s * flipFactor, s * 0.7f)
                                        )
                                    }
                                    ConfettiShape.CIRCLE -> {
                                        drawCircle(
                                            color = c,
                                            radius = s * 0.38f,
                                            center = Offset(px, py)
                                        )
                                    }
                                    ConfettiShape.DIAMOND -> {
                                        drawRect(
                                            color = c,
                                            topLeft = Offset(px - s * 0.35f, py - s * 0.35f),
                                            size = Size(s * 0.7f, s * 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Celebration Modal Card
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .graphicsLayer {
                            scaleX = cardScale.value
                            scaleY = cardScale.value
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // absorb clicks inside card
                        ),
                    shape = RoundedCornerShape(28.dp),
                    borderWidth = 1.5.dp,
                    borderGradient = listOf(accentColor.copy(alpha = 0.65f), HealthColors.PrimaryGlow.copy(alpha = 0.35f)),
                    backgroundColor = if (isDark) Color(0xFF101727).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.97f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Rotating radial glow + icon pod
                        Box(
                            modifier = Modifier.size(94.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(94.dp)
                                    .graphicsLayer { rotationZ = haloRotation }
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                accentColor.copy(alpha = 0.45f),
                                                Color.Transparent,
                                                HealthColors.PrimaryGlow.copy(alpha = 0.45f),
                                                Color.Transparent,
                                                accentColor.copy(alpha = 0.45f)
                                            )
                                        )
                                    )
                            )
                            Surface(
                                shape = CircleShape,
                                color = accentColor.copy(alpha = 0.18f),
                                border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .size(72.dp)
                                    .graphicsLayer {
                                        scaleX = iconPulse
                                        scaleY = iconPulse
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        GradientText(
                            text = event.localizedTitle(lang),
                            colors = listOf(accentColor, HealthColors.PrimaryGlow),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = event.localizedSubtitle(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (event.xpBonus > 0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = accentColor.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "+${event.xpBonus} XP",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = accentColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color(0xFF07111E)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .bounceClick()
                        ) {
                            Text(
                                text = str("Harika!", "Awesome!"),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

