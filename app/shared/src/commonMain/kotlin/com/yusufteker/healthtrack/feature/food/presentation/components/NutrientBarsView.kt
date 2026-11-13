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
import androidx.compose.runtime.remember
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
import com.yusufteker.healthtrack.theme.str
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
                    text = str("Mikro Besinler & Hedefler", "Micronutrients & Goals"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = str("Metabolik referans değerlerine göre", "Based on metabolic reference values"),
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
                Text(str("Düzenle", "Edit"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        val displayBars = remember(bars) {
            bars.filter { it.key !in setOf("calories", "protein", "carbs", "fat") }
        }

        // Horizontally Scrollable Luxury Compact Capsule Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            displayBars.forEach { nutrient ->
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
        modifier = modifier.width(66.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Percentage Readout
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${nutrient.percentage.toInt()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Capsule Outer Well (Height = 54.dp, Width = 26.dp)
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) HealthColors.SurfaceContainerLowest else HealthColors.SurfaceVariantLight)
                    .padding(2.dp),
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
                                .padding(top = 2.dp)
                                .width(8.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.7f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Icon Pod
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = groupColor,
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Label
            Text(
                text = localizedNutrientLabel(nutrient),
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
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun localizedNutrientLabel(nutrient: NutrientProgress): String {
    return when (nutrient.key) {
        "calories" -> str("Kalori", "Calories")
        "protein" -> "Protein"
        "carbs" -> str("Karbonhidrat", "Carbs")
        "fat" -> str("Yağ", "Fat")
        "fiber" -> str("Lif", "Fiber")
        "vit_c" -> "Vit C"
        "vit_d" -> "Vit D"
        "vit_b12" -> "Vit B12"
        "iron" -> str("Demir", "Iron")
        "calcium" -> str("Kalsiyum", "Calcium")
        "magnesium" -> str("Magnezyum", "Magnesium")
        else -> nutrient.label
    }
}

internal fun getNutrientGroupVisuals(name: String): Pair<Color, ImageVector> {
    return when (name.lowercase()) {
        "kalori", "calories" -> HealthColors.Primary to Icons.Default.Bolt
        "protein" -> HealthColors.NutrientProtein to Icons.Default.FitnessCenter
        "karbonhidrat", "karb", "carbs", "carbohydrates" -> HealthColors.NutrientCarbs to Icons.Default.Grain
        "yağ", "fat" -> HealthColors.NutrientLipids to Icons.Default.WaterDrop
        "lif", "fiber" -> HealthColors.NutrientFiber to Icons.Default.Spa
        "c vitamini", "vit c", "vitamin c" -> HealthColors.NutrientVitC to Icons.Default.AutoAwesome
        "d vitamini", "vit d", "vitamin d" -> HealthColors.NutrientVitD to Icons.Default.WbSunny
        "b12 vitamini", "vit b12", "vitamin b12" -> HealthColors.NutrientVitB12 to Icons.Default.Bolt
        "demir", "iron" -> HealthColors.NutrientIron to Icons.Default.Shield
        "kalsiyum", "calcium" -> HealthColors.NutrientCalcium to Icons.Default.Favorite
        "magnezyum", "magnesium" -> HealthColors.NutrientMagnesium to Icons.Default.Eco
        else -> HealthColors.Primary to Icons.Default.Check
    }
}
