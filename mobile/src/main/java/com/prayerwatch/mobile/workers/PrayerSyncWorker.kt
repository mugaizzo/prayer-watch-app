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
     * Build a JSON string matching the watch module's DailyPrayerTimes + PrayerTime model
     * so the WearListenerService can deserialize it directly.
     */
    private fun buildWatchTimesJson(times: DailyPrayerTimes): String {
        return """
        {
          "fajr":    {"name":"Fajr",    "time":"${times.fajr}",    "timeInMillis": 0},
          "dhuhr":   {"name":"Dhuhr",   "time":"${times.dhuhr}",   "timeInMillis": 0},
          "asr":     {"name":"Asr",     "time":"${times.asr}",     "timeInMillis": 0},
          "maghrib": {"name":"Maghrib", "time":"${times.maghrib}", "timeInMillis": 0},
          "isha":    {"name":"Isha",    "time":"${times.isha}",    "timeInMillis": 0},
          "date":    "${times.date}"
        }
        """.trimIndent()
    }

    companion object {
        const val WORK_NAME = "prayer_sync"
        const val PATH_SETTINGS = "/prayer_settings"
        const val PATH_PRAYER_TIMES = "/prayer_times"
        const val KEY_JSON = "json"
        private const val TAG = "PrayerSyncWorker"
    }
}
