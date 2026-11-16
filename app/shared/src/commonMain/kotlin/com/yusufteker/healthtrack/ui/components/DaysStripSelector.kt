package com.yusufteker.healthtrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.utils.WeekDayItem
import com.yusufteker.healthtrack.core.utils.formatDisplayDateTr
import com.yusufteker.healthtrack.core.utils.formatMonthYearHeader
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.core.utils.getWeekDaysForDate
import com.yusufteker.healthtrack.core.utils.shiftDateIso
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str

typealias DayItem = WeekDayItem

/**
 * Stitch Luxury Compact 7-Day Strip Selector.
 * Spans full width without vertical bloat, fitting seamlessly above the fold.
 */
@Composable
fun DaysStripSelector(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onPrevWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    markedDates: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val lang = appLang()
    val todayIso = remember { getTodayIso() }
    val isSelectedToday = selectedDate == todayIso
    val weekDays = remember(selectedDate, lang) { getWeekDaysForDate(selectedDate, lang) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onHorizontalSwipe(
                onSwipeLeft = { onDateSelected(shiftDateIso(selectedDate, 1)) },
                onSwipeRight = { onDateSelected(shiftDateIso(selectedDate, -1)) }
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Month Header & Week Navigation Controls (Height ~28dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatMonthYearHeader(selectedDate, lang),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Today Pill Indicator / Quick Return Button
                val pillBgColor = if (isSelectedToday) {
                    HealthColors.Primary.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.tertiary.copy(alpha = if (isDark) 0.22f else 0.15f)
                }
                val pillTextColor = if (isSelectedToday) {
                    HealthColors.Primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(pillBgColor)
                        .clickable(enabled = !isSelectedToday) {
                            onDateSelected(todayIso)
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isSelectedToday) str("BUGÜN", "TODAY") else str("Bugüne Git", "Go to Today"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = pillTextColor
                    )
                }
            }

            // Compact Chevrons (Shift day-by-day)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        val prev = shiftDateIso(selectedDate, -1)
                        onDateSelected(prev)
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight)
                        .bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = str("Önceki Gün", "Previous Day"),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = {
                        val next = shiftDateIso(selectedDate, 1)
                        onDateSelected(next)
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight)
                        .bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = str("Sonraki Gün", "Next Day"),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Compact 7-Day Row across screen (Height ~48dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekDays.forEach { day ->
                val isSelected = day.dateString == selectedDate
                val bg = if (isSelected) {
                    HealthColors.Primary
                } else if (isDark) {
                    HealthColors.SurfaceContainerLow.copy(alpha = 0.7f)
                } else {
                    HealthColors.SurfaceVariantLight.copy(alpha = 0.8f)
                }

                val contentColor = if (isSelected) {
                    HealthColors.OnPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .clickable { onDateSelected(day.dateString) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = day.dayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                    Text(
                        text = day.dayNumber,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isSelected) HealthColors.OnPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    val hasMark = markedDates.contains(day.dateString)
                    if (isSelected || hasMark) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 3.dp else 4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }
            }
        }

        // Non-today date awareness banner (prevents accidentally logging to the wrong day)
        AnimatedVisibility(
            visible = !isSelectedToday,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val formattedDate = remember(selectedDate, lang) { formatDisplayDateTr(selectedDate, lang) }
            val accentColor = MaterialTheme.colorScheme.tertiary
            Surface(
                color = accentColor.copy(alpha = if (isDark) 0.14f else 0.10f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.38f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDateSelected(todayIso) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = str(
                            "📅 $formattedDate seçili",
                            "📅 $formattedDate selected"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = str("Bugüne Dön →", "Back to Today →"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = HealthColors.Primary
                    )
                }
            }
        }
    }
}
