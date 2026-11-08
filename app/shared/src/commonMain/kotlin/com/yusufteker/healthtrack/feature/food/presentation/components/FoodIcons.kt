package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoNotDisturbAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.ui.components.bounceClick
import healthtrack.app.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

data class FoodIconItem(
    val key: String,
    val titleTr: String,
    val resource: DrawableResource
)

object FoodIcons {
    val ALL: List<FoodIconItem> = listOf(
        FoodIconItem("capsule", "Kapsül / Tablet", Res.drawable.ic_supplement_capsule),
        FoodIconItem("bottle", "Şişe / Shaker", Res.drawable.ic_supplement_bottle),
        FoodIconItem("protein", "Protein Tozu", Res.drawable.ic_supplement_protein),
        FoodIconItem("creatine", "Kreatin", Res.drawable.ic_supplement_creatine),
        FoodIconItem("bcaa", "BCAA", Res.drawable.ic_supplement_bcaa),
        FoodIconItem("carnitine", "L-Karnitin", Res.drawable.ic_supplement_carnitine),
        FoodIconItem("multivitamin", "Multivitamin", Res.drawable.ic_supplement_multivitamin),
        FoodIconItem("omega3", "Omega-3", Res.drawable.ic_supplement_omega3),
        FoodIconItem("vit_d", "D Vitamini", Res.drawable.ic_supplement_vit_d),
        FoodIconItem("vit_b", "B Vitamini", Res.drawable.ic_supplement_vit_b),
        FoodIconItem("vit_a", "A Vitamini", Res.drawable.ic_supplement_vit_a),
        FoodIconItem("coffee", "Kahve / Kafein", Res.drawable.ic_supplement_coffee)
    )

    fun getDrawable(key: String?): DrawableResource? {
        if (key == null) return null
        return ALL.firstOrNull { it.key == key }?.resource
    }

    fun getTitle(key: String?): String? {
        if (key == null) return null
        return ALL.firstOrNull { it.key == key }?.titleTr
    }
}

/**
 * Renders a stylish circular or rounded container badge with the supplement/food icon.
 */
@Composable
fun FoodIconBadge(
    iconKey: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    tint: Color = HealthColors.Primary,
    backgroundColor: Color = tint.copy(alpha = 0.12f)
) {
    val drawableRes = FoodIcons.getDrawable(iconKey) ?: return

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape((size.value * 0.3f).dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(drawableRes),
            contentDescription = FoodIcons.getTitle(iconKey),
            tint = tint,
            modifier = Modifier.size((size.value * 0.62f).dp)
        )
    }
}

/**
 * Interactive selector chip row for choosing a product icon when creating a custom food.
 */
@Composable
fun FoodIconSelector(
    selectedKey: String?,
    onSelectKey: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Option 0: No icon / Default
            val isNoneSelected = selectedKey == null
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isNoneSelected) {
                    HealthColors.Primary.copy(alpha = 0.18f)
                } else {
                    if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (isNoneSelected) {
                            Modifier.border(1.5.dp, HealthColors.Primary, RoundedCornerShape(14.dp))
                        } else Modifier
                    )
                    .clickable { onSelectKey(null) }
                    .bounceClick()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DoNotDisturbAlt,
                        contentDescription = null,
                        tint = if (isNoneSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "İkonsuz",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isNoneSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 12 Supplement Icons
            FoodIcons.ALL.forEach { item ->
                val isSelected = selectedKey == item.key
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) {
                        HealthColors.Primary.copy(alpha = 0.2f)
                    } else {
                        if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (isSelected) {
                                Modifier.border(1.5.dp, HealthColors.Primary, RoundedCornerShape(14.dp))
                            } else Modifier
                        )
                        .clickable { onSelectKey(if (isSelected) null else item.key) }
                        .bounceClick()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) HealthColors.Primary.copy(alpha = 0.25f)
                                    else if (isDark) Color.White.copy(alpha = 0.06f)
                                    else Color.Black.copy(alpha = 0.04f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(item.resource),
                                contentDescription = item.titleTr,
                                tint = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = item.titleTr,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurface
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(HealthColors.Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = HealthColors.OnPrimary,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active selection display
        val activeTitle = FoodIcons.getTitle(selectedKey)
        if (activeTitle != null) {
            Text(
                text = "Seçilen İkon: $activeTitle",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = HealthColors.Primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}
