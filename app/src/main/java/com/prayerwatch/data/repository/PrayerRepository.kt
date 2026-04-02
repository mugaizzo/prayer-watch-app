package com.prayerwatch.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.prayerwatch.data.api.AladhanApi
import com.prayerwatch.domain.model.DailyPrayerTimes
import com.prayerwatch.domain.model.PrayerTime
import com.prayerwatch.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class PrayerRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()
    private val api: AladhanApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(AladhanApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AladhanApi::class.java)
    }

    /**
     * Fetch today's prayer times from the Aladhan API.
     * Results are cached in SharedPreferences.
     */
    suspend fun getPrayerTimes(
        latitude: Double,
        longitude: Double,
        method: Int = AladhanApi.METHOD_ISNA,
        forceRefresh: Boolean = false
    ): Result<DailyPrayerTimes> = withContext(Dispatchers.IO) {
        val today = TimeUtils.getTodayDateString()

        // Return cached data if available and not stale
        if (!forceRefresh) {
            val cached = getCachedPrayerTimes(today)
            if (cached != null) {
                return@withContext Result.success(cached)
            }
        }

        try {
            val response = api.getPrayerTimes(
                date = today,
                latitude = latitude,
                longitude = longitude,
                method = method
            )

            if (response.code == 200 && response.data != null) {
                val timings = response.data.timings
                val dateStr = response.data.date.readable

                val dailyTimes = DailyPrayerTimes(
                    fajr = PrayerTime(
                        name = "Fajr",
                        time = TimeUtils.cleanTime(timings.fajr),
                        timeInMillis = TimeUtils.timeStringToMillis(timings.fajr)
                    ),
                    dhuhr = PrayerTime(
                        name = "Dhuhr",
                        time = TimeUtils.cleanTime(timings.dhuhr),
                        timeInMillis = TimeUtils.timeStringToMillis(timings.dhuhr)
                    ),
                    asr = PrayerTime(
                        name = "Asr",
                        time = TimeUtils.cleanTime(timings.asr),
                        timeInMillis = TimeUtils.timeStringToMillis(timings.asr)
                    ),
                    maghrib = PrayerTime(
                        name = "Maghrib",
                        time = TimeUtils.cleanTime(timings.maghrib),
                        timeInMillis = TimeUtils.timeStringToMillis(timings.maghrib)
                    ),
                    isha = PrayerTime(
                        name = "Isha",
                        time = TimeUtils.cleanTime(timings.isha),
                        timeInMillis = TimeUtils.timeStringToMillis(timings.isha)
                    ),
                    date = dateStr
                )

                // Cache the result
                cachePrayerTimes(today, dailyTimes)
                Result.success(dailyTimes)
            } else {
                Result.failure(Exception("API error: ${response.status}"))
            }
        } catch (e: Exception) {
            // Try to return cached data even if stale on error
            val stale = getCachedPrayerTimes(null)
            if (stale != null) {
                Result.success(stale)
            } else {
                Result.failure(e)
            }
        }
    }

    private fun getCachedPrayerTimes(dateKey: String?): DailyPrayerTimes? {
        val cachedDate = prefs.getString(KEY_CACHED_DATE, null) ?: return null
        if (dateKey != null && cachedDate != dateKey) return null

        val json = prefs.getString(KEY_CACHED_TIMES, null) ?: return null
        return try {
            gson.fromJson(json, DailyPrayerTimes::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun cachePrayerTimes(dateKey: String, times: DailyPrayerTimes) {
        prefs.edit()
            .putString(KEY_CACHED_DATE, dateKey)
            .putString(KEY_CACHED_TIMES, gson.toJson(times))
            .apply()
    }

    /** Save last known location */
    fun saveLocation(latitude: Double, longitude: Double) {
        prefs.edit()
            .putFloat(KEY_LAT, latitude.toFloat())
            .putFloat(KEY_LNG, longitude.toFloat())
            .apply()
    }

    /** Retrieve cached location, or null if not available */
    fun getCachedLocation(): Pair<Double, Double>? {
        if (!prefs.contains(KEY_LAT) || !prefs.contains(KEY_LNG)) return null
        val lat = prefs.getFloat(KEY_LAT, 0f).toDouble()
        val lng = prefs.getFloat(KEY_LNG, 0f).toDouble()
        return Pair(lat, lng)
    }

    /** Save user's preferred calculation method */
    fun saveMethod(method: Int) {
        prefs.edit().putInt(KEY_METHOD, method).apply()
    }

    /** Retrieve user's preferred calculation method */
    fun getSavedMethod(): Int = prefs.getInt(KEY_METHOD, AladhanApi.METHOD_ISNA)

    companion object {
        private const val PREFS_NAME = "prayer_watch_prefs"
        private const val KEY_CACHED_DATE = "cached_date"
        private const val KEY_CACHED_TIMES = "cached_times"
        private const val KEY_LAT = "latitude"
        private const val KEY_LNG = "longitude"
        private const val KEY_METHOD = "calculation_method"
    }
}
