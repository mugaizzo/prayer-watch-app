package com.prayerwatch.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prayerwatch.data.repository.PrayerRepository

/**
 * Background worker that refreshes prayer times daily.
 * Scheduled by WorkManager to run once per day.
 */
class PrayerTimeUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = PrayerRepository(applicationContext)

        val location = repository.getCachedLocation() ?: return Result.success()
        val method = repository.getSavedMethod()

        return try {
            val result = repository.getPrayerTimes(
                latitude = location.first,
                longitude = location.second,
                method = method,
                forceRefresh = true
            )
            if (result.isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "prayer_time_update"
    }
}
