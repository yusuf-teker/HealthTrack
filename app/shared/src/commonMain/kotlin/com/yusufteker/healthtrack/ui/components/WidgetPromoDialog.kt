package com.yusufteker.healthtrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yusufteker.healthtrack.core.model.WidgetTelemetrySnapshot
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.str
import kotlin.math.roundToInt

enum class WidgetPreviewSize {
    MEDIUM,
    COMPACT
}

/**
 * Daily reminder modal encouraging the user to place the HealthTrack home-screen widget
 * whenever no widget is currently configured. Shows a live interactive widget preview,
 * explains what the widget does, and offers one-tap pinning (Android) or a 3-step guide (iOS).
 */
@Composable
fun WidgetPromoDialog(
    snapshot: WidgetTelemetrySnapshot,
    supportsOneTapPinning: Boolean,
    isIosPlatform: Boolean,
    onAddWidgetClick: () -> Unit,
    onDismissForToday: () -> Unit,
    onDisableDailyReminder: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    var previewSize by remember { mutableStateOf(WidgetPreviewSize.MEDIUM) }
    var showStepByStepGuide by remember { mutableStateOf(false) }

    val basePreviewSnapshot = remember(snapshot) {
        if (snapshot.hasMeaningfulData) {
            snapshot
        } else {
            WidgetTelemetrySnapshot.sample(
                languageCode = snapshot.languageCode,
                isDarkTheme = snapshot.isDarkTheme,
                themeAccentId = snapshot.themeAccentId
            )
        }
    }
    var interactiveWaterBonusMl by remember(basePreviewSnapshot) { mutableStateOf(0) }
    val previewSnapshot = remember(basePreviewSnapshot, interactiveWaterBonusMl) {
        basePreviewSnapshot.copy(
            waterMl = (basePreviewSnapshot.waterMl + interactiveWaterBonusMl).coerceAtLeast(0)
        )
    }

    Dialog(
        onDismissRequest = onDismissForToday,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .imePadding()
                .clearFocusOnTap(),
            shape = RoundedCornerShape(28.dp),
            color = if (isDark) Color(0xFF0E1422) else HealthColors.SurfaceLight,
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        HealthColors.Primary.copy(alpha = 0.45f),
                        Color.White.copy(alpha = if (isDark) 0.14f else 0.5f),
                        HealthColors.SecondaryAccent.copy(alpha = 0.25f)
                    )
                )
            ),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            HealthColors.Primary.copy(alpha = 0.24f),
                                            HealthColors.PrimaryGlow.copy(alpha = 0.12f)
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    HealthColors.Primary.copy(alpha = 0.35f),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Widgets,
                                contentDescription = null,
                                tint = HealthColors.PrimaryGlow,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = str("Ana Ekran Widget'ı", "Home Screen Widget"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = str(
                                    "Sağlık telemetrini ana ekranında canlı tut",
                                    "Keep your live health telemetry on your home screen"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissForToday,
                        modifier = Modifier.size(32.dp).bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = str("Kapat", "Close"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Live Preview Size Switcher + Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF06D6A0))
                        )
                        Text(
                            text = if (snapshot.hasMeaningfulData) {
                                str("Canlı Önizleme • Bugünkü Verin", "Live Preview • Today's Data")
                            } else {
                                str("Gerçekçi Önizleme (+250ml'e dokun!)", "Live Preview (Tap +250ml!)")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        PreviewSizeChip(
                            label = str("4×2", "Med"),
                            selected = previewSize == WidgetPreviewSize.MEDIUM,
                            onClick = { previewSize = WidgetPreviewSize.MEDIUM }
                        )
                        PreviewSizeChip(
                            label = str("2×2", "Small"),
                            selected = previewSize == WidgetPreviewSize.COMPACT,
                            onClick = { previewSize = WidgetPreviewSize.COMPACT }
                        )
                    }
                }

                // Simulated Home Screen Frame + Real Widget Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                if (isDark) {
                                    listOf(Color(0xFF091224), Color(0xFF13223D), Color(0xFF0A1426))
                                } else {
                                    listOf(Color(0xFFE2EBF7), Color(0xFFD5E4F6), Color(0xFFEAF1FB))
                                }
                            )
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewSize == WidgetPreviewSize.MEDIUM) {
                        WidgetLivePreviewMedium(
                            snapshot = previewSnapshot,
                            onSimulateWaterTap = { interactiveWaterBonusMl += 250 }
                        )
                    } else {
                        WidgetLivePreviewCompact(
                            snapshot = previewSnapshot,
                            onSimulateWaterTap = { interactiveWaterBonusMl += 250 }
                        )
                    }
                }

                // What It Does Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = str("Bu Widget Ne İşe Yarar?", "What Does This Widget Do?"),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    WidgetFeatureRow(
                        icon = Icons.Rounded.Bolt,
                        iconTint = Color(0xFF38BDF8),
                        title = str("Anlık Kalori & Makro Halkası", "Live Calorie & Macro Ring"),
                        description = str(
                            "Uygulamayı açmadan kalan kalorini, protein/karb/yağ dengeni ve yakılan enerjiyi takip et.",
                            "See remaining calories, protein/carbs/fat balance, and burned energy without opening the app."
                        )
                    )
                    WidgetFeatureRow(
                        icon = Icons.Rounded.WaterDrop,
                        iconTint = Color(0xFF06D6A0),
                        title = str("Ana Ekrandan Tek Dokunuşla Su Ekle", "One-Tap Hydration (+250 ml)"),
                        description = str(
                            "Widget üzerindeki +250ml butonuna basarak su tüketimini anında kaydet.",
                            "Log a glass of water directly from your home screen with a single tap on +250ml."
                        )
                    )
                    WidgetFeatureRow(
                        icon = Icons.Rounded.TouchApp,
                        iconTint = Color(0xFFC084FC),
                        title = str("Hızlı Öğün, Spor & Tartım Kısayolu", "Instant Meal, Workout & Weight"),
                        description = str(
                            "Tek dokunuşla doğrudan yemek ekleme ekranına, spor veya tartım sekmesine geç.",
                            "Jump straight into meal logging, workouts, or weight tracking in one tap."
                        )
                    )
                }

                // Expandable Step-by-Step Guide (iOS or manual Android fallback)
                AnimatedVisibility(
                    visible = showStepByStepGuide,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = HealthColors.Primary.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, HealthColors.Primary.copy(alpha = 0.30f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isIosPlatform) {
                                    str("iPhone Ana Ekranına Nasıl Eklenir?", "How to Add on iPhone")
                                } else {
                                    str("Ana Ekrana Nasıl Eklenir?", "How to Add Manually")
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = HealthColors.PrimaryGlow
                            )
                            GuideStepItem(
                                step = "1",
                                text = if (isIosPlatform) {
                                    str("Ana ekranında boş bir alana titreyene kadar basılı tut.", "Touch and hold an empty area on your Home Screen until apps jiggle.")
                                } else {
                                    str("Ana ekranında boş bir alana basılı tut ve 'Widget'lar'ı seç.", "Press and hold an empty area on your Home Screen and tap 'Widgets'.")
                                }
                            )
                            GuideStepItem(
                                step = "2",
                                text = if (isIosPlatform) {
                                    str("Sol üstteki '+' (Düzenle → Araç Takımı Ekle) butonuna dokun.", "Tap the '+' (Edit → Add Widget) button in the top-left corner.")
                                } else {
                                    str("Listeden 'HealthTrack' uygulamasını bul.", "Scroll or search for 'HealthTrack' in the widget picker.")
                                }
                            )
                            GuideStepItem(
                                step = "3",
                                text = str(
                                    "İstediğin boyutu (Küçük, Orta veya Büyük) seçip 'Widget Ekle'ye dokun.",
                                    "Pick your preferred size (Small, Medium, or Large) and tap 'Add Widget'."
                                )
                            )
                        }
                    }
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (supportsOneTapPinning) {
                                onAddWidgetClick()
                            } else {
                                if (!showStepByStepGuide) {
                                    showStepByStepGuide = true
                                } else {
                                    onDismissForToday()
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HealthColors.Primary,
                            contentColor = HealthColors.OnPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .bounceClick()
                    ) {
                        Icon(
                            imageVector = if (showStepByStepGuide && !supportsOneTapPinning) {
                                Icons.Rounded.CheckCircle
                            } else {
                                Icons.Rounded.Add
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                supportsOneTapPinning -> str("Ana Ekrana Ekle", "Add to Home Screen")
                                showStepByStepGuide -> str("Anladım, Ekleyeceğim", "Got It, I'll Add It")
                                else -> str("Widget Ekle (Nasıl Eklenir?)", "Add Widget (How To)")
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDisableDailyReminder,
                            modifier = Modifier.bounceClick()
                        ) {
                            Text(
                                text = str("Bir daha hatırlatma", "Don't remind again"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }

                        TextButton(
                            onClick = onDismissForToday,
                            modifier = Modifier.bounceClick()
                        ) {
                            Text(
                                text = str("Daha Sonra", "Later"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSizeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) HealthColors.Primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Bold,
            color = if (selected) HealthColors.OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Authentic live Compose replica of the Medium/4x2 HealthTrack Home Screen Widget.
 */
@Composable
fun WidgetLivePreviewMedium(
    snapshot: WidgetTelemetrySnapshot,
    onSimulateWaterTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = snapshot.isDarkTheme
    val bgBrush = Brush.linearGradient(
        if (isDark) listOf(Color(0xFF0C1220), Color(0xFF152036))
        else listOf(Color(0xFFFFFFFF), Color(0xFFF3F7FD))
    )
    val borderColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFD5E1F0)
    val primaryText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val trackColor = if (isDark) Color(0xFF243047) else Color(0xFFE2E8F0)
    val actionBg = if (isDark) Color(0xFF192338) else Color(0xFFEDF2FA)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "H",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "HealthTrack",
                    color = primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (snapshot.burnedCalories > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFB923C),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${snapshot.burnedCalories} kcal",
                            color = Color(0xFFFB923C),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0xFF2E2012))
                        .border(0.75.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(99.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⚡ ${snapshot.streakDays}${str("g", "d")}",
                        color = Color(0xFFFBBF24),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Main Telemetry Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WidgetMiniCalorieRing(
                snapshot = snapshot,
                size = 78.dp,
                isDark = isDark
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                WidgetMacroRow(
                    label = "Protein",
                    current = snapshot.proteinG,
                    target = snapshot.proteinTargetG,
                    color = HealthColors.NutrientProtein,
                    trackColor = trackColor,
                    primaryText = primaryText,
                    secondaryText = secondaryText
                )
                WidgetMacroRow(
                    label = str("Karb", "Carbs"),
                    current = snapshot.carbsG,
                    target = snapshot.carbsTargetG,
                    color = HealthColors.NutrientCarbs,
                    trackColor = trackColor,
                    primaryText = primaryText,
                    secondaryText = secondaryText
                )
                WidgetMacroRow(
                    label = str("Yağ", "Fat"),
                    current = snapshot.fatG,
                    target = snapshot.fatTargetG,
                    color = HealthColors.NutrientLipids,
                    trackColor = trackColor,
                    primaryText = primaryText,
                    secondaryText = secondaryText
                )

                // Interactive Hydration Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "💧 ${snapshot.waterMl} / ${snapshot.waterTargetMl} ml",
                            color = if (snapshot.isWaterGoalMet) Color(0xFF06D6A0) else Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val animatedWater by animateFloatAsState(
                            targetValue = snapshot.waterProgress,
                            animationSpec = tween(350)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(trackColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedWater)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF00B4D8), Color(0xFF06D6A0))
                                        )
                                    )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF193A4E))
                            .border(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .then(
                                if (onSimulateWaterTap != null) {
                                    Modifier.bounceClick().clickable(onClick = onSimulateWaterTap)
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+250ml",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Bottom Quick Shortcuts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WidgetShortcutPill(
                icon = Icons.Rounded.Restaurant,
                label = str("+ Öğün", "+ Meal"),
                tint = Color(0xFF38BDF8),
                bgColor = actionBg,
                modifier = Modifier.weight(1f)
            )
            WidgetShortcutPill(
                icon = Icons.Rounded.FitnessCenter,
                label = str("Spor", "Workout"),
                tint = Color(0xFFFB923C),
                bgColor = actionBg,
                modifier = Modifier.weight(1f)
            )
            WidgetShortcutPill(
                icon = Icons.Rounded.MonitorWeight,
                label = str("Tartım", "Weight"),
                tint = Color(0xFFC084FC),
                bgColor = actionBg,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Authentic live Compose replica of the Small/2x2 HealthTrack Home Screen Widget.
 */
@Composable
fun WidgetLivePreviewCompact(
    snapshot: WidgetTelemetrySnapshot,
    onSimulateWaterTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = snapshot.isDarkTheme
    val bgBrush = Brush.linearGradient(
        if (isDark) listOf(Color(0xFF0C1220), Color(0xFF152036))
        else listOf(Color(0xFFFFFFFF), Color(0xFFF3F7FD))
    )
    val borderColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFD5E1F0)
    val primaryText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val actionBg = if (isDark) Color(0xFF192338) else Color(0xFFEDF2FA)

    val currentL = ((snapshot.waterMl / 1000.0) * 10).roundToInt() / 10.0
    val targetL = ((snapshot.waterTargetMl / 1000.0) * 10).roundToInt() / 10.0

    Column(
        modifier = modifier
            .width(172.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .padding(11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("H", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Text(
                    text = if (snapshot.isOverCalories) {
                        "+${snapshot.overCalories} ${str("aşım", "over")}"
                    } else {
                        "${snapshot.remainingCalories} ${str("kaldı", "left")}"
                    },
                    color = primaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "⚡${snapshot.streakDays}${str("g", "d")}",
                color = Color(0xFFFBBF24),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        WidgetMiniCalorieRing(
            snapshot = snapshot,
            size = 72.dp,
            isDark = isDark
        )

        Text(
            text = "💧 $currentL / ${targetL}L",
            color = if (snapshot.isWaterGoalMet) Color(0xFF06D6A0) else Color(0xFF38BDF8),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF193A4E))
                    .border(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .then(
                        if (onSimulateWaterTap != null) {
                            Modifier.bounceClick().clickable(onClick = onSimulateWaterTap)
                        } else {
                            Modifier
                        }
                    )
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+250ml",
                    color = Color(0xFF38BDF8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(actionBg)
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = str("+ Öğün", "+ Meal"),
                    color = primaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WidgetMiniCalorieRing(
    snapshot: WidgetTelemetrySnapshot,
    size: androidx.compose.ui.unit.Dp,
    isDark: Boolean
) {
    val rawRatio = snapshot.rawCalorieRatio
    val isOver = snapshot.isOverCalories
    val animatedProgress by animateFloatAsState(
        targetValue = snapshot.calorieProgress,
        animationSpec = tween(500)
    )
    val trackColor = if (isDark) Color(0xFF243047) else Color(0xFFE2E8F0)
    val activeColor = CalorieTelemetryColors.getColorForRatio(rawRatio)

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
            val strokePx = (size.toPx() * 0.105f)
            val diameter = this.size.minDimension - strokePx
            val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (isOver) {
                drawArc(
                    color = CalorieTelemetryColors.Red.copy(alpha = 0.22f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx + 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            if (animatedProgress > 0f) {
                rotate(degrees = -90f, pivot = center) {
                    val brush = when {
                        isOver -> Brush.sweepGradient(
                            0f to CalorieTelemetryColors.Orange,
                            0.5f to CalorieTelemetryColors.Red,
                            1f to CalorieTelemetryColors.Orange
                        )
                        rawRatio >= 0.75f -> Brush.sweepGradient(
                            0f to CalorieTelemetryColors.Blue,
                            0.4f to CalorieTelemetryColors.Yellow,
                            0.85f to CalorieTelemetryColors.Orange,
                            1f to CalorieTelemetryColors.Blue
                        )
                        rawRatio >= 0.50f -> Brush.sweepGradient(
                            0f to CalorieTelemetryColors.Blue,
                            0.6f to CalorieTelemetryColors.Yellow,
                            1f to CalorieTelemetryColors.Blue
                        )
                        else -> Brush.sweepGradient(
                            0f to CalorieTelemetryColors.Blue,
                            0.5f to CalorieTelemetryColors.BlueGlow,
                            1f to CalorieTelemetryColors.Blue
                        )
                    }
                    drawArc(
                        brush = brush,
                        startAngle = 0f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isOver) str("AŞILDI", "OVER") else str("ALINAN", "INTAKE"),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOver) CalorieTelemetryColors.Red else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
            Text(
                text = "${snapshot.consumedCalories}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = if (isOver) CalorieTelemetryColors.Red else if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                lineHeight = 16.sp
            )
            Text(
                text = if (isOver) "+${snapshot.overCalories}" else "${snapshot.remainingCalories} ${str("kaldı", "left")}",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = activeColor
            )
        }
    }
}

@Composable
private fun WidgetMacroRow(
    label: String,
    current: Int,
    target: Int,
    color: Color,
    trackColor: Color,
    primaryText: Color,
    secondaryText: Color
) {
    val progress = if (target > 0) (current.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = secondaryText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(42.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(99.dp))
                    .background(color)
            )
        }
        Text(
            text = "${current}/${target}g",
            color = primaryText,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun WidgetShortcutPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(vertical = 6.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WidgetFeatureRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun GuideStepItem(
    step: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(HealthColors.Primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = HealthColors.OnPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

