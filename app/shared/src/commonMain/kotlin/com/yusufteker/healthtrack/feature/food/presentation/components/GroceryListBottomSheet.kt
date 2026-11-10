package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryListBottomSheet(
    title: String,
    items: List<String>,
    selectedItems: Set<String>,
    onToggleItem: (String) -> Unit,
    onSendToPlanora: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
            .clearFocusOnTap()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Market Listesi 🛒",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = title,
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

            // Info Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF6366F1).copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Alınacak eksik malzemeleri seçin. Tek tıkla Planora uygulamanızın To-Do listesine aktarılacaktır.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Items Checklist
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it }) { item ->
                    val isChecked = selectedItems.contains(item)
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onToggleItem(item) }
                            .bounceClick(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onToggleItem(item) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF6366F1),
                                    checkmarkColor = Color.White
                                )
                            )
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Action Button: Send to Planora
            Button(
                onClick = onSendToPlanora,
                enabled = selectedItems.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF6366F1).copy(alpha = 0.4f),
                    disabledContentColor = Color.White
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${selectedItems.size} Malzemeyi Planora'ya Gönder (To-Do)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

