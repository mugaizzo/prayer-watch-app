package com.prayerwatch.mobile.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MobilePrayerRepository(private val context: Context) {

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
     * Fetch today's prayer times. Uses a daily cache keyed by date.
     *
     * @param settings   API parameters
     * @param forceRefresh  Skip cache
     */
    suspend fun getPrayerTimes(
        settings: ApiSettings,
        forceRefresh: Boolean = false
    ): Result<DailyPrayerTimes> = withContext(Dispatchers.IO) {
        val today = todayDateString()

        if (!forceRefresh) {
            val cached = getCachedTimes(today)
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
                val t = response.data.timings
                val daily = DailyPrayerTimes(
                    fajr = cleanTime(t.fajr),
                    dhuhr = cleanTime(t.dhuhr),
                    asr = cleanTime(t.asr),
                    maghrib = cleanTime(t.maghrib),
                    isha = cleanTime(t.isha),
                    date = response.data.date.readable
                )
                cacheTimes(today, daily)
                Result.success(daily)
            } else {
                Result.failure(Exception("API error: ${response.status}"))
            }
        } catch (e: Exception) {
            val stale = getCachedTimes(null)
            if (stale != null) Result.success(stale) else Result.failure(e)
        }
    }

    private fun getCachedTimes(dateKey: String?): DailyPrayerTimes? {
        val cachedDate = prefs.getString(KEY_DATE, null) ?: return null
        if (dateKey != null && cachedDate != dateKey) return null
        val json = prefs.getString(KEY_TIMES, null) ?: return null
        return try { gson.fromJson(json, DailyPrayerTimes::class.java) } catch (e: Exception) { null }
    }

    private fun cacheTimes(dateKey: String, times: DailyPrayerTimes) {
        prefs.edit()
            .putString(KEY_DATE, dateKey)
            .putString(KEY_TIMES, gson.toJson(times))
            .apply()
    }

    fun saveSettings(settings: ApiSettings) {
        prefs.edit().putString(KEY_SETTINGS, gson.toJson(settings)).apply()
    }

    fun getSettings(): ApiSettings {
        val json = prefs.getString(KEY_SETTINGS, null) ?: return ApiSettings()
        return try { gson.fromJson(json, ApiSettings::class.java) ?: ApiSettings() } catch (e: Exception) { ApiSettings() }
    }

    fun getLastSyncTime(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    fun setLastSyncTime(time: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_SYNC, time).apply()
    }

    private fun todayDateString(): String =
        SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())

    private fun cleanTime(raw: String): String = raw.substringBefore("(").trim()

    companion object {
        const val PREFS_NAME = "prayer_mobile_prefs"
        private const val KEY_DATE = "cached_date"
        private const val KEY_TIMES = "cached_times"
        const val KEY_SETTINGS = "api_settings"
        private const val KEY_LAST_SYNC = "last_sync"
    }
}
