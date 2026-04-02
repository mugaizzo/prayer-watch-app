package com.prayerwatch.domain.model

data class PrayerTime(
    val name: String,
    val time: String, // "HH:mm" 24-hour format
    val timeInMillis: Long
)

data class DailyPrayerTimes(
    val fajr: PrayerTime,
    val dhuhr: PrayerTime,
    val asr: PrayerTime,
    val maghrib: PrayerTime,
    val isha: PrayerTime,
    val date: String
) {
    fun asList(): List<PrayerTime> = listOf(fajr, dhuhr, asr, maghrib, isha)
}
