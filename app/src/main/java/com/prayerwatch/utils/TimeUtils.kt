package com.prayerwatch.utils

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    /** Return today's date as "DD-MM-YYYY" string for the API */
    fun getTodayDateString(): String = LocalDate.now().format(dateFormatter)

    /**
     * Convert a time string like "04:32 (BST)" to milliseconds since midnight today.
     * The API sometimes appends timezone names in parentheses.
     */
    fun timeStringToMillis(timeStr: String): Long {
        val cleanedTime = cleanTime(timeStr)
        return try {
            val localTime = LocalTime.parse(cleanedTime, timeFormatter)
            LocalDate.now()
                .atTime(localTime)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }

    /** Strip parenthetical timezone suffixes e.g. "04:32 (BST)" -> "04:32" */
    fun cleanTime(timeStr: String): String = timeStr.substringBefore("(").trim()

    /**
     * Given a list of prayer times in milliseconds (already converted),
     * find which prayer comes next and return its index and remaining ms.
     *
     * @return Pair(nextPrayerIndex, remainingMillis) or null if all prayers are past
     */
    fun getNextPrayerInfo(prayerTimesMillis: List<Long>): Pair<Int, Long>? {
        val now = System.currentTimeMillis()
        for ((index, prayerMs) in prayerTimesMillis.withIndex()) {
            if (prayerMs > now) {
                return Pair(index, prayerMs - now)
            }
        }
        return null
    }

    /**
     * Format remaining milliseconds as "Xh Ym" or "Ym Zs".
     */
    fun formatCountdown(remainingMs: Long): String {
        if (remainingMs <= 0) return "Now"
        val hours = remainingMs / 3_600_000
        val minutes = (remainingMs % 3_600_000) / 60_000
        val seconds = (remainingMs % 60_000) / 1_000

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Convert 24h time string ("HH:mm") to 12h format ("h:mm AM/PM")
     */
    fun to12HourFormat(timeStr: String): String {
        return try {
            LocalTime.parse(timeStr, timeFormatter).format(displayTimeFormatter)
        } catch (e: Exception) {
            timeStr
        }
    }

    /**
     * Get current time as "HH:mm"
     */
    fun getCurrentTimeString(): String = LocalTime.now().format(timeFormatter)
}
