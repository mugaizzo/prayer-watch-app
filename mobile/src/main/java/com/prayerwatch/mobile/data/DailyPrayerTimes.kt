package com.prayerwatch.mobile.data

/** Simplified daily prayer times stored locally and sent to the watch. */
data class DailyPrayerTimes(
    val fajr: String,     // "HH:mm"
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val date: String      // human-readable date string from API
)
