package com.yusufteker.healthtrack.core.utils

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
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

private val ENGLISH_MONTHS = listOf(
    "", "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

private val ENGLISH_MONTHS_SHORT = listOf(
    "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
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
    } catch (_: Throwable) {
        try {
            Clock.System.todayIn(TimeZone.UTC).toString()
        } catch (_: Throwable) {
            val epochDays = (currentTimeMillis() / 86_400_000L).toInt()
            LocalDate.fromEpochDays(epochDays).toString()
        }
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
fun getWeekDaysForDate(
    centerDate: String,
    language: AppLanguage = LocalizationManager.currentLanguage
): List<WeekDayItem> {
    val dayNames = if (language == AppLanguage.TR) {
        listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    } else {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }
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
 * Formats ISO date to Month Year (e.g. "Kasım 2026" or "November 2026").
 */
fun formatMonthYearHeader(
    isoDate: String,
    language: AppLanguage = LocalizationManager.currentLanguage
): String {
    return try {
        val parsed = LocalDate.parse(isoDate.trim())
        val months = if (language == AppLanguage.TR) TURKISH_MONTHS else ENGLISH_MONTHS
        val monthName = months.getOrElse(parsed.month.ordinal + 1) { "" }
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

    val dmyParts = trimmed.split('.', '-', '/')
    if (dmyParts.size == 3) {
        if (dmyParts[0].length == 4) {
            val y = dmyParts[0]
            val m = dmyParts[1].padStart(2, '0')
            val d = dmyParts[2].padStart(2, '0')
            return "$y-$m-$d"
        } else if (dmyParts[2].length == 4) {
            val d = dmyParts[0].padStart(2, '0')
            val m = dmyParts[1].padStart(2, '0')
            val y = dmyParts[2]
            return "$y-$m-$d"
        }
    }
    return trimmed
}

/**
 * Formats ISO date (YYYY-MM-DD) to localized Day Month Year format (e.g. "04 Kasım 2026" / "04 November 2026").
 */
fun formatDisplayDateTr(
    isoDate: String,
    language: AppLanguage = LocalizationManager.currentLanguage
): String {
    return try {
        val parts = isoDate.split("-").map { it.toInt() }
        val day = if (parts[2] < 10) "0${parts[2]}" else "${parts[2]}"
        val months = if (language == AppLanguage.TR) TURKISH_MONTHS else ENGLISH_MONTHS
        val month = months.getOrElse(parts[1]) { "" }
        val year = parts[0]
        "$day $month $year"
    } catch (_: Exception) {
        isoDate
    }
}

/**
 * Formats ISO date (YYYY-MM-DD) to numeric DD.MM.YYYY (TR) or MM/DD/YYYY (EN).
 */
fun formatShortDateTr(
    isoDate: String,
    language: AppLanguage = LocalizationManager.currentLanguage
): String {
    return try {
        val parts = isoDate.split("-").map { it.toInt() }
        val day = if (parts[2] < 10) "0${parts[2]}" else "${parts[2]}"
        val month = if (parts[1] < 10) "0${parts[1]}" else "${parts[1]}"
        val year = parts[0]
        if (language == AppLanguage.TR) "$day.$month.$year" else "$month/$day/$year"
    } catch (_: Exception) {
        isoDate
    }
}

/**
 * Formats ISO date (YYYY-MM-DD) to compact Day Month Year format (e.g. "04 Kas 2026" / "04 Nov 2026").
 */
fun formatDayMonthYear(
    isoDate: String,
    language: AppLanguage = LocalizationManager.currentLanguage
): String {
    return try {
        val parts = isoDate.split("-").map { it.toInt() }
        val day = if (parts[2] < 10) "0${parts[2]}" else "${parts[2]}"
        val monthsShort = if (language == AppLanguage.TR) TURKISH_MONTHS_SHORT else ENGLISH_MONTHS_SHORT
        val month = monthsShort.getOrElse(parts[1]) { "" }
        val year = parts[0]
        "$day $month $year"
    } catch (_: Exception) {
        isoDate
    }
}

/**
 * Returns the Monday and Sunday ISO dates (YYYY-MM-DD) of the week containing [referenceDateIso].
 */
fun getWeekBoundsIso(referenceDateIso: String): Pair<String, String> {
    return try {
        val parsed = LocalDate.parse(referenceDateIso.trim())
        val dayIndex = parsed.dayOfWeek.ordinal // Monday = 0, Sunday = 6
        val monday = parsed.plus(-dayIndex, DateTimeUnit.DAY)
        val sunday = monday.plus(6, DateTimeUnit.DAY)
        monday.toString() to sunday.toString()
    } catch (_: Exception) {
        referenceDateIso to referenceDateIso
    }
}

/**
 * Formats a Monday..Sunday ISO date range into a compact localized label (e.g. "02 - 08 Kas 2026" / "02 - 08 Nov 2026").
 */
fun formatWeekRangeTr(
    mondayIso: String,
    sundayIso: String,
    language: AppLanguage = LocalizationManager.currentLanguage
): String {
    return try {
        val mParts = mondayIso.split("-").map { it.toInt() }
        val sParts = sundayIso.split("-").map { it.toInt() }
        val mDay = if (mParts[2] < 10) "0${mParts[2]}" else "${mParts[2]}"
        val sDay = if (sParts[2] < 10) "0${sParts[2]}" else "${sParts[2]}"
        val monthsShort = if (language == AppLanguage.TR) TURKISH_MONTHS_SHORT else ENGLISH_MONTHS_SHORT
        val mMonth = monthsShort.getOrElse(mParts[1]) { "" }
        val sMonth = monthsShort.getOrElse(sParts[1]) { "" }
        val year = sParts[0]
        if (mParts[1] == sParts[1]) {
            "$mDay - $sDay $sMonth $year"
        } else {
            "$mDay $mMonth - $sDay $sMonth $year"
        }
    } catch (_: Exception) {
        "$mondayIso - $sundayIso"
    }
}

/**
 * Returns difference in days between [earlierDateIso] and [laterDateIso] (later - earlier).
 */
fun calculateDaysBetween(earlierDateIso: String, laterDateIso: String): Int {
    return try {
        val d1 = LocalDate.parse(earlierDateIso.trim())
        val d2 = LocalDate.parse(laterDateIso.trim())
        (d2.toEpochDays() - d1.toEpochDays()).toInt()
    } catch (_: Exception) {
        0
    }
}
