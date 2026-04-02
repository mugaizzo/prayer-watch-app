package com.prayerwatch.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {

    private val dateFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("dd-MM-yyyy", Locale.US)
    }
    private val timeFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("HH:mm", Locale.US)
    }

    /** Return today's date as "DD-MM-YYYY" string for the API */
    fun getTodayDateString(): String = dateFormat.get()!!.format(Date())

    /**
     * Convert a time string like "04:32 (BST)" to milliseconds since midnight today.
     * The API sometimes appends timezone names in parentheses.
     */
    fun timeStringToMillis(timeStr: String): Long {
        val cleanedTime = cleanTime(timeStr)
        val parts = cleanedTime.split(":")
        if (parts.size < 2) return 0L

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, parts[0].trim().toIntOrNull() ?: 0)
        cal.set(Calendar.MINUTE, parts[1].trim().toIntOrNull() ?: 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Strip parenthetical timezone suffixes e.g. "04:32 (BST)" -> "04:32" */
    fun cleanTime(timeStr: String): String {
        return timeStr.substringBefore("(").trim()
    }

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
            val parsed = timeFormat.get()!!.parse(timeStr) ?: return timeStr
            val displayFormat = SimpleDateFormat("h:mm a", Locale.US)
            displayFormat.format(parsed)
        } catch (e: Exception) {
            timeStr
        }
    }

    /**
     * Get current time as "HH:mm"
     */
    fun getCurrentTimeString(): String = timeFormat.get()!!.format(Date())
}
