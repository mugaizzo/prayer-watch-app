package com.prayerwatch.mobile.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.prayerwatch.mobile.data.DailyPrayerTimes
import com.prayerwatch.mobile.data.MobilePrayerRepository
import kotlinx.coroutines.tasks.await

/**
 * Runs once per day:
 *  1. Fetches prayer times from Aladhan API.
 *  2. Stores them locally.
 *  3. Sends settings + prayer times to the paired watch via Wearable Data Layer.
 */
class PrayerSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val gson = Gson()

    override suspend fun doWork(): Result {
        val repository = MobilePrayerRepository(applicationContext)
        val settings = repository.getSettings()

        return try {
            val result = repository.getPrayerTimes(settings, forceRefresh = true)
            result.fold(
                onSuccess = { times ->
                    sendToWatch(repository, times)
                    repository.setLastSyncTime()
                    Log.d(TAG, "Prayer times synced to watch for ${times.date}")
                    Result.success()
                },
                onFailure = { e ->
                    Log.e(TAG, "Fetch failed: ${e.message}")
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed", e)
            Result.retry()
        }
    }

    private suspend fun sendToWatch(
        repository: MobilePrayerRepository,
        times: DailyPrayerTimes
    ) {
        val dataClient = Wearable.getDataClient(applicationContext)

        // Send settings
        val settingsRequest = PutDataMapRequest.create(PATH_SETTINGS).apply {
            dataMap.putString(KEY_JSON, gson.toJson(repository.getSettings()))
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(settingsRequest).await()

        // Send prayer times (using a watch-compatible JSON that matches the wear module's model)
        val timesRequest = PutDataMapRequest.create(PATH_PRAYER_TIMES).apply {
            dataMap.putString(KEY_JSON, buildWatchTimesJson(times))
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(timesRequest).await()
    }

    /**
     * Build a JSON string matching the watch module's DailyPrayerTimes + PrayerTime model.
     * timeInMillis is computed from the HH:mm time strings so the watch countdown works correctly.
     */
    private fun buildWatchTimesJson(times: DailyPrayerTimes): String {
        fun timeToMillis(timeStr: String): Long {
            return try {
                val parts = timeStr.split(":")
                if (parts.size < 2) return 0L
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, parts[0].trim().toInt())
                cal.set(java.util.Calendar.MINUTE, parts[1].trim().toInt())
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            } catch (e: Exception) { 0L }
        }

        return gson.toJson(mapOf(
            "fajr"    to mapOf("name" to "Fajr",    "time" to times.fajr,    "timeInMillis" to timeToMillis(times.fajr)),
            "dhuhr"   to mapOf("name" to "Dhuhr",   "time" to times.dhuhr,   "timeInMillis" to timeToMillis(times.dhuhr)),
            "asr"     to mapOf("name" to "Asr",      "time" to times.asr,     "timeInMillis" to timeToMillis(times.asr)),
            "maghrib" to mapOf("name" to "Maghrib",  "time" to times.maghrib, "timeInMillis" to timeToMillis(times.maghrib)),
            "isha"    to mapOf("name" to "Isha",     "time" to times.isha,    "timeInMillis" to timeToMillis(times.isha)),
            "date"    to times.date
        ))
    }

    companion object {
        const val WORK_NAME = "prayer_sync"
        const val PATH_SETTINGS = "/prayer_settings"
        const val PATH_PRAYER_TIMES = "/prayer_times"
        const val KEY_JSON = "json"
        private const val TAG = "PrayerSyncWorker"
    }
}
