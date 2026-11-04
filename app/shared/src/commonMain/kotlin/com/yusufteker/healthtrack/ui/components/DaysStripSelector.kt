package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme

data class DayItem(
    val dayName: String,
    val dayNumber: String,
    val dateString: String
)

/**
 * Stitch-inspired Horizontal Days Strip with Week Badges & Active Luminous Indicator.
 */
@Composable
fun DaysStripSelector(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onPrevWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val daysList = rememberDaysAround(selectedDate)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Month Header & Navigation Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatMonthHeader(selectedDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(HealthColors.Primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "CANLI",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = HealthColors.Primary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        val prev = shiftDay(selectedDate, -1)
                        onDateSelected(prev)
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight)
                        .bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Önceki",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = {
                        val next = shiftDay(selectedDate, 1)
                        onDateSelected(next)
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight)
                        .bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Sonraki",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Horizontal Days Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            daysList.forEach { day ->
                val isSelected = day.dateString == selectedDate
                val bg = if (isSelected) {
                    HealthColors.Primary
                } else if (isDark) {
                    HealthColors.SurfaceContainerLow.copy(alpha = 0.8f)
                } else {
                    HealthColors.SurfaceLight
                }

                val contentColor = if (isSelected) {
                    HealthColors.OnPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Column(
                    modifier = Modifier
                        .width(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(bg)
                        .clickable { onDateSelected(day.dateString) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = day.dayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = day.dayNumber,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isSelected) HealthColors.OnPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(HealthColors.OnPrimary)
                        )
                    }
                }
            }
        }
    }
}

private fun rememberDaysAround(centerDate: String): List<DayItem> {
    val dayNames = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    val list = mutableListOf<DayItem>()
    for (offset in -3..3) {
        val dStr = shiftDay(centerDate, offset)
        val dayNum = dStr.takeLast(2)
        val nameIndex = (offset + 4).mod(7)
        list.add(DayItem(dayNames[nameIndex], dayNum, dStr))
    }
    return list
}

private fun shiftDay(dateStr: String, days: Int): String {
    return try {
        val parts = dateStr.split("-").map { it.toInt() }
        val day = (parts[2] + days).coerceIn(1, 31)
        val dayFormatted = if (day < 10) "0$day" else "$day"
        val monthFormatted = if (parts[1] < 10) "0${parts[1]}" else "${parts[1]}"
        "${parts[0]}-$monthFormatted-$dayFormatted"
    } catch (_: Exception) {
        dateStr
    }
}

private fun formatMonthHeader(dateStr: String): String {
    val months = listOf("", "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
    return try {
        val parts = dateStr.split("-").map { it.toInt() }
        val monthName = months.getOrElse(parts[1]) { "Ay" }
        "$monthName ${parts[0]}"
    } catch (_: Exception) {
        dateStr
    }
}

