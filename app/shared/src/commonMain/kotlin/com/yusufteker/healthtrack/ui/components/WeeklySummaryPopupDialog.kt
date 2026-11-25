package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yusufteker.healthtrack.core.sync.WeeklyDaySummaryBar
import com.yusufteker.healthtrack.core.sync.WeeklySummarySnapshot
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import kotlin.math.abs

/**
 * One-time weekly recap popup card shown once per completed week (Mon..Sun),
 * summarizing deterministic local telemetry (0 AI quota cost) with Stitch Luxury aesthetics.
 */
@Composable
fun WeeklySummaryPopupDialog(
    summary: WeeklySummarySnapshot,
    onOpenAnalyticsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val lang = appLang()

    val scoreAccent = when {
        summary.consistencyScore >= 75 -> Color(0xFF10B981)
        summary.consistencyScore >= 50 -> Color(0xFF38BDF8)
        else -> Color(0xFFF59E0B)
    }

    Dialog(
        onDismissRequest = onDismiss,
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
                        HealthColors.Primary.copy(alpha = 0.50f),
                        Color.White.copy(alpha = if (isDark) 0.14f else 0.55f),
                        scoreAccent.copy(alpha = 0.35f)
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
                                            HealthColors.Primary.copy(alpha = 0.25f),
                                            scoreAccent.copy(alpha = 0.16f)
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    HealthColors.Primary.copy(alpha = 0.38f),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = HealthColors.PrimaryGlow,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = str("Haftalık Özetin Hazır", "Your Weekly Recap"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = summary.localizedPeriodLabel(lang),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = HealthColors.PrimaryGlow
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = scoreAccent.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, scoreAccent.copy(alpha = 0.40f))
                        ) {
                            Text(
                                text = "${summary.consistencyScore}/100",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = scoreAccent,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp).bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = str("Kapat", "Close"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 7-Day Activity & Calorie Telemetry Strip
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDark) Color(0xFF131B2E) else HealthColors.SurfaceVariantLight.copy(alpha = 0.55f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.22f else 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                        .background(scoreAccent)
                                )
                                Text(
                                    text = str(
                                        "${summary.activeDays}/7 Aktif Gün",
                                        "${summary.activeDays}/7 Active Days"
                                    ),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (summary.currentStreakDays > 0) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color(0xFFFB923C).copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = str(
                                            "🔥 ${summary.currentStreakDays} gün seri",
                                            "🔥 ${summary.currentStreakDays}d streak"
                                        ),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color(0xFFFB923C),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        WeeklyBarsRow(
                            bars = summary.dailyBars,
                            targetCalories = summary.targetCalories,
                            isDark = isDark
                        )
                    }
                }

                // 2x2 Bento Metric Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeeklyMetricTile(
                            icon = Icons.Rounded.LocalFireDepartment,
                            iconTint = Color(0xFFFB923C),
                            label = str("Ort. Kalori", "Avg Calories"),
                            value = if (summary.avgDailyCalories > 0) "${summary.avgDailyCalories} kcal" else "—",
                            subText = str("Hedef: ${summary.targetCalories} kcal", "Goal: ${summary.targetCalories} kcal"),
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        WeeklyMetricTile(
                            icon = Icons.Rounded.Restaurant,
                            iconTint = HealthColors.NutrientProtein,
                            label = str("Ort. Protein", "Avg Protein"),
                            value = if (summary.avgDailyProteinG > 0) "${summary.avgDailyProteinG} g" else "—",
                            subText = str("Hedef: ${summary.targetProteinG} g", "Goal: ${summary.targetProteinG} g"),
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeeklyMetricTile(
                            icon = Icons.Rounded.FitnessCenter,
                            iconTint = Color(0xFF10B981),
                            label = str("Antrenman", "Workouts"),
                            value = str("${summary.workoutSessions} seans", "${summary.workoutSessions} sessions"),
                            subText = if (summary.totalBurnedCalories > 0) {
                                "${summary.totalBurnedCalories} kcal • ${summary.totalWorkoutMinutes} dk"
                            } else {
                                str("Hareket kaydı yok", "No workouts logged")
                            },
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )

                        val weightDelta = summary.weightDeltaKg
                        val weightSub = when {
                            weightDelta != null -> {
                                val sign = if (weightDelta > 0) "+" else "-"
                                "${sign}${abs(weightDelta)} kg ${str("değişim", "shift")}"
                            }
                            summary.weightEndKg != null -> "${summary.weightEndKg} kg"
                            else -> str("Hedef: ${summary.waterTargetMl} ml", "Goal: ${summary.waterTargetMl} ml")
                        }
                        WeeklyMetricTile(
                            icon = if (summary.weightDeltaKg != null) Icons.Rounded.MonitorWeight else Icons.Rounded.WaterDrop,
                            iconTint = if (summary.weightDeltaKg != null) Color(0xFFC084FC) else Color(0xFF38BDF8),
                            label = if (summary.avgDailyWaterMl > 0) str("Su & Kilo", "Water & Weight") else str("Kilo Trendi", "Weight Trend"),
                            value = if (summary.avgDailyWaterMl > 0) {
                                "${summary.avgDailyWaterMl} ml"
                            } else {
                                summary.weightEndKg?.let { "$it kg" } ?: "—"
                            },
                            subText = weightSub,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Highlight & Next-Week Focus Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = scoreAccent.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, scoreAccent.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = scoreAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = str("Haftanın Öne Çıkanı", "Weekly Highlight"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = scoreAccent
                            )
                        }
                        Text(
                            text = summary.localizedHighlight(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = HealthColors.PrimaryGlow,
                                modifier = Modifier.size(15.dp).padding(top = 1.dp)
                            )
                            Text(
                                text = summary.localizedNextWeekTip(lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onOpenAnalyticsClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HealthColors.Primary,
                            contentColor = HealthColors.OnPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = str("Detaylı Analizi Gör", "View Full Analytics"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().bounceClick()
                    ) {
                        Text(
                            text = str("Harika, Yeni Haftaya Başla", "Awesome, Start the New Week"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyBarsRow(
    bars: List<WeeklyDaySummaryBar>,
    targetCalories: Int,
    isDark: Boolean
) {
    val lang = appLang()
    val maxScale = (bars.maxOfOrNull { it.intakeKcal } ?: targetCalories)
        .coerceAtLeast(targetCalories)
        .coerceAtLeast(1200)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            val ratio = if (bar.intakeKcal > 0) {
                (bar.intakeKcal.toFloat() / maxScale.toFloat()).coerceIn(0.16f, 1f)
            } else if (bar.hasActivity) {
                0.22f
            } else {
                0.08f
            }
            val barColor = when {
                !bar.hasActivity -> if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
                bar.intakeKcal > (targetCalories * 1.12) -> Color(0xFFF59E0B)
                bar.intakeKcal > 0 -> HealthColors.Primary
                else -> Color(0xFF10B981)
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(ratio)
                            .clip(RoundedCornerShape(8.dp))
                            .background(barColor)
                    )
                }
                Text(
                    text = bar.localizedLabel(lang),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = if (bar.hasActivity) FontWeight.Bold else FontWeight.Medium,
                    color = if (bar.hasActivity) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun WeeklyMetricTile(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    subText: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF131B2E) else HealthColors.SurfaceVariantLight.copy(alpha = 0.50f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.20f else 0.32f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
