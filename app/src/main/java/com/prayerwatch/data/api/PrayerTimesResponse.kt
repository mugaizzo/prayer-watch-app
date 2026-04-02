package com.prayerwatch.data.api

import com.google.gson.annotations.SerializedName

data class PrayerTimesResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: TimingsData?
)

data class TimingsData(
    @SerializedName("timings") val timings: Timings,
    @SerializedName("date") val date: DateInfo,
    @SerializedName("meta") val meta: MetaInfo
)

data class Timings(
    @SerializedName("Fajr") val fajr: String,
    @SerializedName("Sunrise") val sunrise: String,
    @SerializedName("Dhuhr") val dhuhr: String,
    @SerializedName("Asr") val asr: String,
    @SerializedName("Maghrib") val maghrib: String,
    @SerializedName("Isha") val isha: String,
    @SerializedName("Imsak") val imsak: String?,
    @SerializedName("Midnight") val midnight: String?
)

data class DateInfo(
    @SerializedName("readable") val readable: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("gregorian") val gregorian: GregorianDate?,
    @SerializedName("hijri") val hijri: HijriDate?
)

data class GregorianDate(
    @SerializedName("date") val date: String,
    @SerializedName("day") val day: String,
    @SerializedName("month") val month: MonthInfo?,
    @SerializedName("year") val year: String
)

data class HijriDate(
    @SerializedName("date") val date: String,
    @SerializedName("day") val day: String,
    @SerializedName("month") val month: HijriMonthInfo?,
    @SerializedName("year") val year: String
)

data class MonthInfo(
    @SerializedName("number") val number: Int,
    @SerializedName("en") val en: String
)

data class HijriMonthInfo(
    @SerializedName("number") val number: Int,
    @SerializedName("en") val en: String,
    @SerializedName("ar") val ar: String
)

data class MetaInfo(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timezone") val timezone: String,
    @SerializedName("method") val method: CalculationMethod?
)

data class CalculationMethod(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)
