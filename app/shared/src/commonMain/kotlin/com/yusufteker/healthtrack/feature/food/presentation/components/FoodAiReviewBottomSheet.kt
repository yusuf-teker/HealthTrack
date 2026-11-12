package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.FoodItem
import com.yusufteker.healthtrack.core.network.FoodAiResultDto
import com.yusufteker.healthtrack.feature.food.presentation.FoodScanTarget
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodAiReviewBottomSheet(
    aiResult: FoodAiResultDto,
    target: FoodScanTarget = FoodScanTarget.DAILY_MEAL,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onSaveToInventory: (() -> Unit)? = null,
    onSaveSingleItemToInventory: ((FoodItem) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = LocalIsDarkTheme.current
    val scrollState = rememberScrollState()

    var isConfirming by remember { mutableStateOf(false) }
    var isSavingInventory by remember { mutableStateOf(false) }

    val totalProtein = aiResult.items.sumOf { it.proteinG }
    val totalCarbs = aiResult.items.sumOf { it.carbsG }
    val totalFat = aiResult.items.sumOf { it.fatG }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        containerColor = if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sheet Header
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(HealthColors.Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (target == FoodScanTarget.INVENTORY) Icons.Default.Inventory2 else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (target == FoodScanTarget.INVENTORY) "AI Envanter & Ürün Analizi 🏷️" else "AI Besin Analizi ✨",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (target == FoodScanTarget.INVENTORY) "Tespit edilen ürünleri inceleyip envanterinize kaydedin" else "Tespit edilen besinleri inceleyip onaylayın",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // AI Summary Banner
                if (aiResult.summary.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HealthColors.Primary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HealthColors.Primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Text(
                                text = aiResult.summary,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Total Calories & Macros Overview Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Toplam Kalori",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${aiResult.totalCalories} kcal",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MacroBadge(label = "P", value = "${totalProtein.toInt()}g", color = Color(0xFF38BDF8))
                                MacroBadge(label = "K", value = "${totalCarbs.toInt()}g", color = Color(0xFFFBBF24))
                                MacroBadge(label = "Y", value = "${totalFat.toInt()}g", color = Color(0xFFF87171))
                            }
                        }
                    }
                }

                // Items Section Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ayrıştırılan Öğeler (${aiResult.items.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Food Items List
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    aiResult.items.forEach { item ->
                        ReviewFoodItemCard(
                            item = item,
                            onSaveToInventory = onSaveSingleItemToInventory?.let { action ->
                                { action(item) }
                            }
                        )
                    }
                }

                // Disclaimer
                if (aiResult.disclaimer.isNotBlank()) {
                    Text(
                        text = "ℹ️ ${aiResult.disclaimer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            // Fixed Sticky Footer for Action Buttons (always visible & easily clickable)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (target == FoodScanTarget.INVENTORY) {
                        // Primary: Envanterime Kaydet
                        Button(
                            onClick = {
                                if (!isSavingInventory && !isConfirming) {
                                    isSavingInventory = true
                                    if (onSaveToInventory != null) onSaveToInventory() else onConfirm()
                                }
                            },
                            enabled = !isSavingInventory && !isConfirming,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HealthColors.Primary)
                        ) {
                            if (isSavingInventory) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Kaydediliyor...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (aiResult.items.size > 1) "Tümünü Envanterime Kaydet (${aiResult.items.size})" else "Ürünü Envanterime Kaydet",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Secondary: Günlüğe de Ekle & Vazgeç
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                enabled = !isSavingInventory && !isConfirming,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .bounceClick(),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text("Vazgeç", color = MaterialTheme.colorScheme.onSurface)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!isConfirming && !isSavingInventory) {
                                        isConfirming = true
                                        onConfirm()
                                    }
                                },
                                enabled = !isConfirming && !isSavingInventory,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(46.dp)
                                    .bounceClick(),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, HealthColors.Primary.copy(alpha = 0.5f))
                            ) {
                                if (isConfirming) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = HealthColors.Primary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ekleniyor...", color = HealthColors.Primary, fontWeight = FontWeight.SemiBold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Restaurant,
                                        contentDescription = null,
                                        tint = HealthColors.Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Günlüğe de Ekle", color = HealthColors.Primary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    } else {
                        // Primary: Onayla ve Günlüğe Ekle
                        Button(
                            onClick = {
                                if (!isConfirming && !isSavingInventory) {
                                    isConfirming = true
                                    onConfirm()
                                }
                            },
                            enabled = !isConfirming && !isSavingInventory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HealthColors.Primary)
                        ) {
                            if (isConfirming) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Günlüğe Ekleniyor...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Onayla ve Günlüğe Ekle",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Secondary: Vazgeç & Envantere de Ekle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                enabled = !isConfirming && !isSavingInventory,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .bounceClick(),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text("Vazgeç", color = MaterialTheme.colorScheme.onSurface)
                            }

                            if (onSaveToInventory != null) {
                                OutlinedButton(
                                    onClick = {
                                        if (!isSavingInventory && !isConfirming) {
                                            isSavingInventory = true
                                            onSaveToInventory()
                                        }
                                    },
                                    enabled = !isSavingInventory && !isConfirming,
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .height(46.dp)
                                        .bounceClick(),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, HealthColors.Primary.copy(alpha = 0.5f))
                                ) {
                                    if (isSavingInventory) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = HealthColors.Primary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Kaydediliyor...", color = HealthColors.Primary, fontWeight = FontWeight.SemiBold)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.BookmarkAdd,
                                            contentDescription = null,
                                            tint = HealthColors.Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Envantere de Ekle", color = HealthColors.Primary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroBadge(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ReviewFoodItemCard(
    item: FoodItem,
    onSaveToInventory: (() -> Unit)? = null
) {
    var isSaved by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.formattedPortion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${item.calories} kcal",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = HealthColors.Primary
                )
            }

            // Macros row + Single item inventory save button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Protein: ${item.proteinG.toInt()}g",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Karb: ${item.carbsG.toInt()}g",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Yağ: ${item.fatG.toInt()}g",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onSaveToInventory != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSaved) HealthColors.Primary.copy(alpha = 0.25f) else HealthColors.Primary.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isSaved, onClick = {
                                isSaved = true
                                onSaveToInventory()
                            })
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Check else Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isSaved) "Eklendi ✓" else "Envantere Ekle",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HealthColors.Primary
                            )
                        }
                    }
                }
            }

            // Micronutrients if present
            if (item.micronutrients.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.micronutrients.take(3).forEach { micro ->
                        Text(
                            text = "${micro.name}: ${micro.amount}${micro.unit}",
                            fontSize = 10.sp,
                            color = HealthColors.Secondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(HealthColors.Secondary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
