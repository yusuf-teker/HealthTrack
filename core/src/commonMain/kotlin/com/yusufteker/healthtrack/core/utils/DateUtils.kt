package com.yusufteker.healthtrack.core.utils

import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

private val TURKISH_MONTHS = listOf(
    "", "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
    "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
)

private val TURKISH_MONTHS_SHORT = listOf(
    "", "Oca", "Şub", "Mar", "Nis", "May", "Haz",
    "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"
)

data class WeekDayItem(
    val dayName: String,
    val dayNumber: String,
    val dateString: String
)

/**
 * Returns today's ISO date (YYYY-MM-DD) in current system default timezone.
 */
fun getTodayIso(): String {
    return try {
        Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
    } catch (_: Exception) {
        "2026-11-04"
    }
}

/**
 * Returns current timestamp in epoch milliseconds.
 */
fun currentTimeMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
}

/**
 * Shifts ISO date (YYYY-MM-DD) by [days] (can be positive or negative).
 */
fun shiftDateIso(isoDate: String, days: Int): String {
    return try {
        val parsed = LocalDate.parse(isoDate.trim())
        parsed.plus(days, DateTimeUnit.DAY).toString()
    } catch (_: Exception) {
        isoDate
    }
}

/**
 * Returns the 7 days of the week containing [centerDate], starting with Monday.
 */
fun getWeekDaysForDate(centerDate: String): List<WeekDayItem> {
    val dayNames = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    return try {
        val parsed = LocalDate.parse(centerDate.trim())
        val dayIndex = parsed.dayOfWeek.ordinal // Monday is 0, Sunday is 6
        val monday = parsed.plus(-dayIndex, DateTimeUnit.DAY)
        (0..6).map { i ->
            val d = monday.plus(i, DateTimeUnit.DAY)
            val dayNum = if (d.day < 10) "0${d.day}" else "${d.day}"
            WeekDayItem(
                dayName = dayNames[i],
                dayNumber = dayNum,
                dateString = d.toString()
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Formats ISO date to Month Year (e.g. "Kasım 2026").
 */
fun formatMonthYearHeader(isoDate: String): String {
    return try {
        val parsed = LocalDate.parse(isoDate.trim())
        val monthName = TURKISH_MONTHS.getOrElse(parsed.month.ordinal + 1) { "" }
        "$monthName ${parsed.year}"
    } catch (_: Exception) {
        isoDate
    }
}

/**
 * Normalizes user-entered date (GG.AA.YYYY, GG-AA-YYYY, GG/AA/YYYY, or YYYY-MM-DD)
 * to standard ISO YYYY-MM-DD for database storage.
 */
fun normalizeDateToIso(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return getTodayIso()

    // Check GG.AA.YYYY or GG-AA-YYYY or GG/AA/YYYY
    val dmyParts = trimmed.split('.', '-', '/')
    if (dmyParts.size == 3) {
        if (dmyParts[0].length == 4) {
            // Already YYYY-MM-DD
            val y = dmyParts[0]
            val m = dmyParts[1].padStart(2, '0')
            val d = dmyParts[2].padStart(2, '0')
            return "$y-$m-$d"
        } else if (dmyParts[2].length == 4) {
            // GG.AA.YYYY -> convert to YYYY-MM-DD
            val d = dmyParts[0].padStart(2, '0')
            val m = dmyParts[1].padStart(2, '0')
            val y = dmyParts[2]
            return "$y-$m-$d"
        }
    }
    return trimmed
}

/**
 * Formats ISO date (YYYY-MM-DD) to Turkish Gün Ay Yıl format (Örn: "04 Kasım 2026").
 */
fun formatDisplayDateTr(isoDate: String): String {
    return try {
        val parts = isoDate.split("-").map { it.toInt() }
        val day = if (parts[2] < 10) "0${parts[2]}" else "${parts[2]}"
        val month = TURKISH_MONTHS.getOrElse(parts[1]) { "" }
        val year = parts[0]
        "$day $month $year"
    } catch (_: Exception) {
        isoDate
    }
}

/**
 * Formats ISO date (YYYY-MM-DD) to numeric Gün.Ay.Yıl format (Örn: "04.11.2026").
 */
fun formatShortDateTr(isoDate: String): String {
    return try {
        val parts = isoDate.split("-").map { it.toInt() }
        val day = if (parts[2] < 10) "0${parts[2]}" else "${parts[2]}"
        val month = if (parts[1] < 10) "0${parts[1]}" else "${parts[1]}"
        val year = parts[0]
        "$day.$month.$year"
    } catch (_: Exception) {
        isoDate
    }
}

/**
 * Formats ISO date (YYYY-MM-DD) to compact Gün Ay Yıl format (Örn: "04 Kas 2026").
 */
fun formatDayMonthYear(isoDate: String): String {
    return try {
        val parts = isoDate.split("-").map { it.toInt() }
        val day = if (parts[2] < 10) "0${parts[2]}" else "${parts[2]}"
        val month = TURKISH_MONTHS_SHORT.getOrElse(parts[1]) { "" }
        val year = parts[0]
        "$day $month $year"
    } catch (_: Exception) {
        isoDate
    }
}

