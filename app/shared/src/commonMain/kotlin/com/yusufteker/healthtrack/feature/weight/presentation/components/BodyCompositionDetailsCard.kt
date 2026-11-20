package com.yusufteker.healthtrack.feature.weight.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.utils.FullBodyCompositionReport
import com.yusufteker.healthtrack.core.utils.MetricRangeStatus
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick
import kotlin.math.abs

/**
 * Stitch Luxury Body Composition Analytics Card (Zepp Life / Xiaomi BIA compatible).
 * Displays Body Score, Body Type, headline KPIs (BMI, Body Age, Water %, Visceral Fat)
 * and expands to reveal all 11+ body composition metrics with range badges.
 */
@Composable
fun BodyCompositionDetailsCard(
    report: FullBodyCompositionReport,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    val isDark = LocalIsDarkTheme.current
    val lang = appLang()
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    val scoreColor = when {
        report.bodyScore >= 80 -> Color(0xFF10B981)
        report.bodyScore >= 65 -> HealthColors.Primary
        report.bodyScore >= 50 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header: Score Ring + Body Type + Expand Toggle ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Body Score Badge
                    Surface(
                        shape = CircleShape,
                        color = scoreColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.5.dp, scoreColor.copy(alpha = 0.65f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${report.bodyScore}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = scoreColor,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = str("puan", "pts"),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = scoreColor.copy(alpha = 0.85f),
                                lineHeight = 9.sp
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonitorHeart,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = str("Vücut Analizi", "Body Analysis"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = HealthColors.Primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${str("Vücut Tipi", "Body Type")}: ${report.bodyType.label(lang)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = HealthColors.Primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.bounceClick()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isExpanded) str("Gizle", "Hide") else str("Detaylar", "Details"),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ── Compact 4-KPI Headline Strip (Always Visible) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeadlineKpiChip(
                    label = str("VKİ", "BMI"),
                    value = "${report.bmi}",
                    statusColor = statusColor(report.bmiStatus),
                    modifier = Modifier.weight(1f)
                )
                HeadlineKpiChip(
                    label = str("Vücut Yaşı", "Body Age"),
                    value = "${report.bodyAge}",
                    statusColor = if (report.bodyAge <= report.age) Color(0xFF10B981) else Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                HeadlineKpiChip(
                    label = str("Su", "Water"),
                    value = report.waterPercent?.let { "%$it" } ?: "—",
                    statusColor = report.waterStatus?.let(::statusColor) ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                HeadlineKpiChip(
                    label = str("İç Yağ", "Visceral"),
                    value = "${report.visceralFat}",
                    statusColor = statusColor(report.visceralFatStatus),
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Expandable Full 11-Metric Telemetry Grid ──
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    val items = buildList {
                        add(
                            MetricTileData(
                                title = str("VKİ (BMI)", "BMI"),
                                value = "${report.bmi}",
                                subtitle = str("İdeal: 18.5 – 24.9", "Ideal: 18.5 – 24.9"),
                                status = report.bmiStatus
                            )
                        )
                        add(
                            MetricTileData(
                                title = str("Vücut Yaşı", "Body Age"),
                                value = str("${report.bodyAge} yaş", "${report.bodyAge} yrs"),
                                subtitle = str("Gerçek yaş: ${report.age}", "Actual age: ${report.age}"),
                                status = if (report.bodyAge <= report.age) MetricRangeStatus.NORMAL else MetricRangeStatus.HIGH
                            )
                        )
                        report.bodyFatPercent?.let { fat ->
                            add(
                                MetricTileData(
                                    title = str("Vücut Yağı", "Body Fat"),
                                    value = "%$fat",
                                    subtitle = report.fatMassKg?.let { "$it kg" } ?: "",
                                    status = report.bodyFatStatus ?: MetricRangeStatus.NORMAL
                                )
                            )
                        }
                        report.muscleMassKg?.let { mKg ->
                            add(
                                MetricTileData(
                                    title = str("Kas Kütlesi", "Muscle Mass"),
                                    value = "$mKg kg",
                                    subtitle = report.musclePercent?.let { str("Oran: %$it", "Ratio: $it%") } ?: "",
                                    status = report.muscleStatus ?: MetricRangeStatus.NORMAL
                                )
                            )
                        }
                        report.waterPercent?.let { water ->
                            add(
                                MetricTileData(
                                    title = str("Vücut Suyu", "Body Water"),
                                    value = "%$water",
                                    subtitle = if (report.isMale) str("İdeal: %55–65", "Ideal: 55–65%") else str("İdeal: %45–60", "Ideal: 45–60%"),
                                    status = report.waterStatus ?: MetricRangeStatus.NORMAL
                                )
                            )
                        }
                        add(
                            MetricTileData(
                                title = str("İç Organ Yağı", "Visceral Fat"),
                                value = "${report.visceralFat}",
                                subtitle = str("İdeal: 1 – 9", "Ideal: 1 – 9"),
                                status = report.visceralFatStatus
                            )
                        )
                        add(
                            MetricTileData(
                                title = str("Bazal Metabolizma", "BMR"),
                                value = "${report.bmrKcal} kcal",
                                subtitle = str("Günlük dinlenme harcaması", "Daily resting burn"),
                                status = MetricRangeStatus.NORMAL
                            )
                        )
                        report.boneMassKg?.let { bone ->
                            add(
                                MetricTileData(
                                    title = str("Kemik Kütlesi", "Bone Mass"),
                                    value = "$bone kg",
                                    subtitle = str("İskelet mineral kütlesi", "Skeletal mineral mass"),
                                    status = report.boneStatus ?: MetricRangeStatus.NORMAL
                                )
                            )
                        }
                        report.proteinPercent?.let { protein ->
                            add(
                                MetricTileData(
                                    title = str("Protein Oranı", "Protein"),
                                    value = "%$protein",
                                    subtitle = str("İdeal: %16 – 20", "Ideal: 16 – 20%"),
                                    status = report.proteinStatus ?: MetricRangeStatus.NORMAL
                                )
                            )
                        }
                        report.leanBodyMassKg?.let { lbm ->
                            add(
                                MetricTileData(
                                    title = str("Yağsız Kütle (LBM)", "Lean Body Mass"),
                                    value = "$lbm kg",
                                    subtitle = str("Kas + kemik + su", "Muscle + bone + water"),
                                    status = MetricRangeStatus.NORMAL
                                )
                            )
                        }
                        val deltaSign = if (report.weightDeltaFromIdealKg > 0) "+" else ""
                        add(
                            MetricTileData(
                                title = str("İdeal Kilo", "Ideal Weight"),
                                value = "${report.idealWeightKg} kg",
                                subtitle = str(
                                    "Fark: $deltaSign${report.weightDeltaFromIdealKg} kg",
                                    "Diff: $deltaSign${report.weightDeltaFromIdealKg} kg"
                                ),
                                status = if (abs(report.weightDeltaFromIdealKg) <= 4.0) MetricRangeStatus.NORMAL else MetricRangeStatus.HIGH
                            )
                        )
                    }

                    items.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { item ->
                                MetricDetailTile(
                                    data = item,
                                    isDark = isDark,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Profile calibration note
                    val genderLabel = if (report.isMale) str("Erkek", "Male") else str("Kadın", "Female")
                    val ohmText = report.impedanceOhm?.let { " • $it Ω" }.orEmpty()
                    Text(
                        text = str(
                            "Hesaplama profili: ${report.heightCm.toInt()} cm • ${report.age} yaş • $genderLabel$ohmText (Doğru BIA için Profil bilgilerinin Zepp Life ile aynı olduğundan emin ol.)",
                            "Profile used: ${report.heightCm.toInt()} cm • ${report.age} yrs • $genderLabel$ohmText (Match Profile stats with Zepp Life for consistent BIA.)"
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

private data class MetricTileData(
    val title: String,
    val value: String,
    val subtitle: String,
    val status: MetricRangeStatus
)

@Composable
private fun HeadlineKpiChip(
    label: String,
    value: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) HealthColors.SurfaceContainerLowest else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = statusColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun MetricDetailTile(
    data: MetricTileData,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val lang = appLang()
    val tint = statusColor(data.status)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) HealthColors.SurfaceContainerLowest else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(tint.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = data.status.label(lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = tint,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Text(
                text = data.value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )

            if (data.subtitle.isNotBlank()) {
                Text(
                    text = data.subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun statusColor(status: MetricRangeStatus): Color = when (status) {
    MetricRangeStatus.LOW -> Color(0xFF38BDF8)
    MetricRangeStatus.NORMAL -> Color(0xFF10B981)
    MetricRangeStatus.HIGH -> Color(0xFFF59E0B)
    MetricRangeStatus.VERY_HIGH -> Color(0xFFEF4444)
}
