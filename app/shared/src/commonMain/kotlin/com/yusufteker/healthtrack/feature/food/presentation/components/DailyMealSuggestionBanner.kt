package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.GradientText
import com.yusufteker.healthtrack.ui.components.bounceClick

/**
 * Luxury Bio-Telemetry Daily Meal Suggestion Banner.
 * Appears once per day to recommend a balanced 4-meal plan based on recent nutrient deficiencies.
 * Can be dismissed with the close button or clicked to open the full plan.
 */
@Composable
fun DailyMealSuggestionBanner(
    onOpenPlan: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onOpenPlan() }
            .bounceClick(),
        shape = RoundedCornerShape(22.dp),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: AI Badge + Close (X) Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(HealthColors.Primary, Color(0xFF00E676))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = HealthColors.Primary.copy(alpha = 0.14f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HealthColors.Primary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = str("GÜNÜN ÖĞÜN ÖNERİSİ", "DAILY MEAL SUGGESTION"),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Black,
                            color = HealthColors.Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Dismiss Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = str("Bugünlük Kapat", "Dismiss for Today"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Headline & Description
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                GradientText(
                    text = str("Günün 4 Öğünlük AI Menüsü 🥗", "Today's 4-Meal AI Menu 🥗"),
                    colors = listOf(HealthColors.Primary, Color(0xFF00E676)),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = str(
                        "Son günlerdeki beslenmenize göre eksik kalan protein, lif ve vitaminlerinizi kapatacak, markette kolayca bulunan malzemelerle pratik tarifler hazırlandı.",
                        "Practical recipes with easy-to-find groceries tailored to fill your recent protein, fiber, and vitamin gaps."
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Info Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SuggestionFeatureChip(str("🎯 Eksik Odaklı", "🎯 Gap Focused"), Color(0xFF00B4D8))
                SuggestionFeatureChip(str("⏱️ 5-15 Dk Pratik", "⏱️ 5-15 Min Quick"), Color(0xFFFFB703))
                SuggestionFeatureChip(str("🛒 Kolay Malzemeli", "🛒 Easy Groceries"), Color(0xFF00E676))
            }

            // Bottom CTA Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = str("Kahvaltı • Öğle • Akşam • Ara Öğün", "Breakfast • Lunch • Dinner • Snack"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HealthColors.Primary,
                    modifier = Modifier.bounceClick()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = str("Menüyü İncele", "View Menu"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionFeatureChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

