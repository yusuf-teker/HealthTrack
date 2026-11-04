package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

/**
 * Stitch Luxury Telemetry Vertical Pill Gauges.
 * Displays high-contrast bio-telemetry capsules for all macro & micro nutrients.
 */
@Composable
fun NutrientBarsView(
    bars: List<NutrientProgress>,
    onEditGoalsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header with Edit Goals Pill
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Günlük Besin Hedefleri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Metabolik referans değerlerine göre",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onEditGoalsClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight,
                    contentColor = HealthColors.Primary
                ),
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.bounceClick()
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Düzenle", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        // Horizontally Scrollable Luxury Capsule Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            bars.forEach { nutrient ->
                NutrientCapsulePill(nutrient = nutrient)
            }
        }
    }
}

@Composable
private fun NutrientCapsulePill(
    nutrient: NutrientProgress,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val progress = (nutrient.percentage / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 700)
    )

    val (groupColor, icon) = getNutrientGroupVisuals(nutrient.label)

    GlassCard(
        modifier = modifier.width(78.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Percentage Readout
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${nutrient.percentage.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Capsule Outer Well (Height = 130.dp, Width = 38.dp)
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(130.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) HealthColors.SurfaceContainerLowest else HealthColors.SurfaceVariantLight)
                    .padding(3.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Filled Column
                if (animatedProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedProgress)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        groupColor.copy(alpha = 0.95f),
                                        groupColor.copy(alpha = 0.65f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Glossy Specular Highlight Dot on top of liquid
                        Box(
                            modifier = Modifier
                                .padding(top = 3.dp)
                                .width(12.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.7f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Icon Pod
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = groupColor,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Label
            Text(
                text = nutrient.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            // Ratio
            val formattedValue = ((nutrient.current * 10).toInt() / 10.0)
            Text(
                text = "$formattedValue/${nutrient.target.toInt()}${nutrient.unit}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getNutrientGroupVisuals(name: String): Pair<Color, ImageVector> {
    return when (name.lowercase()) {
        "kalori" -> HealthColors.Primary to Icons.Default.Bolt
        "protein" -> HealthColors.NutrientProtein to Icons.Default.FitnessCenter
        "karbonhidrat", "karb" -> HealthColors.NutrientCarbs to Icons.Default.Grain
        "yağ" -> HealthColors.NutrientLipids to Icons.Default.WaterDrop
        "lif" -> HealthColors.NutrientFiber to Icons.Default.Spa
        "c vitamini", "vit c" -> HealthColors.NutrientVitC to Icons.Default.AutoAwesome
        "d vitamini", "vit d" -> HealthColors.NutrientVitD to Icons.Default.WbSunny
        "b12 vitamini", "vit b12" -> HealthColors.NutrientVitB12 to Icons.Default.Bolt
        "demir" -> HealthColors.NutrientIron to Icons.Default.Shield
        "kalsiyum" -> HealthColors.NutrientCalcium to Icons.Default.Favorite
        "magnezyum" -> HealthColors.NutrientMagnesium to Icons.Default.Eco
        else -> HealthColors.Primary to Icons.Default.Check
    }
}
