package com.prayerwatch.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.prayerwatch.data.api.AladhanApi
import com.prayerwatch.data.model.ApiSettings
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
     * Fetch today's prayer times from the Aladhan API (timingsByCity).
     * Results are cached in SharedPreferences keyed by date.
     *
     * @param settings   API parameters (city, country, method, etc.)
     * @param forceRefresh  Skip cache and always call the network
     */
    suspend fun getPrayerTimes(
        settings: ApiSettings = getSavedSettings(),
        forceRefresh: Boolean = false
    ): Result<DailyPrayerTimes> = withContext(Dispatchers.IO) {
        val today = TimeUtils.getTodayDateString()

        if (!forceRefresh) {
            val cached = getCachedPrayerTimes(today)
            if (cached != null) return@withContext Result.success(cached)
        }

        try {
            val response = api.getPrayerTimes(
                date = today,
                city = settings.city,
                country = settings.country,
                method = settings.method,
                methodSettings = settings.methodSettingsOrNull(),
                school = settings.school
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

                cachePrayerTimes(today, dailyTimes)
                Result.success(dailyTimes)
            } else {
                Result.failure(Exception("API error: ${response.status}"))
            }
        } catch (e: Exception) {
            // Return stale cache rather than nothing on network failure
            val stale = getCachedPrayerTimes(dateKey = null)
            if (stale != null) Result.success(stale) else Result.failure(e)
        }
    }

    /** Return today's cached prayer times (null if not available or stale). */
    fun getCachedPrayerTimes(dateKey: String? = TimeUtils.getTodayDateString()): DailyPrayerTimes? {
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

    /** Persist all API settings. */
    fun saveSettings(settings: ApiSettings) {
        prefs.edit()
            .putString(KEY_SETTINGS, gson.toJson(settings))
            .apply()
    }

    /** Retrieve saved API settings (defaults if none stored yet). */
    fun getSavedSettings(): ApiSettings {
        val json = prefs.getString(KEY_SETTINGS, null) ?: return ApiSettings()
        return try {
            gson.fromJson(json, ApiSettings::class.java) ?: ApiSettings()
        } catch (e: Exception) {
            ApiSettings()
        }
    }

    /** True if settings have been configured (city is not the default empty placeholder). */
    fun isConfigured(): Boolean = prefs.contains(KEY_SETTINGS)

    companion object {
        const val PREFS_NAME = "prayer_watch_prefs"
        private const val KEY_CACHED_DATE = "cached_date"
        private const val KEY_CACHED_TIMES = "cached_times"
        const val KEY_SETTINGS = "api_settings"
    }
}
