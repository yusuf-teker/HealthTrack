package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.NutrientProgress
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralMicronutrientsBottomSheet(
    nutrientBars: List<NutrientProgress>,
    onDismiss: () -> Unit,
    onEditGoalsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val isDark = LocalIsDarkTheme.current
    val scrollState = rememberScrollState()

    val microNutrients = remember(nutrientBars) {
        nutrientBars.filter { it.key !in setOf("calories", "protein", "carbs", "fat") }
    }

    val achievedCount = remember(microNutrients) {
        microNutrients.count { it.percentage >= 100f }
    }

    val averagePercentage = remember(microNutrients) {
        if (microNutrients.isNotEmpty()) {
            (microNutrients.map { it.percentage }.average()).roundToInt()
        } else 0
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .clearFocusOnTap()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(HealthColors.Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Biotech,
                            contentDescription = null,
                            tint = HealthColors.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Genel Mikrolar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Günlük vitamin, mineral & lif dengesi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        onDismiss()
                        onEditGoalsClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HealthColors.Primary.copy(alpha = 0.15f),
                        contentColor = HealthColors.Primary
                    ),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.bounceClick()
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hedefler", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickStatCard(
                    title = "Takip Edilen",
                    value = "${microNutrients.size}",
                    unit = "Öğe",
                    icon = Icons.Default.CheckCircle,
                    color = HealthColors.Primary,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                QuickStatCard(
                    title = "Tamamlanan",
                    value = "$achievedCount/${microNutrients.size}",
                    unit = "Hedef",
                    icon = Icons.Default.DoneAll,
                    color = Color(0xFF4CAF50),
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                QuickStatCard(
                    title = "Ort. Karşılama",
                    value = "%$averagePercentage",
                    unit = "Denge",
                    icon = Icons.Default.Bolt,
                    color = HealthColors.Tertiary,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }

            // Horizontal Capsules Preview Strip
            if (microNutrients.isNotEmpty()) {
                Text(
                    text = "Hızlı Bakış",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    microNutrients.forEach { item ->
                        DetailedCapsulePill(item = item, isDark = isDark)
                    }
                }
            }

            // Detailed List by Category
            Text(
                text = "Detaylı Mikro Besin Dökümü",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                microNutrients.forEach { item ->
                    NutrientDetailRowCard(item = item, isDark = isDark)
                }
            }

            // Close CTA Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .bounceClick(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) HealthColors.SurfaceContainerHighest else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Kapat", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickStatCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailedCapsulePill(
    item: NutrientProgress,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = (item.percentage / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600)
    )
    val (groupColor, icon) = getNutrientGroupVisuals(item.label)

    GlassCard(
        modifier = modifier.width(68.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "%${item.percentage.toInt()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Capsule Outer Well
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) HealthColors.SurfaceContainerLowest else HealthColors.SurfaceVariantLight)
                    .padding(2.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (animatedProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedProgress)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(groupColor, groupColor.copy(alpha = 0.7f))
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(groupColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = groupColor,
                    modifier = Modifier.size(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NutrientDetailRowCard(
    item: NutrientProgress,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = (item.percentage / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600)
    )
    val (groupColor, icon) = getNutrientGroupVisuals(item.label)
    val formattedCurrent = (item.current * 10).roundToInt() / 10.0
    val targetVal = item.target.toInt()
    val isMet = item.percentage >= 100f

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                            .background(groupColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = groupColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$formattedCurrent / $targetVal ${item.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Percentage Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isMet) Color(0xFF4CAF50).copy(alpha = 0.15f)
                            else groupColor.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isMet) "Tamamlandı (%${item.percentage.toInt()})" else "%${item.percentage.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isMet) Color(0xFF4CAF50) else groupColor
                    )
                }
            }

            // Linear Progress Bar Well
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) HealthColors.SurfaceContainerLowest else HealthColors.SurfaceVariantLight)
            ) {
                if (animatedProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(groupColor.copy(alpha = 0.8f), groupColor)
                                )
                            )
                    )
                }
            }
        }
    }
}

